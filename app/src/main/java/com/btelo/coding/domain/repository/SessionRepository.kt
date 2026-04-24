package com.btelo.coding.domain.repository

import com.btelo.coding.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getSessions(): Flow<List<Session>>
    suspend fun createSession(tool: String): Session
    suspend fun deleteSession(sessionId: String)
    fun getSession(sessionId: String): Flow<Session?>
    suspend fun updateSessionConnection(sessionId: String, isConnected: Boolean)
    suspend fun updateSessionLastActive(sessionId: String)
}
