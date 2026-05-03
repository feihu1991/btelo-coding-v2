package com.btelo.coding.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val tool: String,
    val path: String = "",
    val createdAt: Long,
    val lastActiveAt: Long,
    val messageCount: Int = 0,
    val tokenCount: Int = 0,
    val status: String = "ACTIVE",
    val isConnected: Boolean,
    val attentionType: String? = null,
    val attentionTitle: String = "",
    val attentionBody: String = "",
    val attentionUpdatedAt: Long? = null,
    val currentKeyVersion: Int = 1,
    val lastKeyRotation: Long = 0,
    val rotationIntervalDays: Int = 7
)
