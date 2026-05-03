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
    val isConnected: Boolean,
    val attentionType: SessionAttentionType? = null,
    val attentionTitle: String = "",
    val attentionBody: String = "",
    val attentionUpdatedAt: Long? = null
)

enum class SessionStatus {
    ACTIVE, IDLE, COMPLETED
}

enum class SessionAttentionType {
    WAITING_INPUT,
    PERMISSION_REQUEST,
    TASK_COMPLETE
}
