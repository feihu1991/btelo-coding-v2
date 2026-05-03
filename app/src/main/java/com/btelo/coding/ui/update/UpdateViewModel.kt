package com.btelo.coding.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btelo.coding.BuildConfig
import com.btelo.coding.data.update.AppUpdateManager
import com.btelo.coding.data.update.UpdateCheckResult
import com.btelo.coding.data.update.UpdateInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class UpdateUiState(
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val updateInfo: UpdateInfo? = null,
    val downloadedApk: File? = null,
    val message: String? = null,
    val error: String? = null,
    val currentVersion: String = BuildConfig.VERSION_NAME
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateManager: AppUpdateManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var autoChecked = false

    fun checkOnLaunch() {
        if (autoChecked) return
        autoChecked = true
        checkForUpdate(showNoUpdateMessage = false)
    }

    fun checkForUpdate(showNoUpdateMessage: Boolean = true) {
        if (_uiState.value.isChecking || _uiState.value.isDownloading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isChecking = true,
                message = null,
                error = null
            )

            runCatching { updateManager.checkForUpdate() }
                .onSuccess { result ->
                    _uiState.value = when (result) {
                        is UpdateCheckResult.Available -> _uiState.value.copy(
                            isChecking = false,
                            updateInfo = result.info,
                            downloadedApk = null,
                            message = "New version ${result.info.versionName} is ready"
                        )
                        UpdateCheckResult.NotAvailable -> _uiState.value.copy(
                            isChecking = false,
                            message = if (showNoUpdateMessage) "当前已经是最新版本" else null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        error = error.message ?: "Update check failed"
                    )
                }
        }
    }

    fun downloadAndInstall() {
        val info = _uiState.value.updateInfo ?: return
        if (_uiState.value.isDownloading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDownloading = true,
                downloadProgress = 0f,
                error = null,
                message = "Downloading ${info.versionName}"
            )

            runCatching {
                updateManager.downloadApk(info) { progress ->
                    _uiState.value = _uiState.value.copy(downloadProgress = progress.coerceIn(0f, 1f))
                }
            }
                .onSuccess { apk ->
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        downloadedApk = apk,
                        message = "Download complete"
                    )
                    updateManager.installApk(apk)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        error = error.message ?: "APK download failed"
                    )
                }
        }
    }

    fun installDownloaded() {
        _uiState.value.downloadedApk?.let { updateManager.installApk(it) }
    }

    fun dismissUpdate() {
        _uiState.value = _uiState.value.copy(updateInfo = null, message = null, error = null)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }
}
