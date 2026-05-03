package com.btelo.coding.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.btelo.coding.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions ORDER BY lastActiveAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun getSessionCount(): Int

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun getSessionById(sessionId: String): Flow<SessionEntity?>
    
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getSessionByIdSync(sessionId: String): SessionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)
    
    @Update
    suspend fun updateSession(session: SessionEntity)
    
    @Delete
    suspend fun deleteSession(session: SessionEntity)
    
    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)
    
    @Query("UPDATE sessions SET isConnected = :isConnected WHERE id = :sessionId")
    suspend fun updateConnectionStatus(sessionId: String, isConnected: Boolean)
    
    @Query("UPDATE sessions SET lastActiveAt = :timestamp WHERE id = :sessionId")
    suspend fun updateLastActiveTime(sessionId: String, timestamp: Long)

    @Query("SELECT * FROM sessions WHERE name LIKE '%' || :query || '%' OR tool LIKE '%' || :query || '%' ORDER BY lastActiveAt DESC")
    fun searchSessions(query: String): Flow<List<SessionEntity>>

    @Query("UPDATE sessions SET tokenCount = :count WHERE id = :sessionId")
    suspend fun updateTokenCount(sessionId: String, count: Int)

    @Query("UPDATE sessions SET messageCount = :count WHERE id = :sessionId")
    suspend fun updateMessageCount(sessionId: String, count: Int)

    @Query("""
        UPDATE sessions
        SET attentionType = :attentionType,
            attentionTitle = :attentionTitle,
            attentionBody = :attentionBody,
            attentionUpdatedAt = :attentionUpdatedAt
        WHERE id = :sessionId
    """)
    suspend fun updateAttention(
        sessionId: String,
        attentionType: String?,
        attentionTitle: String,
        attentionBody: String,
        attentionUpdatedAt: Long?
    )
}
