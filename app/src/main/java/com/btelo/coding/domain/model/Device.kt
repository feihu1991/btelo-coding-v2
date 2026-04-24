package com.btelo.coding.domain.model

data class Device(
    val id: String,
    val name: String,
    val publicKey: String,
    val isOnline: Boolean,
    val lastSeen: Long
)
