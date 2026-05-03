package com.btelo.coding.data.repository

import com.btelo.coding.data.local.EntityMappers.toDomain
import com.btelo.coding.data.local.EntityMappers.toEntity
import com.btelo.coding.data.local.EntityMappers.toMessageList
import com.btelo.coding.data.local.dao.MessageDao
import com.btelo.coding.data.local.dao.SessionDao
import com.btelo.coding.data.local.entity.MessageEntity
import com.btelo.coding.data.remote.websocket.BteloMessage
import com.btelo.coding.data.remote.websocket.HookEventType
import com.btelo.coding.data.remote.websocket.InputType
import com.btelo.coding.data.remote.websocket.OutputType
import com.btelo.coding.data.remote.websocket.factory.ConnectionState
import com.btelo.coding.data.remote.websocket.factory.ReconnectConfig
import com.btelo.coding.data.remote.websocket.factory.WebSocketClientFactory
import com.btelo.coding.data.remote.websocket.factory.WebSocketConfig
import com.btelo.coding.data.remote.websocket.factory.WebSocketEvent
import com.btelo.coding.domain.model.Message
import com.btelo.coding.domain.model.MessageMetadata
import com.btelo.coding.domain.model.MessageType
import com.btelo.coding.domain.model.OutputType as DomainOutputType
import com.btelo.coding.domain.model.SessionAttentionType
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val sessionDao: SessionDao,
    private val webSocketFactory: WebSocketClientFactory
) : MessageRepository {

    private val tag = "MessageRepository"
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _structuredOutputFlow = MutableSharedFlow<Message>(extraBufferCapacity = 64)

    override fun connect(serverAddress: String, token: String, sessionId: String) {
        if (_currentSessionId.value == sessionId && webSocketFactory.hasClient(sessionId)) {
            Logger.d(tag, "Skipping duplicate connect for session: $sessionId")
            return
        }

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

        scope.launch {
            client.connectionState.collect { state ->
                _connectionState.value = state
                Logger.d(tag, "Connection state changed: $state")
            }
        }

        scope.launch {
            client.events.collect { event ->
                when (event) {
                    is WebSocketEvent.Connected -> {
                        Logger.i(tag, "WebSocket connected, sending select_session: $sessionId")
                        client.send(BteloMessage.SelectSession(sessionId = sessionId))
                    }

                    else -> Unit
                }
            }
        }

        scope.launch {
            client.messages.collect { message ->
                handleMessage(message, sessionId)
            }
        }

        Logger.i(tag, "Connected session $sessionId to $serverAddress")
    }

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
                    Logger.i(tag, "Received history sync: ${message.messages.size} messages")
                    val effectiveSessionId = message.sessionId.ifBlank { sessionId }
                    val entities = message.messages.map { historyMessage ->
                        MessageEntity(
                            id = historyMessage.id,
                            sessionId = effectiveSessionId,
                            content = historyMessage.content,
                            type = if (historyMessage.isFromUser) "COMMAND" else "OUTPUT",
                            timestamp = historyMessage.timestamp,
                            isFromUser = historyMessage.isFromUser
                        )
                    }
                    safeInsert("SyncHistory") {
                        messageDao.deleteMessagesBySessionId(effectiveSessionId)
                        if (entities.isNotEmpty()) {
                            messageDao.insertMessages(entities)
                        }
                    }
                }

                is BteloMessage.NewMessage -> {
                    Logger.i(tag, "Received live message sync: ${message.message.content.take(50)}")
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

                is BteloMessage.StructuredOutput -> {
                    Logger.d(tag, "Received structured output: ${message.outputType}")

                    val domainOutputType = when (message.outputType) {
                        OutputType.CLAUDE_RESPONSE -> DomainOutputType.CLAUDE_RESPONSE
                        OutputType.TOOL_CALL -> DomainOutputType.TOOL_CALL
                        OutputType.FILE_OP -> DomainOutputType.FILE_OP
                        OutputType.THINKING -> DomainOutputType.THINKING
                        OutputType.ERROR -> DomainOutputType.ERROR
                        OutputType.SYSTEM -> DomainOutputType.SYSTEM
                    }

                    val metadata = message.metadata.let { metadata ->
                        MessageMetadata(
                            toolId = metadata.toolId,
                            toolName = metadata.toolName,
                            toolType = metadata.toolType,
                            filePath = metadata.filePath,
                            command = metadata.command,
                            isFileOp = metadata.isFileOp,
                            fileOpType = metadata.fileOpType,
                            isToolResult = metadata.isToolResult,
                            isCollapsed = metadata.isCollapsed,
                            originalLength = metadata.originalLength,
                            errorCode = metadata.errorCode,
                            errorDetails = metadata.errorDetails
                        )
                    }

                    val structuredMessage = Message(
                        id = message.id?.takeIf { it.isNotBlank() }
                            ?: stableStructuredMessageId(message),
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

                    _structuredOutputFlow.emit(structuredMessage)
                    safeInsert("StructuredOutput") { messageDao.insertMessage(structuredMessage.toEntity()) }
                }

                is BteloMessage.HookEvent -> {
                    handleHookEvent(sessionId, message)
                }

                is BteloMessage.SessionState -> {
                    Logger.d(
                        tag,
                        "Session state mobile=${message.mobileConnected}, bridge=${message.bridgeConnected}"
                    )
                    sessionDao.updateConnectionStatus(sessionId, message.bridgeConnected)
                }

                else -> Unit
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
            val sent = webSocketFactory.sendMessage(
                sessionId,
                BteloMessage.Command(content = content, type = InputType.TEXT)
            )

            if (sent) {
                sessionDao.updateAttention(sessionId, null, "", "", null)
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("WebSocket is not connected for session $sessionId"))
            }
        } catch (e: Exception) {
            Logger.e(tag, "Failed to send message", e)
            Result.failure(e)
        }
    }

    override suspend fun sendPermissionDecision(sessionId: String, decision: String): Result<Unit> {
        return try {
            val sent = webSocketFactory.sendMessage(
                sessionId,
                BteloMessage.PermissionResponse(sessionId = sessionId, decision = decision)
            )

            if (sent) {
                sessionDao.updateAttention(sessionId, null, "", "", null)
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("WebSocket is not connected for session $sessionId"))
            }
        } catch (e: Exception) {
            Logger.e(tag, "Failed to send permission response", e)
            Result.failure(e)
        }
    }

    override fun observeOutput(sessionId: String): Flow<Message> {
        var lastCount = 0
        return messageDao.getMessagesBySessionId(sessionId).map { entities ->
            val domainList = entities.toMessageList()
            if (domainList.size > lastCount) {
                val newMessages = domainList.subList(lastCount, domainList.size)
                lastCount = domainList.size
                val combined = newMessages
                    .filter { !it.isFromUser }
                    .joinToString("") { it.content }
                if (combined.isNotEmpty()) {
                    Message(
                        id = "stream",
                        sessionId = sessionId,
                        content = combined,
                        type = MessageType.OUTPUT,
                        timestamp = System.currentTimeMillis(),
                        isFromUser = false
                    )
                } else {
                    null
                }
            } else {
                null
            }
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

    override fun disconnect(sessionId: String) {
        webSocketFactory.destroy(sessionId)
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        _currentSessionId.value = null
        _connectionState.value = ConnectionState.Disconnected
        Logger.i(tag, "Disconnected session: $sessionId")
    }

    override suspend fun cleanOldMessages(sessionId: String, keepDays: Int) {
        val cutoffTimestamp = System.currentTimeMillis() - (keepDays * 24 * 60 * 60 * 1000L)
        messageDao.deleteOldMessages(cutoffTimestamp)
        Logger.i(tag, "Cleaned old messages for $sessionId, keeping $keepDays days")
    }

    private suspend fun safeInsert(operationTag: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Logger.e(tag, "[$operationTag] Insert failed: ${e.message}", e)
        }
    }

    private suspend fun handleHookEvent(sessionId: String, message: BteloMessage.HookEvent) {
        val title: String
        val body: String
        val attentionType: SessionAttentionType

        when (message.eventType) {
            HookEventType.WAITING_INPUT -> {
                attentionType = SessionAttentionType.WAITING_INPUT
                title = "Waiting for your input"
                body = (message.data["message"] as? String).orEmpty().ifBlank {
                    "The assistant paused and needs your next instruction."
                }
            }

            HookEventType.PERMISSION_REQUEST -> {
                attentionType = SessionAttentionType.PERMISSION_REQUEST
                val toolName = (message.data["tool_name"] as? String).orEmpty().ifBlank { "a tool" }
                title = "Permission needed for $toolName"
                body = (message.data["tool_input"] as? String).orEmpty().ifBlank {
                    "Resume this session to review and approve the request."
                }
            }

            HookEventType.TASK_COMPLETE -> {
                attentionType = SessionAttentionType.TASK_COMPLETE
                title = (message.data["query"] as? String).orEmpty().ifBlank { "Task completed" }
                body = (message.data["response"] as? String).orEmpty().ifBlank {
                    "The assistant finished running on your desktop."
                }
            }

            HookEventType.PROMPT_SUBMITTED,
            HookEventType.SESSION_START,
            HookEventType.TOOL_COMPLETED -> {
                sessionDao.updateAttention(sessionId, null, "", "", null)
                return
            }
        }

        sessionDao.updateAttention(
            sessionId = sessionId,
            attentionType = attentionType.name,
            attentionTitle = title.take(160),
            attentionBody = body.take(500),
            attentionUpdatedAt = message.timestamp
        )
    }

    private fun stableStructuredMessageId(message: BteloMessage.StructuredOutput): String {
        val fingerprint = listOf(
            message.outputType.name,
            message.timestamp,
            message.content
        ).joinToString("|").hashCode()
        return "struct-$fingerprint"
    }
}
