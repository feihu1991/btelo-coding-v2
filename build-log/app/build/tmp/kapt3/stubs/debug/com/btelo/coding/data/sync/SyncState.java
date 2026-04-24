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
 * 消息同步状态
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0007\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006j\u0002\b\u0007\u00a8\u0006\b"}, d2 = {"Lcom/btelo/coding/data/sync/SyncState;", "", "(Ljava/lang/String;I)V", "IDLE", "SYNCING", "SUCCESS", "ERROR", "OFFLINE", "app_debug"})
public enum SyncState {
    /*public static final*/ IDLE /* = new IDLE() */,
    /*public static final*/ SYNCING /* = new SYNCING() */,
    /*public static final*/ SUCCESS /* = new SUCCESS() */,
    /*public static final*/ ERROR /* = new ERROR() */,
    /*public static final*/ OFFLINE /* = new OFFLINE() */;
    
    SyncState() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public static kotlin.enums.EnumEntries<com.btelo.coding.data.sync.SyncState> getEntries() {
        return null;
    }
}