package com.btelo.coding.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppExceptionTest {

    @Test
    fun `JsonParseException should capture message and cause`() {
        val cause = RuntimeException("JSON syntax error")
        val exception = AppException.JsonParseException("invalid json", cause)
        
        assertEquals("JSON解析失败: invalid json", exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun `NetworkException should capture message and cause`() {
        val cause = java.net.UnknownHostException("Host not found")
        val exception = AppException.NetworkException("无法连接服务器", cause)
        
        assertEquals("无法连接服务器", exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun `WebSocketException should capture message and cause`() {
        val exception = AppException.WebSocketException("连接超时")
        
        assertEquals("连接超时", exception.message)
        assertEquals(null, exception.cause)
    }

    @Test
    fun `fromThrowable should convert JsonSyntaxException to JsonParseException`() {
        val cause = com.google.gson.JsonSyntaxException("Expected BEGIN_OBJECT but was STRING")
        val exception = AppException.from(cause)
        
        assertTrue(exception is AppException.JsonParseException)
    }

    @Test
    fun `fromThrowable should convert UnknownHostException to NetworkException`() {
        val cause = java.net.UnknownHostException("example.com")
        val exception = AppException.from(cause)
        
        assertTrue(exception is AppException.NetworkException)
    }

    @Test
    fun `fromThrowable should preserve AppException`() {
        val original = AppException.CryptoException("Decryption failed")
        val converted = AppException.from(original)
        
        assertEquals(original, converted)
    }
}
