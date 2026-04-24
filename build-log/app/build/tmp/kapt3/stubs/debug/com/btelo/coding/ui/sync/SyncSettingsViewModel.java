package com.btelo.coding.ui.sync;

import androidx.lifecycle.ViewModel;
import com.btelo.coding.data.local.DataStoreManager;
import com.btelo.coding.data.remote.api.SyncApi;
import com.btelo.coding.data.sync.SyncManager;
import com.btelo.coding.data.sync.SyncProgress;
import com.btelo.coding.data.sync.SyncState;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.*;
import javax.inject.Inject;

/**
 * 同步设置 ViewModel
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u001f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u001a\u0010\u0010\u001a\u000e\u0012\u0004\u0012\u00020\u0012\u0012\u0004\u0012\u00020\u00130\u0011H\u0086@\u00a2\u0006\u0002\u0010\u0014J\b\u0010\u0015\u001a\u00020\u0016H\u0014J\u000e\u0010\u0017\u001a\u00020\u00162\u0006\u0010\u0018\u001a\u00020\u0019J\u000e\u0010\u001a\u001a\u00020\u00162\u0006\u0010\u0018\u001a\u00020\u0019J\u0006\u0010\u001b\u001a\u00020\u0016R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000b0\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000f\u00a8\u0006\u001c"}, d2 = {"Lcom/btelo/coding/ui/sync/SyncSettingsViewModel;", "Landroidx/lifecycle/ViewModel;", "syncManager", "Lcom/btelo/coding/data/sync/SyncManager;", "dataStoreManager", "Lcom/btelo/coding/data/local/DataStoreManager;", "syncApi", "Lcom/btelo/coding/data/remote/api/SyncApi;", "(Lcom/btelo/coding/data/sync/SyncManager;Lcom/btelo/coding/data/local/DataStoreManager;Lcom/btelo/coding/data/remote/api/SyncApi;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/btelo/coding/ui/sync/SyncSettingsUiState;", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "getSyncSummary", "", "", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "onCleared", "", "setAutoSyncEnabled", "enabled", "", "setSyncEnabled", "triggerSync", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class SyncSettingsViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.data.sync.SyncManager syncManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.data.local.DataStoreManager dataStoreManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.data.remote.api.SyncApi syncApi = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.btelo.coding.ui.sync.SyncSettingsUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.btelo.coding.ui.sync.SyncSettingsUiState> uiState = null;
    
    @javax.inject.Inject()
    public SyncSettingsViewModel(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.sync.SyncManager syncManager, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.DataStoreManager dataStoreManager, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.api.SyncApi syncApi) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.btelo.coding.ui.sync.SyncSettingsUiState> getUiState() {
        return null;
    }
    
    /**
     * 切换同步开关
     */
    public final void setSyncEnabled(boolean enabled) {
    }
    
    /**
     * 切换自动同步开关
     */
    public final void setAutoSyncEnabled(boolean enabled) {
    }
    
    /**
     * 手动触发同步
     */
    public final void triggerSync() {
    }
    
    /**
     * 获取同步摘要
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getSyncSummary(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Map<java.lang.String, ? extends java.lang.Object>> $completion) {
        return null;
    }
    
    @java.lang.Override()
    protected void onCleared() {
    }
}