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

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000>\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\u001a\u0018\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0003H\u0003\u001a\"\u0010\u0005\u001a\u00020\u00012\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\u0003H\u0003\u00f8\u0001\u0000\u00a2\u0006\u0004\b\t\u0010\n\u001aP\u0010\u000b\u001a\u00020\u00012\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u00112\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00010\u00132\u0012\u0010\u0014\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00010\u00152\f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00010\u0013H\u0007\u001a0\u0010\u0017\u001a\u00020\u00012\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00010\u00132\b\b\u0002\u0010\u0019\u001a\u00020\u001aH\u0007\u0082\u0002\u0007\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006\u001b"}, d2 = {"DetailRow", "", "label", "", "value", "SyncLegendItem", "color", "Landroidx/compose/ui/graphics/Color;", "text", "SyncLegendItem-DxMtmZc", "(JLjava/lang/String;)V", "SyncStatusDialog", "syncState", "Lcom/btelo/coding/data/sync/SyncState;", "lastSyncTime", "", "isAutoSyncEnabled", "", "onSyncNow", "Lkotlin/Function0;", "onToggleAutoSync", "Lkotlin/Function1;", "onDismiss", "SyncStatusIndicator", "onSyncClick", "modifier", "Landroidx/compose/ui/Modifier;", "app_debug"})
public final class SyncStatusKt {
    
    /**
     * 同步状态指示器
     */
    @androidx.compose.runtime.Composable()
    public static final void SyncStatusIndicator(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.sync.SyncState syncState, long lastSyncTime, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onSyncClick, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier) {
    }
    
    /**
     * 同步状态详情对话框
     */
    @androidx.compose.runtime.Composable()
    public static final void SyncStatusDialog(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.sync.SyncState syncState, long lastSyncTime, boolean isAutoSyncEnabled, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onSyncNow, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onToggleAutoSync, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void DetailRow(java.lang.String label, java.lang.String value) {
    }
}