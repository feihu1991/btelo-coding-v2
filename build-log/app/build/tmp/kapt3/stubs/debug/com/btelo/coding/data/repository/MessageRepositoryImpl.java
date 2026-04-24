package com.btelo.coding.data.repository;

import com.btelo.coding.data.local.dao.MessageDao;
import com.btelo.coding.data.remote.websocket.BteloMessage;
import com.btelo.coding.data.remote.websocket.InputType;
import com.btelo.coding.data.remote.websocket.factory.ConnectionState;
import com.btelo.coding.data.remote.websocket.factory.WebSocketClientFactory;
import com.btelo.coding.data.remote.websocket.factory.WebSocketConfig;
import com.btelo.coding.data.remote.websocket.factory.ReconnectConfig;
import com.btelo.coding.domain.model.Message;
import com.btelo.coding.domain.model.MessageType;
import com.btelo.coding.domain.repository.MessageRepository;
import com.btelo.coding.util.Logger;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000`\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0007\u0018\u00002\u00020\u0001B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u001e\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u000b2\u0006\u0010\u0018\u001a\u00020\u0019H\u0096@\u00a2\u0006\u0002\u0010\u001aJ \u0010\u001b\u001a\u00020\u00162\u0006\u0010\u001c\u001a\u00020\u000b2\u0006\u0010\u001d\u001a\u00020\u000b2\u0006\u0010\u0017\u001a\u00020\u000bH\u0016J\u0010\u0010\u001e\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u000bH\u0016J\u001c\u0010\u001f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\"0!0 2\u0006\u0010\u0017\u001a\u00020\u000bH\u0016J\u0016\u0010#\u001a\b\u0012\u0004\u0012\u00020\"0 2\u0006\u0010\u0017\u001a\u00020\u000bH\u0016J,\u0010$\u001a\b\u0012\u0004\u0012\u00020\u00160%2\u0006\u0010\u0017\u001a\u00020\u000b2\u0006\u0010&\u001a\u00020\u000bH\u0096@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\'\u0010(R\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\n\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000b0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\f\u001a\b\u0012\u0004\u0012\u00020\t0\rX\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u0019\u0010\u0010\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000b0\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u000fR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0013X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0014\u001a\u00020\u000bX\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006)"}, d2 = {"Lcom/btelo/coding/data/repository/MessageRepositoryImpl;", "Lcom/btelo/coding/domain/repository/MessageRepository;", "messageDao", "Lcom/btelo/coding/data/local/dao/MessageDao;", "webSocketFactory", "Lcom/btelo/coding/data/remote/websocket/factory/WebSocketClientFactory;", "(Lcom/btelo/coding/data/local/dao/MessageDao;Lcom/btelo/coding/data/remote/websocket/factory/WebSocketClientFactory;)V", "_connectionState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/btelo/coding/data/remote/websocket/factory/ConnectionState;", "_currentSessionId", "", "connectionState", "Lkotlinx/coroutines/flow/StateFlow;", "getConnectionState", "()Lkotlinx/coroutines/flow/StateFlow;", "currentSessionId", "getCurrentSessionId", "scope", "Lkotlinx/coroutines/CoroutineScope;", "tag", "cleanOldMessages", "", "sessionId", "keepDays", "", "(Ljava/lang/String;ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "connect", "serverAddress", "token", "disconnect", "getMessages", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/btelo/coding/domain/model/Message;", "observeOutput", "sendMessage", "Lkotlin/Result;", "content", "sendMessage-0E7RQCE", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class MessageRepositoryImpl implements com.btelo.coding.domain.repository.MessageRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.data.local.dao.MessageDao messageDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.data.remote.websocket.factory.WebSocketClientFactory webSocketFactory = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String tag = "MessageRepository";
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope scope = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _currentSessionId = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> currentSessionId = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.btelo.coding.data.remote.websocket.factory.ConnectionState> _connectionState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.btelo.coding.data.remote.websocket.factory.ConnectionState> connectionState = null;
    
    @javax.inject.Inject()
    public MessageRepositoryImpl(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.dao.MessageDao messageDao, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.websocket.factory.WebSocketClientFactory webSocketFactory) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getCurrentSessionId() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.StateFlow<com.btelo.coding.data.remote.websocket.factory.ConnectionState> getConnectionState() {
        return null;
    }
    
    @java.lang.Override()
    public void connect(@org.jetbrains.annotations.NotNull()
    java.lang.String serverAddress, @org.jetbrains.annotations.NotNull()
    java.lang.String token, @org.jetbrains.annotations.NotNull()
    java.lang.String sessionId) {
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<java.util.List<com.btelo.coding.domain.model.Message>> getMessages(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<com.btelo.coding.domain.model.Message> observeOutput(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId) {
        return null;
    }
    
    @java.lang.Override()
    public void disconnect(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId) {
    }
    
    /**
     * 清理旧的聊天记录（保留最近的消息）
     */
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object cleanOldMessages(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId, int keepDays, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}