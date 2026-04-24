package com.btelo.coding.data.local

import com.btelo.coding.data.local.entity.DeviceEntity
import com.btelo.coding.data.local.entity.MessageEntity
import com.btelo.coding.data.local.entity.SessionEntity
import com.btelo.coding.domain.model.Device
import com.btelo.coding.domain.model.Message
import com.btelo.coding.domain.model.MessageType
import com.btelo.coding.domain.model.Session

/**
 * 数据层与领域层之间的实体转换工具
 */
object EntityMappers {
    
    // Session 映射
    fun SessionEntity.toDomain(): Session {
        return Session(
            id = id,
            name = name,
            tool = tool,
            createdAt = createdAt,
            lastActiveAt = lastActiveAt,
            isConnected = isConnected
        )
    }
    
    fun Session.toEntity(): SessionEntity {
        return SessionEntity(
            id = id,
            name = name,
            tool = tool,
            createdAt = createdAt,
            lastActiveAt = lastActiveAt,
            isConnected = isConnected
        )
    }
    
    // Message 映射
    fun MessageEntity.toDomain(): Message {
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
            isFromUser = isFromUser
        )
    }
    
    fun Message.toEntity(): MessageEntity {
        return MessageEntity(
            id = id,
            sessionId = sessionId,
            content = content,
            type = type.name,
            timestamp = timestamp,
            isFromUser = isFromUser
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
