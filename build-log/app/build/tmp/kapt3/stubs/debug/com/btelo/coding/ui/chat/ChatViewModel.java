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

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000H\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u000b\b\u0007\u0018\u00002\u00020\u0001B\u001f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0006\u0010\u0015\u001a\u00020\u0016J\b\u0010\u0017\u001a\u00020\u0016H\u0002J\b\u0010\u0018\u001a\u00020\u0016H\u0002J\b\u0010\u0019\u001a\u00020\u0016H\u0002J\b\u0010\u001a\u001a\u00020\u0016H\u0014J\u0006\u0010\u001b\u001a\u00020\u0016J\u000e\u0010\u001c\u001a\u00020\u00162\u0006\u0010\u001d\u001a\u00020\u0010J\u0006\u0010\u001e\u001a\u00020\u0016J\u000e\u0010\u001f\u001a\u00020\u00162\u0006\u0010 \u001a\u00020\u0010R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000e0\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0012\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014\u00a8\u0006!"}, d2 = {"Lcom/btelo/coding/ui/chat/ChatViewModel;", "Landroidx/lifecycle/ViewModel;", "messageRepository", "Lcom/btelo/coding/domain/repository/MessageRepository;", "sessionRepository", "Lcom/btelo/coding/domain/repository/SessionRepository;", "authRepository", "Lcom/btelo/coding/domain/repository/AuthRepository;", "(Lcom/btelo/coding/domain/repository/MessageRepository;Lcom/btelo/coding/domain/repository/SessionRepository;Lcom/btelo/coding/domain/repository/AuthRepository;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/btelo/coding/ui/chat/ChatUiState;", "coroutineJobs", "", "Lkotlinx/coroutines/Job;", "sessionId", "", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "dismissConnectionDetails", "", "loadMessages", "observeConnectionState", "observeOutput", "onCleared", "sendMessage", "setSessionId", "id", "toggleConnectionDetails", "updateInputText", "text", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class ChatViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.domain.repository.MessageRepository messageRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.domain.repository.SessionRepository sessionRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.domain.repository.AuthRepository authRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.btelo.coding.ui.chat.ChatUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.btelo.coding.ui.chat.ChatUiState> uiState = null;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String sessionId = "";
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<kotlinx.coroutines.Job> coroutineJobs = null;
    
    @javax.inject.Inject()
    public ChatViewModel(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.domain.repository.MessageRepository messageRepository, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.domain.repository.SessionRepository sessionRepository, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.domain.repository.AuthRepository authRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.btelo.coding.ui.chat.ChatUiState> getUiState() {
        return null;
    }
    
    public final void setSessionId(@org.jetbrains.annotations.NotNull()
    java.lang.String id) {
    }
    
    private final void loadMessages() {
    }
    
    private final void observeOutput() {
    }
    
    private final void observeConnectionState() {
    }
    
    public final void updateInputText(@org.jetbrains.annotations.NotNull()
    java.lang.String text) {
    }
    
    public final void sendMessage() {
    }
    
    public final void toggleConnectionDetails() {
    }
    
    public final void dismissConnectionDetails() {
    }
    
    @java.lang.Override()
    protected void onCleared() {
    }
}