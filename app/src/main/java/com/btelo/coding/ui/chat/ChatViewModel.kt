package com.btelo.coding.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btelo.coding.BuildConfig
import com.btelo.coding.data.local.DataStoreManager
import com.btelo.coding.data.remote.AppUpdateInfo
import com.btelo.coding.data.remote.websocket.OutputType
import com.btelo.coding.data.remote.websocket.factory.ConnectionState
import com.btelo.coding.domain.model.ActiveTurnState
import com.btelo.coding.domain.model.Message
import com.btelo.coding.domain.model.MessageMetadata
import com.btelo.coding.domain.model.MessageType
import com.btelo.coding.domain.model.OutputType as DomainOutputType
import com.btelo.coding.domain.repository.AuthRepository
import com.btelo.coding.domain.repository.MessageRepository
import com.btelo.coding.domain.repository.SessionRepository
import com.btelo.coding.util.Logger
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
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
    val structuredOutputBuffer: StructuredOutputBuffer = StructuredOutputBuffer(),
    val controlMessage: String? = null,
    val updateInfo: AppUpdateInfo? = null
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
    private val dataStoreManager: DataStoreManager,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
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

        coroutineJobs.forEach { it.cancel() }
        coroutineJobs.clear()
        sessionId = effectiveId

        coroutineJobs += viewModelScope.launch {
            sessionRepository.createSessionWithId(effectiveId, "Claude Code", "claude")

            val serverAddress = authRepository.getServerAddress().firstOrNull().orEmpty()
            val wsToken = dataStoreManager.getWsTokenSync()
            val authToken = dataStoreManager.getTokenSync()
            val token = wsToken ?: authToken ?: ""
            if (serverAddress.isNotBlank() && token.isNotBlank()) {
                messageRepository.connect(serverAddress, token, effectiveId)
                checkForUpdate(serverAddress)
            }
        }

        observeMessages()
        observeConnectionState()
        observeSession()
        observeStructuredOutput()
        observeActiveTurn()
        observeBridgeControlResults()
    }

    private fun observeMessages() {
        coroutineJobs += viewModelScope.launch {
            messageRepository.getMessages(sessionId).collect { messages ->
                val current = _uiState.value
                val latest = messages.lastOrNull()
                val finishedByTranscript = latest != null &&
                    !latest.isFromUser &&
                    (latest.outputType == DomainOutputType.CLAUDE_RESPONSE || latest.type == MessageType.ERROR)

                _uiState.value = current.copy(
                    messages = messages,
                    isLoading = if (finishedByTranscript) false else current.isLoading,
                    isStreaming = if (finishedByTranscript) false else current.isStreaming,
                    streamingContent = if (finishedByTranscript) "" else current.streamingContent,
                    thinkingSession = if (finishedByTranscript) ThinkingSession() else current.thinkingSession
                )
            }
        }
    }

    private fun observeConnectionState() {
        coroutineJobs += viewModelScope.launch {
            messageRepository.connectionState.collect { state ->
                val isConnected = state is ConnectionState.Connected
                val lastConnectedTime = if (isConnected) {
                    System.currentTimeMillis()
                } else {
                    _uiState.value.lastConnectedTime
                }

                _uiState.value = _uiState.value.copy(
                    connectionState = state,
                    isConnected = isConnected,
                    reconnectAttempts = (state as? ConnectionState.Reconnecting)?.attempt ?: 0,
                    errorMessage = (state as? ConnectionState.Error)?.message,
                    lastConnectedTime = lastConnectedTime
                )

                if (sessionId.isNotBlank()) {
                    sessionRepository.updateSessionConnection(sessionId, isConnected)
                }
            }
        }
    }

    private fun observeSession() {
        coroutineJobs += viewModelScope.launch {
            sessionRepository.getSession(sessionId).collect { session ->
                session?.let {
                    _uiState.value = _uiState.value.copy(sessionName = it.name)
                }
            }
        }
    }

    private fun observeStructuredOutput() {
        coroutineJobs += viewModelScope.launch {
            messageRepository.observeStructuredOutput(sessionId).collect { message ->
                processStructuredOutput(message)
            }
        }
    }

    private fun observeActiveTurn() {
        coroutineJobs += viewModelScope.launch {
            messageRepository.activeTurnState.collect { activeTurn ->
                if (activeTurn.sessionId.isNotBlank() && activeTurn.sessionId != sessionId) return@collect

                if (activeTurn.isActive) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = true,
                        isStreaming = true,
                        streamingContent = activeTurn.textTail.ifBlank { "..." },
                        thinkingSession = ThinkingSession(
                            isActive = true,
                            currentMessage = activeTurn.displayLabel()
                        )
                    )
                } else if (_uiState.value.isStreaming || _uiState.value.isLoading) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isStreaming = false,
                        streamingContent = "",
                        thinkingSession = ThinkingSession()
                    )
                }
            }
        }
    }

    private fun observeBridgeControlResults() {
        coroutineJobs += viewModelScope.launch {
            messageRepository.bridgeControlResults.collect { result ->
                _uiState.value = _uiState.value.copy(
                    controlMessage = result.displayMessage(),
                    isLoading = if (result.action == "build_apk") false else _uiState.value.isLoading
                )

                if (result.action == "build_apk" && result.success) {
                    checkForUpdate()
                }
            }
        }
    }

    private fun processStructuredOutput(message: Message) {
        val outputType = message.outputType ?: return

        if (outputType == DomainOutputType.CLAUDE_RESPONSE) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isStreaming = false,
                streamingContent = "",
                thinkingSession = ThinkingSession(),
                structuredOutputBuffer = StructuredOutputBuffer()
            )
            return
        }

        val thinkingType = when (outputType) {
            DomainOutputType.THINKING -> ThinkingMessageType.THINKING
            DomainOutputType.TOOL_CALL -> ThinkingMessageType.TOOL_CALL
            DomainOutputType.FILE_OP -> ThinkingMessageType.FILE_OP
            DomainOutputType.ERROR -> ThinkingMessageType.ERROR
            DomainOutputType.SYSTEM -> ThinkingMessageType.SYSTEM
            else -> null
        } ?: return

        val current = _uiState.value.thinkingSession
        val nextMessage = ThinkingMessage(
            type = thinkingType,
            content = message.content
        )
        val currentMessage = when (thinkingType) {
            ThinkingMessageType.TOOL_CALL -> "Running ${message.metadata?.toolName ?: "tool"}"
            ThinkingMessageType.FILE_OP -> "Updating file"
            ThinkingMessageType.THINKING -> "Thinking..."
            ThinkingMessageType.ERROR -> "Error"
            ThinkingMessageType.SYSTEM -> "System event"
        }

        _uiState.value = _uiState.value.copy(
            isStreaming = true,
            streamingContent = "...",
            thinkingSession = ThinkingSession(
                isActive = true,
                messages = current.messages + nextMessage,
                currentMessageType = thinkingType,
                currentMessage = currentMessage
            )
        )
    }

    private fun ActiveTurnState.displayLabel(): String {
        return when (status) {
            "input_pending" -> when {
                pendingInputCount > 1 -> "Waiting for $pendingInputCount queued phone inputs"
                pendingPreview.isNotBlank() -> "Waiting for transcript confirmation"
                else -> "Waiting for Claude Code"
            }
            "waiting_for_assistant" -> "Waiting for Claude Code"
            "tool_activity" -> "Running ${activeToolName ?: "tool"}"
            "assistant_activity" -> "Claude Code is responding"
            else -> "Syncing Claude Code state"
        }
    }

    fun onImageSelected(uri: String) {
        _uiState.value = _uiState.value.copy(selectedImageUri = uri)
    }

    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage() {
        val content = _uiState.value.inputText.trim()
        if (content.isBlank() || sessionId.isBlank()) return

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            inputText = "",
            thinkingSession = ThinkingSession(
                isActive = true,
                currentMessage = "Waiting for transcript..."
            ),
            isStreaming = true,
            streamingContent = "...",
            structuredOutputBuffer = StructuredOutputBuffer(),
            error = null
        )

        viewModelScope.launch {
            messageRepository.sendMessage(sessionId, content)
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

    fun sendBridgeControl(action: String) {
        if (sessionId.isBlank()) return

        _uiState.value = _uiState.value.copy(
            controlMessage = null,
            error = null,
            isLoading = action == "build_apk" || _uiState.value.isLoading
        )

        viewModelScope.launch {
            messageRepository.sendBridgeControl(sessionId, action)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(controlMessage = action.sentMessage())
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        controlMessage = exception.message ?: "Bridge control failed"
                    )
                }
        }
    }

    fun checkForUpdate(server: String? = null) {
        viewModelScope.launch {
            val targetServer = server?.takeIf { it.isNotBlank() }
                ?: authRepository.getServerAddress().firstOrNull().orEmpty()
            if (targetServer.isBlank()) return@launch

            try {
                val body = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("$targetServer/app/latest?current_version_code=${BuildConfig.VERSION_CODE}")
                        .get()
                        .build()
                    okHttpClient.newCall(request).execute().body?.string() ?: ""
                }
                val updateInfo = gson.fromJson(body, AppUpdateInfo::class.java)
                if (updateInfo != null && updateInfo.success && updateInfo.updateAvailable && updateInfo.apkAvailable) {
                    _uiState.value = _uiState.value.copy(updateInfo = updateInfo)
                }
            } catch (e: Exception) {
                Logger.w("ChatVM", "Update check failed: ${e.message}")
            }
        }
    }

    fun dismissUpdatePrompt() {
        _uiState.value = _uiState.value.copy(updateInfo = null)
    }

    fun dismissControlMessage() {
        _uiState.value = _uiState.value.copy(controlMessage = null)
    }

    private fun com.btelo.coding.domain.model.BridgeControlActionResult.displayMessage(): String {
        val label = action.actionLabel()
        return if (success) {
            "$label 完成"
        } else {
            "$label 失败${message?.let { ": $it" } ?: exitCode?.let { ": exit $it" }.orEmpty()}"
        }
    }

    private fun String.sentMessage(): String {
        return "${actionLabel()} 已发送"
    }

    private fun String.actionLabel(): String {
        return when (this) {
            "build_apk" -> "打包 APK"
            "restart_bridge" -> "重启桥接"
            "restart_relay" -> "重启中继"
            else -> this
        }
    }

    fun onStreamComplete() {
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

    fun disconnect() {
        viewModelScope.launch {
            messageRepository.disconnect(sessionId)
            dataStoreManager.clearConnection()
            coroutineJobs.forEach { it.cancel() }
            coroutineJobs.clear()
        }
    }

    override fun onCleared() {
        coroutineJobs.forEach { it.cancel() }
        coroutineJobs.clear()
        super.onCleared()
    }
}
