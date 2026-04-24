package com.btelo.coding.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 消息同步 API 接口
 * 用于与中继服务器进行消息同步
 */
interface SyncApi {

    /**
     * 同步消息到服务器
     */
    @POST("sync/messages")
    suspend fun syncMessage(@Body request: SyncMessageRequest): Response<SyncMessageResponse>

    /**
     * 拉取消息（增量同步）
     */
    @POST("sync/pull")
    suspend fun pullMessages(@Body request: PullMessagesRequest): Response<PullMessagesResponse>

    /**
     * 删除消息（软删除）
     */
    @POST("sync/delete")
    suspend fun deleteMessage(@Body request: DeleteMessageRequest): Response<DeleteMessageResponse>

    /**
     * 获取设备同步状态
     */
    @GET("sync/state/{deviceId}")
    suspend fun getSyncState(@Path("deviceId") deviceId: String): Response<SyncStateResponse>

    /**
     * 获取设备参与的所有会话
     */
    @GET("sync/sessions/{deviceId}")
    suspend fun getDeviceSessions(@Path("deviceId") deviceId: String): Response<DeviceSessionsResponse>

    /**
     * 获取同步统计
     */
    @GET("sync/stats")
    suspend fun getSyncStats(): Response<SyncStatsResponse>
}

// ============ 请求/响应模型 ============

data class SyncMessageRequest(
    val session_id: String,
    val device_id: String,
    val content: String,
    val msg_type: String,  // COMMAND, OUTPUT, ERROR
    val timestamp: Long    // 毫秒时间戳
)

data class SyncMessageResponse(
    val success: Boolean,
    val message_id: String,
    val version: Int
)

data class DeleteMessageRequest(
    val message_id: String,
    val device_id: String
)

data class DeleteMessageResponse(
    val success: Boolean,
    val message: String
)

data class PullMessagesRequest(
    val session_id: String,
    val device_id: String,
    val since_timestamp: Long = 0,  // 毫秒时间戳
    val limit: Int = 100,
    val offset: Int = 0
)

data class PullMessagesResponse(
    val success: Boolean,
    val messages: List<ServerMessage>,
    val total: Int,
    val has_more: Boolean,
    val next_offset: Int
)

data class ServerMessage(
    val id: String,
    val session_id: String,
    val device_id: String,
    val content: String,
    val msg_type: String,
    val timestamp: Long,
    val version: Int,
    val is_deleted: Boolean
)

data class SyncStateResponse(
    val device_id: String,
    val last_sync_timestamp: Long,
    val last_sync_version: Int
)

data class DeviceSessionsResponse(
    val device_id: String,
    val sessions: List<String>,
    val count: Int
)

data class SyncStatsResponse(
    val total_messages: Int,
    val total_sessions: Int,
    val db_path: String
)
