package com.btelo.coding.data.remote.websocket;

import com.btelo.coding.util.AppException;
import com.btelo.coding.util.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0010\u0010\u0007\u001a\u0004\u0018\u00010\b2\u0006\u0010\t\u001a\u00020\u0006J\u0012\u0010\n\u001a\u0004\u0018\u00010\b2\u0006\u0010\u000b\u001a\u00020\fH\u0002J\u000e\u0010\r\u001a\u00020\u00062\u0006\u0010\u000e\u001a\u00020\bR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082D\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000f"}, d2 = {"Lcom/btelo/coding/data/remote/websocket/MessageProtocol;", "", "gson", "Lcom/google/gson/Gson;", "(Lcom/google/gson/Gson;)V", "tag", "", "deserialize", "Lcom/btelo/coding/data/remote/websocket/BteloMessage;", "json", "deserializeInternal", "jsonObject", "Lcom/google/gson/JsonObject;", "serialize", "message", "app_debug"})
public final class MessageProtocol {
    @org.jetbrains.annotations.NotNull()
    private final com.google.gson.Gson gson = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String tag = "MessageProtocol";
    
    public MessageProtocol(@org.jetbrains.annotations.NotNull()
    com.google.gson.Gson gson) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String serialize(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.websocket.BteloMessage message) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.btelo.coding.data.remote.websocket.BteloMessage deserialize(@org.jetbrains.annotations.NotNull()
    java.lang.String json) {
        return null;
    }
    
    private final com.btelo.coding.data.remote.websocket.BteloMessage deserializeInternal(com.google.gson.JsonObject jsonObject) {
        return null;
    }
}