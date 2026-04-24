package com.btelo.coding.data.repository

import com.btelo.coding.data.local.EntityMappers.toDomain
import com.btelo.coding.data.local.EntityMappers.toEntity
import com.btelo.coding.data.local.EntityMappers.toMessageList
import com.btelo.coding.data.local.dao.MessageDao
import com.btelo.coding.data.remote.websocket.BteloMessage
import com.btelo.coding.data.remote.websocket.InputType
import com.btelo.coding.data.remote.websocket.factory.ConnectionState
import com.btelo.coding.data.remote.websocket.factory.WebSocketClientFactory
import com.btelo.coding.data.remote.websocket.factory.WebSocketConfig
import com.btelo.coding.data.remote.websocket.factory.ReconnectConfig
import com.btelo.coding.domain.model.Message
import com.btelo.coding.domain.model.MessageType
import com.btelo.coding.domain.repository.MessageRepository
import com.btelo.coding.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val webSocketFactory: WebSocketClientFactory
) : MessageRepository {
    
    private val tag = "MessageRepository"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 当前连接的会话ID
    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()
    
    // 连接状态
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override fun connect(serverAddress: String, token: String, sessionId: String) {
        _currentSessionId.value = sessionId
        
        val config = WebSocketConfig(
            sessionId = sessionId,
            serverAddress = serverAddress,
            token = token,
            reconnectConfig = ReconnectConfig(
                initialDelayMs = 1000L,
                maxDelayMs = 30000L,
                maxAttempts = 10,
                multiplier = 2.0,
                jitterPercent = 20
            ),
            pingIntervalMs = 30000L,
            pongTimeoutMs = 10000L
        )
        
        val client = webSocketFactory.getOrCreate(config)
        
        // 监听连接状态
        scope.launch {
            client.connectionState.collect { state ->
                _connectionState.value = state
                Logger.d(tag, "连接状态变化: $state")
            }
        }
        
        // 监听消息并持久化
        scope.launch {
            client.messages.collect { message ->
                if (message is BteloMessage.Output) {
                    val domainMessage = Message(
                        id = java.util.UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        content = message.data,
                        type = when (message.stream) {
                            com.btelo.coding.data.remote.websocket.StreamType.STDERR -> MessageType.ERROR
                            else -> MessageType.OUTPUT
                        },
                        timestamp = System.currentTimeMillis(),
                        isFromUser = false
                    )
                    messageDao.insertMessage(domainMessage.toEntity())
                }
            }
        }
        
        Logger.i(tag, "连接会话: $sessionId 到 $serverAddress")
    }

    override fun getMessages(sessionId: String): Flow<List<Message>> {
        return messageDao.getMessagesBySessionId(sessionId).map { entities ->
            entities.toMessageList()
        }
    }

    override suspend fun sendMessage(sessionId: String, content: String): Result<Unit> {
        return try {
            // 保存用户消息到数据库
            val userMessage = Message(
                id = java.util.UUID.randomUUID().toString(),
                sessionId = sessionId,
                content = content,
                type = MessageType.COMMAND,
                timestamp = System.currentTimeMillis(),
                isFromUser = true
            )
            messageDao.insertMessage(userMessage.toEntity())
            
            // 发送WebSocket消息
            webSocketFactory.sendMessage(
                sessionId,
                BteloMessage.Command(content = content, type = InputType.TEXT)
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(tag, "发送消息失败", e)
            Result.failure(e)
        }
    }

    override fun observeOutput(sessionId: String): Flow<Message> {
        return messageDao.getMessagesBySessionId(sessionId).map { entities ->
            entities.lastOrNull()?.toDomain()
        }.mapNotNull { it }
    }
    
    override fun disconnect(sessionId: String) {
        webSocketFactory.destroy(sessionId)
        _currentSessionId.value = null
        _connectionState.value = ConnectionState.Disconnected
        Logger.i(tag, "断开会话: $sessionId")
    }
    
    // 移除显式的 getConnectionState 方法，因为 val connectionState 会自动生成 getter
    
    /**
     * 清理旧的聊天记录（保留最近的消息）
     */
    override suspend fun cleanOldMessages(sessionId: String, keepDays: Int) {
        val cutoffTimestamp = System.currentTimeMillis() - (keepDays * 24 * 60 * 60 * 1000L)
        messageDao.deleteOldMessages(cutoffTimestamp)
        Logger.i(tag, "清理会话 $sessionId 的旧消息 (保留 $keepDays 天)")
    }
}
