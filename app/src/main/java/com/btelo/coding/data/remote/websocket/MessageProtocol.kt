package com.btelo.coding.data.remote.websocket

import com.btelo.coding.util.AppException
import com.btelo.coding.util.Logger
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException

/**
 * Structured Output Type Enum for BTELO Coding v2
 * Used for parsing Claude Code stream-json output into structured messages
 */
enum class OutputType {
    /** Regular text response from Claude */
    CLAUDE_RESPONSE,
    
    /** Tool usage call (Bash, Read, Edit, etc.) */
    TOOL_CALL,
    
    /** File operation details */
    FILE_OP,
    
    /** Thinking/thought process */
    THINKING,
    
    /** Error message */
    ERROR,
    
    /** System message */
    SYSTEM
}

/**
 * Hook Event Types from Claude Code BTELO Plugin
 */
enum class HookEventType {
    SESSION_START,
    TASK_COMPLETE,
    WAITING_INPUT,
    PERMISSION_REQUEST,
    PROMPT_SUBMITTED,
    TOOL_COMPLETED
}

sealed class BteloMessage {
    data class Command(val content: String, val type: InputType) : BteloMessage()
    data class Output(val data: String, val stream: StreamType) : BteloMessage()
    data class Status(val connected: Boolean) : BteloMessage()
    data class PublicKey(val key: String) : BteloMessage()
    
    // 密钥轮换消息
    data class KeyRotation(
        val action: String,
        val newPublicKey: String,
        val keyVersion: Int,
        val timestamp: Long
    ) : BteloMessage()
    
    // 带密钥版本的数据消息
    data class EncryptedData(
        val data: String,
        val stream: StreamType,
        val keyVersion: Int
    ) : BteloMessage()
    
    // 会话同步消息
    data class SyncHistory(
        val sessionId: String,
        val messages: List<HistoryMessage>
    ) : BteloMessage()
    
    data class NewMessage(
        val sessionId: String,
        val message: HistoryMessage
    ) : BteloMessage()
    
    data class SelectSession(
        val sessionId: String
    ) : BteloMessage()
    
    /**
     * Structured Output Message (BTELO Coding v2)
     * 
     * Represents parsed Claude Code stream-json output with type classification:
     * - claude_response: Regular text output
     * - tool_call: Tool usage request with metadata
     * - file_op: File operation details
     * - thinking: Thinking process
     * - error: Error message
     * - system: System message
     */
    data class StructuredOutput(
        val outputType: OutputType,
        val content: String,
        val metadata: OutputMetadata,
        val timestamp: String
    ) : BteloMessage()
    
    /**
     * Session State Message (BTELO Coding v2)
     * Broadcasts connection state between peers
     */
    data class SessionState(
        val mobileConnected: Boolean,
        val bridgeConnected: Boolean,
        val timestamp: Long
    ) : BteloMessage()
    
    /**
     * Hook Event from Claude Code BTELO Plugin
     * Triggered by Claude Code lifecycle events
     */
    data class HookEvent(
        val eventType: HookEventType,
        val sessionId: String,
        val data: Map<String, Any>,
        val timestamp: Long
    ) : BteloMessage()
    
    /**
     * Permission Response sent from mobile to server
     * User approves/denies a Claude Code permission request
     */
    data class PermissionResponse(
        val sessionId: String,
        val decision: String // "allow" or "deny"
    ) : BteloMessage()
}

/**
 * Metadata for structured output messages
 */
data class OutputMetadata(
    val toolId: String? = null,
    val toolName: String? = null,
    val toolType: String? = null,
    val filePath: String? = null,
    val command: String? = null,
    val parameters: Map<String, Any>? = null,
    val isFileOp: Boolean = false,
    val fileOpType: String? = null,
    val isToolResult: Boolean = false,
    val isCollapsed: Boolean = false,
    val originalLength: Int = 0,
    val errorCode: String? = null,
    val errorDetails: String? = null,
    val parserVersion: String = "1.0.0"
)

data class HistoryMessage(
    val id: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long
)

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
            is BteloMessage.SyncHistory -> {
                json.addProperty("type", "sync_history")
                json.addProperty("session_id", message.sessionId)
                json.add("messages", gson.toJsonTree(message.messages))
            }
            is BteloMessage.NewMessage -> {
                json.addProperty("type", "new_message")
                json.addProperty("session_id", message.sessionId)
                json.add("message", gson.toJsonTree(message.message))
            }
            is BteloMessage.SelectSession -> {
                json.addProperty("type", "select_session")
                json.addProperty("session_id", message.sessionId)
            }
            is BteloMessage.StructuredOutput -> {
                json.addProperty("type", "structured_output")
                json.addProperty("output_type", message.outputType.name.lowercase())
                json.addProperty("content", message.content)
                json.add("metadata", gson.toJsonTree(message.metadata))
                json.addProperty("timestamp", message.timestamp)
            }
            is BteloMessage.SessionState -> {
                json.addProperty("type", "session_state")
                json.addProperty("mobile_connected", message.mobileConnected)
                json.addProperty("bridge_connected", message.bridgeConnected)
                json.addProperty("timestamp", message.timestamp)
            }
            is BteloMessage.HookEvent -> {
                json.addProperty("type", "hook_event")
                json.addProperty("event", message.eventType.name.lowercase())
                json.addProperty("session_id", message.sessionId)
                json.add("data", gson.toJsonTree(message.data))
                json.addProperty("timestamp", message.timestamp)
            }
            is BteloMessage.PermissionResponse -> {
                json.addProperty("type", "permission_response")
                json.addProperty("session_id", message.sessionId)
                json.addProperty("decision", message.decision)
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
            when (val type = jsonObject.get("type")?.asString) {
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
                "sync_history" -> {
                    val messagesArray = jsonObject.getAsJsonArray("messages")
                    val messages = messagesArray?.map { element ->
                        val obj = element.asJsonObject
                        HistoryMessage(
                            id = obj.get("id")?.asString ?: "",
                            content = obj.get("content")?.asString ?: "",
                            isFromUser = obj.get("isFromUser")?.asBoolean ?: false,
                            timestamp = obj.get("timestamp")?.asLong ?: 0L
                        )
                    } ?: emptyList()
                    BteloMessage.SyncHistory(
                        sessionId = jsonObject.get("session_id")?.asString ?: "",
                        messages = messages
                    )
                }
                "new_message" -> {
                    val msgObj = jsonObject.getAsJsonObject("message")
                    val historyMsg = if (msgObj != null) {
                        HistoryMessage(
                            id = msgObj.get("id")?.asString ?: "",
                            content = msgObj.get("content")?.asString ?: "",
                            isFromUser = msgObj.get("isFromUser")?.asBoolean ?: false,
                            timestamp = msgObj.get("timestamp")?.asLong ?: 0L
                        )
                    } else {
                        HistoryMessage("", "", false, 0L)
                    }
                    BteloMessage.NewMessage(
                        sessionId = jsonObject.get("session_id")?.asString ?: "",
                        message = historyMsg
                    )
                }
                "select_session" -> BteloMessage.SelectSession(
                    sessionId = jsonObject.get("session_id")?.asString ?: ""
                )
                
                // BTELO Coding v2: Structured Output
                "structured_output" -> {
                    val outputTypeStr = jsonObject.get("output_type")?.asString?.uppercase() ?: "CLAUDE_RESPONSE"
                    val outputType = try {
                        OutputType.valueOf(outputTypeStr)
                    } catch (e: IllegalArgumentException) {
                        OutputType.CLAUDE_RESPONSE
                    }
                    
                    val metadataObj = jsonObject.getAsJsonObject("metadata")
                    val metadata = if (metadataObj != null) {
                        OutputMetadata(
                            toolId = metadataObj.get("toolId")?.asString,
                            toolName = metadataObj.get("toolName")?.asString,
                            toolType = metadataObj.get("toolType")?.asString,
                            filePath = metadataObj.get("filePath")?.asString,
                            command = metadataObj.get("command")?.asString,
                            isFileOp = metadataObj.get("isFileOp")?.asBoolean ?: false,
                            fileOpType = metadataObj.get("fileOpType")?.asString,
                            isToolResult = metadataObj.get("isToolResult")?.asBoolean ?: false,
                            isCollapsed = metadataObj.get("isCollapsed")?.asBoolean ?: false,
                            originalLength = metadataObj.get("originalLength")?.asInt ?: 0,
                            errorCode = metadataObj.get("code")?.asString,
                            errorDetails = metadataObj.get("details")?.asString,
                            parserVersion = metadataObj.get("parserVersion")?.asString ?: "1.0.0"
                        )
                    } else {
                        OutputMetadata()
                    }
                    
                    BteloMessage.StructuredOutput(
                        outputType = outputType,
                        content = jsonObject.get("content")?.asString ?: "",
                        metadata = metadata,
                        timestamp = jsonObject.get("timestamp")?.asString ?: ""
                    )
                }
                
                // BTELO Coding v2: Hook Event
                "hook_event" -> {
                    val eventStr = jsonObject.get("event")?.asString?.uppercase() ?: ""
                    val hookEventType = try {
                        HookEventType.valueOf(eventStr)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                    
                    val dataObj = jsonObject.getAsJsonObject("data")
                    val dataMap = mutableMapOf<String, Any>()
                    if (dataObj != null) {
                        for ((key, value) in dataObj.entrySet()) {
                            dataMap[key] = when {
                                value.isJsonPrimitive && value.asJsonPrimitive.isBoolean -> value.asBoolean
                                value.isJsonPrimitive && value.asJsonPrimitive.isNumber -> value.asNumber.toDouble()
                                value.isJsonPrimitive -> value.asString
                                else -> value.toString()
                            }
                        }
                    }
                    
                    if (hookEventType != null) {
                        BteloMessage.HookEvent(
                            eventType = hookEventType,
                            sessionId = jsonObject.get("session_id")?.asString ?: dataMap["session_id"]?.toString() ?: "",
                            data = dataMap,
                            timestamp = jsonObject.get("timestamp")?.asLong ?: System.currentTimeMillis()
                        )
                    } else null
                }
                
                // BTELO Coding v2: Session State
                "session_state" -> {
                    BteloMessage.SessionState(
                        mobileConnected = jsonObject.get("mobile_connected")?.asBoolean ?: false,
                        bridgeConnected = jsonObject.get("bridge_connected")?.asBoolean ?: false,
                        timestamp = jsonObject.get("timestamp")?.asLong ?: System.currentTimeMillis()
                    )
                }
                
                else -> {
                    Logger.w(tag, "未知的消息类型: $type")
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
