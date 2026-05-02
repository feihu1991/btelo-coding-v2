package com.btelo.coding.data.remote.websocket

import com.btelo.coding.util.AppException
import com.btelo.coding.util.Logger
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

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
    data class Command(
        val content: String,
        val type: InputType,
        val clientMessageId: String? = null
    ) : BteloMessage()
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

    data class TranscriptSnapshot(
        val sessionId: String,
        val claudeSessionId: String,
        val cursor: Int,
        val events: List<TranscriptEvent>
    ) : BteloMessage()

    data class TranscriptDelta(
        val sessionId: String,
        val claudeSessionId: String,
        val cursor: Int,
        val events: List<TranscriptEvent>
    ) : BteloMessage()

    data class ActiveTurnSnapshot(
        val sessionId: String,
        val claudeSessionId: String,
        val workspaceRoot: String? = null,
        val cursor: Int,
        val activeTurn: ActiveTurn
    ) : BteloMessage()

    data class TerminalFrame(
        val encoding: String,
        val stream: String,
        val data: String,
        val timestamp: Long = 0L
    ) : BteloMessage()

    data class TerminalExit(
        val exitCode: Int?,
        val signal: String?,
        val timestamp: Long = 0L
    ) : BteloMessage()

    data class BridgeControl(
        val action: String
    ) : BteloMessage()

    data class BridgeControlResult(
        val action: String,
        val success: Boolean,
        val message: String? = null,
        val exitCode: Int? = null
    ) : BteloMessage()

    data class InputStatus(
        val clientMessageId: String?,
        val status: String,
        val message: String? = null,
        val transcriptEventId: String? = null,
        val inputMode: String? = null,
        val timestamp: Long = 0L
    ) : BteloMessage()

    data class BridgeStatus(
        val sessionId: String,
        val claudeSessionId: String,
        val connected: Boolean,
        val mode: String? = null,
        val inputMode: String? = null,
        val message: String? = null
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
        val timestamp: String,
        val id: String? = null
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
    val sourceKind: String? = null,
    val terminality: String? = null,
    val fingerprint: String? = null,
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

data class TranscriptEvent(
    val id: String,
    val seq: Int,
    val role: String,
    val kind: String,
    val content: String,
    val timestamp: Long,
    val metadata: OutputMetadata = OutputMetadata()
)

data class ActiveTurn(
    val isActive: Boolean,
    val status: String,
    val pendingInputs: List<PendingInput> = emptyList(),
    val lastUserEventId: String? = null,
    val lastAssistantEventId: String? = null,
    val lastTerminalEventId: String? = null,
    val activeTools: List<ActiveTool> = emptyList(),
    val textTail: String = "",
    val updatedAt: Long = 0L
)

data class PendingInput(
    val clientMessageId: String,
    val contentPreview: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class ActiveTool(
    val eventId: String,
    val toolId: String? = null,
    val toolName: String? = null,
    val kind: String,
    val content: String,
    val startedAt: Long
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
                message.clientMessageId?.takeIf { it.isNotBlank() }?.let {
                    json.addProperty("client_message_id", it)
                }
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
            is BteloMessage.TranscriptSnapshot -> {
                json.addProperty("type", "transcript_snapshot")
                json.addProperty("version", 3)
                json.addProperty("session_id", message.sessionId)
                json.addProperty("claude_session_id", message.claudeSessionId)
                json.addProperty("cursor", message.cursor)
                json.add("events", gson.toJsonTree(message.events))
            }
            is BteloMessage.TranscriptDelta -> {
                json.addProperty("type", "transcript_delta")
                json.addProperty("version", 3)
                json.addProperty("session_id", message.sessionId)
                json.addProperty("claude_session_id", message.claudeSessionId)
                json.addProperty("cursor", message.cursor)
                json.add("events", gson.toJsonTree(message.events))
            }
            is BteloMessage.ActiveTurnSnapshot -> {
                json.addProperty("type", "active_turn_snapshot")
                json.addProperty("version", 3)
                json.addProperty("session_id", message.sessionId)
                json.addProperty("claude_session_id", message.claudeSessionId)
                message.workspaceRoot?.let { json.addProperty("workspace_root", it) }
                json.addProperty("cursor", message.cursor)
                json.add("active_turn", gson.toJsonTree(message.activeTurn))
            }
            is BteloMessage.TerminalFrame -> {
                json.addProperty("type", "terminal_frame")
                json.addProperty("encoding", message.encoding)
                json.addProperty("stream", message.stream)
                json.addProperty("data", message.data)
                json.addProperty("timestamp", message.timestamp)
            }
            is BteloMessage.TerminalExit -> {
                json.addProperty("type", "terminal_exit")
                message.exitCode?.let { json.addProperty("exit_code", it) }
                message.signal?.let { json.addProperty("signal", it) }
                json.addProperty("timestamp", message.timestamp)
            }
            is BteloMessage.BridgeControl -> {
                json.addProperty("type", "bridge_control")
                json.addProperty("action", message.action)
            }
            is BteloMessage.BridgeControlResult -> {
                json.addProperty("type", "bridge_control_result")
                json.addProperty("action", message.action)
                json.addProperty("success", message.success)
                message.message?.let { json.addProperty("message", it) }
                message.exitCode?.let { json.addProperty("exit_code", it) }
            }
            is BteloMessage.InputStatus -> {
                json.addProperty("type", "input_status")
                json.addProperty("client_message_id", message.clientMessageId)
                json.addProperty("status", message.status)
                message.message?.let { json.addProperty("message", it) }
                message.transcriptEventId?.let { json.addProperty("transcript_event_id", it) }
                message.inputMode?.let { json.addProperty("input_mode", it) }
                json.addProperty("timestamp", message.timestamp)
            }
            is BteloMessage.BridgeStatus -> {
                json.addProperty("type", "bridge_status")
                json.addProperty("session_id", message.sessionId)
                json.addProperty("claude_session_id", message.claudeSessionId)
                json.addProperty("connected", message.connected)
                message.mode?.let { json.addProperty("mode", it) }
                message.inputMode?.let { json.addProperty("input_mode", it) }
                message.message?.let { json.addProperty("message", it) }
            }
            is BteloMessage.SelectSession -> {
                json.addProperty("type", "select_session")
                json.addProperty("session_id", message.sessionId)
            }
            is BteloMessage.StructuredOutput -> {
                json.addProperty("type", "structured_output")
                message.id?.takeIf { it.isNotBlank() }?.let { json.addProperty("id", it) }
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
                "command" -> {
                    val content = optionalString(jsonObject, "content") ?: return null
                    BteloMessage.Command(
                        content = content,
                        type = InputType.valueOf(
                            optionalString(jsonObject, "inputType")?.uppercase() ?: "TEXT"
                        ),
                        clientMessageId = optionalString(jsonObject, "client_message_id")
                            ?: optionalString(jsonObject, "clientMessageId")
                    )
                }
                "output" -> BteloMessage.Output(
                    data = optionalString(jsonObject, "data") ?: return null,
                    stream = StreamType.valueOf(
                        optionalString(jsonObject, "stream")?.uppercase() ?: "STDOUT"
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
                "transcript_snapshot" -> {
                    BteloMessage.TranscriptSnapshot(
                        sessionId = optionalString(jsonObject, "session_id") ?: "",
                        claudeSessionId = optionalString(jsonObject, "claude_session_id") ?: "",
                        cursor = jsonObject.get("cursor")?.asInt ?: 0,
                        events = parseTranscriptEvents(jsonObject)
                    )
                }
                "transcript_delta" -> {
                    BteloMessage.TranscriptDelta(
                        sessionId = optionalString(jsonObject, "session_id") ?: "",
                        claudeSessionId = optionalString(jsonObject, "claude_session_id") ?: "",
                        cursor = jsonObject.get("cursor")?.asInt ?: 0,
                        events = parseTranscriptEvents(jsonObject)
                    )
                }
                "active_turn_snapshot" -> {
                    BteloMessage.ActiveTurnSnapshot(
                        sessionId = optionalString(jsonObject, "session_id") ?: "",
                        claudeSessionId = optionalString(jsonObject, "claude_session_id") ?: "",
                        workspaceRoot = optionalString(jsonObject, "workspace_root"),
                        cursor = jsonObject.get("cursor")?.asInt ?: 0,
                        activeTurn = parseActiveTurn(jsonObject.getAsJsonObject("active_turn"))
                    )
                }
                "terminal_frame" -> BteloMessage.TerminalFrame(
                    encoding = optionalString(jsonObject, "encoding") ?: "utf-8",
                    stream = optionalString(jsonObject, "stream") ?: "stdout",
                    data = optionalString(jsonObject, "data") ?: "",
                    timestamp = jsonObject.get("timestamp")?.asLong ?: 0L
                )
                "terminal_exit" -> BteloMessage.TerminalExit(
                    exitCode = jsonObject.get("exit_code")?.asInt,
                    signal = optionalString(jsonObject, "signal"),
                    timestamp = jsonObject.get("timestamp")?.asLong ?: 0L
                )
                "bridge_control" -> BteloMessage.BridgeControl(
                    action = optionalString(jsonObject, "action") ?: ""
                )
                "bridge_control_result" -> BteloMessage.BridgeControlResult(
                    action = optionalString(jsonObject, "action") ?: "",
                    success = jsonObject.get("success")?.asBoolean ?: false,
                    message = optionalString(jsonObject, "message"),
                    exitCode = jsonObject.get("exit_code")?.asInt
                )
                "input_status" -> BteloMessage.InputStatus(
                    clientMessageId = optionalString(jsonObject, "client_message_id")
                        ?: optionalString(jsonObject, "clientMessageId"),
                    status = optionalString(jsonObject, "status") ?: "",
                    message = optionalString(jsonObject, "message"),
                    transcriptEventId = optionalString(jsonObject, "transcript_event_id")
                        ?: optionalString(jsonObject, "transcriptEventId"),
                    inputMode = optionalString(jsonObject, "input_mode")
                        ?: optionalString(jsonObject, "inputMode"),
                    timestamp = jsonObject.get("timestamp")?.asLong ?: 0L
                )
                "bridge_status" -> BteloMessage.BridgeStatus(
                    sessionId = optionalString(jsonObject, "session_id") ?: "",
                    claudeSessionId = optionalString(jsonObject, "claude_session_id") ?: "",
                    connected = jsonObject.get("connected")?.asBoolean ?: false,
                    mode = optionalString(jsonObject, "mode"),
                    inputMode = optionalString(jsonObject, "input_mode"),
                    message = optionalString(jsonObject, "message")
                )
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
                    val metadata = parseOutputMetadata(metadataObj)
                    
                    BteloMessage.StructuredOutput(
                        outputType = outputType,
                        content = optionalString(jsonObject, "content") ?: "",
                        metadata = metadata,
                        timestamp = optionalString(jsonObject, "timestamp") ?: "",
                        id = optionalString(jsonObject, "id") ?: optionalString(jsonObject, "event_id")
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

    private fun optionalString(jsonObject: JsonObject, field: String): String? {
        val element = jsonObject.get(field) ?: return null
        if (element.isJsonNull) return null
        return element.asString
    }

    private fun parseTranscriptEvents(jsonObject: JsonObject): List<TranscriptEvent> {
        val eventsArray = jsonObject.getAsJsonArray("events") ?: return emptyList()
        return eventsArray.mapNotNull { element ->
            if (!element.isJsonObject) return@mapNotNull null
            val obj = element.asJsonObject
            val id = optionalString(obj, "id") ?: return@mapNotNull null
            TranscriptEvent(
                id = id,
                seq = obj.get("seq")?.asInt ?: 0,
                role = optionalString(obj, "role") ?: "",
                kind = optionalString(obj, "kind") ?: "text",
                content = optionalString(obj, "content") ?: "",
                timestamp = obj.get("timestamp")?.asLong ?: 0L,
                metadata = parseOutputMetadata(
                    obj.get("metadata")?.takeIf { it.isJsonObject }?.asJsonObject
                )
            )
        }
    }

    private fun parseActiveTurn(activeTurnObj: JsonObject?): ActiveTurn {
        if (activeTurnObj == null) {
            return ActiveTurn(isActive = false, status = "idle")
        }

        val pendingInputs = activeTurnObj.getAsJsonArray("pending_inputs")
            ?.mapNotNull { element ->
                if (!element.isJsonObject) return@mapNotNull null
                val obj = element.asJsonObject
                PendingInput(
                    clientMessageId = optionalString(obj, "client_message_id") ?: "",
                    contentPreview = optionalString(obj, "content_preview") ?: "",
                    status = optionalString(obj, "status") ?: "pending",
                    createdAt = obj.get("created_at")?.asLong ?: 0L,
                    updatedAt = obj.get("updated_at")?.asLong ?: 0L
                )
            }
            ?: emptyList()

        val activeTools = activeTurnObj.getAsJsonArray("active_tools")
            ?.mapNotNull { element ->
                if (!element.isJsonObject) return@mapNotNull null
                val obj = element.asJsonObject
                ActiveTool(
                    eventId = optionalString(obj, "event_id") ?: "",
                    toolId = optionalString(obj, "tool_id"),
                    toolName = optionalString(obj, "tool_name"),
                    kind = optionalString(obj, "kind") ?: "",
                    content = optionalString(obj, "content") ?: "",
                    startedAt = obj.get("started_at")?.asLong ?: 0L
                )
            }
            ?: emptyList()

        return ActiveTurn(
            isActive = activeTurnObj.get("is_active")?.asBoolean ?: false,
            status = optionalString(activeTurnObj, "status") ?: "idle",
            pendingInputs = pendingInputs,
            lastUserEventId = optionalString(activeTurnObj, "last_user_event_id"),
            lastAssistantEventId = optionalString(activeTurnObj, "last_assistant_event_id"),
            lastTerminalEventId = optionalString(activeTurnObj, "last_terminal_event_id"),
            activeTools = activeTools,
            textTail = optionalString(activeTurnObj, "text_tail") ?: "",
            updatedAt = activeTurnObj.get("updated_at")?.asLong ?: 0L
        )
    }

    private fun parseOutputMetadata(metadataObj: JsonObject?): OutputMetadata {
        if (metadataObj == null) return OutputMetadata()
        return OutputMetadata(
            sourceKind = optionalString(metadataObj, "sourceKind") ?: optionalString(metadataObj, "source_kind"),
            terminality = optionalString(metadataObj, "terminality"),
            fingerprint = optionalString(metadataObj, "fingerprint"),
            toolId = optionalString(metadataObj, "toolId"),
            toolName = optionalString(metadataObj, "toolName"),
            toolType = optionalString(metadataObj, "toolType"),
            filePath = optionalString(metadataObj, "filePath"),
            command = optionalString(metadataObj, "command"),
            parameters = metadataObj.get("parameters")?.let { element ->
                if (element.isJsonObject) {
                    val type = object : TypeToken<Map<String, Any>>() {}.type
                    gson.fromJson<Map<String, Any>>(element, type)
                } else null
            },
            isFileOp = metadataObj.get("isFileOp")?.asBoolean ?: false,
            fileOpType = optionalString(metadataObj, "fileOpType"),
            isToolResult = metadataObj.get("isToolResult")?.asBoolean ?: false,
            isCollapsed = metadataObj.get("isCollapsed")?.asBoolean ?: false,
            originalLength = metadataObj.get("originalLength")?.asInt ?: 0,
            errorCode = optionalString(metadataObj, "errorCode") ?: optionalString(metadataObj, "code"),
            errorDetails = optionalString(metadataObj, "errorDetails") ?: optionalString(metadataObj, "details"),
            parserVersion = optionalString(metadataObj, "parserVersion") ?: "1.0.0"
        )
    }
}
