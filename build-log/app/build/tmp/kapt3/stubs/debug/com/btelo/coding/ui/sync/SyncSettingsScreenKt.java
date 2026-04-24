package com.btelo.coding.ui.sync;

import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material.icons.filled.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import java.text.SimpleDateFormat;
import java.util.*;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000<\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\u001a\u0018\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u0003\u001a@\u0010\u0006\u001a\u00020\u00012\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\b2\u0012\u0010\n\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u00010\u000b2\u0012\u0010\f\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u00010\u000bH\u0003\u001a \u0010\r\u001a\u00020\u00012\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\u000f2\b\b\u0002\u0010\u0010\u001a\u00020\u0011H\u0007\u001a\u001e\u0010\u0012\u001a\u00020\u00012\u0006\u0010\u0013\u001a\u00020\u00142\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00010\u000fH\u0003\u00a8\u0006\u0016"}, d2 = {"SyncInfoCard", "", "lastSyncTime", "", "unsyncedCount", "", "SyncSettingsCard", "syncEnabled", "", "autoSyncEnabled", "onSyncEnabledChange", "Lkotlin/Function1;", "onAutoSyncEnabledChange", "SyncSettingsScreen", "onNavigateBack", "Lkotlin/Function0;", "viewModel", "Lcom/btelo/coding/ui/sync/SyncSettingsViewModel;", "SyncStatusCard", "uiState", "Lcom/btelo/coding/ui/sync/SyncSettingsUiState;", "onSyncClick", "app_debug"})
public final class SyncSettingsScreenKt {
    
    /**
     * 同步设置页面
     */
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void SyncSettingsScreen(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onNavigateBack, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.ui.sync.SyncSettingsViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void SyncStatusCard(com.btelo.coding.ui.sync.SyncSettingsUiState uiState, kotlin.jvm.functions.Function0<kotlin.Unit> onSyncClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void SyncSettingsCard(boolean syncEnabled, boolean autoSyncEnabled, kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onSyncEnabledChange, kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onAutoSyncEnabledChange) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void SyncInfoCard(long lastSyncTime, int unsyncedCount) {
    }
}