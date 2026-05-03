package com.btelo.coding.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btelo.coding.data.local.DataStoreManager
import com.btelo.coding.domain.model.Session
import com.btelo.coding.domain.model.SessionAttentionType
import com.btelo.coding.domain.repository.MessageRepository
import com.btelo.coding.domain.repository.SessionRepository
import com.btelo.coding.util.Logger
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

data class ScanUiState(
    val serverAddress: String = "https://boned-uncorrupt-java.ngrok-free.dev",
    val isDiscovering: Boolean = false,
    val isConnecting: Boolean = false,
    val isSendingQuickReply: Boolean = false,
    val isSendingPermissionDecision: Boolean = false,
    val isConnected: Boolean = false,
    val error: String? = null,
    val bridges: List<BridgeInfo> = emptyList(),
    val snapshots: List<Session> = emptyList(),
    val dashboardItems: List<DashboardSessionItem> = emptyList(),
    val selectedBridgeId: String? = null,
    val quickReplySessionId: String? = null,
    val permissionDecisionSessionId: String? = null,
    val sessionId: String? = null,
    val wsToken: String? = null,
    val currentSessionId: String? = null
)

data class BridgeInfo(
    val id: String,
    @field:SerializedName("device_name") val deviceName: String,
    @field:SerializedName("work_dir") val workDir: String,
    val mode: String,
    @field:SerializedName("bridge_connected") val bridgeConnected: Boolean,
    @field:SerializedName("mobile_connected") val mobileConnected: Boolean,
    @field:SerializedName("created_at") val createdAt: Long = 0L
)

data class DashboardSessionItem(
    val key: String,
    val sessionId: String?,
    val bridgeId: String?,
    val title: String,
    val workDir: String,
    val mode: String,
    val isLive: Boolean,
    val isConnectedOnPhone: Boolean,
    val hasSnapshot: Boolean,
    val lastActiveAt: Long?,
    val messageCount: Int,
    val isCurrentSession: Boolean,
    val attentionType: SessionAttentionType? = null,
    val attentionTitle: String = "",
    val attentionBody: String = "",
    val attentionUpdatedAt: Long? = null
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
    private val sessionRepository: SessionRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {

    private var discoveryCall: Call? = null

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    init {
        observeSnapshots()
        loadSavedContext()
    }

    private fun observeSnapshots() {
        viewModelScope.launch {
            sessionRepository.getSessions().collectLatest { sessions ->
                _uiState.value = _uiState.value.copy(
                    snapshots = sessions,
                    dashboardItems = buildDashboardItems(
                        bridges = _uiState.value.bridges,
                        snapshots = sessions,
                        currentSessionId = _uiState.value.currentSessionId
                    )
                )
            }
        }
    }

    private fun loadSavedContext() {
        viewModelScope.launch {
            val savedServer = dataStoreManager.getServerAddressSync()
            val savedSession = dataStoreManager.getSessionIdSync()
            val savedWsToken = dataStoreManager.getWsTokenSync()
            val savedAuthToken = dataStoreManager.getTokenSync()
            _uiState.value = _uiState.value.copy(
                serverAddress = savedServer ?: _uiState.value.serverAddress,
                currentSessionId = savedSession
            )

            if (!savedServer.isNullOrBlank() && !savedSession.isNullOrBlank()) {
                val token = savedWsToken ?: savedAuthToken
                if (!token.isNullOrBlank()) {
                    messageRepository.connect(savedServer, token, savedSession)
                }
            }

            if (!savedServer.isNullOrBlank()) {
                discoverBridges(silent = true)
            }
        }
    }

    fun setServerAddress(address: String) {
        _uiState.value = _uiState.value.copy(serverAddress = address.trimEnd('/'))
    }

    fun discoverBridges(silent: Boolean = false) {
        val server = _uiState.value.serverAddress.ifBlank {
            _uiState.value = _uiState.value.copy(error = "Please enter your relay server address.")
            return
        }

        discoveryCall?.cancel()
        _uiState.value = _uiState.value.copy(
            isDiscovering = true,
            error = if (silent) _uiState.value.error else null
        )

        val request = Request.Builder()
            .url("$server/bridges")
            .get()
            .build()

        val call = okHttpClient.newCall(request)
        discoveryCall = call

        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (call.isCanceled()) return

                val body = response.body?.string().orEmpty()
                val resp = runCatching { gson.fromJson(body, BridgesResponse::class.java) }.getOrNull()
                if (response.isSuccessful && resp?.success == true) {
                    viewModelScope.launch { dataStoreManager.saveServerAddress(server) }
                    _uiState.value = _uiState.value.copy(
                        isDiscovering = false,
                        bridges = resp.bridges,
                        dashboardItems = buildDashboardItems(
                            bridges = resp.bridges,
                            snapshots = _uiState.value.snapshots,
                            currentSessionId = _uiState.value.currentSessionId
                        ),
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isDiscovering = false,
                        error = if (silent) null else "No available desktop sessions were found."
                    )
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                if (call.isCanceled()) return
                _uiState.value = _uiState.value.copy(
                    isDiscovering = false,
                    error = if (silent) null else "Could not reach relay server: ${e.message}"
                )
            }
        })
    }

    fun stopDiscovery() {
        discoveryCall?.cancel()
        discoveryCall = null
        _uiState.value = _uiState.value.copy(isDiscovering = false)
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
                    dataStoreManager.saveServerAddress(server)
                    dataStoreManager.saveWsToken(resp.wsToken)
                    dataStoreManager.saveSessionId(resp.sessionId)

                    sessionRepository.createSessionWithId(
                        sessionId = resp.sessionId,
                        name = resp.bridgeInfo?.deviceName ?: "Desktop Session",
                        tool = "claude",
                        path = resp.bridgeInfo?.workDir.orEmpty()
                    )

                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        isConnected = true,
                        sessionId = resp.sessionId,
                        wsToken = resp.wsToken,
                        currentSessionId = resp.sessionId
                    )
                    Logger.i("ScanVM", "Connected to bridge $bridgeId")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        selectedBridgeId = null,
                        error = "Authentication failed. Please verify the 6-digit code."
                    )
                }
            } catch (e: Exception) {
                Logger.e("ScanVM", "Connect failed", e)
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    selectedBridgeId = null,
                    error = "Connection failed: ${e.message}"
                )
            }
        }
    }

    fun reopenSnapshot(sessionId: String) {
        _uiState.value = _uiState.value.copy(
            isConnected = true,
            sessionId = sessionId,
            currentSessionId = sessionId
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun respondToPermissionRequest(sessionId: String, decision: String) {
        _uiState.value = _uiState.value.copy(
            isSendingPermissionDecision = true,
            permissionDecisionSessionId = sessionId,
            error = null
        )

        viewModelScope.launch {
            val result = messageRepository.sendPermissionDecision(sessionId, decision)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isSendingPermissionDecision = false,
                    permissionDecisionSessionId = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isSendingPermissionDecision = false,
                    permissionDecisionSessionId = null,
                    error = result.exceptionOrNull()?.message
                        ?: "Failed to send permission decision."
                )
            }
        }
    }

    fun sendQuickReply(sessionId: String, content: String) {
        _uiState.value = _uiState.value.copy(
            isSendingQuickReply = true,
            quickReplySessionId = sessionId,
            error = null
        )

        viewModelScope.launch {
            val result = messageRepository.sendMessage(sessionId, content)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isSendingQuickReply = false,
                    quickReplySessionId = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isSendingQuickReply = false,
                    quickReplySessionId = null,
                    error = result.exceptionOrNull()?.message
                        ?: "Failed to send reply."
                )
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            dataStoreManager.clearConnection()
            _uiState.value = _uiState.value.copy(
                isConnected = false,
                sessionId = null,
                wsToken = null,
                currentSessionId = null,
                dashboardItems = buildDashboardItems(
                    bridges = _uiState.value.bridges,
                    snapshots = _uiState.value.snapshots,
                    currentSessionId = null
                )
            )
        }
    }

    private fun buildDashboardItems(
        bridges: List<BridgeInfo>,
        snapshots: List<Session>,
        currentSessionId: String?
    ): List<DashboardSessionItem> {
        val matchedSnapshots = mutableSetOf<String>()
        val items = mutableListOf<DashboardSessionItem>()

        bridges.sortedWith(
            compareByDescending<BridgeInfo> { it.bridgeConnected }
                .thenByDescending { it.mobileConnected }
                .thenByDescending { it.createdAt }
        ).forEach { bridge ->
            val snapshot = snapshots.firstOrNull { session ->
                session.id == bridge.id ||
                    (session.path.isNotBlank() && session.path == bridge.workDir)
            }
            snapshot?.id?.let(matchedSnapshots::add)

            items += DashboardSessionItem(
                key = "bridge-${bridge.id}",
                sessionId = snapshot?.id,
                bridgeId = bridge.id,
                title = if (bridge.deviceName.isNotBlank()) bridge.deviceName else (snapshot?.name ?: "Desktop Session"),
                workDir = bridge.workDir.ifBlank { snapshot?.path.orEmpty() },
                mode = bridge.mode,
                isLive = bridge.bridgeConnected,
                isConnectedOnPhone = bridge.mobileConnected,
                hasSnapshot = snapshot != null,
                lastActiveAt = snapshot?.lastActiveAt ?: bridge.createdAt.takeIf { it > 0 },
                messageCount = snapshot?.messageCount ?: 0,
                isCurrentSession = snapshot?.id == currentSessionId,
                attentionType = snapshot?.attentionType,
                attentionTitle = snapshot?.attentionTitle.orEmpty(),
                attentionBody = snapshot?.attentionBody.orEmpty(),
                attentionUpdatedAt = snapshot?.attentionUpdatedAt
            )
        }

        snapshots
            .filterNot { matchedSnapshots.contains(it.id) }
            .sortedByDescending { it.lastActiveAt }
            .forEach { snapshot ->
                items += DashboardSessionItem(
                    key = "snapshot-${snapshot.id}",
                    sessionId = snapshot.id,
                    bridgeId = null,
                    title = snapshot.name.ifBlank { "Session Snapshot" },
                    workDir = snapshot.path,
                    mode = snapshot.tool,
                    isLive = false,
                    isConnectedOnPhone = false,
                    hasSnapshot = true,
                    lastActiveAt = snapshot.lastActiveAt,
                    messageCount = snapshot.messageCount,
                    isCurrentSession = snapshot.id == currentSessionId,
                    attentionType = snapshot.attentionType,
                    attentionTitle = snapshot.attentionTitle,
                    attentionBody = snapshot.attentionBody,
                    attentionUpdatedAt = snapshot.attentionUpdatedAt
                )
            }

        return items
    }
}
