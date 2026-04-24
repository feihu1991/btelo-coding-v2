package com.btelo.coding.data.remote.websocket.factory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebSocketConfigTest {

    @Test
    fun `default ReconnectConfig should have reasonable values`() {
        val config = ReconnectConfig()
        
        assertEquals(1000L, config.initialDelayMs)
        assertEquals(30000L, config.maxDelayMs)
        assertEquals(Int.MAX_VALUE, config.maxAttempts)
        assertEquals(2.0, config.multiplier, 0.0)
        assertEquals(20, config.jitterPercent)
    }

    @Test
    fun `custom ReconnectConfig should preserve values`() {
        val config = ReconnectConfig(
            initialDelayMs = 500L,
            maxDelayMs = 60000L,
            maxAttempts = 5,
            multiplier = 1.5,
            jitterPercent = 10
        )
        
        assertEquals(500L, config.initialDelayMs)
        assertEquals(60000L, config.maxDelayMs)
        assertEquals(5, config.maxAttempts)
        assertEquals(1.5, config.multiplier, 0.0)
        assertEquals(10, config.jitterPercent)
    }

    @Test
    fun `default WebSocketConfig should have reasonable values`() {
        val config = WebSocketConfig(
            sessionId = "test-session",
            serverAddress = "https://example.com",
            token = "test-token"
        )
        
        assertEquals("test-session", config.sessionId)
        assertEquals("https://example.com", config.serverAddress)
        assertEquals("test-token", config.token)
        assertEquals(ReconnectConfig(), config.reconnectConfig)
        assertEquals(30000L, config.pingIntervalMs)
        assertEquals(10000L, config.pongTimeoutMs)
    }

    @Test
    fun `custom WebSocketConfig should preserve all values`() {
        val reconnectConfig = ReconnectConfig(maxAttempts = 3)
        val config = WebSocketConfig(
            sessionId = "custom-session",
            serverAddress = "ws://localhost:8080",
            token = "custom-token",
            reconnectConfig = reconnectConfig,
            pingIntervalMs = 15000L,
            pongTimeoutMs = 5000L
        )
        
        assertEquals("custom-session", config.sessionId)
        assertEquals("ws://localhost:8080", config.serverAddress)
        assertEquals("custom-token", config.token)
        assertEquals(reconnectConfig, config.reconnectConfig)
        assertEquals(15000L, config.pingIntervalMs)
        assertEquals(5000L, config.pongTimeoutMs)
    }

    @Test
    fun `ConnectionState should be sealed class with correct states`() {
        val disconnected = ConnectionState.Disconnected
        val connecting = ConnectionState.Connecting
        val connected = ConnectionState.Connected
        val reconnecting = ConnectionState.Reconnecting(3)
        val error = ConnectionState.Error("Test error")
        
        assertTrue(disconnected is ConnectionState.Disconnected)
        assertTrue(connecting is ConnectionState.Connecting)
        assertTrue(connected is ConnectionState.Connected)
        assertTrue(reconnecting is ConnectionState.Reconnecting)
        assertTrue(error is ConnectionState.Error)
        
        val reconnectingState = reconnecting as ConnectionState.Reconnecting
        assertEquals(3, reconnectingState.attempt)
        
        val errorState = error as ConnectionState.Error
        assertEquals("Test error", errorState.message)
    }

    @Test
    fun `WebSocketEvent should be sealed class with correct variants`() {
        val connected = WebSocketEvent.Connected("session-1")
        val disconnected = WebSocketEvent.Disconnected("session-1", "User requested")
        val message = WebSocketEvent.MessageReceived(com.btelo.coding.data.remote.websocket.BteloMessage.Status(true))
        val reconnecting = WebSocketEvent.Reconnecting("session-1", 2, 1000L)
        val reconnectFailed = WebSocketEvent.ReconnectFailed("session-1", "Max attempts reached")
        val error = WebSocketEvent.Error("session-1", "Network error")
        
        assertTrue(connected is WebSocketEvent.Connected)
        assertTrue(disconnected is WebSocketEvent.Disconnected)
        assertTrue(message is WebSocketEvent.MessageReceived)
        assertTrue(reconnecting is WebSocketEvent.Reconnecting)
        assertTrue(reconnectFailed is WebSocketEvent.ReconnectFailed)
        assertTrue(error is WebSocketEvent.Error)
    }
}
