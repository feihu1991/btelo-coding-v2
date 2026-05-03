package com.btelo.coding.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btelo.coding.data.local.DataStoreManager
import com.btelo.coding.data.remote.websocket.OutputType
import com.btelo.coding.data.remote.websocket.factory.ConnectionState
import com.btelo.coding.domain.model.Message
import com.btelo.coding.domain.model.MessageMetadata
import com.btelo.coding.domain.model.MessageType
import com.btelo.coding.domain.model.OutputType as DomainOutputType
import com.btelo.coding.domain.repository.AuthRepository
import com.btelo.coding.domain.repository.MessageRepository
import com.btelo.coding.domain.repository.SessionRepository
import com.btelo.coding.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThinkingMessage(
    val type: ThinkingMessageType,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ThinkingMessageType {
    THINKING,
    TOOL_CALL,
    FILE_OP,
    ERROR,
    SYSTEM
}

data class ThinkingSession(
    val isActive: Boolean = false,
    val isCompleted: Boolean = false,
    val messages: List<ThinkingMessage> = emptyList(),
    val currentMessageType: ThinkingMessageType? = null,
    val currentMessage: String = ""
)

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val reconnectAttempts: Int = 0,
    val lastConnectedTime: Long? = null,
    val errorMessage: String? = null,
    val error: String? = null,
    val showConnectionDetails: Boolean = false,
    val streamingContent: String = "",
    val isStreaming: Boolean = false,
    val sessionName: String = "Claude Code",
    val selectedImageUri: String = "",
    val thinkingSession: ThinkingSession = ThinkingSession(),
    val structuredOutputBuffer: StructuredOutputBuffer = StructuredOutputBuffer()
)

data class StructuredOutputBuffer(
    val messageId: String = "",
    val parts: List<StructuredPart> = emptyList(),
    val isComplete: Boolean = false
)

data class StructuredPart(
    val outputType: OutputType,
    val content: String,
    val metadata: MessageMetadata? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var sessionId: String = ""
    private val coroutineJobs = mutableListOf<Job>()

    fun setSessionId(id: String) {
        val effectiveId = if (id.isBlank()) {
            val saved = dataStoreManager.getSessionIdSync()
            Logger.w("ChatVM", "setSessionId called with blank id, fallback from store: $saved")
            saved ?: return
        } else {
            id
        }

        if (effectiveId.isBlank()) return
        if (sessionId == effectiveId && coroutineJobs.isNotEmpty()) return
        sessionId = effectiveId

        val job = viewModelScope.launch {
            sessionRepository.createSessionWithId(effectiveId, "Claude Code", "claude")

            val serverAddress = authRepository.getServerAddress().firstOrNull().orEmpty()
            val wsToken = dataStoreManager.getWsTokenSync()
            val authToken = dataStoreManager.getTokenSync()
            val token = wsToken ?: authToken ?: ""
            if (serverAddress.isNotBlank() && token.isNotBlank()) {
                messageRepository.connect(serverAddress, token, effectiveId)
            }
        }
        coroutineJobs.add(job)
        loadMessages()
        observeConnectionState()
        observeSession()
        observeOutput()
        observeStructuredOutput()
    }

    private fun loadMessages() {
        val job = viewModelScope.launch {
            messageRepository.getMessages(sessionId).collect { messages ->
                val currentState = _uiState.value
                val latestMessage = messages.lastOrNull()
                val shouldClearPending = latestMessage != null &&
                    !latestMessage.isFromUser &&
                    (currentState.isLoading || currentState.isStreaming || currentState.thinkingSession.isActive)

                _uiState.value = currentState.copy(
                    messages = messages,
                    isLoading = if (shouldClearPending) false else currentState.isLoading,
                    isStreaming = if (shouldClearPending) false else currentState.isStreaming,
                    streamingContent = if (shouldClearPending) "" else currentState.streamingContent,
                    thinkingSession = if (shouldClearPending && currentState.thinkingSession.isActive) {
                        currentState.thinkingSession.copy(isActive = false, isCompleted = true, currentMessage = "Completed")
                    } else if (shouldClearPending) {
                        ThinkingSession()
                    } else {
                        currentState.thinkingSession
                    }
                )
            }
        }
        coroutineJobs.add(job)
    }

    private fun observeOutput() {
        val job = viewModelScope.launch {
            messageRepository.observeOutput(sessionId).collect { message ->
                val current = _uiState.value.streamingContent
                val newContent = if (current.isEmpty() || current == "...") {
                    message.content
                } else {
                    current + message.content
                }
                _uiState.value = _uiState.value.copy(
                    streamingContent = newContent,
                    isStreaming = true
                )

                delay(2000)
                if (_uiState.value.isStreaming && _uiState.value.streamingContent == newContent) {
                    _uiState.value = _uiState.value.copy(
                        streamingContent = "",
                        isStreaming = false
                    )
                }
            }
        }
        coroutineJobs.add(job)
    }

    private fun observeStructuredOutput() {
        val job = viewModelScope.launch {
            messageRepository.observeStructuredOutput(sessionId).collect { structuredMessage ->
                processStructuredOutput(structuredMessage)
            }
        }
        coroutineJobs.add(job)
    }

    private fun processStructuredOutput(structuredMsg: Message) {
        val currentSession = _uiState.value.thinkingSession
        val outputType = structuredMsg.outputType

        if (outputType == DomainOutputType.THINKING && !currentSession.isActive) {
            val thinkingMsg = ThinkingMessage(
                type = ThinkingMessageType.THINKING,
                content = structuredMsg.content
            )
            _uiState.value = _uiState.value.copy(
                thinkingSession = ThinkingSession(
                    isActive = true,
                    messages = listOf(thinkingMsg),
                    currentMessageType = ThinkingMessageType.THINKING,
                    currentMessage = "Thinking"
                ),
                isStreaming = true,
                streamingContent = "..."
            )
            return
        }

        if (currentSession.isActive && outputType != DomainOutputType.CLAUDE_RESPONSE) {
            val thinkingType = when (outputType) {
                DomainOutputType.THINKING -> ThinkingMessageType.THINKING
                DomainOutputType.TOOL_CALL -> ThinkingMessageType.TOOL_CALL
                DomainOutputType.FILE_OP -> ThinkingMessageType.FILE_OP
                DomainOutputType.ERROR -> ThinkingMessageType.ERROR
                DomainOutputType.SYSTEM -> ThinkingMessageType.SYSTEM
                else -> null
            }

            if (thinkingType != null) {
                val newMsg = ThinkingMessage(
                    type = thinkingType,
                    content = structuredMsg.content
                )
                val currentMessage = when (thinkingType) {
                    ThinkingMessageType.TOOL_CALL -> "Running ${structuredMsg.metadata?.toolName ?: "tool"}"
                    ThinkingMessageType.FILE_OP -> "File ${structuredMsg.metadata?.fileOpType ?: "operation"}"
                    ThinkingMessageType.THINKING -> "Thinking"
                    ThinkingMessageType.ERROR -> "Error"
                    ThinkingMessageType.SYSTEM -> "System"
                }
                _uiState.value = _uiState.value.copy(
                    thinkingSession = currentSession.copy(
                        messages = currentSession.messages + newMsg,
                        currentMessageType = thinkingType,
                        currentMessage = currentMessage
                    )
                )
            }
            return
        }

        if (currentSession.isActive && outputType == DomainOutputType.CLAUDE_RESPONSE) {
            if (currentSession.messages.isNotEmpty()) {
                val thinkingContent = currentSession.messages
                    .filter { it.type == ThinkingMessageType.THINKING }
                    .joinToString("\n") { it.content }
                    .ifEmpty { "Thinking" }

                val toolNames = currentSession.messages
                    .filter { it.type == ThinkingMessageType.TOOL_CALL || it.type == ThinkingMessageType.FILE_OP }
                    .map { it.content }

                val thinkingMsg = Message(
                    id = "think-${System.currentTimeMillis()}",
                    sessionId = sessionId,
                    content = thinkingContent,
                    type = MessageType.THINKING,
                    timestamp = System.currentTimeMillis() - 1000,
                    isFromUser = false,
                    outputType = DomainOutputType.THINKING,
                    metadata = MessageMetadata(
                        toolNames = toolNames,
                        isCollapsed = true
                    )
                )
                viewModelScope.launch { messageRepository.saveMessage(thinkingMsg) }
            }

            // Keep thinking session visible but mark as completed (lightbulb stops)
            _uiState.value = _uiState.value.copy(
                thinkingSession = currentSession.copy(
                    isActive = false,
                    isCompleted = true,
                    currentMessage = "Completed"
                ),
                isStreaming = true,
                streamingContent = structuredMsg.content
            )
            return
        }

        val currentBuffer = _uiState.value.structuredOutputBuffer
        val newPart = StructuredPart(
            outputType = structuredMsg.outputType?.let {
                when (it) {
                    DomainOutputType.CLAUDE_RESPONSE -> OutputType.CLAUDE_RESPONSE
                    DomainOutputType.TOOL_CALL -> OutputType.TOOL_CALL
                    DomainOutputType.FILE_OP -> OutputType.FILE_OP
                    DomainOutputType.THINKING -> OutputType.THINKING
                    DomainOutputType.ERROR -> OutputType.ERROR
                    DomainOutputType.SYSTEM -> OutputType.SYSTEM
                }
            } ?: OutputType.CLAUDE_RESPONSE,
            content = structuredMsg.content,
            metadata = structuredMsg.metadata
        )

        val isComplete = newPart.outputType == OutputType.CLAUDE_RESPONSE &&
            currentBuffer.messageId.isNotEmpty()

        if (isComplete) {
            val finalMessage = buildStructuredMessage(
                currentBuffer.copy(
                    parts = currentBuffer.parts + newPart,
                    isComplete = true
                )
            )

            viewModelScope.launch {
                messageRepository.saveMessage(finalMessage)
            }

            _uiState.value = _uiState.value.copy(
                structuredOutputBuffer = StructuredOutputBuffer(),
                streamingContent = "",
                isStreaming = false
            )
        } else {
            val newMessageId = currentBuffer.messageId.ifEmpty { "struct-${System.currentTimeMillis()}" }

            _uiState.value = _uiState.value.copy(
                structuredOutputBuffer = currentBuffer.copy(
                    messageId = newMessageId,
                    parts = currentBuffer.parts + newPart,
                    isComplete = isComplete
                ),
                streamingContent = if (newPart.outputType == OutputType.THINKING) "..." else structuredMsg.content,
                isStreaming = true
            )
        }
    }

    private fun buildStructuredMessage(buffer: StructuredOutputBuffer): Message {
        val parts = buffer.parts
        val primaryType = parts.firstOrNull()?.outputType ?: OutputType.CLAUDE_RESPONSE
        val content = parts.joinToString("") { it.content }
        val toolCallPart = parts.find { it.outputType == OutputType.TOOL_CALL }
        val metadata = toolCallPart?.metadata ?: MessageMetadata()
        val thinkingPart = parts.find { it.outputType == OutputType.THINKING }

        val domainOutputType = when (primaryType) {
            OutputType.CLAUDE_RESPONSE -> DomainOutputType.CLAUDE_RESPONSE
            OutputType.TOOL_CALL -> DomainOutputType.TOOL_CALL
            OutputType.FILE_OP -> DomainOutputType.FILE_OP
            OutputType.THINKING -> DomainOutputType.THINKING
            OutputType.ERROR -> DomainOutputType.ERROR
            OutputType.SYSTEM -> DomainOutputType.SYSTEM
        }

        return Message(
            id = buffer.messageId,
            sessionId = sessionId,
            content = content,
            type = MessageType.OUTPUT,
            timestamp = System.currentTimeMillis(),
            isFromUser = false,
            outputType = domainOutputType,
            metadata = metadata,
            thinkingContent = thinkingPart?.content
        )
    }

    private fun observeConnectionState() {
        val job = viewModelScope.launch {
            messageRepository.connectionState.collect { state ->
                val isConnected = state is ConnectionState.Connected
                val reconnectAttempts = when (state) {
                    is ConnectionState.Reconnecting -> state.attempt
                    else -> 0
                }
                val errorMessage = when (state) {
                    is ConnectionState.Error -> state.message
                    else -> null
                }
                val lastConnectedTime = if (state is ConnectionState.Connected) {
                    System.currentTimeMillis()
                } else {
                    _uiState.value.lastConnectedTime
                }

                _uiState.value = _uiState.value.copy(
                    connectionState = state,
                    isConnected = isConnected,
                    reconnectAttempts = reconnectAttempts,
                    errorMessage = errorMessage,
                    lastConnectedTime = lastConnectedTime
                )

                if (sessionId.isNotBlank()) {
                    sessionRepository.updateSessionConnection(sessionId, isConnected)
                }
            }
        }
        coroutineJobs.add(job)
    }

    private fun observeSession() {
        val job = viewModelScope.launch {
            sessionRepository.getSession(sessionId).collect { session ->
                session?.let {
                    _uiState.value = _uiState.value.copy(sessionName = it.name)
                }
            }
        }
        coroutineJobs.add(job)
    }

    fun onImageSelected(uri: String) {
        _uiState.value = _uiState.value.copy(selectedImageUri = uri)
    }

    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage() {
        val content = _uiState.value.inputText.trim()
        if (content.isBlank()) return

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            inputText = "",
            thinkingSession = ThinkingSession(
                isActive = true,
                isCompleted = false,
                messages = emptyList(),
                currentMessageType = null,
                currentMessage = "Waiting for Claude"
            ),
            isStreaming = true,
            streamingContent = "...",
            structuredOutputBuffer = StructuredOutputBuffer()
        )

        viewModelScope.launch {
            messageRepository.sendMessage(sessionId, content)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isStreaming = false,
                        streamingContent = "",
                        thinkingSession = ThinkingSession(),
                        error = exception.message
                    )
                }
        }
    }

    fun onStreamComplete() {
        val buffer = _uiState.value.structuredOutputBuffer
        if (buffer.parts.isNotEmpty() && !buffer.isComplete) {
            val finalMessage = buildStructuredMessage(buffer.copy(isComplete = true))
            viewModelScope.launch {
                messageRepository.saveMessage(finalMessage)
            }
        }

        val currentSession = _uiState.value.thinkingSession
        _uiState.value = _uiState.value.copy(
            streamingContent = "",
            isStreaming = false,
            structuredOutputBuffer = StructuredOutputBuffer(),
            thinkingSession = if (currentSession.isActive) {
                currentSession.copy(isActive = false, isCompleted = true, currentMessage = "Completed")
            } else {
                ThinkingSession()
            }
        )
    }

    fun toggleConnectionDetails() {
        _uiState.value = _uiState.value.copy(
            showConnectionDetails = !_uiState.value.showConnectionDetails
        )
    }

    fun dismissConnectionDetails() {
        _uiState.value = _uiState.value.copy(showConnectionDetails = false)
    }

    fun disconnect() {
        viewModelScope.launch {
            messageRepository.disconnect(sessionId)
            dataStoreManager.clearConnection()
            coroutineJobs.forEach { job ->
                if (job.isActive) job.cancel()
            }
            coroutineJobs.clear()
        }
    }

    override fun onCleared() {
        super.onCleared()
        coroutineJobs.forEach { job ->
            if (job.isActive) job.cancel()
        }
        coroutineJobs.clear()
    }
}
