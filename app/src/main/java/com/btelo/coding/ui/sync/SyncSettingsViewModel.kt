package com.btelo.coding.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btelo.coding.data.local.DataStoreManager
import com.btelo.coding.data.remote.api.SyncApi
import com.btelo.coding.data.sync.SyncManager
import com.btelo.coding.data.sync.SyncProgress
import com.btelo.coding.data.sync.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 同步设置页面的UI状态
 */
data class SyncSettingsUiState(
    val syncEnabled: Boolean = true,
    val autoSyncEnabled: Boolean = true,
    val syncState: SyncState = SyncState.IDLE,
    val syncProgress: SyncProgress = SyncProgress(SyncState.IDLE),
    val lastSyncTime: Long = 0,
    val unsyncedCount: Int = 0,
    val isLoading: Boolean = false
)

/**
 * 同步设置 ViewModel
 */
@HiltViewModel
class SyncSettingsViewModel @Inject constructor(
    private val syncManager: SyncManager,
    private val dataStoreManager: DataStoreManager,
    private val syncApi: SyncApi
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SyncSettingsUiState())
    val uiState: StateFlow<SyncSettingsUiState> = _uiState.asStateFlow()
    
    init {
        // 加载同步状态
        _uiState.update {
            it.copy(
                syncEnabled = dataStoreManager.isSyncEnabled(),
                autoSyncEnabled = dataStoreManager.isAutoSyncEnabled(),
                lastSyncTime = dataStoreManager.getLastSyncTimestamp()
            )
        }
        
        // 监听同步状态变化
        viewModelScope.launch {
            syncManager.syncState.collect { state ->
                _uiState.update { it.copy(syncState = state) }
            }
        }
        
        // 监听同步进度
        viewModelScope.launch {
            syncManager.syncProgress.collect { progress ->
                _uiState.update { it.copy(syncProgress = progress) }
            }
        }
        
        // 监听最后同步时间
        viewModelScope.launch {
            syncManager.lastSyncTime.collect { time ->
                _uiState.update { it.copy(lastSyncTime = time) }
            }
        }
    }
    
    /**
     * 切换同步开关
     */
    fun setSyncEnabled(enabled: Boolean) {
        dataStoreManager.setSyncEnabled(enabled)
        _uiState.update { it.copy(syncEnabled = enabled) }
    }
    
    /**
     * 切换自动同步开关
     */
    fun setAutoSyncEnabled(enabled: Boolean) {
        dataStoreManager.setAutoSyncEnabled(enabled)
        _uiState.update { it.copy(autoSyncEnabled = enabled) }
        
        if (enabled) {
            syncManager.startPeriodicSync()
        } else {
            syncManager.stopPeriodicSync()
        }
    }
    
    /**
     * 手动触发同步
     */
    fun triggerSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                syncManager.triggerSync()
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    /**
     * 获取同步摘要
     */
    fun getSyncSummary(): Map<String, Any> {
        return syncManager.getSyncSummary()
    }
    
    override fun onCleared() {
        super.onCleared()
        // 不清理 syncManager，因为它是单例
    }
}
