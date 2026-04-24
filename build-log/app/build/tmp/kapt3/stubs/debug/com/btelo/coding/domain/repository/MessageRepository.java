package com.btelo.coding.domain.repository;

import com.btelo.coding.data.remote.websocket.factory.ConnectionState;
import com.btelo.coding.domain.model.Message;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.StateFlow;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\bf\u0018\u00002\u00020\u0001J \u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\n2\b\b\u0002\u0010\u000b\u001a\u00020\fH\u00a6@\u00a2\u0006\u0002\u0010\rJ \u0010\u000e\u001a\u00020\b2\u0006\u0010\u000f\u001a\u00020\n2\u0006\u0010\u0010\u001a\u00020\n2\u0006\u0010\t\u001a\u00020\nH&J\u0010\u0010\u0011\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nH&J\u001c\u0010\u0012\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00150\u00140\u00132\u0006\u0010\t\u001a\u00020\nH&J\u0016\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00150\u00132\u0006\u0010\t\u001a\u00020\nH&J,\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\b0\u00182\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u0019\u001a\u00020\nH\u00a6@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u001a\u0010\u001bR\u0018\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0005\u0010\u0006\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006\u001c"}, d2 = {"Lcom/btelo/coding/domain/repository/MessageRepository;", "", "connectionState", "Lkotlinx/coroutines/flow/StateFlow;", "Lcom/btelo/coding/data/remote/websocket/factory/ConnectionState;", "getConnectionState", "()Lkotlinx/coroutines/flow/StateFlow;", "cleanOldMessages", "", "sessionId", "", "keepDays", "", "(Ljava/lang/String;ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "connect", "serverAddress", "token", "disconnect", "getMessages", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/btelo/coding/domain/model/Message;", "observeOutput", "sendMessage", "Lkotlin/Result;", "content", "sendMessage-0E7RQCE", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface MessageRepository {
    
    public abstract void connect(@org.jetbrains.annotations.NotNull()
    java.lang.String serverAddress, @org.jetbrains.annotations.NotNull()
    java.lang.String token, @org.jetbrains.annotations.NotNull()
    java.lang.String sessionId);
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.btelo.coding.domain.model.Message>> getMessages(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId);
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<com.btelo.coding.domain.model.Message> observeOutput(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId);
    
    public abstract void disconnect(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId);
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.StateFlow<com.btelo.coding.data.remote.websocket.factory.ConnectionState> getConnectionState();
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object cleanOldMessages(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId, int keepDays, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
    }
}