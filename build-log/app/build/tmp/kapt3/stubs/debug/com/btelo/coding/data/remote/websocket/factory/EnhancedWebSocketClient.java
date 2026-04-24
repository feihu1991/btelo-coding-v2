package com.btelo.coding.data.remote.websocket.factory;

import com.btelo.coding.data.remote.encryption.CryptoManager;
import com.btelo.coding.data.remote.encryption.KeyPair;
import com.btelo.coding.data.remote.encryption.KeyRotationManager;
import com.btelo.coding.data.remote.encryption.KeyRotationState;
import com.btelo.coding.data.remote.network.NetworkMonitor;
import com.btelo.coding.data.remote.websocket.BteloMessage;
import com.btelo.coding.data.remote.websocket.MessageProtocol;
import com.btelo.coding.util.Logger;
import com.google.gson.Gson;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.SharedFlow;
import kotlinx.coroutines.flow.StateFlow;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 增强的WebSocket客户端，支持重连机制、心跳保活和密钥轮换（前向保密）
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u00c0\u0001\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0010\u0006\n\u0002\b\u0002\u0018\u00002\u00020\u0001B9\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\r\u00a2\u0006\u0002\u0010\u000eJ\u000e\u0010<\u001a\u00020=2\u0006\u0010\u001b\u001a\u00020\u001cJ\u0006\u0010>\u001a\u00020=J\u0006\u0010?\u001a\u00020(J\u0006\u0010@\u001a\u00020\"J\u0010\u0010A\u001a\u00020=2\u0006\u0010B\u001a\u00020CH\u0002J\u0010\u0010D\u001a\u00020=2\u0006\u0010E\u001a\u000209H\u0002J\b\u0010F\u001a\u00020=H\u0002J\u000e\u0010G\u001a\u00020(2\u0006\u0010B\u001a\u00020\u0018J\b\u0010H\u001a\u00020=H\u0002J\b\u0010I\u001a\u00020=H\u0002J\u0010\u0010J\u001a\u00020=2\u0006\u0010K\u001a\u00020LH\u0002J\b\u0010M\u001a\u00020=H\u0002J\u0006\u0010N\u001a\u00020(J\u0014\u0010O\u001a\u00020P*\u00020P2\u0006\u0010Q\u001a\u00020PH\u0002R\u0014\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00110\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00140\u0013X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00160\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00180\u0013X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0019\u001a\u0004\u0018\u00010\u001aX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u001b\u001a\u0004\u0018\u00010\u001cX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u00110\u001e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010 R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010!\u001a\u00020\"X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0017\u0010#\u001a\b\u0012\u0004\u0012\u00020\u00140$\u00a2\u0006\b\n\u0000\u001a\u0004\b%\u0010&R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\'\u001a\u00020(X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010)\u001a\u0004\u0018\u00010*X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010+\u001a\u0004\u0018\u00010,X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\f\u001a\u0004\u0018\u00010\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010-\u001a\b\u0012\u0004\u0012\u00020\u00160\u001e\u00a2\u0006\b\n\u0000\u001a\u0004\b.\u0010 R\u0017\u0010/\u001a\b\u0012\u0004\u0012\u00020\u00180$\u00a2\u0006\b\n\u0000\u001a\u0004\b0\u0010&R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u00101\u001a\u0004\u0018\u00010,X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u00102\u001a\u000203X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u00104\u001a\u00020\"X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u00105\u001a\u0004\u0018\u00010,X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u00106\u001a\u000207X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u00108\u001a\u000209X\u0082D\u00a2\u0006\u0002\n\u0000R\u0010\u0010:\u001a\u0004\u0018\u00010;X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006R"}, d2 = {"Lcom/btelo/coding/data/remote/websocket/factory/EnhancedWebSocketClient;", "", "okHttpClient", "Lokhttp3/OkHttpClient;", "gson", "Lcom/google/gson/Gson;", "cryptoManager", "Lcom/btelo/coding/data/remote/encryption/CryptoManager;", "networkMonitor", "Lcom/btelo/coding/data/remote/network/NetworkMonitor;", "secureKeyStore", "Lcom/btelo/coding/data/remote/encryption/SecureKeyStore;", "keyRotationManager", "Lcom/btelo/coding/data/remote/encryption/KeyRotationManager;", "(Lokhttp3/OkHttpClient;Lcom/google/gson/Gson;Lcom/btelo/coding/data/remote/encryption/CryptoManager;Lcom/btelo/coding/data/remote/network/NetworkMonitor;Lcom/btelo/coding/data/remote/encryption/SecureKeyStore;Lcom/btelo/coding/data/remote/encryption/KeyRotationManager;)V", "_connectionState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/btelo/coding/data/remote/websocket/factory/ConnectionState;", "_events", "Lkotlinx/coroutines/flow/MutableSharedFlow;", "Lcom/btelo/coding/data/remote/websocket/factory/WebSocketEvent;", "_keyRotationState", "Lcom/btelo/coding/data/remote/encryption/KeyRotationState;", "_messages", "Lcom/btelo/coding/data/remote/websocket/BteloMessage;", "cipher", "Lcom/google/crypto/tink/subtle/ChaCha20Poly1305;", "config", "Lcom/btelo/coding/data/remote/websocket/factory/WebSocketConfig;", "connectionState", "Lkotlinx/coroutines/flow/StateFlow;", "getConnectionState", "()Lkotlinx/coroutines/flow/StateFlow;", "currentKeyVersion", "", "events", "Lkotlinx/coroutines/flow/SharedFlow;", "getEvents", "()Lkotlinx/coroutines/flow/SharedFlow;", "isEncrypted", "", "keyPair", "Lcom/btelo/coding/data/remote/encryption/KeyPair;", "keyRotationCheckJob", "Lkotlinx/coroutines/Job;", "keyRotationState", "getKeyRotationState", "messages", "getMessages", "pingJob", "protocol", "Lcom/btelo/coding/data/remote/websocket/MessageProtocol;", "reconnectAttempt", "reconnectJob", "scope", "Lkotlinx/coroutines/CoroutineScope;", "tag", "", "webSocket", "Lokhttp3/WebSocket;", "connect", "", "destroy", "disconnect", "getCurrentKeyVersion", "handleKeyRotationMessage", "message", "Lcom/btelo/coding/data/remote/websocket/BteloMessage$KeyRotation;", "handleMessage", "text", "scheduleReconnect", "send", "startConnection", "startKeyRotationCheck", "startPingPong", "intervalMs", "", "stopPingPong", "triggerKeyRotation", "pow", "", "n", "app_debug"})
public final class EnhancedWebSocketClient {
    @org.jetbrains.annotations.NotNull()
    private final okhttp3.OkHttpClient okHttpClient = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.gson.Gson gson = null;
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.data.remote.encryption.CryptoManager cryptoManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.data.remote.network.NetworkMonitor networkMonitor = null;
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.data.remote.encryption.SecureKeyStore secureKeyStore = null;
    @org.jetbrains.annotations.Nullable()
    private final com.btelo.coding.data.remote.encryption.KeyRotationManager keyRotationManager = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String tag = "EnhancedWebSocket";
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope scope = null;
    @org.jetbrains.annotations.Nullable()
    private okhttp3.WebSocket webSocket;
    @org.jetbrains.annotations.Nullable()
    private com.btelo.coding.data.remote.websocket.factory.WebSocketConfig config;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job reconnectJob;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job pingJob;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job keyRotationCheckJob;
    @org.jetbrains.annotations.Nullable()
    private com.btelo.coding.data.remote.encryption.KeyPair keyPair;
    @org.jetbrains.annotations.Nullable()
    private com.google.crypto.tink.subtle.ChaCha20Poly1305 cipher;
    private boolean isEncrypted = false;
    private int currentKeyVersion = 1;
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.data.remote.websocket.MessageProtocol protocol = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.btelo.coding.data.remote.websocket.factory.ConnectionState> _connectionState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.btelo.coding.data.remote.websocket.factory.ConnectionState> connectionState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableSharedFlow<com.btelo.coding.data.remote.websocket.BteloMessage> _messages = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.SharedFlow<com.btelo.coding.data.remote.websocket.BteloMessage> messages = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableSharedFlow<com.btelo.coding.data.remote.websocket.factory.WebSocketEvent> _events = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.SharedFlow<com.btelo.coding.data.remote.websocket.factory.WebSocketEvent> events = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.btelo.coding.data.remote.encryption.KeyRotationState> _keyRotationState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.btelo.coding.data.remote.encryption.KeyRotationState> keyRotationState = null;
    private int reconnectAttempt = 0;
    
    public EnhancedWebSocketClient(@org.jetbrains.annotations.NotNull()
    okhttp3.OkHttpClient okHttpClient, @org.jetbrains.annotations.NotNull()
    com.google.gson.Gson gson, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.encryption.CryptoManager cryptoManager, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.network.NetworkMonitor networkMonitor, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.encryption.SecureKeyStore secureKeyStore, @org.jetbrains.annotations.Nullable()
    com.btelo.coding.data.remote.encryption.KeyRotationManager keyRotationManager) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.btelo.coding.data.remote.websocket.factory.ConnectionState> getConnectionState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.SharedFlow<com.btelo.coding.data.remote.websocket.BteloMessage> getMessages() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.SharedFlow<com.btelo.coding.data.remote.websocket.factory.WebSocketEvent> getEvents() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.btelo.coding.data.remote.encryption.KeyRotationState> getKeyRotationState() {
        return null;
    }
    
    /**
     * 连接到服务器
     */
    public final void connect(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.websocket.factory.WebSocketConfig config) {
    }
    
    private final void startConnection() {
    }
    
    private final void handleMessage(java.lang.String text) {
    }
    
    /**
     * 处理密钥轮换消息
     */
    private final void handleKeyRotationMessage(com.btelo.coding.data.remote.websocket.BteloMessage.KeyRotation message) {
    }
    
    /**
     * 触发密钥轮换
     */
    public final boolean triggerKeyRotation() {
        return false;
    }
    
    /**
     * 启动密钥轮换检查（定时检查是否需要轮换）
     */
    private final void startKeyRotationCheck() {
    }
    
    /**
     * 获取当前密钥版本
     */
    public final int getCurrentKeyVersion() {
        return 0;
    }
    
    private final void scheduleReconnect() {
    }
    
    private final void startPingPong(long intervalMs) {
    }
    
    private final void stopPingPong() {
    }
    
    public final boolean send(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.websocket.BteloMessage message) {
        return false;
    }
    
    public final boolean disconnect() {
        return false;
    }
    
    public final void destroy() {
    }
    
    private final double pow(double $this$pow, double n) {
        return 0.0;
    }
}