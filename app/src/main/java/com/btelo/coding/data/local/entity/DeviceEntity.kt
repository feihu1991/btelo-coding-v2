package com.btelo.coding.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val publicKey: String,
    val isOnline: Boolean,
    val lastSeen: Long
)
