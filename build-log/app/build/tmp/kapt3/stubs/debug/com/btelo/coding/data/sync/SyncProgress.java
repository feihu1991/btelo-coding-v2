package com.btelo.coding.data.sync;

import com.btelo.coding.data.local.DataStoreManager;
import com.btelo.coding.data.local.dao.MessageDao;
import com.btelo.coding.data.local.entity.MessageEntity;
import com.btelo.coding.data.remote.api.SyncApi;
import com.btelo.coding.data.remote.api.SyncMessageRequest;
import com.btelo.coding.data.remote.api.PullMessagesRequest;
import com.btelo.coding.data.remote.api.DeleteMessageRequest;
import com.btelo.coding.data.remote.api.ServerMessage;
import com.btelo.coding.data.remote.network.NetworkMonitor;
import com.btelo.coding.util.Logger;
import kotlinx.coroutines.*;
import kotlinx.coroutines.flow.*;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 同步进度
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0013\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0086\b\u0018\u00002\u00020\u0001BC\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0005\u0012\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\b\u0012\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\b\u0012\b\b\u0002\u0010\n\u001a\u00020\u000b\u00a2\u0006\u0002\u0010\fJ\t\u0010\u0017\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0018\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0019\u001a\u00020\u0005H\u00c6\u0003J\u000b\u0010\u001a\u001a\u0004\u0018\u00010\bH\u00c6\u0003J\u000b\u0010\u001b\u001a\u0004\u0018\u00010\bH\u00c6\u0003J\t\u0010\u001c\u001a\u00020\u000bH\u00c6\u0003JI\u0010\u001d\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00052\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\b2\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\b2\b\b\u0002\u0010\n\u001a\u00020\u000bH\u00c6\u0001J\u0013\u0010\u001e\u001a\u00020\u001f2\b\u0010 \u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010!\u001a\u00020\u0005H\u00d6\u0001J\t\u0010\"\u001a\u00020\bH\u00d6\u0001R\u0013\u0010\u0007\u001a\u0004\u0018\u00010\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0013\u0010\t\u001a\u0004\u0018\u00010\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u000eR\u0011\u0010\n\u001a\u00020\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u0011\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0015\u00a8\u0006#"}, d2 = {"Lcom/btelo/coding/data/sync/SyncProgress;", "", "state", "Lcom/btelo/coding/data/sync/SyncState;", "totalMessages", "", "syncedMessages", "currentSession", "", "lastError", "lastSyncTime", "", "(Lcom/btelo/coding/data/sync/SyncState;IILjava/lang/String;Ljava/lang/String;J)V", "getCurrentSession", "()Ljava/lang/String;", "getLastError", "getLastSyncTime", "()J", "getState", "()Lcom/btelo/coding/data/sync/SyncState;", "getSyncedMessages", "()I", "getTotalMessages", "component1", "component2", "component3", "component4", "component5", "component6", "copy", "equals", "", "other", "hashCode", "toString", "app_debug"})
public final class SyncProgress {
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.data.sync.SyncState state = null;
    private final int totalMessages = 0;
    private final int syncedMessages = 0;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String currentSession = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String lastError = null;
    private final long lastSyncTime = 0L;
    
    public SyncProgress(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.sync.SyncState state, int totalMessages, int syncedMessages, @org.jetbrains.annotations.Nullable()
    java.lang.String currentSession, @org.jetbrains.annotations.Nullable()
    java.lang.String lastError, long lastSyncTime) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.sync.SyncState getState() {
        return null;
    }
    
    public final int getTotalMessages() {
        return 0;
    }
    
    public final int getSyncedMessages() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getCurrentSession() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getLastError() {
        return null;
    }
    
    public final long getLastSyncTime() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.sync.SyncState component1() {
        return null;
    }
    
    public final int component2() {
        return 0;
    }
    
    public final int component3() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component4() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component5() {
        return null;
    }
    
    public final long component6() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.sync.SyncProgress copy(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.sync.SyncState state, int totalMessages, int syncedMessages, @org.jetbrains.annotations.Nullable()
    java.lang.String currentSession, @org.jetbrains.annotations.Nullable()
    java.lang.String lastError, long lastSyncTime) {
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