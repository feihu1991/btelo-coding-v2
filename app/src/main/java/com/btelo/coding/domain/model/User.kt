package com.btelo.coding.domain.model

data class User(
    val id: String,
    val username: String,
    val token: String
)

data class Device(
    val deviceId: String,
    val pairingCode: String,
    val expiresAt: String? = null
)
