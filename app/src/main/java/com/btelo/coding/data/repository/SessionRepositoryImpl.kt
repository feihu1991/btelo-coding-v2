package com.btelo.coding.data.repository

import com.btelo.coding.data.local.EntityMappers.toDomain
import com.btelo.coding.data.local.EntityMappers.toEntity
import com.btelo.coding.data.local.EntityMappers.toSessionList
import com.btelo.coding.data.local.dao.SessionDao
import com.btelo.coding.domain.model.Session
import com.btelo.coding.domain.repository.SessionRepository
import com.btelo.coding.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao
) : SessionRepository {
    
    private val tag = "SessionRepository"

    override fun getSessions(): Flow<List<Session>> {
        return sessionDao.getAllSessions().map { entities ->
            entities.toSessionList()
        }
    }

    override suspend fun createSession(tool: String): Session {
        // 使用高效的计数查询获取当前会话数量
        val sessionCount = sessionDao.getSessionCount()

        val session = Session(
            id = java.util.UUID.randomUUID().toString(),
            name = "会话 ${sessionCount + 1}",
            tool = tool,
            createdAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis(),
            isConnected = false
        )

        sessionDao.insertSession(session.toEntity())
        Logger.i(tag, "创建会话: ${session.id}")

        return session
    }

    override suspend fun deleteSession(sessionId: String) {
        sessionDao.deleteSessionById(sessionId)
        Logger.i(tag, "删除会话: $sessionId")
    }

    override fun getSession(sessionId: String): Flow<Session?> {
        return sessionDao.getSessionById(sessionId).map { entity ->
            entity?.toDomain()
        }
    }
    
    override suspend fun updateSessionConnection(sessionId: String, isConnected: Boolean) {
        sessionDao.updateConnectionStatus(sessionId, isConnected)
    }
    
    override suspend fun updateSessionLastActive(sessionId: String) {
        sessionDao.updateLastActiveTime(sessionId, System.currentTimeMillis())
    }
}
