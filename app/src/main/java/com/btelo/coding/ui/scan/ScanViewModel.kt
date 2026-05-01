package com.btelo.coding.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btelo.coding.data.local.DataStoreManager
import com.btelo.coding.domain.repository.SessionRepository
import com.btelo.coding.util.Logger
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

data class ScanUiState(
    val serverAddress: String = "",
    val isDiscovering: Boolean = false,
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val error: String? = null,
    val bridges: List<BridgeInfo> = emptyList(),
    val selectedBridgeId: String? = null,
    val sessionId: String? = null,
    val wsToken: String? = null
)

data class BridgeInfo(
    val id: String,
    @field:SerializedName("device_name") val deviceName: String,
    @field:SerializedName("work_dir") val workDir: String,
    val mode: String,
    @field:SerializedName("bridge_connected") val bridgeConnected: Boolean,
    @field:SerializedName("mobile_connected") val mobileConnected: Boolean
)

data class BridgesResponse(
    val success: Boolean,
    val bridges: List<BridgeInfo>
)

data class BridgeConnectResponse(
    val success: Boolean,
    @field:SerializedName("session_id") val sessionId: String,
    @field:SerializedName("ws_token") val wsToken: String,
    @field:SerializedName("ws_url") val wsUrl: String,
    @field:SerializedName("bridge_info") val bridgeInfo: BridgeInfoRaw?
)

data class BridgeInfoRaw(
    @field:SerializedName("device_name") val deviceName: String,
    @field:SerializedName("work_dir") val workDir: String,
    val mode: String
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val dataStoreManager: DataStoreManager,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    init {
        loadSavedServerAddress()
    }

    private fun loadSavedServerAddress() {
        viewModelScope.launch {
            val saved = dataStoreManager.getServerAddressSync()
            if (saved != null) {
                _uiState.value = _uiState.value.copy(serverAddress = saved)
            }
        }
    }

    fun setServerAddress(address: String) {
        _uiState.value = _uiState.value.copy(serverAddress = address.trimEnd('/'))
    }

    fun discoverBridges() {
        val server = _uiState.value.serverAddress.ifBlank {
            _uiState.value = _uiState.value.copy(error = "请输入服务器地址")
            return
        }

        _uiState.value = _uiState.value.copy(isDiscovering = true, error = null, bridges = emptyList())

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("$server/bridges")
                        .get()
                        .build()
                    okHttpClient.newCall(request).execute().body?.string() ?: ""
                }

                val resp = gson.fromJson(result, BridgesResponse::class.java)
                if (resp != null && resp.success) {
                    dataStoreManager.saveServerAddress(server)
                    _uiState.value = _uiState.value.copy(
                        isDiscovering = false,
                        bridges = resp.bridges
                    )
                    Logger.i("ScanVM", "Found ${resp.bridges.size} bridge(s)")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isDiscovering = false,
                        error = "未发现可用设备"
                    )
                }
            } catch (e: Exception) {
                Logger.e("ScanVM", "Discover failed", e)
                _uiState.value = _uiState.value.copy(
                    isDiscovering = false,
                    error = "无法连接服务器: ${e.message}"
                )
            }
        }
    }

    fun connectToBridge(bridgeId: String, authCode: String) {
        val server = _uiState.value.serverAddress
        if (server.isBlank()) return

        _uiState.value = _uiState.value.copy(
            isConnecting = true,
            error = null,
            selectedBridgeId = bridgeId
        )

        viewModelScope.launch {
            try {
                val body = """{"auth_code":"$authCode"}"""
                val result = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("$server/bridges/$bridgeId/connect")
                        .post(body.toRequestBody("application/json".toMediaType()))
                        .build()
                    okHttpClient.newCall(request).execute().body?.string() ?: ""
                }

                val resp = gson.fromJson(result, BridgeConnectResponse::class.java)
                if (resp != null && resp.success) {
                    dataStoreManager.saveWsToken(resp.wsToken)
                    dataStoreManager.saveSessionId(resp.sessionId)

                    sessionRepository.createSessionWithId(
                        sessionId = resp.sessionId,
                        name = resp.bridgeInfo?.deviceName ?: "Claude Code",
                        tool = "claude"
                    )

                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        isConnected = true,
                        sessionId = resp.sessionId,
                        wsToken = resp.wsToken
                    )
                    Logger.i("ScanVM", "Connected to bridge $bridgeId")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        selectedBridgeId = null,
                        error = "认证失败: 验证码不正确"
                    )
                }
            } catch (e: Exception) {
                Logger.e("ScanVM", "Connect failed", e)
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    selectedBridgeId = null,
                    error = "连接失败: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun disconnect() {
        viewModelScope.launch {
            dataStoreManager.clearConnection()
            _uiState.value = ScanUiState(serverAddress = _uiState.value.serverAddress)
        }
    }
}
