package com.btelo.coding.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val content: String,
    val type: String, // COMMAND, OUTPUT, ERROR, TOOL, THINKING
    val timestamp: Long,
    val isFromUser: Boolean,
    val sender: String = "",
    val toolsJson: String? = null,
    
    // BTELO Coding v2: Structured Output fields
    // Output type enum (CLAUDE_RESPONSE, TOOL_CALL, FILE_OP, THINKING, ERROR, SYSTEM)
    val outputType: String? = null,
    // JSON string of MessageMetadata
    val metadataJson: String? = null,
    // Thinking content (collapsible)
    val thinkingContent: String? = null,
    
    // 同步相关字段
    val version: Int = 1,
    val deviceId: String? = null,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    
    // 密钥版本字段（用于前向保密）
    val keyVersion: Int = 1
)
