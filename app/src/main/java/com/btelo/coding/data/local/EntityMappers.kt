package com.btelo.coding.data.local

import com.btelo.coding.data.local.entity.DeviceEntity
import com.btelo.coding.data.local.entity.MessageEntity
import com.btelo.coding.data.local.entity.SessionEntity
import com.btelo.coding.domain.model.Device
import com.btelo.coding.domain.model.Message
import com.btelo.coding.domain.model.MessageType
import com.btelo.coding.domain.model.Session
import com.btelo.coding.domain.model.SessionStatus
import com.btelo.coding.domain.model.ToolExecution
import com.btelo.coding.domain.model.ToolStatus
import com.btelo.coding.domain.model.ToolType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 数据层与领域层之间的实体转换工具
 */
object EntityMappers {

    private val gson = Gson()

    // Session 映射
    fun SessionEntity.toDomain(): Session {
        return Session(
            id = id,
            name = name,
            tool = tool,
            path = path,
            createdAt = createdAt,
            lastActiveAt = lastActiveAt,
            messageCount = messageCount,
            tokenCount = tokenCount,
            status = try {
                SessionStatus.valueOf(status)
            } catch (e: IllegalArgumentException) {
                SessionStatus.ACTIVE
            },
            isConnected = isConnected
        )
    }

    fun Session.toEntity(): SessionEntity {
        return SessionEntity(
            id = id,
            name = name,
            tool = tool,
            path = path,
            createdAt = createdAt,
            lastActiveAt = lastActiveAt,
            messageCount = messageCount,
            tokenCount = tokenCount,
            status = status.name,
            isConnected = isConnected
        )
    }

    // Message 映射
    fun MessageEntity.toDomain(): Message {
        val tools = if (!toolsJson.isNullOrBlank()) {
            try {
                val listType = object : TypeToken<List<ToolExecution>>() {}.type
                gson.fromJson<List<ToolExecution>>(toolsJson, listType)
            } catch (e: Exception) {
                null
            }
        } else null

        return Message(
            id = id,
            sessionId = sessionId,
            content = content,
            type = try {
                MessageType.valueOf(type)
            } catch (e: IllegalArgumentException) {
                MessageType.TEXT
            },
            timestamp = timestamp,
            isFromUser = isFromUser,
            sender = sender,
            tools = tools
        )
    }

    fun Message.toEntity(): MessageEntity {
        val toolsJson = if (!tools.isNullOrEmpty()) {
            gson.toJson(tools)
        } else null

        return MessageEntity(
            id = id,
            sessionId = sessionId,
            content = content,
            type = type.name,
            timestamp = timestamp,
            isFromUser = isFromUser,
            sender = sender,
            toolsJson = toolsJson,
            version = 1,
            deviceId = null,
            isSynced = false,
            isDeleted = false
        )
    }

    // MessageEntity 到 MessageEntity（带同步字段的完整映射）
    fun MessageEntity.toEntityWithSync(version: Int, deviceId: String?, isSynced: Boolean, isDeleted: Boolean = false): MessageEntity {
        return MessageEntity(
            id = id,
            sessionId = sessionId,
            content = content,
            type = type,
            timestamp = timestamp,
            isFromUser = isFromUser,
            sender = sender,
            toolsJson = toolsJson,
            version = version,
            deviceId = deviceId,
            isSynced = isSynced,
            isDeleted = isDeleted
        )
    }

    // Device 映射
    fun DeviceEntity.toDomain(): Device {
        return Device(
            id = id,
            name = name,
            publicKey = publicKey,
            isOnline = isOnline,
            lastSeen = lastSeen
        )
    }

    fun Device.toEntity(): DeviceEntity {
        return DeviceEntity(
            id = id,
            name = name,
            publicKey = publicKey,
            isOnline = isOnline,
            lastSeen = lastSeen
        )
    }

    // 批量转换
    fun List<SessionEntity>.toSessionList(): List<Session> = map { it.toDomain() }
    fun List<MessageEntity>.toMessageList(): List<Message> = map { it.toDomain() }
    fun List<DeviceEntity>.toDeviceList(): List<Device> = map { it.toDomain() }
}
