package com.btelo.coding.domain.repository

import com.btelo.coding.domain.model.DeviceRegisterResponse
import com.btelo.coding.domain.model.DeviceStatusResponse
import com.btelo.coding.domain.model.PairingCodeResponse
import com.btelo.coding.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(serverAddress: String, username: String, password: String): Result<User>
    suspend fun register(serverAddress: String, username: String, password: String, name: String): Result<User>
    suspend fun logout()
    fun isLoggedIn(): Flow<Boolean>
    fun getCurrentUser(): Flow<User?>
    fun getServerAddress(): Flow<String?>
    fun getToken(): Flow<String?>
    fun getTokenSync(): String?
    fun getWsTokenSync(): String?

    // Device pairing methods
    suspend fun registerDevice(serverAddress: String, deviceName: String, deviceType: String): Result<DeviceRegisterResponse>
    suspend fun getPairingCode(serverAddress: String, deviceId: String): Result<PairingCodeResponse>
    suspend fun getDeviceStatus(serverAddress: String, deviceId: String): Result<DeviceStatusResponse>
    suspend fun saveDeviceId(deviceId: String)
    fun getDeviceId(): Flow<String?>
    suspend fun saveSessionId(sessionId: String)
    fun getSessionId(): Flow<String?>
    fun getSessionIdSync(): String?
}
