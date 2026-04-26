package com.btelo.coding.ui.agents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btelo.coding.data.remote.websocket.factory.ConnectionState
import com.btelo.coding.domain.model.Message
import com.btelo.coding.domain.repository.AuthRepository
import com.btelo.coding.domain.repository.MessageRepository
import com.btelo.coding.domain.repository.SessionRepository
import com.btelo.coding.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionInfo(
    val id: String,
    val name: String,
    val isConnected: Boolean = false,
    val unreadCount: Int = 0
)

data class AgentsUiState(
    val sessions: List<SessionInfo> = emptyList(),
    val currentSessionId: String? = null,
    val currentSessionName: String = "BTELO Coding",
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val streamingContent: String = "",
    val isStreaming: Boolean = false,
    val errorMessage: String? = null,
    val quickActions: List<String> = listOf(
        "Build feature",
        "Fix bug",
        "Run tests",
        "Deploy"
    )
)

@HiltViewModel
class AgentsViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentsUiState())
    val uiState: StateFlow<AgentsUiState> = _uiState.asStateFlow()

    private val coroutineJobs = mutableListOf<Job>()

    init {
        loadSessions()
        observeConnectionState()
    }

    private fun loadSessions() {
        val job = viewModelScope.launch {
            sessionRepository.getSessions().collect { sessions ->
                val sessionInfos = sessions.map { session ->
                    SessionInfo(
                        id = session.id,
                        name = session.name,
                        isConnected = session.isConnected
                    )
                }
                _uiState.value = _uiState.value.copy(sessions = sessionInfos)

                // Auto-select first session if none selected
                if (_uiState.value.currentSessionId == null && sessionInfos.isNotEmpty()) {
                    switchSession(sessionInfos.first().id)
                }
            }
        }
        coroutineJobs.add(job)
    }

    private fun observeConnectionState() {
        val job = viewModelScope.launch {
            messageRepository.connectionState.collect { state ->
                val isConnected = state is ConnectionState.Connected
                val errorMessage = when (state) {
                    is ConnectionState.Error -> state.message
                    else -> null
                }
                _uiState.value = _uiState.value.copy(
                    connectionState = state,
                    isConnected = isConnected,
                    errorMessage = errorMessage
                )
            }
        }
        coroutineJobs.add(job)
    }

    fun switchSession(sessionId: String) {
        // Cancel previous session jobs
        coroutineJobs.forEach { if (isActive(it)) it.cancel() }
        coroutineJobs.clear()

        _uiState.value = _uiState.value.copy(
            currentSessionId = sessionId,
            messages = emptyList(),
            streamingContent = "",
            isStreaming = false
        )

        // Find session name
        val session = _uiState.value.sessions.find { it.id == sessionId }
        _uiState.value = _uiState.value.copy(
            currentSessionName = session?.name ?: "Claude"
        )

        // Connect to session
        connectToSession(sessionId)
        loadMessages(sessionId)
        observeOutput(sessionId)
        observeConnectionState()
    }

    private fun connectToSession(sessionId: String) {
        val job = viewModelScope.launch {
            val serverAddress = authRepository.getServerAddress().firstOrNull() ?: ""
            val wsToken = authRepository.getWsTokenSync()
            val authToken = authRepository.getTokenSync()
            val token = wsToken ?: authToken ?: ""
            if (serverAddress.isNotBlank() && token.isNotBlank()) {
                messageRepository.connect(serverAddress, token, sessionId)
            }
        }
        coroutineJobs.add(job)
    }

    private fun loadMessages(sessionId: String) {
        val job = viewModelScope.launch {
            messageRepository.getMessages(sessionId).collect { messages ->
                _uiState.value = _uiState.value.copy(messages = messages)
            }
        }
        coroutineJobs.add(job)
    }

    private fun observeOutput(sessionId: String) {
        val job = viewModelScope.launch {
            messageRepository.observeOutput(sessionId).collect { message ->
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

    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage() {
        val content = _uiState.value.inputText.trim()
        if (content.isBlank()) return

        val sessionId = _uiState.value.currentSessionId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                inputText = "",
                streamingContent = "",
                isStreaming = true
            )

            messageRepository.sendMessage(sessionId, content)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isStreaming = false,
                        errorMessage = exception.message
                    )
                }
        }
    }

    fun createSession() {
        viewModelScope.launch {
            try {
                val session = sessionRepository.createSession("Claude", "claude")
                _uiState.value = _uiState.value.copy(
                    currentSessionId = session.id,
                    currentSessionName = session.name
                )
                switchSession(session.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    private fun isActive(job: Job): Boolean = job.isActive

    override fun onCleared() {
        super.onCleared()
        coroutineJobs.forEach { if (it.isActive) it.cancel() }
        coroutineJobs.clear()
    }
}
