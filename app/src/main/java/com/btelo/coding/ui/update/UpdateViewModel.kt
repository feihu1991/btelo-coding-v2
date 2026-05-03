package com.btelo.coding.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btelo.coding.BuildConfig
import com.btelo.coding.data.update.AppUpdateManager
import com.btelo.coding.data.update.InstallLaunchResult
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
    val needsInstallPermission: Boolean = false,
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
        restorePreparedUpdate()
        prepareNextUpdateInBackground()
    }

    private fun restorePreparedUpdate() {
        viewModelScope.launch {
            runCatching { updateManager.restorePendingUpdate() }
                .onSuccess { restored ->
                    if (restored != null) {
                        _uiState.value = _uiState.value.copy(
                            updateInfo = restored.first,
                            downloadedApk = restored.second,
                            needsInstallPermission = !updateManager.canInstallPackageUpdates(),
                            message = "Update ${restored.first.versionName} is ready to install"
                        )
                    }
                }
        }
    }

    private fun prepareNextUpdateInBackground() {
        if (_uiState.value.isChecking || _uiState.value.isDownloading) return

        viewModelScope.launch {
            runCatching { updateManager.checkForUpdate() }
                .onSuccess { result ->
                    if (result is UpdateCheckResult.Available) {
                        val prepared = updateManager.prepareUpdateInBackground(result.info)
                        if (prepared != null) {
                            _uiState.value = _uiState.value.copy(
                                updateInfo = result.info,
                                downloadedApk = prepared,
                                needsInstallPermission = !updateManager.canInstallPackageUpdates(),
                                message = "Update ${result.info.versionName} is ready to install"
                            )
                        }
                    }
                }
        }
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
                        is UpdateCheckResult.Available -> {
                            val restored = updateManager.restorePendingUpdate()
                            val downloadedApk =
                                restored?.takeIf { it.first.versionName == result.info.versionName }?.second
                            _uiState.value.copy(
                                isChecking = false,
                                updateInfo = result.info,
                                downloadedApk = downloadedApk,
                                needsInstallPermission = downloadedApk != null &&
                                    !updateManager.canInstallPackageUpdates(),
                                message = if (downloadedApk != null) {
                                    "Update ${result.info.versionName} is ready to install"
                                } else {
                                    "New version ${result.info.versionName} is ready"
                                }
                            )
                        }

                        UpdateCheckResult.NotAvailable -> _uiState.value.copy(
                            isChecking = false,
                            message = if (showNoUpdateMessage) "Already up to date" else null
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
                    _uiState.value =
                        _uiState.value.copy(downloadProgress = progress.coerceIn(0f, 1f))
                }
            }
                .onSuccess { apk ->
                    val installResult = updateManager.installApk(apk)
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        downloadedApk = apk,
                        needsInstallPermission = installResult == InstallLaunchResult.PermissionRequired,
                        message = if (installResult == InstallLaunchResult.PermissionRequired) {
                            "Allow installs for BTELO Coding, then tap install again"
                        } else {
                            "Download complete"
                        }
                    )
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
        _uiState.value.downloadedApk?.let { apk ->
            val result = updateManager.installApk(apk)
            _uiState.value = _uiState.value.copy(
                needsInstallPermission = result == InstallLaunchResult.PermissionRequired,
                message = if (result == InstallLaunchResult.PermissionRequired) {
                    "Allow installs for BTELO Coding, then tap install again"
                } else {
                    "Opening Android installer"
                }
            )
        }
    }

    fun openInstallSettings() {
        updateManager.openInstallPermissionSettings()
        _uiState.value = _uiState.value.copy(
            needsInstallPermission = !updateManager.canInstallPackageUpdates(),
            message = "Enable install permission, then return here to finish the update"
        )
    }

    fun dismissUpdate() {
        _uiState.value = _uiState.value.copy(updateInfo = null, message = null, error = null)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }
}
