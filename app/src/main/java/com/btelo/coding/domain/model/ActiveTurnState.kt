package com.btelo.coding.domain.model

data class ActiveTurnState(
    val sessionId: String = "",
    val claudeSessionId: String = "",
    val workspaceRoot: String? = null,
    val cursor: Int = 0,
    val isActive: Boolean = false,
    val status: String = "idle",
    val pendingInputCount: Int = 0,
    val pendingPreview: String = "",
    val activeToolName: String? = null,
    val textTail: String = "",
    val updatedAt: Long = 0L
) {
    companion object {
        val Idle = ActiveTurnState()
    }
}
