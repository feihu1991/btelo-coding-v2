package com.btelo.coding.data.remote.websocket.factory

import com.btelo.coding.data.remote.encryption.CryptoManager
import com.btelo.coding.data.remote.network.NetworkMonitor
import com.btelo.coding.data.remote.websocket.BteloMessage
import com.btelo.coding.util.Logger
import com.google.gson.Gson
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebSocket客户端工厂
 * 管理多个会话的WebSocket连接
 */
@Singleton
class WebSocketClientFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val cryptoManager: CryptoManager,
    private val networkMonitor: NetworkMonitor,
    private val secureKeyStore: com.btelo.coding.data.remote.encryption.SecureKeyStore
) {
    private val tag = "WebSocketFactory"
    
    // 存储所有活跃的WebSocket客户端
    private val clients = ConcurrentHashMap<String, EnhancedWebSocketClient>()
    
    /**
     * 为指定会话创建或获取WebSocket客户端
     */
    fun getOrCreate(config: WebSocketConfig): EnhancedWebSocketClient {
        return clients.getOrPut(config.sessionId) {
            Logger.i(tag, "创建新的WebSocket客户端: ${config.sessionId}")
            EnhancedWebSocketClient(okHttpClient, gson, cryptoManager, networkMonitor, secureKeyStore).apply {
                connect(config)
            }
        }
    }
    
    /**
     * 检查指定会话是否有活跃的客户端
     */
    fun hasClient(sessionId: String): Boolean {
        return clients.containsKey(sessionId)
    }
    
    /**
     * 获取指定会话的客户端
     */
    fun getClient(sessionId: String): EnhancedWebSocketClient? {
        return clients[sessionId]
    }
    
    /**
     * 获取指定会话的连接状态
     */
    fun getConnectionState(sessionId: String): StateFlow<ConnectionState>? {
        return clients[sessionId]?.connectionState
    }
    
    /**
     * 获取指定会话的消息流
     */
    fun getMessages(sessionId: String): SharedFlow<BteloMessage>? {
        return clients[sessionId]?.messages
    }
    
    /**
     * 向指定会话发送消息
     */
    fun sendMessage(sessionId: String, message: BteloMessage): Boolean {
        return clients[sessionId]?.send(message) ?: run {
            Logger.w(tag, "尝试向不存在的会话发送消息: $sessionId")
            false
        }
    }
    
    /**
     * 断开指定会话的连接
     */
    fun disconnect(sessionId: String): Boolean {
        return clients[sessionId]?.let { client ->
            Logger.i(tag, "断开会话: $sessionId")
            client.disconnect()
            clients.remove(sessionId)
            true
        } ?: false
    }
    
    /**
     * 销毁指定会话的客户端并清理资源
     */
    fun destroy(sessionId: String) {
        clients[sessionId]?.let { client ->
            Logger.i(tag, "销毁会话客户端: $sessionId")
            client.destroy()
            clients.remove(sessionId)
        }
    }
    
    /**
     * 断开所有连接并清理
     */
    fun destroyAll() {
        Logger.i(tag, "销毁所有WebSocket客户端")
        clients.forEach { (sessionId, client) ->
            client.destroy()
        }
        clients.clear()
    }
    
    /**
     * 获取所有活跃会话ID
     */
    fun getActiveSessionIds(): Set<String> {
        return clients.keys.toSet()
    }
    
    /**
     * 获取活跃客户端数量
     */
    fun getActiveClientCount(): Int {
        return clients.size
    }
}
