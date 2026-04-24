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
 * 同步设置页面的UI状态
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\b\n\u0002\b\u0019\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001BK\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0006\u0012\b\b\u0002\u0010\u0007\u001a\u00020\b\u0012\b\b\u0002\u0010\t\u001a\u00020\n\u0012\b\b\u0002\u0010\u000b\u001a\u00020\f\u0012\b\b\u0002\u0010\r\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u000eJ\t\u0010\u001a\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u001b\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u001c\u001a\u00020\u0006H\u00c6\u0003J\t\u0010\u001d\u001a\u00020\bH\u00c6\u0003J\t\u0010\u001e\u001a\u00020\nH\u00c6\u0003J\t\u0010\u001f\u001a\u00020\fH\u00c6\u0003J\t\u0010 \u001a\u00020\u0003H\u00c6\u0003JO\u0010!\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\n2\b\b\u0002\u0010\u000b\u001a\u00020\f2\b\b\u0002\u0010\r\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\"\u001a\u00020\u00032\b\u0010#\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010$\u001a\u00020\fH\u00d6\u0001J\t\u0010%\u001a\u00020&H\u00d6\u0001R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u0011\u0010\r\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u0010R\u0011\u0010\t\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0010R\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017R\u0011\u0010\u000b\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019\u00a8\u0006\'"}, d2 = {"Lcom/btelo/coding/ui/sync/SyncSettingsUiState;", "", "syncEnabled", "", "autoSyncEnabled", "syncState", "Lcom/btelo/coding/data/sync/SyncState;", "syncProgress", "Lcom/btelo/coding/data/sync/SyncProgress;", "lastSyncTime", "", "unsyncedCount", "", "isLoading", "(ZZLcom/btelo/coding/data/sync/SyncState;Lcom/btelo/coding/data/sync/SyncProgress;JIZ)V", "getAutoSyncEnabled", "()Z", "getLastSyncTime", "()J", "getSyncEnabled", "getSyncProgress", "()Lcom/btelo/coding/data/sync/SyncProgress;", "getSyncState", "()Lcom/btelo/coding/data/sync/SyncState;", "getUnsyncedCount", "()I", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "copy", "equals", "other", "hashCode", "toString", "", "app_debug"})
public final class SyncSettingsUiState {
    private final boolean syncEnabled = false;
    private final boolean autoSyncEnabled = false;
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.data.sync.SyncState syncState = null;
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.data.sync.SyncProgress syncProgress = null;
    private final long lastSyncTime = 0L;
    private final int unsyncedCount = 0;
    private final boolean isLoading = false;
    
    public SyncSettingsUiState(boolean syncEnabled, boolean autoSyncEnabled, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.sync.SyncState syncState, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.sync.SyncProgress syncProgress, long lastSyncTime, int unsyncedCount, boolean isLoading) {
        super();
    }
    
    public final boolean getSyncEnabled() {
        return false;
    }
    
    public final boolean getAutoSyncEnabled() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.sync.SyncState getSyncState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.sync.SyncProgress getSyncProgress() {
        return null;
    }
    
    public final long getLastSyncTime() {
        return 0L;
    }
    
    public final int getUnsyncedCount() {
        return 0;
    }
    
    public final boolean isLoading() {
        return false;
    }
    
    public SyncSettingsUiState() {
        super();
    }
    
    public final boolean component1() {
        return false;
    }
    
    public final boolean component2() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.sync.SyncState component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.sync.SyncProgress component4() {
        return null;
    }
    
    public final long component5() {
        return 0L;
    }
    
    public final int component6() {
        return 0;
    }
    
    public final boolean component7() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.ui.sync.SyncSettingsUiState copy(boolean syncEnabled, boolean autoSyncEnabled, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.sync.SyncState syncState, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.sync.SyncProgress syncProgress, long lastSyncTime, int unsyncedCount, boolean isLoading) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
}