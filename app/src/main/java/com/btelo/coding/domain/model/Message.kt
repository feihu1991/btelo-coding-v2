package com.btelo.coding.domain.model

data class Message(
    val id: String,
    val sessionId: String,
    val content: String,
    val type: MessageType,
    val timestamp: Long,
    val isFromUser: Boolean
)

enum class MessageType {
    TEXT, COMMAND, OUTPUT, ERROR
}
