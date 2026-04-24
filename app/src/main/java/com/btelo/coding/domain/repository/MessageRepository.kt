package com.btelo.coding.domain.repository

import com.btelo.coding.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun connect(serverAddress: String, token: String)
    fun getMessages(sessionId: String): Flow<List<Message>>
    suspend fun sendMessage(sessionId: String, content: String): Result<Unit>
    fun observeOutput(sessionId: String): Flow<Message>
}
