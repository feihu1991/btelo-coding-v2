package com.btelo.coding.data.remote.api;

import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * 消息同步 API 接口
 * 用于与中继服务器进行消息同步
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\u001e\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\u0007J\u001e\u0010\b\u001a\b\u0012\u0004\u0012\u00020\t0\u00032\b\b\u0001\u0010\n\u001a\u00020\u000bH\u00a7@\u00a2\u0006\u0002\u0010\fJ\u001e\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000e0\u00032\b\b\u0001\u0010\n\u001a\u00020\u000bH\u00a7@\u00a2\u0006\u0002\u0010\fJ\u0014\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00100\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u0011J\u001e\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00130\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u0014H\u00a7@\u00a2\u0006\u0002\u0010\u0015J\u001e\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00170\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u0018H\u00a7@\u00a2\u0006\u0002\u0010\u0019\u00a8\u0006\u001a"}, d2 = {"Lcom/btelo/coding/data/remote/api/SyncApi;", "", "deleteMessage", "Lretrofit2/Response;", "Lcom/btelo/coding/data/remote/api/DeleteMessageResponse;", "request", "Lcom/btelo/coding/data/remote/api/DeleteMessageRequest;", "(Lcom/btelo/coding/data/remote/api/DeleteMessageRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getDeviceSessions", "Lcom/btelo/coding/data/remote/api/DeviceSessionsResponse;", "deviceId", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getSyncState", "Lcom/btelo/coding/data/remote/api/SyncStateResponse;", "getSyncStats", "Lcom/btelo/coding/data/remote/api/SyncStatsResponse;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "pullMessages", "Lcom/btelo/coding/data/remote/api/PullMessagesResponse;", "Lcom/btelo/coding/data/remote/api/PullMessagesRequest;", "(Lcom/btelo/coding/data/remote/api/PullMessagesRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "syncMessage", "Lcom/btelo/coding/data/remote/api/SyncMessageResponse;", "Lcom/btelo/coding/data/remote/api/SyncMessageRequest;", "(Lcom/btelo/coding/data/remote/api/SyncMessageRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface SyncApi {
    
    /**
     * 同步消息到服务器
     */
    @retrofit2.http.POST(value = "sync/messages")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object syncMessage(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.api.SyncMessageRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.btelo.coding.data.remote.api.SyncMessageResponse>> $completion);
    
    /**
     * 拉取消息（增量同步）
     */
    @retrofit2.http.POST(value = "sync/pull")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object pullMessages(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.api.PullMessagesRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.btelo.coding.data.remote.api.PullMessagesResponse>> $completion);
    
    /**
     * 删除消息（软删除）
     */
    @retrofit2.http.POST(value = "sync/delete")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteMessage(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.api.DeleteMessageRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.btelo.coding.data.remote.api.DeleteMessageResponse>> $completion);
    
    /**
     * 获取设备同步状态
     */
    @retrofit2.http.GET(value = "sync/state/{deviceId}")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getSyncState(@retrofit2.http.Path(value = "deviceId")
    @org.jetbrains.annotations.NotNull()
    java.lang.String deviceId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.btelo.coding.data.remote.api.SyncStateResponse>> $completion);
    
    /**
     * 获取设备参与的所有会话
     */
    @retrofit2.http.GET(value = "sync/sessions/{deviceId}")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getDeviceSessions(@retrofit2.http.Path(value = "deviceId")
    @org.jetbrains.annotations.NotNull()
    java.lang.String deviceId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.btelo.coding.data.remote.api.DeviceSessionsResponse>> $completion);
    
    /**
     * 获取同步统计
     */
    @retrofit2.http.GET(value = "sync/stats")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getSyncStats(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.btelo.coding.data.remote.api.SyncStatsResponse>> $completion);
}