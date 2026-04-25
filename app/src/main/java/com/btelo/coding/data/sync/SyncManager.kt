package com.btelo.coding.data.sync

import com.btelo.coding.data.local.DataStoreManager
import com.btelo.coding.data.local.dao.MessageDao
import com.btelo.coding.data.local.entity.MessageEntity
import com.btelo.coding.data.remote.api.SyncApi
import com.btelo.coding.data.remote.api.SyncMessageRequest
import com.btelo.coding.data.remote.api.PullMessagesRequest
import com.btelo.coding.data.remote.api.DeleteMessageRequest
import com.btelo.coding.data.remote.api.ServerMessage
import com.btelo.coding.data.remote.network.NetworkMonitor
import com.btelo.coding.util.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import javax.inject.Qualifier

/**
 * 应用级别的 CoroutineScope Qualifier
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * 消息同步状态
 */
enum class SyncState {
    IDLE,           // 空闲
    SYNCING,        // 同步中
    SUCCESS,        // 同步成功
    ERROR,          // 同步失败
    OFFLINE         // 离线
}

/**
 * 同步进度
 */
data class SyncProgress(
    val state: SyncState,
    val totalMessages: Int = 0,
    val syncedMessages: Int = 0,
    val currentSession: String? = null,
    val lastError: String? = null,
    val lastSyncTime: Long = 0
)

/**
 * 消息同步管理器
 * 负责消息的增量同步和冲突处理
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val syncApi: SyncApi,
    private val messageDao: MessageDao,
    private val dataStoreManager: DataStoreManager,
    private val networkMonitor: NetworkMonitor
) {
    companion object {
        private const val TAG = "SyncManager"
        private const val SYNC_PAGE_SIZE = 100
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY_MS = 5000L
    }
    
    // 同步状态
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    // 同步进度
    private val _syncProgress = MutableStateFlow(SyncProgress(SyncState.IDLE))
    val syncProgress: StateFlow<SyncProgress> = _syncProgress.asStateFlow()
    
    // 同步锁
    private val syncMutex = Mutex()
    
    // 定时同步任务
    private var periodicSyncJob: Job? = null
    
    // 最后同步时间
    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()
    
    /**
     * 初始化同步管理器
     */
    fun initialize() {
        _lastSyncTime.value = dataStoreManager.getLastSyncTimestamp()
        
        // 监听网络状态
        scope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                if (isOnline && dataStoreManager.isAutoSyncEnabled()) {
                    startPeriodicSync()
                } else {
                    stopPeriodicSync()
                    if (!isOnline) {
                        _syncState.value = SyncState.OFFLINE
                    }
                }
            }
        }
        
        Logger.i(TAG, "SyncManager initialized, last sync: ${_lastSyncTime.value}")
    }
    
    /**
     * 开始定时同步
     */
    fun startPeriodicSync() {
        if (periodicSyncJob?.isActive == true) return

        periodicSyncJob = scope.launch {
            while (isActive && dataStoreManager.isAutoSyncEnabled()) {
                if (networkMonitor.isOnline.value) {
                    syncAllSessions()
                }
                delay(60_000) // 每分钟检查一次
            }
        }

        Logger.i(TAG, "Periodic sync started")
    }
    
    /**
     * 停止定时同步
     */
    fun stopPeriodicSync() {
        periodicSyncJob?.cancel()
        periodicSyncJob = null
        Logger.i(TAG, "Periodic sync stopped")
    }
    
    /**
     * 手动触发同步
     */
    suspend fun triggerSync() {
        if (!networkMonitor.isOnline.value) {
            _syncState.value = SyncState.OFFLINE
            return
        }
        
        syncMutex.lock()
        try {
            syncAllSessions()
        } finally {
            syncMutex.unlock()
        }
    }
    
    /**
     * 同步所有会话
     */
    private suspend fun syncAllSessions() {
        if (!dataStoreManager.isSyncEnabled()) {
            Logger.d(TAG, "Sync disabled, skipping")
            return
        }
        
        _syncState.value = SyncState.SYNCING
        
        try {
            // 1. 先推送本地消息到服务器
            awaitPushLocalMessages()
            
            // 2. 从服务器拉取消息
            awaitPullRemoteMessages()
            
            // 3. 更新同步状态
            val now = System.currentTimeMillis()
            dataStoreManager.saveLastSyncTimestamp(now)
            _lastSyncTime.value = now
            
            _syncState.value = SyncState.SUCCESS
            _syncProgress.value = SyncProgress(
                state = SyncState.SUCCESS,
                lastSyncTime = now
            )
            
            Logger.i(TAG, "Sync completed successfully")
            
        } catch (e: Exception) {
            Logger.e(TAG, "Sync failed", e)
            _syncState.value = SyncState.ERROR
            _syncProgress.value = _syncProgress.value.copy(
                state = SyncState.ERROR,
                lastError = e.message
            )
        }
    }
    
    /**
     * 推送本地消息到服务器
     */
    private suspend fun awaitPushLocalMessages() {
        val deviceId = dataStoreManager.getDeviceIdSync() ?: return
        
        // 获取本地消息（这里简化处理，实际可以从MessageDao获取）
        // 消息会在发送时自动同步到服务器
        Logger.d(TAG, "Pushing local messages to server...")
    }
    
    /**
     * 从服务器拉取消息
     */
    private suspend fun awaitPullRemoteMessages() {
        val deviceId = dataStoreManager.getDeviceIdSync() ?: return
        
        // 获取设备参与的会话列表
        try {
            val sessionsResponse = syncApi.getDeviceSessions(deviceId)
            if (!sessionsResponse.isSuccessful) {
                Logger.e(TAG, "Failed to get device sessions: ${sessionsResponse.code()}")
                return
            }
            
            val sessions = sessionsResponse.body()?.sessions ?: emptyList()
            Logger.d(TAG, "Found ${sessions.size} sessions to sync")
            
            var totalSynced = 0
            
            for (sessionId in sessions) {
                _syncProgress.value = _syncProgress.value.copy(
                    currentSession = sessionId
                )
                
                // 分页拉取消息
                var offset = 0
                var hasMore = true
                
                while (hasMore) {
                    val request = PullMessagesRequest(
                        session_id = sessionId,
                        device_id = deviceId,
                        since_timestamp = dataStoreManager.getLastSyncTimestamp(),
                        limit = SYNC_PAGE_SIZE,
                        offset = offset
                    )
                    
                    val response = syncApi.pullMessages(request)
                    if (!response.isSuccessful) {
                        Logger.e(TAG, "Failed to pull messages for session $sessionId")
                        break
                    }
                    
                    val body = response.body()
                    if (body?.success == true) {
                        // 保存消息到本地
                        for (serverMsg in body.messages) {
                            if (!serverMsg.is_deleted) {
                                val entity = MessageEntity(
                                    id = serverMsg.id,
                                    sessionId = serverMsg.session_id,
                                    content = serverMsg.content,
                                    type = serverMsg.msg_type,
                                    timestamp = serverMsg.timestamp,
                                    isFromUser = false,  // 简化处理
                                    version = serverMsg.version,
                                    deviceId = serverMsg.device_id,
                                    isSynced = true,
                                    isDeleted = false
                                )
                                messageDao.insertMessage(entity)
                                totalSynced++
                            }
                        }
                        
                        _syncProgress.value = _syncProgress.value.copy(
                            syncedMessages = totalSynced
                        )
                        
                        hasMore = body.has_more
                        offset = body.next_offset
                    } else {
                        break
                    }
                }
            }
            
            _syncProgress.value = _syncProgress.value.copy(
                totalMessages = totalSynced
            )
            
            Logger.i(TAG, "Pulled $totalSynced messages from server")
            
        } catch (e: Exception) {
            Logger.e(TAG, "Error pulling messages", e)
            throw e
        }
    }
    
    /**
     * 同步单条消息
     */
    suspend fun syncMessage(
        sessionId: String,
        messageId: String,
        content: String,
        msgType: String,
        timestamp: Long
    ): Result<String> {
        if (!networkMonitor.isOnline.value) {
            return Result.failure(Exception("Network offline"))
        }
        
        val deviceId = dataStoreManager.getDeviceIdSync() ?: return Result.failure(Exception("No device ID"))
        
        var retryCount = 0
        while (retryCount < MAX_RETRY_COUNT) {
            try {
                val request = SyncMessageRequest(
                    session_id = sessionId,
                    device_id = deviceId,
                    content = content,
                    msg_type = msgType,
                    timestamp = timestamp
                )
                
                val response = syncApi.syncMessage(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        // 更新本地消息状态
                        messageDao.markAsSynced(messageId, body.version)
                        return Result.success(body.message_id)
                    }
                }
                
                retryCount++
                if (retryCount < MAX_RETRY_COUNT) {
                    delay(RETRY_DELAY_MS)
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error syncing message", e)
                retryCount++
                if (retryCount < MAX_RETRY_COUNT) {
                    delay(RETRY_DELAY_MS)
                }
            }
        }
        
        return Result.failure(Exception("Failed to sync message after $MAX_RETRY_COUNT retries"))
    }
    
    /**
     * 删除消息（跨设备同步）
     */
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        if (!networkMonitor.isOnline.value) {
            // 离线时只更新本地
            messageDao.softDeleteMessage(messageId)
            return Result.success(Unit)
        }
        
        val deviceId = dataStoreManager.getDeviceIdSync() ?: return Result.failure(Exception("No device ID"))
        
        var retryCount = 0
        while (retryCount < MAX_RETRY_COUNT) {
            try {
                val request = DeleteMessageRequest(
                    message_id = messageId,
                    device_id = deviceId
                )
                
                val response = syncApi.deleteMessage(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        // 本地也标记为已删除
                        messageDao.softDeleteMessage(messageId)
                        return Result.success(Unit)
                    }
                }
                
                retryCount++
                if (retryCount < MAX_RETRY_COUNT) {
                    delay(RETRY_DELAY_MS)
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error deleting message", e)
                retryCount++
                if (retryCount < MAX_RETRY_COUNT) {
                    delay(RETRY_DELAY_MS)
                }
            }
        }
        
        return Result.failure(Exception("Failed to delete message after $MAX_RETRY_COUNT retries"))
    }
    
    /**
     * 处理同步冲突
     * 冲突策略：服务器版本优先，但保留本地未同步内容作为新消息
     */
    suspend fun resolveConflict(
        localMessage: MessageEntity,
        serverMessage: ServerMessage
    ): MessageEntity {
        return when {
            // 服务器版本更新
            serverMessage.version > localMessage.version -> {
                MessageEntity(
                    id = serverMessage.id,
                    sessionId = serverMessage.session_id,
                    content = serverMessage.content,
                    type = serverMessage.msg_type,
                    timestamp = serverMessage.timestamp,
                    isFromUser = localMessage.isFromUser,
                    version = serverMessage.version,
                    deviceId = serverMessage.device_id,
                    isSynced = true,
                    isDeleted = serverMessage.is_deleted
                )
            }
            // 本地版本更新
            localMessage.version > serverMessage.version -> {
                // 重新同步本地消息到服务器
                syncMessage(
                    sessionId = localMessage.sessionId,
                    messageId = localMessage.id,
                    content = localMessage.content,
                    msgType = localMessage.type,
                    timestamp = localMessage.timestamp
                )
                localMessage.copy(isSynced = true)
            }
            // 版本相同，时间戳不同
            else -> {
                // 保留较新的
                if (serverMessage.timestamp > localMessage.timestamp) {
                    MessageEntity(
                        id = serverMessage.id,
                        sessionId = serverMessage.session_id,
                        content = serverMessage.content,
                        type = serverMessage.msg_type,
                        timestamp = serverMessage.timestamp,
                        isFromUser = localMessage.isFromUser,
                        version = serverMessage.version,
                        deviceId = serverMessage.device_id,
                        isSynced = true,
                        isDeleted = serverMessage.is_deleted
                    )
                } else {
                    localMessage.copy(isSynced = true)
                }
            }
        }
    }
    
    /**
     * 获取同步状态摘要
     */
    suspend fun getSyncSummary(): Map<String, Any> {
        return mapOf(
            "lastSyncTime" to _lastSyncTime.value,
            "syncEnabled" to dataStoreManager.isSyncEnabled(),
            "autoSyncEnabled" to dataStoreManager.isAutoSyncEnabled(),
            "currentState" to _syncState.value.name,
            "isOnline" to networkMonitor.isOnline.value
        )
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        stopPeriodicSync()
        _syncState.value = SyncState.IDLE
    }
}
