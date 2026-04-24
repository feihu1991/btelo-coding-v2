package com.btelo.coding.ui.chat;

import androidx.lifecycle.ViewModel;
import com.btelo.coding.data.remote.websocket.factory.ConnectionState;
import com.btelo.coding.domain.model.Message;
import com.btelo.coding.domain.repository.AuthRepository;
import com.btelo.coding.domain.repository.MessageRepository;
import com.btelo.coding.domain.repository.SessionRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\t\n\u0002\b$\b\u0086\b\u0018\u00002\u00020\u0001Bu\u0012\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0006\u0012\b\b\u0002\u0010\u0007\u001a\u00020\b\u0012\b\b\u0002\u0010\t\u001a\u00020\b\u0012\b\b\u0002\u0010\n\u001a\u00020\u000b\u0012\b\b\u0002\u0010\f\u001a\u00020\r\u0012\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u000f\u0012\n\b\u0002\u0010\u0010\u001a\u0004\u0018\u00010\u0006\u0012\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u0006\u0012\b\b\u0002\u0010\u0012\u001a\u00020\b\u00a2\u0006\u0002\u0010\u0013J\u000f\u0010#\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00c6\u0003J\t\u0010$\u001a\u00020\bH\u00c6\u0003J\t\u0010%\u001a\u00020\u0006H\u00c6\u0003J\t\u0010&\u001a\u00020\bH\u00c6\u0003J\t\u0010\'\u001a\u00020\bH\u00c6\u0003J\t\u0010(\u001a\u00020\u000bH\u00c6\u0003J\t\u0010)\u001a\u00020\rH\u00c6\u0003J\u0010\u0010*\u001a\u0004\u0018\u00010\u000fH\u00c6\u0003\u00a2\u0006\u0002\u0010\u001cJ\u000b\u0010+\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003J\u000b\u0010,\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003J~\u0010-\u001a\u00020\u00002\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\b2\b\b\u0002\u0010\n\u001a\u00020\u000b2\b\b\u0002\u0010\f\u001a\u00020\r2\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u000f2\n\b\u0002\u0010\u0010\u001a\u0004\u0018\u00010\u00062\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u00062\b\b\u0002\u0010\u0012\u001a\u00020\bH\u00c6\u0001\u00a2\u0006\u0002\u0010.J\u0013\u0010/\u001a\u00020\b2\b\u00100\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u00101\u001a\u00020\rH\u00d6\u0001J\t\u00102\u001a\u00020\u0006H\u00d6\u0001R\u0011\u0010\n\u001a\u00020\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u0013\u0010\u0011\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017R\u0013\u0010\u0010\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0017R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0017R\u0011\u0010\t\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\u001aR\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\u001aR\u0015\u0010\u000e\u001a\u0004\u0018\u00010\u000f\u00a2\u0006\n\n\u0002\u0010\u001d\u001a\u0004\b\u001b\u0010\u001cR\u0017\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001fR\u0011\u0010\f\u001a\u00020\r\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010!R\u0011\u0010\u0012\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010\u001a\u00a8\u00063"}, d2 = {"Lcom/btelo/coding/ui/chat/ChatUiState;", "", "messages", "", "Lcom/btelo/coding/domain/model/Message;", "inputText", "", "isLoading", "", "isConnected", "connectionState", "Lcom/btelo/coding/data/remote/websocket/factory/ConnectionState;", "reconnectAttempts", "", "lastConnectedTime", "", "errorMessage", "error", "showConnectionDetails", "(Ljava/util/List;Ljava/lang/String;ZZLcom/btelo/coding/data/remote/websocket/factory/ConnectionState;ILjava/lang/Long;Ljava/lang/String;Ljava/lang/String;Z)V", "getConnectionState", "()Lcom/btelo/coding/data/remote/websocket/factory/ConnectionState;", "getError", "()Ljava/lang/String;", "getErrorMessage", "getInputText", "()Z", "getLastConnectedTime", "()Ljava/lang/Long;", "Ljava/lang/Long;", "getMessages", "()Ljava/util/List;", "getReconnectAttempts", "()I", "getShowConnectionDetails", "component1", "component10", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "(Ljava/util/List;Ljava/lang/String;ZZLcom/btelo/coding/data/remote/websocket/factory/ConnectionState;ILjava/lang/Long;Ljava/lang/String;Ljava/lang/String;Z)Lcom/btelo/coding/ui/chat/ChatUiState;", "equals", "other", "hashCode", "toString", "app_debug"})
public final class ChatUiState {
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.btelo.coding.domain.model.Message> messages = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String inputText = null;
    private final boolean isLoading = false;
    private final boolean isConnected = false;
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.data.remote.websocket.factory.ConnectionState connectionState = null;
    private final int reconnectAttempts = 0;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Long lastConnectedTime = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String errorMessage = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String error = null;
    private final boolean showConnectionDetails = false;
    
    public ChatUiState(@org.jetbrains.annotations.NotNull()
    java.util.List<com.btelo.coding.domain.model.Message> messages, @org.jetbrains.annotations.NotNull()
    java.lang.String inputText, boolean isLoading, boolean isConnected, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.websocket.factory.ConnectionState connectionState, int reconnectAttempts, @org.jetbrains.annotations.Nullable()
    java.lang.Long lastConnectedTime, @org.jetbrains.annotations.Nullable()
    java.lang.String errorMessage, @org.jetbrains.annotations.Nullable()
    java.lang.String error, boolean showConnectionDetails) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.btelo.coding.domain.model.Message> getMessages() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getInputText() {
        return null;
    }
    
    public final boolean isLoading() {
        return false;
    }
    
    public final boolean isConnected() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.remote.websocket.factory.ConnectionState getConnectionState() {
        return null;
    }
    
    public final int getReconnectAttempts() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long getLastConnectedTime() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getErrorMessage() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getError() {
        return null;
    }
    
    public final boolean getShowConnectionDetails() {
        return false;
    }
    
    public ChatUiState() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.btelo.coding.domain.model.Message> component1() {
        return null;
    }
    
    public final boolean component10() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    public final boolean component3() {
        return false;
    }
    
    public final boolean component4() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.remote.websocket.factory.ConnectionState component5() {
        return null;
    }
    
    public final int component6() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long component7() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component8() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component9() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.ui.chat.ChatUiState copy(@org.jetbrains.annotations.NotNull()
    java.util.List<com.btelo.coding.domain.model.Message> messages, @org.jetbrains.annotations.NotNull()
    java.lang.String inputText, boolean isLoading, boolean isConnected, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.websocket.factory.ConnectionState connectionState, int reconnectAttempts, @org.jetbrains.annotations.Nullable()
    java.lang.Long lastConnectedTime, @org.jetbrains.annotations.Nullable()
    java.lang.String errorMessage, @org.jetbrains.annotations.Nullable()
    java.lang.String error, boolean showConnectionDetails) {
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