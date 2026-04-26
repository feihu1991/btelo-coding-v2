package com.btelo.coding.domain.repository

import com.btelo.coding.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getSessions(): Flow<List<Session>>
    fun searchSessions(query: String): Flow<List<Session>>
    suspend fun createSession(name: String = "Claude", tool: String = "claude"): Session
    suspend fun createSessionWithId(sessionId: String, name: String = "Claude", tool: String = "claude"): Session
    suspend fun deleteSession(sessionId: String)
    fun getSession(sessionId: String): Flow<Session?>
    suspend fun updateSessionConnection(sessionId: String, isConnected: Boolean)
    suspend fun updateSessionLastActive(sessionId: String)
}
