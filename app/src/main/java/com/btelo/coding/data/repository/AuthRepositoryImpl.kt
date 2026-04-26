package com.btelo.coding.data.repository

import com.btelo.coding.data.local.DataStoreManager
import com.btelo.coding.data.remote.api.AuthApi
import com.btelo.coding.domain.model.DeviceRegisterResponse
import com.btelo.coding.domain.model.DeviceStatusResponse
import com.btelo.coding.domain.model.PairingCodeResponse
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

    override suspend fun register(
        serverAddress: String,
        username: String,
        password: String,
        name: String
    ): Result<User> {
        return authApi.register(serverAddress, username, password, name).map { response ->
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

    override fun getTokenSync(): String? {
        return dataStoreManager.getTokenSync()
    }

    override fun getWsTokenSync(): String? {
        return dataStoreManager.getWsTokenSync()
    }

    // ========== Device Pairing Methods ==========

    override suspend fun registerDevice(
        serverAddress: String,
        deviceName: String,
        deviceType: String
    ): Result<DeviceRegisterResponse> {
        return authApi.registerDevice(serverAddress, deviceName, deviceType).map { response ->
            if (!response.success) {
                throw Exception(response.message)
            }
            // Save device info and token
            dataStoreManager.saveServerAddress(serverAddress)
            dataStoreManager.saveDeviceId(response.device_id)
            if (response.token != null) {
                try {
                    dataStoreManager.saveToken(response.token)
                } catch (e: Exception) {
                    dataStoreManager.saveTokenFallback(response.token)
                }
            }
            DeviceRegisterResponse(
                deviceId = response.device_id,
                pairingCode = response.pairing_code ?: ""
            )
        }
    }

    override suspend fun getPairingCode(
        serverAddress: String,
        deviceId: String
    ): Result<PairingCodeResponse> {
        return authApi.getPairingCode(serverAddress, deviceId).map { response ->
            PairingCodeResponse(
                deviceId = response.device_id,
                pairingCode = response.pairing_code,
                expiresAt = response.expires_at
            )
        }
    }

    override suspend fun getDeviceStatus(
        serverAddress: String,
        deviceId: String
    ): Result<DeviceStatusResponse> {
        return authApi.getDeviceStatus(serverAddress, deviceId).map { response ->
            DeviceStatusResponse(
                deviceId = response.device_id,
                paired = response.paired,
                sessionId = response.session_id,
                terminalConnected = response.terminal_connected
            )
        }
    }

    override suspend fun saveDeviceId(deviceId: String) {
        dataStoreManager.saveDeviceId(deviceId)
    }

    override fun getDeviceId(): Flow<String?> {
        return dataStoreManager.deviceId
    }

    override suspend fun saveSessionId(sessionId: String) {
        dataStoreManager.saveSessionId(sessionId)
    }

    override fun getSessionId(): Flow<String?> {
        return dataStoreManager.sessionId
    }

    override fun getSessionIdSync(): String? {
        return dataStoreManager.getSessionIdSync()
    }
}
