package com.btelo.coding.data.remote.websocket

import com.btelo.coding.util.AppException
import com.btelo.coding.util.Logger
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException

sealed class BteloMessage {
    data class Command(val content: String, val type: InputType) : BteloMessage()
    data class Output(val data: String, val stream: StreamType) : BteloMessage()
    data class Status(val connected: Boolean) : BteloMessage()
    data class PublicKey(val key: String) : BteloMessage()
    // 密钥轮换消息
    data class KeyRotation(
        val action: String,        // "initiate" | "accept" | "complete"
        val newPublicKey: String,  // Base64编码的新公钥
        val keyVersion: Int,       // 密钥版本号
        val timestamp: Long
    ) : BteloMessage()
    // 带密钥版本的数据消息
    data class EncryptedData(
        val data: String,
        val stream: StreamType,
        val keyVersion: Int  // 用于标识使用哪个密钥版本解密
    ) : BteloMessage()
}

enum class InputType {
    TEXT, VOICE, IMAGE
}

enum class StreamType {
    STDOUT, STDERR
}

class MessageProtocol(private val gson: Gson) {
    private val tag = "MessageProtocol"

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
            is BteloMessage.KeyRotation -> {
                json.addProperty("type", "keyRotation")
                json.addProperty("action", message.action)
                json.addProperty("newPublicKey", message.newPublicKey)
                json.addProperty("keyVersion", message.keyVersion)
                json.addProperty("timestamp", message.timestamp)
            }
            is BteloMessage.EncryptedData -> {
                json.addProperty("type", "encryptedData")
                json.addProperty("data", message.data)
                json.addProperty("stream", message.stream.name)
                json.addProperty("keyVersion", message.keyVersion)
            }
        }
        return gson.toJson(json)
    }

    fun deserialize(json: String): BteloMessage? {
        return try {
            val jsonObject = gson.fromJson(json, JsonObject::class.java)
            deserializeInternal(jsonObject)
        } catch (e: JsonSyntaxException) {
            Logger.e(tag, "JSON解析异常: $json", e)
            null
        } catch (e: Exception) {
            Logger.e(tag, "反序列化消息失败: $json", e)
            null
        }
    }

    private fun deserializeInternal(jsonObject: JsonObject): BteloMessage? {
        return try {
            when (jsonObject.get("type")?.asString) {
                "command" -> BteloMessage.Command(
                    content = jsonObject.get("content")?.asString ?: "",
                    type = InputType.valueOf(
                        jsonObject.get("inputType")?.asString?.uppercase() ?: "TEXT"
                    )
                )
                "output" -> BteloMessage.Output(
                    data = jsonObject.get("data")?.asString ?: "",
                    stream = StreamType.valueOf(
                        jsonObject.get("stream")?.asString?.uppercase() ?: "STDOUT"
                    )
                )
                "status" -> BteloMessage.Status(
                    connected = jsonObject.get("connected")?.asBoolean ?: false
                )
                "publicKey" -> BteloMessage.PublicKey(
                    key = jsonObject.get("key")?.asString ?: ""
                )
                "keyRotation" -> BteloMessage.KeyRotation(
                    action = jsonObject.get("action")?.asString ?: "",
                    newPublicKey = jsonObject.get("newPublicKey")?.asString ?: "",
                    keyVersion = jsonObject.get("keyVersion")?.asInt ?: 0,
                    timestamp = jsonObject.get("timestamp")?.asLong ?: 0L
                )
                "encryptedData" -> BteloMessage.EncryptedData(
                    data = jsonObject.get("data")?.asString ?: "",
                    stream = StreamType.valueOf(
                        jsonObject.get("stream")?.asString?.uppercase() ?: "STDOUT"
                    ),
                    keyVersion = jsonObject.get("keyVersion")?.asInt ?: 0
                )
                else -> {
                    Logger.w(tag, "未知的消息类型: ${jsonObject.get("type")?.asString}")
                    null
                }
            }
        } catch (e: IllegalArgumentException) {
            Logger.e(tag, "消息类型解析失败", e)
            null
        } catch (e: Exception) {
            Logger.e(tag, "消息字段解析失败", e)
            null
        }
    }
}
