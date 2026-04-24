package com.btelo.coding.data.repository

import com.btelo.coding.data.local.DataStoreManager
import com.btelo.coding.data.remote.api.AuthApi
import com.btelo.coding.domain.model.Device
import com.btelo.coding.domain.model.User
import com.btelo.coding.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val dataStoreManager: DataStoreManager
) : AuthRepository {

    override suspend fun login(
        serverAddress: String,
        username: String,
        password: String
    ): Result<User> {
        return authApi.login(serverAddress, username, password).map { response ->
            // Save token using encrypted storage
            dataStoreManager.saveAuth(
                token = response.token,
                userId = response.userId,
                username = response.username,
                serverAddress = serverAddress
            )
            User(
                id = response.userId,
                username = response.username,
                token = response.token
            )
        }
    }

    override suspend fun logout() {
        dataStoreManager.clearAuth()
    }

    override fun isLoggedIn(): Flow<Boolean> {
        return dataStoreManager.serverAddress.map { 
            dataStoreManager.hasToken()
        }
    }

    override fun getCurrentUser(): Flow<User?> {
        return combine(
            dataStoreManager.getTokenFlow(),
            dataStoreManager.userId,
            dataStoreManager.username
        ) { token, userId, username ->
            if (token != null && userId != null && username != null) {
                User(
                    id = userId,
                    username = username,
                    token = token
                )
            } else {
                null
            }
        }
    }

    override fun getServerAddress(): Flow<String?> {
        return dataStoreManager.serverAddress
    }

    override fun getToken(): Flow<String?> {
        return dataStoreManager.getTokenFlow()
    }

    // ========== Device Pairing Methods ==========

    override suspend fun registerDevice(
        serverAddress: String,
        deviceName: String,
        deviceType: String
    ): Result<Device> {
        return authApi.registerDevice(serverAddress, deviceName, deviceType).map { response ->
            if (!response.success) {
                throw Exception(response.message)
            }
            // Save device info
            dataStoreManager.saveServerAddress(serverAddress)
            dataStoreManager.saveDeviceId(response.device_id)
            Device(
                deviceId = response.device_id,
                pairingCode = response.pairing_code ?: ""
            )
        }
    }

    override suspend fun getPairingCode(
        serverAddress: String,
        deviceId: String
    ): Result<Device> {
        return authApi.getPairingCode(serverAddress, deviceId).map { response ->
            Device(
                deviceId = response.device_id,
                pairingCode = response.pairing_code,
                expiresAt = response.expires_at
            )
        }
    }

    override suspend fun saveDeviceId(deviceId: String) {
        dataStoreManager.saveDeviceId(deviceId)
    }

    override fun getDeviceId(): Flow<String?> {
        return dataStoreManager.deviceId
    }
}
