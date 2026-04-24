package com.btelo.coding.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val error: String? = null
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
    
    // Store coroutine jobs for proper cancellation
    private val coroutineJobs = mutableListOf<Job>()

    fun setSessionId(id: String) {
        sessionId = id
        val job = viewModelScope.launch {
            val serverAddress = authRepository.getServerAddress().firstOrNull() ?: ""
            val token = authRepository.getToken().firstOrNull() ?: ""
            if (serverAddress.isNotBlank() && token.isNotBlank()) {
                messageRepository.connect(serverAddress, token, sessionId)
            }
        }
        coroutineJobs.add(job)
        loadMessages()
        observeOutput()
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
                // Messages are already added in repository
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
            _uiState.value = _uiState.value.copy(isLoading = true, inputText = "")

            messageRepository.sendMessage(sessionId, content)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Cancel all coroutine jobs to prevent memory leaks
        coroutineJobs.forEach { job ->
            if (job.isActive) {
                job.cancel()
            }
        }
        coroutineJobs.clear()
    }
}
