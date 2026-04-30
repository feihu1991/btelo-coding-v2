package com.btelo.coding.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btelo.coding.data.local.DataStoreManager
import com.btelo.coding.domain.repository.SessionRepository
import com.btelo.coding.util.Logger
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

data class ScanUiState(
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val error: String? = null,
    val serverAddress: String? = null,
    val sessionId: String? = null,
    val wsToken: String? = null,
    val claudeVersion: String? = null,
    val remoteSessions: List<RemoteSession> = emptyList()
)

data class RemoteSession(
    val session_id: String,
    val cwd: String,
    val is_alive: Boolean,
    val message_count: Int,
    val last_message: LastMessageInfo?
)

data class LastMessageInfo(
    val content: String,
    val timestamp: Long
)

data class ConnectResponse(
    val success: Boolean,
    val session_id: String,
    val ws_token: String,
    val ws_url: String,
    val server_address: String,
    val claude_code: ClaudeCodeInfo?,
    val available_sessions: List<RemoteSession>?
)

data class ClaudeCodeInfo(
    val installed: Boolean,
    val path: String?,
    val version: String?
)

data class SessionsResponse(
    val success: Boolean,
    val sessions: List<RemoteSession>
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
        checkSavedConnection()
    }

    private fun checkSavedConnection() {
        viewModelScope.launch {
            val serverAddress = dataStoreManager.getServerAddressSync()
            val wsToken = dataStoreManager.getWsTokenSync()
            if (serverAddress != null && wsToken != null) {
                _uiState.value = _uiState.value.copy(
                    isConnected = true,
                    serverAddress = serverAddress,
                    wsToken = wsToken
                )
            }
        }
    }

    fun onQrCodeScanned(qrContent: String) {
        if (_uiState.value.isConnecting || _uiState.value.isConnected) return

        Logger.i("ScanVM", "QR scanned: $qrContent")

        val parsed = parseQrCode(qrContent)
        if (parsed == null) {
            _uiState.value = _uiState.value.copy(error = "Invalid QR code format")
            return
        }

        val (host, port, token) = parsed
        val serverUrl = "http://$host:$port"

        connect(serverUrl, token)
    }

    fun connectManually(url: String) {
        if (_uiState.value.isConnecting || _uiState.value.isConnected) return

        val parsed = parseQrCode(url)
        if (parsed != null) {
            val (host, port, token) = parsed
            connect("http://$host:$port", token)
            return
        }

        if (url.startsWith("http://") || url.startsWith("https://")) {
            val cleanUrl = url.trimEnd('/')
            val pathParts = cleanUrl.substringAfter("://").split("/", limit = 2)
            val hostPort = pathParts[0]
            val pathToken = pathParts.getOrNull(1)

            if (pathToken != null && pathToken.isNotBlank()) {
                val scheme = if (url.startsWith("https://")) "https" else "http"
                connect("$scheme://$hostPort", pathToken)
            } else {
                connect(cleanUrl, null)
            }
            return
        }

        _uiState.value = _uiState.value.copy(error = "Invalid URL format")
    }

    private fun connect(serverAddress: String, token: String?) {
        _uiState.value = _uiState.value.copy(isConnecting = true, error = null)

        viewModelScope.launch {
            try {
                val url = if (token != null) {
                    "$serverAddress/connect?token=$token"
                } else {
                    "$serverAddress/connect"
                }

                val result = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url(url)
                        .get()
                        .build()
                    val response = okHttpClient.newCall(request).execute()
                    response.body?.string() ?: ""
                }

                val connectResp = try {
                    gson.fromJson(result, ConnectResponse::class.java)
                } catch (e: Exception) {
                    null
                }

                if (connectResp != null && connectResp.success) {
                    dataStoreManager.saveServerAddress(serverAddress)
                    dataStoreManager.saveWsToken(connectResp.ws_token)
                    dataStoreManager.saveSessionId(connectResp.session_id)

                    val sessions = connectResp.available_sessions ?: emptyList()

                    // Auto-select session: prefer active, then most recent
                    val selectedSessionId = autoSelectSession(sessions) ?: connectResp.session_id

                    sessionRepository.createSessionWithId(
                        sessionId = selectedSessionId,
                        name = "Claude Code",
                        tool = "claude"
                    )

                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        isConnected = true,
                        serverAddress = serverAddress,
                        sessionId = selectedSessionId,
                        wsToken = connectResp.ws_token,
                        claudeVersion = connectResp.claude_code?.version,
                        remoteSessions = sessions
                    )
                    Logger.i("ScanVM", "Connected to $serverAddress, selected session: $selectedSessionId")
                } else if (token == null) {
                    tryConnectStatus(serverAddress)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        error = "Connection failed: invalid token or server error"
                    )
                }
            } catch (e: Exception) {
                Logger.e("ScanVM", "Connection failed", e)
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    error = "Connection failed: ${e.message}"
                )
            }
        }
    }

    /**
     * Auto-select a session from available sessions
     * Priority: active sessions > most messages > first available
     */
    private fun autoSelectSession(sessions: List<RemoteSession>): String? {
        if (sessions.isEmpty()) return null

        // First, try to find an active session with most messages
        val activeSessions = sessions.filter { it.is_alive }
        if (activeSessions.isNotEmpty()) {
            return activeSessions.maxByOrNull { it.message_count }?.session_id
        }

        // Fallback: most messages overall
        return sessions.maxByOrNull { it.message_count }?.session_id
    }

    private suspend fun tryConnectStatus(serverAddress: String) {
        try {
            val result = withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url("$serverAddress/status")
                    .get()
                    .build()
                val response = okHttpClient.newCall(request).execute()
                response.body?.string() ?: ""
            }

            val statusResp = try {
                gson.fromJson(result, com.google.gson.JsonObject::class.java)
            } catch (e: Exception) {
                null
            }

            if (statusResp != null && statusResp.has("success") && statusResp.get("success").asBoolean) {
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    error = "Server is reachable. Scan QR code or enter a connection token."
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    error = "Server responded but no token provided. Scan QR code to get a token."
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isConnecting = false,
                error = "Cannot reach server at $serverAddress: ${e.message}"
            )
        }
    }

    private fun parseQrCode(content: String): Triple<String, Int, String>? {
        if (!content.startsWith("btelo://")) return null

        val withoutProtocol = content.removePrefix("btelo://")
        val parts = withoutProtocol.split("/", limit = 2)
        if (parts.size != 2) return null

        val hostPort = parts[0]
        val token = parts[1]

        val hostPortParts = hostPort.split(":", limit = 2)
        if (hostPortParts.size != 2) return null

        val host = hostPortParts[0]
        val port = hostPortParts[1].toIntOrNull() ?: return null

        return Triple(host, port, token)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun disconnect() {
        viewModelScope.launch {
            dataStoreManager.clearConnection()
            _uiState.value = ScanUiState()
        }
    }
}
