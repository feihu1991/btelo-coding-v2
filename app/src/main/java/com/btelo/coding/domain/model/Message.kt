package com.btelo.coding.domain.model

data class Message(
    val id: String,
    val sessionId: String,
    val content: String,
    val type: MessageType,
    val timestamp: Long,
    val isFromUser: Boolean,
    val sender: String = "",
    val tools: List<ToolExecution>? = null
)

enum class MessageType {
    TEXT, COMMAND, OUTPUT, ERROR, TOOL, THINKING
}

data class ToolExecution(
    val type: ToolType,
    val command: String,
    val output: String? = null,
    val status: ToolStatus
)

enum class ToolType {
    BASH, READ, EDIT, WRITE, GREP
}

enum class ToolStatus {
    SUCCESS, ERROR, RUNNING
}
