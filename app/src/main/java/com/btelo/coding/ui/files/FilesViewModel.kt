package com.btelo.coding.ui.files

import androidx.lifecycle.ViewModel
import com.btelo.coding.domain.model.GitRepoInfo
import com.btelo.coding.domain.repository.SessionRepository
import com.btelo.coding.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class FilesUiState(
    val gitRepos: List<GitRepoInfo> = emptyList(),
    val currentPath: String = "/opt",
    val directories: List<String> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    init {
        loadMockData()
    }

    private fun loadMockData() {
        _uiState.value = FilesUiState(
            gitRepos = listOf(
                GitRepoInfo("her", "/opt/her", "main", System.currentTimeMillis()),
                GitRepoInfo("next-dashboard", "/opt/next-dashboard", "main", System.currentTimeMillis()),
                GitRepoInfo("shopify-store", "/opt/shopify-store", "main", System.currentTimeMillis()),
                GitRepoInfo("go-api", "/opt/go-api", "main", System.currentTimeMillis()),
                GitRepoInfo("react-native-app", "/opt/react-native-app", "main", System.currentTimeMillis()),
                GitRepoInfo("btelo-server", "/opt/btelo-server", "main", System.currentTimeMillis())
            ),
            currentPath = "/opt",
            directories = listOf("homebrew", "source", "workspace", "projects")
        )
    }

    fun navigateToPath(path: String) {
        _uiState.value = _uiState.value.copy(currentPath = path)
        Logger.i("FilesVM", "Navigate to: $path")
    }

    fun createSessionAtPath(path: String) {
        Logger.i("FilesVM", "Create session at: $path")
    }
}
