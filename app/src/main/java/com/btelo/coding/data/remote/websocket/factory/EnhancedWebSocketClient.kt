package com.btelo.coding.data.remote.websocket.factory

import com.btelo.coding.data.remote.encryption.CryptoManager
import com.btelo.coding.data.remote.encryption.KeyPair
import com.btelo.coding.data.remote.encryption.KeyRotationManager
import com.btelo.coding.data.remote.encryption.KeyRotationState
import com.btelo.coding.data.remote.network.NetworkMonitor
import com.btelo.coding.data.remote.websocket.BteloMessage
import com.btelo.coding.data.remote.websocket.MessageProtocol
import com.btelo.coding.util.Logger
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * 增强的WebSocket客户端，支持重连机制、心跳保活和密钥轮换（前向保密）
 */
class EnhancedWebSocketClient(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val cryptoManager: CryptoManager,
    private val networkMonitor: NetworkMonitor,
    private val secureKeyStore: com.btelo.coding.data.remote.encryption.SecureKeyStore,
    private val keyRotationManager: KeyRotationManager? = null  // 密钥轮换管理器（可选）
) {
    private val tag = "EnhancedWebSocket"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var webSocket: WebSocket? = null
    private var config: WebSocketConfig? = null
    private var reconnectJob: Job? = null
    private var keyRotationCheckJob: Job? = null
    
    private var keyPair: KeyPair? = null
    private var cipher: com.google.crypto.tink.subtle.ChaCha20Poly1305? = null
    private var isEncrypted = false
    private var currentKeyVersion = 1  // 当前密钥版本
    
    private val protocol = MessageProtocol(gson)
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _messages = MutableSharedFlow<BteloMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<BteloMessage> = _messages.asSharedFlow()
    
    private val _events = MutableSharedFlow<WebSocketEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<WebSocketEvent> = _events.asSharedFlow()
    
    private val _keyRotationState = MutableStateFlow<KeyRotationState>(KeyRotationState.Idle)
    val keyRotationState: StateFlow<KeyRotationState> = _keyRotationState.asStateFlow()
    
    private var reconnectAttempt = 0
    
    /**
     * 连接到服务器
     */
    fun connect(config: WebSocketConfig) {
        this.config = config
        reconnectAttempt = 0
        
        // 监听网络状态
        scope.launch {
            networkMonitor.isConnected.collect { isConnected ->
                if (isConnected && _connectionState.value is ConnectionState.Disconnected) {
                    // 网络恢复且当前断开，尝试重连
                    startConnection()
                } else if (!isConnected) {
                    // 网络断开
                    _connectionState.value = ConnectionState.Error("网络已断开")
                }
            }
        }
        
        startConnection()
    }
    
    private fun startConnection() {
        val config = config ?: return

        _connectionState.value = ConnectionState.Connecting
        Logger.i(tag, "正在连接: ${config.sessionId}")

        val wsUrl = config.serverAddress
            .replace("http://", "ws://")
            .replace("https://", "wss://") + "/ws?token=${config.token}"

        Logger.i(tag, "WebSocket URL: $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .build()
        
        // 尝试使用SecureKeyStore获取或生成密钥对
        keyPair = secureKeyStore.getKeyPair(config.sessionId) ?: secureKeyStore.generateAndStoreKeyPair(config.sessionId)
        cipher = null
        isEncrypted = false
        
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Logger.i(tag, "WebSocket连接成功")
                reconnectAttempt = 0
                _connectionState.value = ConnectionState.Connected

                // E2E encryption disabled - server doesn't support real key exchange yet
                // All messages are sent as plaintext
                isEncrypted = false

                scope.launch {
                    _events.emit(WebSocketEvent.Connected(config.sessionId))
                }

                startKeyRotationCheck()
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Logger.e(tag, "WebSocket连接失败", t)
                _connectionState.value = ConnectionState.Error(t.message ?: "连接失败")
                
                scope.launch {
                    _events.emit(WebSocketEvent.Error(config.sessionId, t.message ?: "连接失败"))
                }
                
                // 尝试重连
                scheduleReconnect()
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Logger.i(tag, "WebSocket关闭: code=$code, reason=$reason")
                _connectionState.value = ConnectionState.Disconnected

                scope.launch {
                    _events.emit(WebSocketEvent.Disconnected(config.sessionId, reason))
                }

                // 非正常关闭时尝试重连
                if (code != 1000) {
                    scheduleReconnect()
                }
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Logger.d(tag, "WebSocket正在关闭: code=$code, reason=$reason")
            }
        })
    }
    
    private fun handleMessage(text: String) {
        val message = protocol.deserialize(text) ?: return
        
        when (message) {
            is BteloMessage.PublicKey -> {
                try {
                    val remotePublicKey = android.util.Base64.decode(
                        message.key, android.util.Base64.NO_WRAP
                    )
                    val sharedSecret = cryptoManager.deriveSharedSecret(
                        keyPair!!.privateKey, remotePublicKey
                    )
                    // Use HKDF to derive the cipher key from shared secret (following NIST SP 800-56C)
                    cipher = cryptoManager.createCipherFromSharedSecret(sharedSecret)
                    isEncrypted = true
                    _messages.tryEmit(BteloMessage.Status(connected = true))
                } catch (e: Exception) {
                    Logger.e(tag, "密钥协商失败", e)
                }
            }
            is BteloMessage.Output -> {
                if (isEncrypted && cipher != null) {
                    try {
                        val encryptedData = android.util.Base64.decode(
                            message.data, android.util.Base64.NO_WRAP
                        )
                        val decryptedData = cryptoManager.decrypt(encryptedData, cipher!!)
                        val decryptedMessage = message.copy(data = String(decryptedData))
                        _messages.tryEmit(decryptedMessage)
                    } catch (e: Exception) {
                        // 解密失败，回退到明文
                        Logger.w(tag, "消息解密失败，使用明文", e)
                        _messages.tryEmit(message)
                    }
                } else {
                    _messages.tryEmit(message)
                }
            }
            is BteloMessage.Command -> _messages.tryEmit(message)
            is BteloMessage.Status -> {
                if (!message.connected) {
                    _connectionState.value = ConnectionState.Disconnected
                    scope.launch { _events.emit(WebSocketEvent.Disconnected(config?.sessionId ?: "", "server")) }
                }
                _messages.tryEmit(message)
            }
            is BteloMessage.SyncHistory -> _messages.tryEmit(message)
            is BteloMessage.NewMessage -> _messages.tryEmit(message)
            is BteloMessage.TranscriptSnapshot -> _messages.tryEmit(message)
            is BteloMessage.TranscriptDelta -> _messages.tryEmit(message)
            is BteloMessage.ActiveTurnSnapshot -> _messages.tryEmit(message)
            is BteloMessage.TerminalFrame -> _messages.tryEmit(message)
            is BteloMessage.TerminalExit -> _messages.tryEmit(message)
            is BteloMessage.BridgeControl -> _messages.tryEmit(message)
            is BteloMessage.BridgeControlResult -> _messages.tryEmit(message)
            is BteloMessage.InputStatus -> _messages.tryEmit(message)
            is BteloMessage.BridgeStatus -> _messages.tryEmit(message)
            is BteloMessage.SelectSession -> _messages.tryEmit(message)
            is BteloMessage.KeyRotation -> {
                handleKeyRotationMessage(message)
            }
            is BteloMessage.EncryptedData -> {
                // 处理带密钥版本的数据消息
                if (isEncrypted && cipher != null) {
                    try {
                        val encryptedData = android.util.Base64.decode(
                            message.data, android.util.Base64.NO_WRAP
                        )
                        // 使用 KeyRotationManager 尝试所有历史密钥
                        val decryptedData = keyRotationManager?.decryptWithHistory(
                            config?.sessionId ?: "", encryptedData, message.keyVersion
                        )
                        if (decryptedData != null) {
                            val decryptedMessage = BteloMessage.Output(
                                data = String(decryptedData),
                                stream = message.stream
                            )
                            _messages.tryEmit(decryptedMessage)
                        } else {
                            Logger.w(tag, "无法用任何密钥版本解密消息")
                            _messages.tryEmit(message)
                        }
                    } catch (e: Exception) {
                        Logger.w(tag, "消息解密失败", e)
                        _messages.tryEmit(message)
                    }
                } else {
                    _messages.tryEmit(message)
                }
            }
            is BteloMessage.StructuredOutput -> _messages.tryEmit(message)
            is BteloMessage.SessionState -> _messages.tryEmit(message)
            is BteloMessage.HookEvent -> _messages.tryEmit(message)
            is BteloMessage.PermissionResponse -> { /* handled via WebSocket send, not receive */ }
        }
    }
    
    /**
     * 处理密钥轮换消息
     */
    private fun handleKeyRotationMessage(message: BteloMessage.KeyRotation) {
        val sessionId = config?.sessionId ?: return
        
        Logger.i(tag, "收到密钥轮换消息: action=${message.action}, version=${message.keyVersion}")
        
        // 转发给 KeyRotationManager 处理
        keyRotationManager?.let { manager ->
            val rotationMessage = com.btelo.coding.data.remote.encryption.KeyRotationMessage(
                action = message.action,
                newPublicKey = message.newPublicKey,
                keyVersion = message.keyVersion,
                timestamp = message.timestamp
            )
            
            val response = manager.handleRotationHandshake(sessionId, rotationMessage)
            _keyRotationState.value = manager.rotationState.value
            
            // 发送响应消息
            response?.let {
                val responseMessage = BteloMessage.KeyRotation(
                    action = it.action,
                    newPublicKey = it.newPublicKey,
                    keyVersion = it.keyVersion,
                    timestamp = it.timestamp
                )
                webSocket?.send(protocol.serialize(responseMessage))
            }
            
            // 轮换完成后，重新协商新的会话密钥
            if (message.action == "complete" || response?.action == "complete") {
                // 通知密钥轮换完成，需要重新进行密钥协商
                scope.launch {
                    _events.emit(WebSocketEvent.KeyRotationCompleted(currentKeyVersion, message.keyVersion))
                }
            }
        }
    }
    
    /**
     * 触发密钥轮换
     */
    fun triggerKeyRotation(): Boolean {
        val sessionId = config?.sessionId ?: return false
        
        Logger.i(tag, "触发密钥轮换")
        
        keyRotationManager?.let { manager ->
            val handshake = manager.generateRotationHandshake(sessionId)
            _keyRotationState.value = manager.rotationState.value
            
            val message = BteloMessage.KeyRotation(
                action = handshake.action,
                newPublicKey = handshake.newPublicKey,
                keyVersion = handshake.keyVersion,
                timestamp = handshake.timestamp
            )
            
            return webSocket?.send(protocol.serialize(message)) ?: false
        }
        
        return false
    }
    
    /**
     * 启动密钥轮换检查（定时检查是否需要轮换）
     */
    private fun startKeyRotationCheck() {
        val sessionId = config?.sessionId ?: return
        
        keyRotationCheckJob = scope.launch {
            while (true) {
                delay(60 * 1000) // 每分钟检查一次
                
                keyRotationManager?.let { manager ->
                    if (manager.shouldRotate(sessionId)) {
                        Logger.i(tag, "触发定时密钥轮换")
                        triggerKeyRotation()
                    }
                }
            }
        }
    }
    
    /**
     * 获取当前密钥版本
     */
    fun getCurrentKeyVersion(): Int = currentKeyVersion
    
    private fun scheduleReconnect() {
        val config = config ?: return
        val reconnectConfig = config.reconnectConfig
        
        if (reconnectAttempt >= reconnectConfig.maxAttempts) {
            Logger.w(tag, "达到最大重连次数，停止重连")
            scope.launch {
                _events.emit(WebSocketEvent.ReconnectFailed(config.sessionId, "达到最大重连次数"))
            }
            return
        }
        
        // 计算延迟时间（指数退避 + 抖动）
        val baseDelay = minOf(
            reconnectConfig.initialDelayMs * (reconnectConfig.multiplier.pow(reconnectAttempt.toDouble())).toLong(),
            reconnectConfig.maxDelayMs
        )
        
        val jitter = baseDelay * reconnectConfig.jitterPercent / 100
        val delayMs = baseDelay + Random.nextLong(-jitter, jitter)
        
        reconnectAttempt++
        _connectionState.value = ConnectionState.Reconnecting(reconnectAttempt)
        
        Logger.i(tag, "计划 ${delayMs}ms 后重连 (尝试 $reconnectAttempt)")
        
        reconnectJob = scope.launch {
            _events.emit(WebSocketEvent.Reconnecting(config.sessionId, reconnectAttempt, delayMs))
            delay(delayMs)
            
            // 检查网络状态
            if (networkMonitor.isCurrentlyConnected()) {
                startConnection()
            } else {
                // 网络不可用，等待网络恢复
                networkMonitor.isConnected.collect { isConnected ->
                    if (isConnected) {
                        startConnection()
                        return@collect
                    }
                }
            }
        }
    }
    
    fun send(message: BteloMessage): Boolean {
        val messageToSend = if (isEncrypted && cipher != null && message is BteloMessage.Command) {
            try {
                val encryptedData = cryptoManager.encrypt(
                    message.content.toByteArray(), cipher!!
                )
                val encryptedBase64 = android.util.Base64.encodeToString(
                    encryptedData, android.util.Base64.NO_WRAP
                )
                message.copy(content = encryptedBase64)
            } catch (e: Exception) {
                Logger.e(tag, "消息加密失败", e)
                message
            }
        } else {
            message
        }
        
        val json = protocol.serialize(messageToSend)
        return webSocket?.send(json) ?: false
    }
    
    fun disconnect(): Boolean {
        reconnectJob?.cancel()
        keyRotationCheckJob?.cancel()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
        _keyRotationState.value = KeyRotationState.Idle
        keyPair = null
        cipher = null
        isEncrypted = false
        currentKeyVersion = 1
        return true
    }
    
    fun destroy() {
        disconnect()
        scope.cancel()
    }
    
    private fun Double.pow(n: Double): Double = Math.pow(this, n)
}
