package com.btelo.coding.domain.model

data class Device(
    val id: String,
    val name: String,
    val publicKey: String,
    val isOnline: Boolean,
    val lastSeen: Long
)

// 设备注册响应
data class DeviceRegisterResponse(
    val deviceId: String,
    val pairingCode: String
)

// 配对码响应
data class PairingCodeResponse(
    val deviceId: String,
    val pairingCode: String,
    val expiresAt: String? = null
)
