package com.btelo.coding.data.remote.websocket.factory;

import com.btelo.coding.data.remote.encryption.CryptoManager;
import com.btelo.coding.data.remote.network.NetworkMonitor;
import com.btelo.coding.data.remote.websocket.BteloMessage;
import com.btelo.coding.util.Logger;
import com.google.gson.Gson;
import kotlinx.coroutines.flow.SharedFlow;
import kotlinx.coroutines.flow.StateFlow;
import okhttp3.OkHttpClient;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * WebSocket客户端工厂
 * 管理多个会话的WebSocket连接
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000t\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\"\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0007\u0018\u00002\u00020\u0001B/\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u00a2\u0006\u0002\u0010\fJ\u000e\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u000fJ\u0006\u0010\u0015\u001a\u00020\u0013J\u000e\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0014\u001a\u00020\u000fJ\u0006\u0010\u0018\u001a\u00020\u0019J\f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u000f0\u001bJ\u0010\u0010\u001c\u001a\u0004\u0018\u00010\u00102\u0006\u0010\u0014\u001a\u00020\u000fJ\u0016\u0010\u001d\u001a\n\u0012\u0004\u0012\u00020\u001f\u0018\u00010\u001e2\u0006\u0010\u0014\u001a\u00020\u000fJ\u0016\u0010 \u001a\n\u0012\u0004\u0012\u00020\"\u0018\u00010!2\u0006\u0010\u0014\u001a\u00020\u000fJ\u000e\u0010#\u001a\u00020\u00102\u0006\u0010$\u001a\u00020%J\u000e\u0010&\u001a\u00020\u00172\u0006\u0010\u0014\u001a\u00020\u000fJ\u0016\u0010\'\u001a\u00020\u00172\u0006\u0010\u0014\u001a\u00020\u000f2\u0006\u0010(\u001a\u00020\"R\u001a\u0010\r\u001a\u000e\u0012\u0004\u0012\u00020\u000f\u0012\u0004\u0012\u00020\u00100\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u000fX\u0082D\u00a2\u0006\u0002\n\u0000\u00a8\u0006)"}, d2 = {"Lcom/btelo/coding/data/remote/websocket/factory/WebSocketClientFactory;", "", "okHttpClient", "Lokhttp3/OkHttpClient;", "gson", "Lcom/google/gson/Gson;", "cryptoManager", "Lcom/btelo/coding/data/remote/encryption/CryptoManager;", "networkMonitor", "Lcom/btelo/coding/data/remote/network/NetworkMonitor;", "secureKeyStore", "Lcom/btelo/coding/data/remote/encryption/SecureKeyStore;", "(Lokhttp3/OkHttpClient;Lcom/google/gson/Gson;Lcom/btelo/coding/data/remote/encryption/CryptoManager;Lcom/btelo/coding/data/remote/network/NetworkMonitor;Lcom/btelo/coding/data/remote/encryption/SecureKeyStore;)V", "clients", "Ljava/util/concurrent/ConcurrentHashMap;", "", "Lcom/btelo/coding/data/remote/websocket/factory/EnhancedWebSocketClient;", "tag", "destroy", "", "sessionId", "destroyAll", "disconnect", "", "getActiveClientCount", "", "getActiveSessionIds", "", "getClient", "getConnectionState", "Lkotlinx/coroutines/flow/StateFlow;", "Lcom/btelo/coding/data/remote/websocket/factory/ConnectionState;", "getMessages", "Lkotlinx/coroutines/flow/SharedFlow;", "Lcom/btelo/coding/data/remote/websocket/BteloMessage;", "getOrCreate", "config", "Lcom/btelo/coding/data/remote/websocket/factory/WebSocketConfig;", "hasClient", "sendMessage", "message", "app_debug"})
public final class WebSocketClientFactory {
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
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String tag = "WebSocketFactory";
    @org.jetbrains.annotations.NotNull()
    private final java.util.concurrent.ConcurrentHashMap<java.lang.String, com.btelo.coding.data.remote.websocket.factory.EnhancedWebSocketClient> clients = null;
    
    @javax.inject.Inject()
    public WebSocketClientFactory(@org.jetbrains.annotations.NotNull()
    okhttp3.OkHttpClient okHttpClient, @org.jetbrains.annotations.NotNull()
    com.google.gson.Gson gson, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.encryption.CryptoManager cryptoManager, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.network.NetworkMonitor networkMonitor, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.encryption.SecureKeyStore secureKeyStore) {
        super();
    }
    
    /**
     * 为指定会话创建或获取WebSocket客户端
     */
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.remote.websocket.factory.EnhancedWebSocketClient getOrCreate(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.websocket.factory.WebSocketConfig config) {
        return null;
    }
    
    /**
     * 检查指定会话是否有活跃的客户端
     */
    public final boolean hasClient(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId) {
        return false;
    }
    
    /**
     * 获取指定会话的客户端
     */
    @org.jetbrains.annotations.Nullable()
    public final com.btelo.coding.data.remote.websocket.factory.EnhancedWebSocketClient getClient(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId) {
        return null;
    }
    
    /**
     * 获取指定会话的连接状态
     */
    @org.jetbrains.annotations.Nullable()
    public final kotlinx.coroutines.flow.StateFlow<com.btelo.coding.data.remote.websocket.factory.ConnectionState> getConnectionState(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId) {
        return null;
    }
    
    /**
     * 获取指定会话的消息流
     */
    @org.jetbrains.annotations.Nullable()
    public final kotlinx.coroutines.flow.SharedFlow<com.btelo.coding.data.remote.websocket.BteloMessage> getMessages(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId) {
        return null;
    }
    
    /**
     * 向指定会话发送消息
     */
    public final boolean sendMessage(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.websocket.BteloMessage message) {
        return false;
    }
    
    /**
     * 断开指定会话的连接
     */
    public final boolean disconnect(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId) {
        return false;
    }
    
    /**
     * 销毁指定会话的客户端并清理资源
     */
    public final void destroy(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId) {
    }
    
    /**
     * 断开所有连接并清理
     */
    public final void destroyAll() {
    }
    
    /**
     * 获取所有活跃会话ID
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.Set<java.lang.String> getActiveSessionIds() {
        return null;
    }
    
    /**
     * 获取活跃客户端数量
     */
    public final int getActiveClientCount() {
        return 0;
    }
}