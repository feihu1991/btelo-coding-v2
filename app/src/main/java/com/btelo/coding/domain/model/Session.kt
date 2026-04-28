package com.btelo.coding.domain.model

data class Session(
    val id: String,
    val name: String,
    val tool: String, // "claude", "codex", "openclaw"
    val path: String = "",
    val createdAt: Long,
    val lastActiveAt: Long,
    val messageCount: Int = 0,
    val tokenCount: Int = 0,
    val status: SessionStatus = SessionStatus.ACTIVE,
    val isConnected: Boolean
)

enum class SessionStatus {
    ACTIVE, IDLE, COMPLETED
}
