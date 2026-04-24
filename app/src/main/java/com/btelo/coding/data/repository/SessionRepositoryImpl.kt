package com.btelo.coding.data.repository

import com.btelo.coding.domain.model.Session
import com.btelo.coding.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor() : SessionRepository {

    private val sessions = MutableStateFlow<List<Session>>(emptyList())

    override fun getSessions(): Flow<List<Session>> = sessions.asStateFlow()

    override suspend fun createSession(tool: String): Session {
        val session = Session(
            id = UUID.randomUUID().toString(),
            name = "会话 ${sessions.value.size + 1}",
            tool = tool,
            createdAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis(),
            isConnected = false
        )
        sessions.value = sessions.value + session
        return session
    }

    override suspend fun deleteSession(sessionId: String) {
        sessions.value = sessions.value.filter { it.id != sessionId }
    }

    override fun getSession(sessionId: String): Flow<Session?> {
        return sessions.asStateFlow().map { list ->
            list.find { it.id == sessionId }
        }
    }
}
