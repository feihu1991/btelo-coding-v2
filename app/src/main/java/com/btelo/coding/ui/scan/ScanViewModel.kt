package com.btelo.coding.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btelo.coding.data.local.DataStoreManager
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
    val remoteSessions: List<RemoteSession> = emptyList(),
    val showSessionPicker: Boolean = false,
    val selectedClaudeSessionId: String? = null
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
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    init {
        // Check if we have saved connection info
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

    /**
     * Parse QR code content and connect to server.
     * Expected format: btelo://IP:PORT/TOKEN
     * Uses 127.0.0.1 via USB port forwarding for reliable connection.
     */
    fun onQrCodeScanned(qrContent: String) {
        if (_uiState.value.isConnecting || _uiState.value.isConnected) return

        Logger.i("ScanVM", "QR scanned: $qrContent")

        // Parse btelo://IP:PORT/TOKEN
        val parsed = parseQrCode(qrContent)
        if (parsed == null) {
            _uiState.value = _uiState.value.copy(error = "Invalid QR code format")
            return
        }

        val (_, port, token) = parsed
        // Always use 127.0.0.1 via USB reverse port forwarding
        val serverUrl = "http://127.0.0.1:$port"

        connect(serverUrl, token)
    }

    /**
     * Connect manually with a URL string.
     */
    fun connectManually(url: String) {
        if (_uiState.value.isConnecting || _uiState.value.isConnected) return

        // Try parsing as btelo:// format first
        val parsed = parseQrCode(url)
        if (parsed != null) {
            val (host, port, token) = parsed
            connect("http://$host:$port", token)
            return
        }

        // Try as plain URL like http://IP:PORT
        if (url.startsWith("http://") || url.startsWith("https://")) {
            // Need to get a token first — try /status endpoint
            connect(url.trimEnd('/'), null)
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
                    "$serverAddress/status"
                }

                val result = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url(url)
                        .get()
                        .build()
                    val response = okHttpClient.newCall(request).execute()
                    response.body?.string() ?: ""
                }

                if (token != null) {
                    // Parse connect response
                    val connectResp = gson.fromJson(result, ConnectResponse::class.java)
                    if (connectResp.success) {
                        // Save connection info - use the actual server URL we connected to,
                        // not the one from the response (which might be WiFi IP)
                        dataStoreManager.saveServerAddress(serverAddress)
                        dataStoreManager.saveWsToken(connectResp.ws_token)
                        dataStoreManager.saveSessionId(connectResp.session_id)

                        val sessions = connectResp.available_sessions ?: emptyList()

                        _uiState.value = _uiState.value.copy(
                            isConnecting = false,
                            isConnected = true,
                            serverAddress = serverAddress,
                            sessionId = connectResp.session_id,
                            wsToken = connectResp.ws_token,
                            claudeVersion = connectResp.claude_code?.version,
                            remoteSessions = sessions,
                            showSessionPicker = sessions.isNotEmpty()
                        )
                        Logger.i("ScanVM", "Connected to $serverAddress, ${sessions.size} sessions found")
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isConnecting = false,
                            error = "Connection failed"
                        )
                    }
                } else {
                    // Status check — server is reachable but no token
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        error = "Please scan QR code to get a connection token"
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

    private fun parseQrCode(content: String): Triple<String, Int, String>? {
        // Format: btelo://IP:PORT/TOKEN
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

    /**
     * Fetch available Claude Code sessions from the server
     */
    fun fetchRemoteSessions() {
        val serverAddress = _uiState.value.serverAddress ?: return

        viewModelScope.launch {
            try {
                val url = "$serverAddress/sessions"
                val result = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url(url)
                        .get()
                        .build()
                    val response = okHttpClient.newCall(request).execute()
                    response.body?.string() ?: ""
                }

                val sessionsResponse = gson.fromJson(result, SessionsResponse::class.java)
                if (sessionsResponse.success) {
                    _uiState.value = _uiState.value.copy(
                        remoteSessions = sessionsResponse.sessions,
                        showSessionPicker = sessionsResponse.sessions.isNotEmpty()
                    )
                }
            } catch (e: Exception) {
                Logger.e("ScanVM", "Failed to fetch sessions", e)
            }
        }
    }

    /**
     * Select a Claude Code session to connect to
     */
    fun selectSession(sessionId: String) {
        _uiState.value = _uiState.value.copy(
            selectedClaudeSessionId = sessionId,
            showSessionPicker = false
        )
        viewModelScope.launch {
            dataStoreManager.saveClaudeSessionId(sessionId)
        }
        Logger.i("ScanVM", "Selected Claude session: $sessionId")
    }

    /**
     * Dismiss the session picker
     */
    fun dismissSessionPicker() {
        _uiState.value = _uiState.value.copy(showSessionPicker = false)
    }

    fun disconnect() {
        viewModelScope.launch {
            dataStoreManager.clearConnection()
            _uiState.value = ScanUiState()
        }
    }
}
