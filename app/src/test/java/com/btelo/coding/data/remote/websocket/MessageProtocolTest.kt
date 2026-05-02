package com.btelo.coding.data.remote.websocket.factory

import com.btelo.coding.data.remote.websocket.BteloMessage
import com.btelo.coding.data.remote.websocket.InputType
import com.btelo.coding.data.remote.websocket.MessageProtocol
import com.btelo.coding.data.remote.websocket.StreamType
import com.btelo.coding.data.remote.websocket.TranscriptEvent
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
        val message = BteloMessage.Command("ls -la", InputType.TEXT, "phone-1")
        val json = protocol.serialize(message)
        
        assertTrue(json.contains("\"type\":\"command\""))
        assertTrue(json.contains("\"content\":\"ls -la\""))
        assertTrue(json.contains("\"inputType\":\"TEXT\""))
        assertTrue(json.contains("\"client_message_id\":\"phone-1\""))
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

    @Test
    fun `deserialize transcript snapshot should parse stable canonical events`() {
        val json = """
            {
              "type": "transcript_snapshot",
              "version": 3,
              "session_id": "relay-1",
              "claude_session_id": "claude-1",
              "cursor": 2,
              "events": [
                {
                  "id": "u-1",
                  "seq": 1,
                  "role": "user",
                  "kind": "text",
                  "content": "fix tests",
                  "timestamp": 1710000000000,
                  "metadata": {}
                },
                {
                  "id": "a-1-0",
                  "seq": 2,
                  "role": "assistant",
                  "kind": "tool_call",
                  "content": "npm test",
                  "timestamp": 1710000001000,
                  "metadata": {
                    "toolId": "toolu_1",
                    "toolName": "Bash",
                    "command": "npm test",
                    "isCollapsed": true
                  }
                }
              ]
            }
        """.trimIndent()

        val message = protocol.deserialize(json)

        assertNotNull(message)
        assertTrue(message is BteloMessage.TranscriptSnapshot)
        val snapshot = message as BteloMessage.TranscriptSnapshot
        assertEquals("relay-1", snapshot.sessionId)
        assertEquals("claude-1", snapshot.claudeSessionId)
        assertEquals(2, snapshot.cursor)
        assertEquals(2, snapshot.events.size)
        assertEquals("u-1", snapshot.events[0].id)
        assertEquals("tool_call", snapshot.events[1].kind)
        assertEquals("Bash", snapshot.events[1].metadata.toolName)
        assertEquals("npm test", snapshot.events[1].metadata.command)
    }

    @Test
    fun `roundtrip transcript delta should preserve events`() {
        val original = BteloMessage.TranscriptDelta(
            sessionId = "relay-1",
            claudeSessionId = "claude-1",
            cursor = 5,
            events = listOf(
                TranscriptEvent(
                    id = "a-5-0",
                    seq = 5,
                    role = "assistant",
                    kind = "text",
                    content = "Done",
                    timestamp = 1710000002000
                )
            )
        )

        val restored = protocol.deserialize(protocol.serialize(original))

        assertNotNull(restored)
        assertTrue(restored is BteloMessage.TranscriptDelta)
        val delta = restored as BteloMessage.TranscriptDelta
        assertEquals(original.sessionId, delta.sessionId)
        assertEquals(original.claudeSessionId, delta.claudeSessionId)
        assertEquals("Done", delta.events.single().content)
    }
}
