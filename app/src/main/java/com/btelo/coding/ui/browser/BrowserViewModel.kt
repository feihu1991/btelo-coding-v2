package com.btelo.coding.ui.browser

import androidx.lifecycle.ViewModel
import com.btelo.coding.domain.model.ProxyEntry
import com.btelo.coding.domain.model.ProxyStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class BrowserUiState(
    val proxies: List<ProxyEntry> = emptyList(),
    val autoProxyEnabled: Boolean = true,
    val showAddPortDialog: Boolean = false,
    val showAddWebsiteDialog: Boolean = false
)

@HiltViewModel
class BrowserViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    init {
        loadMockData()
    }

    private fun loadMockData() {
        _uiState.value = BrowserUiState(
            proxies = listOf(
                ProxyEntry("1", "localhost:8802", "http://localhost:8802", ProxyStatus.ACTIVE),
                ProxyEntry("2", "localhost:3000", "http://localhost:3000", ProxyStatus.ACTIVE),
                ProxyEntry(
                    "3", "localhost:5173", "http://localhost:5173",
                    ProxyStatus.ERROR,
                    "502 Send failed: request failed: dial tcp 127.0.0.1:5173: connect: connection refused"
                )
            )
        )
    }

    fun setAutoProxy(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoProxyEnabled = enabled)
    }

    fun showAddPortDialog() {
        _uiState.value = _uiState.value.copy(showAddPortDialog = true)
    }

    fun showAddWebsiteDialog() {
        _uiState.value = _uiState.value.copy(showAddWebsiteDialog = true)
    }

    fun dismissDialogs() {
        _uiState.value = _uiState.value.copy(
            showAddPortDialog = false,
            showAddWebsiteDialog = false
        )
    }
}
