package com.btelo.coding.data.remote.websocket.factory

import com.btelo.coding.data.remote.encryption.CryptoManager
import com.btelo.coding.data.remote.encryption.KeyPair
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
 * 增强的WebSocket客户端，支持重连机制和心跳保活
 */
class EnhancedWebSocketClient(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val cryptoManager: CryptoManager,
    private val networkMonitor: NetworkMonitor,
    private val secureKeyStore: com.btelo.coding.data.remote.encryption.SecureKeyStore
) {
    private val tag = "EnhancedWebSocket"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var webSocket: WebSocket? = null
    private var config: WebSocketConfig? = null
    private var reconnectJob: Job? = null
    private var pingJob: Job? = null
    
    private var keyPair: KeyPair? = null
    private var cipher: com.google.crypto.tink.subtle.ChaCha20Poly1305? = null
    private var isEncrypted = false
    
    private val protocol = MessageProtocol(gson)
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _messages = MutableSharedFlow<BteloMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<BteloMessage> = _messages.asSharedFlow()
    
    private val _events = MutableSharedFlow<WebSocketEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<WebSocketEvent> = _events.asSharedFlow()
    
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
            .replace("https://", "wss://") + "/ws"
        
        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("Authorization", "Bearer ${config.token}")
            .build()
        
        // 尝试使用SecureKeyStore获取或生成密钥对
        keyPair = secureKeyStore.getKeyPair(config.sessionId) ?: secureKeyStore.generateAndStoreKeyPair(config.sessionId)
        cipher = null
        isEncrypted = false
        return true
        
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Logger.i(tag, "WebSocket连接成功")
                reconnectAttempt = 0
                _connectionState.value = ConnectionState.Connected
                
                // 发送公钥
                val publicKeyBase64 = android.util.Base64.encodeToString(
                    keyPair!!.publicKey, android.util.Base64.NO_WRAP
                )
                webSocket.send(protocol.serialize(BteloMessage.PublicKey(publicKeyBase64)))
                
                // 启动心跳
                startPingPong(config.pingIntervalMs)
                
                scope.launch {
                    _events.emit(WebSocketEvent.Connected(config.sessionId))
                }
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
                stopPingPong()
                
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
            is BteloMessage.Status -> _messages.tryEmit(message)
        }
    }
    
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
    
    private fun startPingPong(intervalMs: Long) {
        pingJob = scope.launch {
            while (true) {
                delay(intervalMs)
                webSocket?.send(protocol.serialize(BteloMessage.Command("ping", com.btelo.coding.data.remote.websocket.InputType.TEXT)))
            }
        }
    }
    
    private fun stopPingPong() {
        pingJob?.cancel()
        pingJob = null
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
        stopPingPong()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
        keyPair = null
        cipher = null
        isEncrypted = false
        return true
    }
    
    fun destroy() {
        disconnect()
        scope.cancel()
    }
    
    private fun Double.pow(n: Double): Double = Math.pow(this, n)
}
