package com.btelo.coding.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val tool: String,
    val createdAt: Long,
    val lastActiveAt: Long,
    val isConnected: Boolean
)
