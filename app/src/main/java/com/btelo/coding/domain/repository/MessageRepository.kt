package com.btelo.coding.domain.repository

import com.btelo.coding.data.remote.websocket.factory.ConnectionState
import com.btelo.coding.domain.model.ActiveTurnState
import com.btelo.coding.domain.model.BridgeControlActionResult
import com.btelo.coding.domain.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MessageRepository {
    fun connect(serverAddress: String, token: String, sessionId: String)
    fun getMessages(sessionId: String): Flow<List<Message>>
    suspend fun getLastMessage(sessionId: String): Message?
    suspend fun sendMessage(sessionId: String, content: String): Result<Unit>
    suspend fun sendBridgeControl(sessionId: String, action: String): Result<Unit>
    fun observeOutput(sessionId: String): Flow<Message>
    
    /**
     * Observe structured output messages (BTELO Coding v2)
     * These are parsed Claude Code stream-json outputs with type classification
     */
    fun observeStructuredOutput(sessionId: String): Flow<Message>
    
    /**
     * Save a message directly (for structured output buffering)
     */
    suspend fun saveMessage(message: Message)
    
    fun disconnect(sessionId: String)
    val connectionState: StateFlow<ConnectionState>
    val activeTurnState: StateFlow<ActiveTurnState>
    val bridgeControlResults: Flow<BridgeControlActionResult>
    suspend fun cleanOldMessages(sessionId: String, keepDays: Int = 30)
}
