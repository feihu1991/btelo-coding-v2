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
    val type: String, // COMMAND, OUTPUT, ERROR
    val timestamp: Long,
    val isFromUser: Boolean,
    // 同步相关字段
    val version: Int = 1,           // 版本号，用于冲突解决
    val deviceId: String? = null,  // 发送设备ID
    val isSynced: Boolean = false, // 是否已同步到服务器
    val isDeleted: Boolean = false, // 软删除标记
    // 密钥版本字段（用于前向保密）
    val keyVersion: Int = 1        // 加密该消息时使用的密钥版本
)
