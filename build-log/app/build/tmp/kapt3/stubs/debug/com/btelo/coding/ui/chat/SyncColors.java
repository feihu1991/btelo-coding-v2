package com.btelo.coding.ui.chat;

import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import com.btelo.coding.data.sync.SyncState;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 同步状态颜色
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\f\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0019\u0010\u0003\u001a\u00020\u0004\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\n\n\u0002\u0010\u0007\u001a\u0004\b\u0005\u0010\u0006R\u0019\u0010\b\u001a\u00020\u0004\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\n\n\u0002\u0010\u0007\u001a\u0004\b\t\u0010\u0006R\u0019\u0010\n\u001a\u00020\u0004\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\n\n\u0002\u0010\u0007\u001a\u0004\b\u000b\u0010\u0006R\u0019\u0010\f\u001a\u00020\u0004\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\n\n\u0002\u0010\u0007\u001a\u0004\b\r\u0010\u0006R\u0019\u0010\u000e\u001a\u00020\u0004\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\n\n\u0002\u0010\u0007\u001a\u0004\b\u000f\u0010\u0006\u0082\u0002\u000b\n\u0005\b\u00a1\u001e0\u0001\n\u0002\b!\u00a8\u0006\u0010"}, d2 = {"Lcom/btelo/coding/ui/chat/SyncColors;", "", "()V", "Error", "Landroidx/compose/ui/graphics/Color;", "getError-0d7_KjU", "()J", "J", "Idle", "getIdle-0d7_KjU", "Offline", "getOffline-0d7_KjU", "Success", "getSuccess-0d7_KjU", "Syncing", "getSyncing-0d7_KjU", "app_debug"})
public final class SyncColors {
    private static final long Idle = 0L;
    private static final long Syncing = 0L;
    private static final long Success = 0L;
    private static final long Error = 0L;
    private static final long Offline = 0L;
    @org.jetbrains.annotations.NotNull()
    public static final com.btelo.coding.ui.chat.SyncColors INSTANCE = null;
    
    private SyncColors() {
        super();
    }
}