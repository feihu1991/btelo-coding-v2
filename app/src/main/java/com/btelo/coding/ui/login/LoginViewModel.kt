package com.btelo.coding.ui.login

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btelo.coding.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val serverAddress: String = "",
    val deviceId: String = "",
    val pairingCode: String = "",
    val expiresAt: String? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false
)

enum class ConnectionStatus {
    DISCONNECTED,
    REGISTERING,
    WAITING_FOR_PAIRING,
    PAIRED
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // Load saved server address and device ID
        viewModelScope.launch {
            authRepository.getServerAddress().collect { address ->
                if (address != null) {
                    _uiState.value = _uiState.value.copy(serverAddress = address)
                }
            }
        }
        viewModelScope.launch {
            authRepository.getDeviceId().collect { deviceId ->
                if (deviceId != null) {
                    _uiState.value = _uiState.value.copy(deviceId = deviceId)
                }
            }
        }
    }

    fun updateServerAddress(address: String) {
        _uiState.value = _uiState.value.copy(serverAddress = address)
    }

    fun connect() {
        val state = _uiState.value
        if (state.serverAddress.isBlank()) {
            _uiState.value = state.copy(error = "请输入服务器地址")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null, connectionStatus = ConnectionStatus.REGISTERING)

            // Get device name based on device model
            val deviceName = "${Build.MANUFACTURER}_${Build.MODEL}".replace(" ", "_")

            authRepository.registerDevice(
                serverAddress = state.serverAddress,
                deviceName = deviceName,
                deviceType = "mobile"
            ).onSuccess { device ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    deviceId = device.deviceId,
                    pairingCode = device.pairingCode,
                    connectionStatus = ConnectionStatus.WAITING_FOR_PAIRING
                )
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "连接失败",
                    connectionStatus = ConnectionStatus.DISCONNECTED
                )
            }
        }
    }

    fun refreshPairingCode() {
        val state = _uiState.value
        if (state.deviceId.isBlank()) {
            _uiState.value = state.copy(error = "请先连接设备")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)

            authRepository.getPairingCode(
                serverAddress = state.serverAddress,
                deviceId = state.deviceId
            ).onSuccess { device ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    pairingCode = device.pairingCode,
                    expiresAt = device.expiresAt
                )
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "获取配对码失败"
                )
            }
        }
    }

    fun onDevicePaired() {
        _uiState.value = _uiState.value.copy(
            connectionStatus = ConnectionStatus.PAIRED,
            isLoggedIn = true
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
