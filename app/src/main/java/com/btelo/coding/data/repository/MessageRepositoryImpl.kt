package com.btelo.coding.data.repository

import com.btelo.coding.data.local.EntityMappers.toDomain
import com.btelo.coding.data.local.EntityMappers.toEntity
import com.btelo.coding.data.local.EntityMappers.toMessageList
import com.btelo.coding.data.local.dao.MessageDao
import com.btelo.coding.data.local.entity.MessageEntity
import com.btelo.coding.data.remote.websocket.BteloMessage
import com.btelo.coding.data.remote.websocket.InputType
import com.btelo.coding.data.remote.websocket.OutputType
import com.btelo.coding.data.remote.websocket.factory.ConnectionState
import com.btelo.coding.data.remote.websocket.factory.WebSocketClientFactory
import com.btelo.coding.data.remote.websocket.factory.WebSocketConfig
import com.btelo.coding.data.remote.websocket.factory.ReconnectConfig
import com.btelo.coding.data.remote.websocket.factory.WebSocketEvent
import com.btelo.coding.domain.model.Message
import com.btelo.coding.domain.model.MessageMetadata
import com.btelo.coding.domain.model.MessageType
import com.btelo.coding.domain.model.OutputType as DomainOutputType
import com.btelo.coding.domain.repository.MessageRepository
import com.btelo.coding.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 当前连接的会话ID
    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()
    
    // 连接状态
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    // Structured output flow for BTELO Coding v2
    private val _structuredOutputFlow = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    
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

        // 监听连接事件，连接成功后发送 select_session
        scope.launch {
            client.events.collect { event ->
                when (event) {
                    is WebSocketEvent.Connected -> {
                        Logger.i(tag, "WebSocket 已连接，发送 select_session: $sessionId")
                        client.send(BteloMessage.SelectSession(sessionId = sessionId))
                    }
                    else -> { /* ignore */ }
                }
            }
        }

        // 监听消息并持久化
        scope.launch {
            client.messages.collect { message ->
                handleMessage(message, sessionId)
            }
        }
        
        Logger.i(tag, "连接会话: $sessionId 到 $serverAddress")
    }
    
    /**
     * Handle incoming WebSocket messages
     * Dispatches to appropriate handlers based on message type
     */
    private suspend fun handleMessage(message: BteloMessage, sessionId: String) {
        try {
        when (message) {
            is BteloMessage.Output -> {
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
                safeInsert("Output") { messageDao.insertMessage(domainMessage.toEntity()) }
            }
            
            is BteloMessage.SyncHistory -> {
                // Bulk insert history from Claude Code session
                Logger.i(tag, "收到历史同步: ${message.messages.size} 条消息")
                val entities = message.messages.map { hm ->
                    MessageEntity(
                        id = hm.id,
                        sessionId = message.sessionId,
                        content = hm.content,
                        type = if (hm.isFromUser) "COMMAND" else "OUTPUT",
                        timestamp = hm.timestamp,
                        isFromUser = hm.isFromUser
                    )
                }
                safeInsert("SyncHistory") { messageDao.insertMessages(entities) }
            }
            
            is BteloMessage.NewMessage -> {
                // Real-time new message from JSONL file watcher
                Logger.i(tag, "收到新消息同步: ${message.message.content.take(50)}")
                val entity = MessageEntity(
                    id = message.message.id,
                    sessionId = message.sessionId,
                    content = message.message.content,
                    type = if (message.message.isFromUser) "COMMAND" else "OUTPUT",
                    timestamp = message.message.timestamp,
                    isFromUser = message.message.isFromUser
                )
                safeInsert("NewMessage") { messageDao.insertMessage(entity) }
            }

            // BTELO Coding v2: Structured Output handling
            is BteloMessage.StructuredOutput -> {
                Logger.d(tag, "收到结构化输出: ${message.outputType}")
                
                val domainOutputType = when (message.outputType) {
                    OutputType.CLAUDE_RESPONSE -> DomainOutputType.CLAUDE_RESPONSE
                    OutputType.TOOL_CALL -> DomainOutputType.TOOL_CALL
                    OutputType.FILE_OP -> DomainOutputType.FILE_OP
                    OutputType.THINKING -> DomainOutputType.THINKING
                    OutputType.ERROR -> DomainOutputType.ERROR
                    OutputType.SYSTEM -> DomainOutputType.SYSTEM
                }
                
                val metadata = message.metadata?.let { m ->
                    MessageMetadata(
                        toolId = m.toolId,
                        toolName = m.toolName,
                        toolType = m.toolType,
                        filePath = m.filePath,
                        command = m.command,
                        isFileOp = m.isFileOp,
                        fileOpType = m.fileOpType,
                        isToolResult = m.isToolResult,
                        isCollapsed = m.isCollapsed,
                        originalLength = m.originalLength,
                        errorCode = m.errorCode,
                        errorDetails = m.errorDetails
                    )
                }
                
                val structuredMessage = Message(
                    id = "struct-${System.currentTimeMillis()}-${java.util.UUID.randomUUID().toString().take(4)}",
                    sessionId = sessionId,
                    content = message.content,
                    type = when (message.outputType) {
                        OutputType.ERROR -> MessageType.ERROR
                        OutputType.TOOL_CALL -> MessageType.TOOL
                        OutputType.THINKING -> MessageType.THINKING
                        else -> MessageType.OUTPUT
                    },
                    timestamp = message.timestamp.toLongOrNull() ?: System.currentTimeMillis(),
                    isFromUser = false,
                    outputType = domainOutputType,
                    metadata = metadata,
                    thinkingContent = if (message.outputType == OutputType.THINKING) message.content else null
                )
                
                // Emit to structured output flow
                _structuredOutputFlow.emit(structuredMessage)
                
                // Also save to database for persistence
                safeInsert("StructuredOutput") { messageDao.insertMessage(structuredMessage.toEntity()) }
            }
            
            is BteloMessage.SessionState -> {
                // Connection state broadcast (logged for debugging)
                Logger.d(tag, "会话状态: mobile=${message.mobileConnected}, bridge=${message.bridgeConnected}")
            }
            
            else -> { /* ignore other message types */ }
        }
        } catch (e: Exception) {
            Logger.e(tag, "handleMessage error (sessionId=$sessionId): ${e.message}", e)
        }
    }

    override fun getMessages(sessionId: String): Flow<List<Message>> {
        return messageDao.getMessagesBySessionId(sessionId).map { entities ->
            entities.toMessageList()
        }
    }

    override suspend fun getLastMessage(sessionId: String): Message? {
        return messageDao.getLastMessage(sessionId)?.toDomain()
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
            safeInsert("UserMessage") { messageDao.insertMessage(userMessage.toEntity()) }
            
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
        // Track the last seen message count to only emit new messages
        var lastCount = 0
        return messageDao.getMessagesBySessionId(sessionId).map { entities ->
            val domainList = entities.toMessageList()
            if (domainList.size > lastCount) {
                // Only return the new messages since last emission
                val newMessages = domainList.subList(lastCount, domainList.size)
                lastCount = domainList.size
                // Combine new output messages into one
                val combined = newMessages
                    .filter { !it.isFromUser }
                    .joinToString("") { it.content }
                if (combined.isNotEmpty()) {
                    Message(
                        id = "stream",
                        sessionId = sessionId,
                        content = combined,
                        type = com.btelo.coding.domain.model.MessageType.OUTPUT,
                        timestamp = System.currentTimeMillis(),
                        isFromUser = false
                    )
                } else null
            } else null
        }.mapNotNull { it }
    }
    
    override fun observeStructuredOutput(sessionId: String): Flow<Message> {
        return _structuredOutputFlow
            .map { it.copy(sessionId = sessionId) }
            .mapNotNull { it }
    }
    
    override suspend fun saveMessage(message: Message) {
        safeInsert("SaveMessage") { messageDao.insertMessage(message.toEntity()) }
    }

    private suspend fun safeInsert(tag: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Logger.e(this.tag, "[$tag] Insert failed: ${e.message}", e)
        }
    }
    
    override fun disconnect(sessionId: String) {
        webSocketFactory.destroy(sessionId)
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        _currentSessionId.value = null
        _connectionState.value = ConnectionState.Disconnected
        Logger.i(tag, "断开会话: $sessionId")
    }
    
    /**
     * 清理旧的聊天记录（保留最近的消息）
     */
    override suspend fun cleanOldMessages(sessionId: String, keepDays: Int) {
        val cutoffTimestamp = System.currentTimeMillis() - (keepDays * 24 * 60 * 60 * 1000L)
        messageDao.deleteOldMessages(cutoffTimestamp)
        Logger.i(tag, "清理会话 $sessionId 的旧消息 (保留 $keepDays 天)")
    }
}
