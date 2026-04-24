package com.btelo.coding.domain.model

data class Session(
    val id: String,
    val name: String,
    val tool: String, // "claude", "codex", "openclaw"
    val createdAt: Long,
    val lastActiveAt: Long,
    val isConnected: Boolean
)
