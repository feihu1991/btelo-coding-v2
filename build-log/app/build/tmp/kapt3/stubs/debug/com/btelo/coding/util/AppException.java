package com.btelo.coding.util;

/**
 * 统一的异常封装类
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0003\n\u0002\b\n\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u0000 \t2\u00060\u0001j\u0002`\u0002:\t\b\t\n\u000b\f\r\u000e\u000f\u0010B\u001b\b\u0004\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\u0002\u0010\u0007\u0082\u0001\b\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u00a8\u0006\u0019"}, d2 = {"Lcom/btelo/coding/util/AppException;", "Ljava/lang/Exception;", "Lkotlin/Exception;", "message", "", "cause", "", "(Ljava/lang/String;Ljava/lang/Throwable;)V", "AuthException", "Companion", "CryptoException", "DatabaseException", "JsonParseException", "KeyStoreException", "NetworkException", "UnknownException", "WebSocketException", "Lcom/btelo/coding/util/AppException$AuthException;", "Lcom/btelo/coding/util/AppException$CryptoException;", "Lcom/btelo/coding/util/AppException$DatabaseException;", "Lcom/btelo/coding/util/AppException$JsonParseException;", "Lcom/btelo/coding/util/AppException$KeyStoreException;", "Lcom/btelo/coding/util/AppException$NetworkException;", "Lcom/btelo/coding/util/AppException$UnknownException;", "Lcom/btelo/coding/util/AppException$WebSocketException;", "app_debug"})
public abstract class AppException extends java.lang.Exception {
    @org.jetbrains.annotations.NotNull()
    public static final com.btelo.coding.util.AppException.Companion Companion = null;
    
    private AppException(java.lang.String message, java.lang.Throwable cause) {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0003\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0019\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\u0002\u0010\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/btelo/coding/util/AppException$AuthException;", "Lcom/btelo/coding/util/AppException;", "message", "", "cause", "", "(Ljava/lang/String;Ljava/lang/Throwable;)V", "app_debug"})
    public static final class AuthException extends com.btelo.coding.util.AppException {
        
        public AuthException(@org.jetbrains.annotations.NotNull()
        java.lang.String message, @org.jetbrains.annotations.Nullable()
        java.lang.Throwable cause) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0003\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/btelo/coding/util/AppException$Companion;", "", "()V", "from", "Lcom/btelo/coding/util/AppException;", "throwable", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.btelo.coding.util.AppException from(@org.jetbrains.annotations.NotNull()
        java.lang.Throwable throwable) {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0003\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0019\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\u0002\u0010\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/btelo/coding/util/AppException$CryptoException;", "Lcom/btelo/coding/util/AppException;", "message", "", "cause", "", "(Ljava/lang/String;Ljava/lang/Throwable;)V", "app_debug"})
    public static final class CryptoException extends com.btelo.coding.util.AppException {
        
        public CryptoException(@org.jetbrains.annotations.NotNull()
        java.lang.String message, @org.jetbrains.annotations.Nullable()
        java.lang.Throwable cause) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0003\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0019\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\u0002\u0010\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/btelo/coding/util/AppException$DatabaseException;", "Lcom/btelo/coding/util/AppException;", "message", "", "cause", "", "(Ljava/lang/String;Ljava/lang/Throwable;)V", "app_debug"})
    public static final class DatabaseException extends com.btelo.coding.util.AppException {
        
        public DatabaseException(@org.jetbrains.annotations.NotNull()
        java.lang.String message, @org.jetbrains.annotations.Nullable()
        java.lang.Throwable cause) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0003\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/btelo/coding/util/AppException$JsonParseException;", "Lcom/btelo/coding/util/AppException;", "json", "", "cause", "", "(Ljava/lang/String;Ljava/lang/Throwable;)V", "app_debug"})
    public static final class JsonParseException extends com.btelo.coding.util.AppException {
        
        public JsonParseException(@org.jetbrains.annotations.NotNull()
        java.lang.String json, @org.jetbrains.annotations.NotNull()
        java.lang.Throwable cause) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0003\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0019\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\u0002\u0010\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/btelo/coding/util/AppException$KeyStoreException;", "Lcom/btelo/coding/util/AppException;", "message", "", "cause", "", "(Ljava/lang/String;Ljava/lang/Throwable;)V", "app_debug"})
    public static final class KeyStoreException extends com.btelo.coding.util.AppException {
        
        public KeyStoreException(@org.jetbrains.annotations.NotNull()
        java.lang.String message, @org.jetbrains.annotations.Nullable()
        java.lang.Throwable cause) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0003\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0019\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\u0002\u0010\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/btelo/coding/util/AppException$NetworkException;", "Lcom/btelo/coding/util/AppException;", "message", "", "cause", "", "(Ljava/lang/String;Ljava/lang/Throwable;)V", "app_debug"})
    public static final class NetworkException extends com.btelo.coding.util.AppException {
        
        public NetworkException(@org.jetbrains.annotations.NotNull()
        java.lang.String message, @org.jetbrains.annotations.Nullable()
        java.lang.Throwable cause) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0003\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004\u00a8\u0006\u0005"}, d2 = {"Lcom/btelo/coding/util/AppException$UnknownException;", "Lcom/btelo/coding/util/AppException;", "cause", "", "(Ljava/lang/Throwable;)V", "app_debug"})
    public static final class UnknownException extends com.btelo.coding.util.AppException {
        
        public UnknownException(@org.jetbrains.annotations.NotNull()
        java.lang.Throwable cause) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0003\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0019\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\u0002\u0010\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/btelo/coding/util/AppException$WebSocketException;", "Lcom/btelo/coding/util/AppException;", "message", "", "cause", "", "(Ljava/lang/String;Ljava/lang/Throwable;)V", "app_debug"})
    public static final class WebSocketException extends com.btelo.coding.util.AppException {
        
        public WebSocketException(@org.jetbrains.annotations.NotNull()
        java.lang.String message, @org.jetbrains.annotations.Nullable()
        java.lang.Throwable cause) {
        }
    }
}