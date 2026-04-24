package com.btelo.coding.data.remote.websocket

import com.google.gson.Gson
import com.google.gson.JsonObject

sealed class BteloMessage {
    data class Command(val content: String, val type: InputType) : BteloMessage()
    data class Output(val data: String, val stream: StreamType) : BteloMessage()
    data class Status(val connected: Boolean) : BteloMessage()
    data class PublicKey(val key: String) : BteloMessage()
}

enum class InputType {
    TEXT, VOICE, IMAGE
}

enum class StreamType {
    STDOUT, STDERR
}

class MessageProtocol(private val gson: Gson) {

    fun serialize(message: BteloMessage): String {
        val json = JsonObject()
        when (message) {
            is BteloMessage.Command -> {
                json.addProperty("type", "command")
                json.addProperty("content", message.content)
                json.addProperty("inputType", message.type.name)
            }
            is BteloMessage.Output -> {
                json.addProperty("type", "output")
                json.addProperty("data", message.data)
                json.addProperty("stream", message.stream.name)
            }
            is BteloMessage.Status -> {
                json.addProperty("type", "status")
                json.addProperty("connected", message.connected)
            }
            is BteloMessage.PublicKey -> {
                json.addProperty("type", "publicKey")
                json.addProperty("key", message.key)
            }
        }
        return gson.toJson(json)
    }

    fun deserialize(json: String): BteloMessage? {
        val jsonObject = gson.fromJson(json, JsonObject::class.java)
        return when (jsonObject.get("type")?.asString) {
            "command" -> BteloMessage.Command(
                content = jsonObject.get("content").asString,
                type = InputType.valueOf(jsonObject.get("inputType").asString.uppercase())
            )
            "output" -> BteloMessage.Output(
                data = jsonObject.get("data").asString,
                stream = StreamType.valueOf(jsonObject.get("stream").asString.uppercase())
            )
            "status" -> BteloMessage.Status(
                connected = jsonObject.get("connected").asBoolean
            )
            "publicKey" -> BteloMessage.PublicKey(
                key = jsonObject.get("key").asString
            )
            else -> null
        }
    }
}
