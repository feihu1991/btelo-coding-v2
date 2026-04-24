package com.btelo.coding.ui.chat;

import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.CardDefaults;
import androidx.compose.material3.ExperimentalMaterial3Api;
import androidx.compose.material3.SnackbarHostState;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import com.btelo.coding.data.remote.websocket.factory.ConnectionState;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000@\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\t\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\u001a(\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u0007H\u0007\u001a?\u0010\b\u001a\u00020\u00012\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\b\u0010\r\u001a\u0004\u0018\u00010\u000e2\b\u0010\u000f\u001a\u0004\u0018\u00010\u00032\f\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00010\u0005H\u0007\u00a2\u0006\u0002\u0010\u0011\u001a\u001a\u0010\u0012\u001a\u00020\u00012\u0006\u0010\t\u001a\u00020\n2\b\b\u0002\u0010\u0013\u001a\u00020\u0014H\u0007\u001a\u0018\u0010\u0015\u001a\u00020\u00012\u0006\u0010\u0016\u001a\u00020\u00032\u0006\u0010\u0017\u001a\u00020\u0003H\u0003\u001a\"\u0010\u0018\u001a\u00020\u00012\u0006\u0010\u0019\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00020\u0003H\u0003\u00f8\u0001\u0000\u00a2\u0006\u0004\b\u001c\u0010\u001d\u0082\u0002\u0007\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006\u001e"}, d2 = {"ChatScreen", "", "sessionId", "", "onBack", "Lkotlin/Function0;", "viewModel", "Lcom/btelo/coding/ui/chat/ChatViewModel;", "ConnectionDetailsDialog", "connectionState", "Lcom/btelo/coding/data/remote/websocket/factory/ConnectionState;", "reconnectAttempts", "", "lastConnectedTime", "", "errorMessage", "onDismiss", "(Lcom/btelo/coding/data/remote/websocket/factory/ConnectionState;ILjava/lang/Long;Ljava/lang/String;Lkotlin/jvm/functions/Function0;)V", "ConnectionStatusIndicator", "modifier", "Landroidx/compose/ui/Modifier;", "DetailRow", "label", "value", "StatusLegendItem", "color", "Landroidx/compose/ui/graphics/Color;", "text", "StatusLegendItem-DxMtmZc", "(JLjava/lang/String;)V", "app_debug"})
public final class ChatScreenKt {
    
    @androidx.compose.runtime.Composable()
    public static final void ConnectionStatusIndicator(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.websocket.factory.ConnectionState connectionState, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier) {
    }
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void ChatScreen(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onBack, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.ui.chat.ChatViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void ConnectionDetailsDialog(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.websocket.factory.ConnectionState connectionState, int reconnectAttempts, @org.jetbrains.annotations.Nullable()
    java.lang.Long lastConnectedTime, @org.jetbrains.annotations.Nullable()
    java.lang.String errorMessage, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void DetailRow(java.lang.String label, java.lang.String value) {
    }
}