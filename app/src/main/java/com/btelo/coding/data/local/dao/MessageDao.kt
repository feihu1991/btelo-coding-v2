package com.btelo.coding.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.btelo.coding.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesBySessionId(sessionId: String): Flow<List<MessageEntity>>
    
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesBySessionIdSync(sessionId: String): List<MessageEntity>
    
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)
    
    @Update
    suspend fun updateMessage(message: MessageEntity)
    
    @Delete
    suspend fun deleteMessage(message: MessageEntity)
    
    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesBySessionId(sessionId: String)
    
    @Query("SELECT COUNT(*) FROM messages WHERE sessionId = :sessionId")
    suspend fun getMessageCount(sessionId: String): Int
    
    @Query("DELETE FROM messages WHERE timestamp < :timestamp")
    suspend fun deleteOldMessages(timestamp: Long)
    
    // ========== 同步相关方法 ==========
    
    /**
     * 获取未同步的消息
     */
    @Query("SELECT * FROM messages WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedMessages(): List<MessageEntity>
    
    /**
     * 获取指定会话未同步的消息
     */
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId AND isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedMessagesBySession(sessionId: String): List<MessageEntity>
    
    /**
     * 获取指定时间之后的消息（用于增量同步）
     */
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId AND timestamp > :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getMessagesSince(sessionId: String, sinceTimestamp: Long): List<MessageEntity>
    
    /**
     * 标记消息为已同步
     */
    @Query("UPDATE messages SET isSynced = 1, version = :version WHERE id = :messageId")
    suspend fun markAsSynced(messageId: String, version: Int)
    
    /**
     * 批量标记消息为已同步
     */
    @Query("UPDATE messages SET isSynced = 1 WHERE id IN (:messageIds)")
    suspend fun markAsSyncedBatch(messageIds: List<String>)
    
    /**
     * 获取消息数量（用于同步进度）
     */
    @Query("SELECT COUNT(*) FROM messages WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int
    
    /**
     * 获取会话中最后一条消息的时间戳
     */
    @Query("SELECT MAX(timestamp) FROM messages WHERE sessionId = :sessionId")
    suspend fun getLastMessageTimestamp(sessionId: String): Long?
    
    /**
     * 软删除消息（标记为已删除）
     */
    @Query("UPDATE messages SET isDeleted = 1 WHERE id = :messageId")
    suspend fun softDeleteMessage(messageId: String)
    
    /**
     * 检查消息是否存在
     */
    @Query("SELECT EXISTS(SELECT 1 FROM messages WHERE id = :messageId)")
    suspend fun messageExists(messageId: String): Boolean
}
