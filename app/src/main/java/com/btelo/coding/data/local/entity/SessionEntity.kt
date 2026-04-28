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
    // 密钥版本相关字段
    val currentKeyVersion: Int = 1,           // 当前密钥版本
    val lastKeyRotation: Long = 0,           // 上次密钥轮换时间戳
    val rotationIntervalDays: Int = 7         // 密钥轮换间隔（天）
)
