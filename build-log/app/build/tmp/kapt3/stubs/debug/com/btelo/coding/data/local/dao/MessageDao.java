package com.btelo.coding.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.btelo.coding.data.local.entity.MessageEntity;
import kotlinx.coroutines.flow.Flow;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\t\n\u0002\b\u0005\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0012\n\u0002\u0010\u000b\n\u0002\b\u0003\bg\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u0007\u001a\u00020\u00032\u0006\u0010\b\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u0016\u0010\u000b\u001a\u00020\u00032\u0006\u0010\f\u001a\u00020\rH\u00a7@\u00a2\u0006\u0002\u0010\u000eJ\u0018\u0010\u000f\u001a\u0004\u0018\u00010\r2\u0006\u0010\b\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u0018\u0010\u0010\u001a\u0004\u0018\u00010\u00052\u0006\u0010\u0011\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u0016\u0010\u0012\u001a\u00020\u00132\u0006\u0010\b\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u001c\u0010\u0014\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\u00160\u00152\u0006\u0010\b\u001a\u00020\tH\'J\u001c\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00050\u00162\u0006\u0010\b\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ$\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00050\u00162\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\u0019\u001a\u00020\rH\u00a7@\u00a2\u0006\u0002\u0010\u001aJ\u000e\u0010\u001b\u001a\u00020\u0013H\u00a7@\u00a2\u0006\u0002\u0010\u001cJ\u0014\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u00050\u0016H\u00a7@\u00a2\u0006\u0002\u0010\u001cJ\u001c\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u00050\u00162\u0006\u0010\b\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u0016\u0010\u001f\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u001c\u0010 \u001a\u00020\u00032\f\u0010!\u001a\b\u0012\u0004\u0012\u00020\u00050\u0016H\u00a7@\u00a2\u0006\u0002\u0010\"J\u001e\u0010#\u001a\u00020\u00032\u0006\u0010\u0011\u001a\u00020\t2\u0006\u0010$\u001a\u00020\u0013H\u00a7@\u00a2\u0006\u0002\u0010%J\u001c\u0010&\u001a\u00020\u00032\f\u0010\'\u001a\b\u0012\u0004\u0012\u00020\t0\u0016H\u00a7@\u00a2\u0006\u0002\u0010\"J\u0016\u0010(\u001a\u00020)2\u0006\u0010\u0011\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u0016\u0010*\u001a\u00020\u00032\u0006\u0010\u0011\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u0016\u0010+\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006\u00a8\u0006,"}, d2 = {"Lcom/btelo/coding/data/local/dao/MessageDao;", "", "deleteMessage", "", "message", "Lcom/btelo/coding/data/local/entity/MessageEntity;", "(Lcom/btelo/coding/data/local/entity/MessageEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteMessagesBySessionId", "sessionId", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteOldMessages", "timestamp", "", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getLastMessageTimestamp", "getMessageById", "messageId", "getMessageCount", "", "getMessagesBySessionId", "Lkotlinx/coroutines/flow/Flow;", "", "getMessagesBySessionIdSync", "getMessagesSince", "sinceTimestamp", "(Ljava/lang/String;JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getUnsyncedCount", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getUnsyncedMessages", "getUnsyncedMessagesBySession", "insertMessage", "insertMessages", "messages", "(Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "markAsSynced", "version", "(Ljava/lang/String;ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "markAsSyncedBatch", "messageIds", "messageExists", "", "softDeleteMessage", "updateMessage", "app_debug"})
@androidx.room.Dao()
public abstract interface MessageDao {
    
    @androidx.room.Query(value = "SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.btelo.coding.data.local.entity.MessageEntity>> getMessagesBySessionId(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId);
    
    @androidx.room.Query(value = "SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getMessagesBySessionIdSync(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.btelo.coding.data.local.entity.MessageEntity>> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM messages WHERE id = :messageId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getMessageById(@org.jetbrains.annotations.NotNull()
    java.lang.String messageId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.btelo.coding.data.local.entity.MessageEntity> $completion);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertMessage(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.entity.MessageEntity message, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertMessages(@org.jetbrains.annotations.NotNull()
    java.util.List<com.btelo.coding.data.local.entity.MessageEntity> messages, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Update()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateMessage(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.entity.MessageEntity message, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Delete()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteMessage(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.entity.MessageEntity message, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "DELETE FROM messages WHERE sessionId = :sessionId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteMessagesBySessionId(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "SELECT COUNT(*) FROM messages WHERE sessionId = :sessionId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getMessageCount(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Integer> $completion);
    
    @androidx.room.Query(value = "DELETE FROM messages WHERE timestamp < :timestamp")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteOldMessages(long timestamp, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * 获取未同步的消息
     */
    @androidx.room.Query(value = "SELECT * FROM messages WHERE isSynced = 0 ORDER BY timestamp ASC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getUnsyncedMessages(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.btelo.coding.data.local.entity.MessageEntity>> $completion);
    
    /**
     * 获取指定会话未同步的消息
     */
    @androidx.room.Query(value = "SELECT * FROM messages WHERE sessionId = :sessionId AND isSynced = 0 ORDER BY timestamp ASC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getUnsyncedMessagesBySession(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.btelo.coding.data.local.entity.MessageEntity>> $completion);
    
    /**
     * 获取指定时间之后的消息（用于增量同步）
     */
    @androidx.room.Query(value = "SELECT * FROM messages WHERE sessionId = :sessionId AND timestamp > :sinceTimestamp ORDER BY timestamp ASC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getMessagesSince(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId, long sinceTimestamp, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.btelo.coding.data.local.entity.MessageEntity>> $completion);
    
    /**
     * 标记消息为已同步
     */
    @androidx.room.Query(value = "UPDATE messages SET isSynced = 1, version = :version WHERE id = :messageId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object markAsSynced(@org.jetbrains.annotations.NotNull()
    java.lang.String messageId, int version, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * 批量标记消息为已同步
     */
    @androidx.room.Query(value = "UPDATE messages SET isSynced = 1 WHERE id IN (:messageIds)")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object markAsSyncedBatch(@org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> messageIds, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * 获取消息数量（用于同步进度）
     */
    @androidx.room.Query(value = "SELECT COUNT(*) FROM messages WHERE isSynced = 0")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getUnsyncedCount(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Integer> $completion);
    
    /**
     * 获取会话中最后一条消息的时间戳
     */
    @androidx.room.Query(value = "SELECT MAX(timestamp) FROM messages WHERE sessionId = :sessionId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getLastMessageTimestamp(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Long> $completion);
    
    /**
     * 软删除消息（标记为已删除）
     */
    @androidx.room.Query(value = "UPDATE messages SET isDeleted = 1 WHERE id = :messageId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object softDeleteMessage(@org.jetbrains.annotations.NotNull()
    java.lang.String messageId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * 检查消息是否存在
     */
    @androidx.room.Query(value = "SELECT EXISTS(SELECT 1 FROM messages WHERE id = :messageId)")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object messageExists(@org.jetbrains.annotations.NotNull()
    java.lang.String messageId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion);
}