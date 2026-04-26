package com.btelo.coding.ui.login

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btelo.coding.domain.repository.AuthRepository
import com.btelo.coding.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val serverAddress: String = "",
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val confirmPassword: String = "",
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

    private val _uiState = MutableStateFlow(LoginUiState(
        serverAddress = "http://10.0.2.2:8080"
    ))
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
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
        // Auto-login if token exists
        checkAutoLogin()
    }

    private fun checkAutoLogin() {
        // Synchronous check for immediate result
        try {
            val token = authRepository.getTokenSync()
            if (token != null) {
                _uiState.value = _uiState.value.copy(isLoggedIn = true)
                return
            }
        } catch (_: Throwable) {}
        // Fallback to flow-based check
        viewModelScope.launch {
            authRepository.getToken().collect { token ->
                if (token != null) {
                    _uiState.value = _uiState.value.copy(isLoggedIn = true)
                }
            }
        }
    }

    fun updateServerAddress(address: String) {
        _uiState.value = _uiState.value.copy(serverAddress = address)
    }

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun updateConfirmPassword(password: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = password)
    }

    fun login() {
        val state = _uiState.value
        if (state.serverAddress.isBlank()) {
            _uiState.value = state.copy(error = "请输入服务器地址")
            return
        }
        if (state.email.isBlank()) {
            _uiState.value = state.copy(error = "请输入账号")
            return
        }
        if (state.password.isBlank()) {
            _uiState.value = state.copy(error = "请输入密码")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, connectionStatus = ConnectionStatus.REGISTERING)

            // Step 1: Authenticate with server
            authRepository.login(
                serverAddress = state.serverAddress,
                username = state.email,
                password = state.password
            ).onSuccess {
                // Step 2: Register device and get pairing code
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
                    startPairingPolling()
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "设备注册失败",
                        connectionStatus = ConnectionStatus.DISCONNECTED
                    )
                }
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "登录失败",
                    connectionStatus = ConnectionStatus.DISCONNECTED
                )
            }
        }
    }

    fun register() {
        val state = _uiState.value
        if (state.serverAddress.isBlank()) {
            _uiState.value = state.copy(error = "请输入服务器地址")
            return
        }
        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "请输入昵称")
            return
        }
        if (state.email.isBlank()) {
            _uiState.value = state.copy(error = "请输入账号")
            return
        }
        if (state.password.isBlank()) {
            _uiState.value = state.copy(error = "请输入密码")
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.value = state.copy(error = "两次密码输入不一致")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, connectionStatus = ConnectionStatus.REGISTERING)

            // Register with server
            authRepository.register(
                serverAddress = state.serverAddress,
                username = state.email,
                password = state.password,
                name = state.name
            ).onSuccess {
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
                    startPairingPolling()
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "设备注册失败",
                        connectionStatus = ConnectionStatus.DISCONNECTED
                    )
                }
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "注册失败",
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

    /**
     * Start polling the server to detect when the device gets paired by the terminal.
     * Polls every 3 seconds. Stops when pairing is detected or state changes.
     */
    private fun startPairingPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            val state = _uiState.value
            val serverAddress = state.serverAddress
            val deviceId = state.deviceId

            if (serverAddress.isBlank() || deviceId.isBlank()) {
                Logger.w("LoginViewModel", "Cannot start polling: missing serverAddress or deviceId")
                return@launch
            }

            Logger.i("LoginViewModel", "Starting pairing status polling for device $deviceId")

            while (true) {
                delay(3000) // Poll every 3 seconds

                val currentState = _uiState.value
                // Stop polling if we're no longer waiting
                if (currentState.connectionStatus != ConnectionStatus.WAITING_FOR_PAIRING) {
                    Logger.d("LoginViewModel", "Stopping polling: status is ${currentState.connectionStatus}")
                    break
                }

                try {
                    val result = authRepository.getDeviceStatus(serverAddress, deviceId)
                    result.onSuccess { status ->
                        if (status.paired && status.sessionId != null) {
                            Logger.i("LoginViewModel", "Device paired! Session ID: ${status.sessionId}")
                            // Save the server-assigned session ID
                            authRepository.saveSessionId(status.sessionId)
                            // Auto-complete pairing
                            _uiState.value = _uiState.value.copy(
                                connectionStatus = ConnectionStatus.PAIRED,
                                isLoggedIn = true
                            )
                        }
                    }.onFailure { e ->
                        Logger.w("LoginViewModel", "Polling failed: ${e.message}")
                    }
                } catch (e: Exception) {
                    Logger.w("LoginViewModel", "Polling error: ${e.message}")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
