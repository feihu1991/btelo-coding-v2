package com.btelo.coding.data.remote.websocket.factory

import com.btelo.coding.data.remote.websocket.BteloMessage

/**
 * WebSocket连接状态
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Reconnecting(val attempt: Int) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/**
 * 重连配置
 */
data class ReconnectConfig(
    val initialDelayMs: Long = 1000L,      // 初始延迟 1s
    val maxDelayMs: Long = 30000L,          // 最大延迟 30s
    val maxAttempts: Int = Int.MAX_VALUE,   // 最大重试次数
    val multiplier: Double = 2.0,           // 退避乘数
    val jitterPercent: Int = 20             // 抖动百分比
)

/**
 * WebSocket事件
 */
sealed class WebSocketEvent {
    data class Connected(val sessionId: String) : WebSocketEvent()
    data class Disconnected(val sessionId: String, val reason: String) : WebSocketEvent()
    data class MessageReceived(val message: BteloMessage) : WebSocketEvent()
    data class Reconnecting(val sessionId: String, val attempt: Int, val delayMs: Long) : WebSocketEvent()
    data class ReconnectFailed(val sessionId: String, val error: String) : WebSocketEvent()
    data class Error(val sessionId: String, val error: String) : WebSocketEvent()
    // 密钥轮换事件
    data class KeyRotationStarted(val sessionId: String, val newVersion: Int) : WebSocketEvent()
    data class KeyRotationCompleted(val previousVersion: Int, val newVersion: Int) : WebSocketEvent()
    data class KeyRotationFailed(val sessionId: String, val error: String) : WebSocketEvent()
}

/**
 * WebSocket客户端配置
 */
data class WebSocketConfig(
    val sessionId: String,
    val serverAddress: String,
    val token: String,
    val reconnectConfig: ReconnectConfig = ReconnectConfig(),
    val pingIntervalMs: Long = 30000L,      // 心跳间隔 30s
    val pongTimeoutMs: Long = 10000L        // pong超时 10s
)
