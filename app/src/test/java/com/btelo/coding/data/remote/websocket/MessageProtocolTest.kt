package com.btelo.coding.data.remote.websocket

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class MessageProtocolTest {

    private val gson = Gson()
    private val protocol = MessageProtocol(gson)

    @Test
    fun `serialize command message should produce valid JSON`() {
        val message = BteloMessage.Command(
            content = "git status",
            type = InputType.TEXT
        )

        val json = protocol.serialize(message)

        assertTrue(json.contains("\"type\":\"command\""))
        assertTrue(json.contains("\"content\":\"git status\""))
    }

    @Test
    fun `deserialize output message should return correct object`() {
        val json = """{"type":"output","data":"On branch main","stream":"STDOUT"}"""

        val message = protocol.deserialize(json)

        assertTrue(message is BteloMessage.Output)
        assertEquals("On branch main", (message as BteloMessage.Output).data)
    }

    @Test
    fun `deserialize unknown type should return null`() {
        val json = """{"type":"unknown"}"""

        val message = protocol.deserialize(json)

        assertNull(message)
    }
}
