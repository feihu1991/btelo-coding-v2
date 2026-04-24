package com.btelo.coding.data.repository

import com.btelo.coding.data.remote.websocket.BteloMessage
import com.btelo.coding.data.remote.websocket.BteloWebSocketClient
import com.btelo.coding.data.remote.websocket.InputType
import com.btelo.coding.domain.model.Message
import com.btelo.coding.domain.model.MessageType
import com.btelo.coding.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val webSocketClient: BteloWebSocketClient
) : MessageRepository {

    private val messages = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    private var isConnected = false

    override fun connect(serverAddress: String, token: String) {
        if (!isConnected) {
            val wsUrl = serverAddress.replace("http://", "ws://").replace("https://", "wss://") + "/ws"
            webSocketClient.connect(wsUrl, token)
            isConnected = true
        }
    }

    override fun getMessages(sessionId: String): Flow<List<Message>> {
        return messages.asStateFlow().map { it[sessionId] ?: emptyList() }
    }

    override suspend fun sendMessage(sessionId: String, content: String): Result<Unit> {
        return try {
            val message = Message(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                content = content,
                type = MessageType.COMMAND,
                timestamp = System.currentTimeMillis(),
                isFromUser = true
            )
            addMessage(sessionId, message)

            webSocketClient.send(
                BteloMessage.Command(
                    content = content,
                    type = InputType.TEXT
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeOutput(sessionId: String): Flow<Message> {
        return webSocketClient.messages
            .filterIsInstance<BteloMessage.Output>()
            .map { bteloMessage ->
                val message = Message(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    content = bteloMessage.data,
                    type = MessageType.OUTPUT,
                    timestamp = System.currentTimeMillis(),
                    isFromUser = false
                )
                addMessage(sessionId, message)
                message
            }
    }

    private fun addMessage(sessionId: String, message: Message) {
        val currentMessages = messages.value.toMutableMap()
        val sessionMessages = currentMessages[sessionId]?.toMutableList() ?: mutableListOf()
        sessionMessages.add(message)
        currentMessages[sessionId] = sessionMessages
        messages.value = currentMessages
    }
}
