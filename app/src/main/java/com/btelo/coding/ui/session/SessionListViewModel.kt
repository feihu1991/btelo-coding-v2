package com.btelo.coding.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btelo.coding.domain.model.Message
import com.btelo.coding.domain.model.Session
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

data class SessionWithPreview(
    val session: Session,
    val lastMessage: Message? = null
)

data class SessionListUiState(
    val sessions: List<SessionWithPreview> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateToSessionId: String? = null,
    val searchQuery: String = "",
    val serverAddress: String = ""
)

@HiltViewModel
class SessionListViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionListUiState())
    val uiState: StateFlow<SessionListUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadSessions()
        ensureServerSession()
        loadServerAddress()
    }

    private fun loadServerAddress() {
        viewModelScope.launch {
            authRepository.getServerAddress().collect { address ->
                if (address != null) {
                    _uiState.value = _uiState.value.copy(serverAddress = address)
                }
            }
        }
    }

    private fun loadSessions() {
        viewModelScope.launch {
            sessionRepository.getSessions().collect { sessions ->
                val sessionsWithPreviews = sessions.map { session ->
                    SessionWithPreview(
                        session = session,
                        lastMessage = messageRepository.getLastMessage(session.id)
                    )
                }
                _uiState.value = _uiState.value.copy(sessions = sessionsWithPreviews)
            }
        }
    }

    /**
     * Ensure a local session exists for the server-assigned session ID.
     * After pairing, the server assigns a session ID. We need a local session
     * with that same ID so the WebSocket connection routes correctly.
     */
    private fun ensureServerSession() {
        viewModelScope.launch {
            val serverSessionId = authRepository.getSessionId().firstOrNull()
            if (serverSessionId != null) {
                Logger.i("SessionListVM", "Server session ID found: $serverSessionId")
                // Check if a session with this ID already exists
                val existingSession = sessionRepository.getSession(serverSessionId).firstOrNull()
                if (existingSession == null) {
                    // Create a local session with the server session ID
                    sessionRepository.createSessionWithId(serverSessionId, "Claude", "claude")
                    Logger.i("SessionListVM", "Created local session for server ID: $serverSessionId")
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Debounce 300ms
            if (query.isBlank()) {
                loadSessions()
            } else {
                sessionRepository.searchSessions(query).collect { sessions ->
                    val sessionsWithPreviews = sessions.map { session ->
                        SessionWithPreview(
                            session = session,
                            lastMessage = messageRepository.getLastMessage(session.id)
                        )
                    }
                    _uiState.value = _uiState.value.copy(sessions = sessionsWithPreviews)
                }
            }
        }
    }

    fun createSession(name: String = "Claude", tool: String = "claude") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Use server session ID if available, otherwise generate a new one
                val serverSessionId = authRepository.getSessionId().firstOrNull()
                val session = if (serverSessionId != null) {
                    // Check if session already exists
                    val existing = sessionRepository.getSession(serverSessionId).firstOrNull()
                    if (existing != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            navigateToSessionId = existing.id
                        )
                        return@launch
                    }
                    sessionRepository.createSessionWithId(serverSessionId, name, tool)
                } else {
                    sessionRepository.createSession(name, tool)
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    navigateToSessionId = session.id
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun onNavigatedToSession() {
        _uiState.value = _uiState.value.copy(navigateToSessionId = null)
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            sessionRepository.deleteSession(sessionId)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
