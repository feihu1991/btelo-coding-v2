package com.btelo.coding.domain.repository

import com.btelo.coding.data.remote.websocket.factory.ConnectionState
import com.btelo.coding.domain.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MessageRepository {
    fun connect(serverAddress: String, token: String, sessionId: String)
    fun getMessages(sessionId: String): Flow<List<Message>>
    suspend fun sendMessage(sessionId: String, content: String): Result<Unit>
    fun observeOutput(sessionId: String): Flow<Message>
    fun disconnect(sessionId: String)
    val connectionState: StateFlow<ConnectionState>
    suspend fun cleanOldMessages(sessionId: String, keepDays: Int = 30)
}
