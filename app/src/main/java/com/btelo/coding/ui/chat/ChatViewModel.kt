package com.btelo.coding.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btelo.coding.data.remote.websocket.factory.ConnectionState
import com.btelo.coding.data.remote.websocket.OutputType
import com.btelo.coding.domain.model.Message
import com.btelo.coding.domain.model.MessageMetadata
import com.btelo.coding.domain.model.OutputType as DomainOutputType
import com.btelo.coding.domain.repository.AuthRepository
import com.btelo.coding.domain.repository.MessageRepository
import com.btelo.coding.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    val sessionName: String = "Claude",
    
    // BTELO Coding v2: Structured output state
    val structuredOutputBuffer: StructuredOutputBuffer = StructuredOutputBuffer()
)

/**
 * Buffer for accumulating structured output messages
 * Groups related structured messages together
 */
data class StructuredOutputBuffer(
    val messageId: String = "",
    val parts: List<StructuredPart> = emptyList(),
    val isComplete: Boolean = false
)

/**
 * A single part of structured output
 */
data class StructuredPart(
    val outputType: OutputType,
    val content: String,
    val metadata: MessageMetadata? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var sessionId: String = ""

    private val coroutineJobs = mutableListOf<Job>()

    fun setSessionId(id: String) {
        sessionId = id
        val job = viewModelScope.launch {
            val serverAddress = authRepository.getServerAddress().firstOrNull() ?: ""
            // Try ws_token first (new flow), fallback to auth_token (old flow)
            val wsToken = authRepository.getWsTokenSync()
            val authToken = authRepository.getTokenSync()
            val token = wsToken ?: authToken ?: ""
            if (serverAddress.isNotBlank() && token.isNotBlank()) {
                messageRepository.connect(serverAddress, token, sessionId)
            }
        }
        coroutineJobs.add(job)
        loadMessages()
        observeOutput()
        observeStructuredOutput()
        observeConnectionState()
        observeSession()
    }

    private fun loadMessages() {
        val job = viewModelScope.launch {
            messageRepository.getMessages(sessionId).collect { messages ->
                _uiState.value = _uiState.value.copy(messages = messages)
            }
        }
        coroutineJobs.add(job)
    }

    private fun observeOutput() {
        val job = viewModelScope.launch {
            messageRepository.observeOutput(sessionId).collect { message ->
                // Accumulate streaming content instead of replacing
                val current = _uiState.value.streamingContent
                val newContent = if (current.isEmpty()) {
                    message.content
                } else {
                    current + message.content
                }
                _uiState.value = _uiState.value.copy(
                    streamingContent = newContent,
                    isStreaming = true
                )

                // Auto-clear streaming after 2 seconds of inactivity
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
    
    /**
     * Observe structured output messages (BTELO Coding v2)
     * These are parsed Claude Code stream-json outputs with type classification
     */
    private fun observeStructuredOutput() {
        val job = viewModelScope.launch {
            messageRepository.observeStructuredOutput(sessionId).collect { structuredMessage ->
                processStructuredOutput(structuredMessage)
            }
        }
        coroutineJobs.add(job)
    }
    
    /**
     * Process a structured output message
     * Groups related parts into a single message for display
     */
    private fun processStructuredOutput(structuredMsg: Message) {
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
                    else -> OutputType.CLAUDE_RESPONSE
                }
            } ?: OutputType.CLAUDE_RESPONSE,
            content = structuredMsg.content,
            metadata = structuredMsg.metadata
        )
        
        // Check if this completes a message (claude_response after tool_call)
        val isComplete = newPart.outputType == OutputType.CLAUDE_RESPONSE && 
                         !currentBuffer.messageId.isEmpty()
        
        if (isComplete) {
            // Flush buffer to messages
            val finalMessage = buildStructuredMessage(currentBuffer.copy(
                parts = currentBuffer.parts + newPart,
                isComplete = true
            ))
            
            // Save to repository
            viewModelScope.launch {
                messageRepository.saveMessage(finalMessage)
            }
            
            // Clear buffer
            _uiState.value = _uiState.value.copy(
                structuredOutputBuffer = StructuredOutputBuffer(),
                streamingContent = "",
                isStreaming = false
            )
        } else {
            // Add to buffer
            val newMessageId = if (currentBuffer.messageId.isEmpty()) {
                "struct-${System.currentTimeMillis()}"
            } else {
                currentBuffer.messageId
            }
            
            _uiState.value = _uiState.value.copy(
                structuredOutputBuffer = currentBuffer.copy(
                    messageId = newMessageId,
                    parts = currentBuffer.parts + newPart,
                    isComplete = isComplete
                ),
                streamingContent = structuredMsg.content,
                isStreaming = true
            )
        }
    }
    
    /**
     * Build a single Message from structured parts
     */
    private fun buildStructuredMessage(buffer: StructuredOutputBuffer): Message {
        val parts = buffer.parts
        
        // Determine primary output type
        val primaryType = parts.firstOrNull()?.outputType ?: OutputType.CLAUDE_RESPONSE
        
        // Concatenate content
        val content = parts.joinToString("") { it.content }
        
        // Get metadata from relevant parts
        val toolCallPart = parts.find { it.outputType == OutputType.TOOL_CALL }
        val metadata = toolCallPart?.metadata ?: MessageMetadata()
        
        // Extract thinking content
        val thinkingPart = parts.find { it.outputType == OutputType.THINKING }
        
        // Determine domain output type
        val domainOutputType = when (primaryType) {
            OutputType.CLAUDE_RESPONSE -> DomainOutputType.CLAUDE_RESPONSE
            OutputType.TOOL_CALL -> DomainOutputType.TOOL_CALL
            OutputType.FILE_OP -> DomainOutputType.FILE_OP
            OutputType.THINKING -> DomainOutputType.THINKING
            OutputType.ERROR -> DomainOutputType.ERROR
            OutputType.SYSTEM -> DomainOutputType.SYSTEM
            else -> DomainOutputType.CLAUDE_RESPONSE
        }
        
        return Message(
            id = buffer.messageId,
            sessionId = sessionId,
            content = content,
            type = com.btelo.coding.domain.model.MessageType.OUTPUT,
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

                // Sync connection state to Room DB so SessionListScreen can display it
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

    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage() {
        val content = _uiState.value.inputText.trim()
        if (content.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                inputText = "",
                streamingContent = "",
                isStreaming = true,
                structuredOutputBuffer = StructuredOutputBuffer() // Clear buffer
            )

            messageRepository.sendMessage(sessionId, content)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isStreaming = false,
                        error = exception.message
                    )
                }
        }
    }

    /**
     * Called when streaming output is complete (e.g., stream end signal or timeout).
     * Flushes accumulated streaming content into the message list.
     */
    fun onStreamComplete() {
        // Flush any remaining structured output buffer
        val buffer = _uiState.value.structuredOutputBuffer
        if (buffer.parts.isNotEmpty() && !buffer.isComplete) {
            val finalMessage = buildStructuredMessage(buffer.copy(isComplete = true))
            viewModelScope.launch {
                messageRepository.saveMessage(finalMessage)
            }
        }
        
        _uiState.value = _uiState.value.copy(
            streamingContent = "",
            isStreaming = false,
            structuredOutputBuffer = StructuredOutputBuffer()
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

    override fun onCleared() {
        super.onCleared()
        coroutineJobs.forEach { job ->
            if (job.isActive) {
                job.cancel()
            }
        }
        coroutineJobs.clear()
    }
}
