package com.btelo.coding.data.remote.websocket.factory

import com.btelo.coding.data.remote.websocket.BteloMessage
import com.btelo.coding.data.remote.websocket.InputType
import com.btelo.coding.data.remote.websocket.MessageProtocol
import com.btelo.coding.data.remote.websocket.StreamType
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MessageProtocolTest {

    private lateinit var protocol: MessageProtocol

    @Before
    fun setup() {
        protocol = MessageProtocol(Gson())
    }

    @Test
    fun `serialize Command message should produce valid JSON`() {
        val message = BteloMessage.Command("ls -la", InputType.TEXT)
        val json = protocol.serialize(message)
        
        assertTrue(json.contains("\"type\":\"command\""))
        assertTrue(json.contains("\"content\":\"ls -la\""))
        assertTrue(json.contains("\"inputType\":\"TEXT\""))
    }

    @Test
    fun `serialize Output message should produce valid JSON`() {
        val message = BteloMessage.Output("Hello World", StreamType.STDOUT)
        val json = protocol.serialize(message)
        
        assertTrue(json.contains("\"type\":\"output\""))
        assertTrue(json.contains("\"data\":\"Hello World\""))
        assertTrue(json.contains("\"stream\":\"STDOUT\""))
    }

    @Test
    fun `serialize Status message should produce valid JSON`() {
        val message = BteloMessage.Status(connected = true)
        val json = protocol.serialize(message)
        
        assertTrue(json.contains("\"type\":\"status\""))
        assertTrue(json.contains("\"connected\":true"))
    }

    @Test
    fun `serialize PublicKey message should produce valid JSON`() {
        val message = BteloMessage.PublicKey("abc123")
        val json = protocol.serialize(message)
        
        assertTrue(json.contains("\"type\":\"publicKey\""))
        assertTrue(json.contains("\"key\":\"abc123\""))
    }

    @Test
    fun `deserialize Command message should parse correctly`() {
        val json = """{"type":"command","content":"test command","inputType":"TEXT"}"""
        val message = protocol.deserialize(json)
        
        assertNotNull(message)
        assertTrue(message is BteloMessage.Command)
        assertEquals("test command", (message as BteloMessage.Command).content)
        assertEquals(InputType.TEXT, message.type)
    }

    @Test
    fun `deserialize Output message should parse correctly`() {
        val json = """{"type":"output","data":"output data","stream":"STDERR"}"""
        val message = protocol.deserialize(json)
        
        assertNotNull(message)
        assertTrue(message is BteloMessage.Output)
        assertEquals("output data", (message as BteloMessage.Output).data)
        assertEquals(StreamType.STDERR, message.stream)
    }

    @Test
    fun `deserialize Status message should parse correctly`() {
        val json = """{"type":"status","connected":false}"""
        val message = protocol.deserialize(json)
        
        assertNotNull(message)
        assertTrue(message is BteloMessage.Status)
        assertEquals(false, (message as BteloMessage.Status).connected)
    }

    @Test
    fun `deserialize PublicKey message should parse correctly`() {
        val json = """{"type":"publicKey","key":"base64key=="}"""
        val message = protocol.deserialize(json)
        
        assertNotNull(message)
        assertTrue(message is BteloMessage.PublicKey)
        assertEquals("base64key==", (message as BteloMessage.PublicKey).key)
    }

    @Test
    fun `deserialize invalid JSON should return null`() {
        val json = """{invalid json"""
        val message = protocol.deserialize(json)
        
        assertNull(message)
    }

    @Test
    fun `deserialize unknown type should return null`() {
        val json = """{"type":"unknown_type","data":"test"}"""
        val message = protocol.deserialize(json)
        
        assertNull(message)
    }

    @Test
    fun `deserialize missing fields should return null`() {
        val json = """{"type":"command"}"""
        val message = protocol.deserialize(json)
        
        // 应该处理缺失字段的情况，返回部分数据
        assertNull(message)
    }

    @Test
    fun `deserialize lowercase inputType should handle correctly`() {
        val json = """{"type":"command","content":"test","inputType":"text"}"""
        val message = protocol.deserialize(json)
        
        assertNotNull(message)
        assertTrue(message is BteloMessage.Command)
        assertEquals(InputType.TEXT, (message as BteloMessage.Command).type)
    }

    @Test
    fun `deserialize malformed JSON should return null`() {
        val json = """{"type":}"""
        val message = protocol.deserialize(json)
        
        assertNull(message)
    }

    @Test
    fun `roundtrip serialize and deserialize should preserve data`() {
        val original = BteloMessage.Command("echo hello", InputType.TEXT)
        val json = protocol.serialize(original)
        val restored = protocol.deserialize(json)
        
        assertNotNull(restored)
        assertTrue(restored is BteloMessage.Command)
        val restoredCommand = restored as BteloMessage.Command
        assertEquals(original.content, restoredCommand.content)
        assertEquals(original.type, restoredCommand.type)
    }
}
