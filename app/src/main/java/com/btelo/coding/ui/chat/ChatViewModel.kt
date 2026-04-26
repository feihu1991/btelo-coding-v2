package com.btelo.coding.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btelo.coding.data.remote.websocket.factory.ConnectionState
import com.btelo.coding.domain.model.Message
import com.btelo.coding.domain.repository.AuthRepository
import com.btelo.coding.domain.repository.MessageRepository
import com.btelo.coding.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    val sessionName: String = "Claude"
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
                kotlinx.coroutines.delay(2000)
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
                isStreaming = true
            )

            messageRepository.sendMessage(sessionId, content)
                .onSuccess {
                    // Keep isStreaming = true until output is fully received
                    // The streaming content will accumulate from observeOutput
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
        val streaming = _uiState.value.streamingContent
        if (streaming.isNotBlank()) {
            // The content is already persisted by MessageRepositoryImpl,
            // just clear the streaming state
            _uiState.value = _uiState.value.copy(
                streamingContent = "",
                isStreaming = false
            )
        }
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
