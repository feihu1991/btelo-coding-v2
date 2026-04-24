package com.btelo.coding.data.repository

import com.btelo.coding.data.local.dao.DeviceDao
import com.btelo.coding.data.local.entity.DeviceEntity
import com.btelo.coding.data.remote.encryption.CryptoManager
import com.btelo.coding.domain.model.Device
import com.btelo.coding.domain.repository.DeviceRepository
import com.btelo.coding.domain.repository.PairingCodeInfo
import com.btelo.coding.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.SecureRandom
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val deviceDao: DeviceDao,
    private val cryptoManager: CryptoManager
) : DeviceRepository {
    
    companion object {
        private const val TAG = "DeviceRepository"
        private const val PAIRING_CODE_LENGTH = 6
        private const val PAIRING_CODE_VALIDITY_MS = 5 * 60 * 1000L // 5 minutes
        
        // In-memory storage for pending pairing codes (in production, this should be server-side)
        private val pendingPairingCodes = mutableMapOf<String, PendingPairingData>()
    }
    
    override fun getAllDevices(): Flow<List<Device>> {
        return deviceDao.getAllDevices().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getOnlineDevices(): Flow<List<Device>> {
        return deviceDao.getOnlineDevices().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getDeviceById(deviceId: String): Flow<Device?> {
        return deviceDao.getDeviceById(deviceId).map { entity ->
            entity?.toDomain()
        }
    }
    
    override suspend fun generatePairingCode(deviceName: String): Result<PairingCodeInfo> {
        return try {
            Logger.i(TAG, "Generating pairing code for device: $deviceName")
            
            // Generate key pair for this device
            val keyPair = cryptoManager.generateKeyPair()
            val publicKeyBase64 = android.util.Base64.encodeToString(
                keyPair.publicKey, android.util.Base64.NO_WRAP
            )
            
            // Generate secure random pairing code
            val pairingCode = generateSecurePairingCode()
            val expiresAt = System.currentTimeMillis() + PAIRING_CODE_VALIDITY_MS
            
            // Store pending pairing data
            val pairingData = PendingPairingData(
                deviceName = deviceName,
                publicKey = publicKeyBase64,
                expiresAt = expiresAt
            )
            pendingPairingCodes[pairingCode] = pairingData
            
            Logger.i(TAG, "Pairing code generated: $pairingCode for device: $deviceName")
            
            Result.success(
                PairingCodeInfo(
                    code = pairingCode,
                    deviceName = deviceName,
                    expiresAt = expiresAt,
                    publicKey = publicKeyBase64
                )
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to generate pairing code", e)
            Result.failure(e)
        }
    }
    
    override suspend fun verifyPairingCode(pairingCode: String): Result<Device> {
        return try {
            Logger.i(TAG, "Verifying pairing code: $pairingCode")
            
            val pendingData = pendingPairingCodes.remove(pairingCode)
                ?: return Result.failure(IllegalArgumentException("Invalid or expired pairing code"))
            
            // Check if code has expired
            if (System.currentTimeMillis() > pendingData.expiresAt) {
                return Result.failure(IllegalArgumentException("Pairing code has expired"))
            }
            
            // Create new device with unique ID
            val deviceId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            
            val deviceEntity = DeviceEntity(
                id = deviceId,
                name = pendingData.deviceName,
                publicKey = pendingData.publicKey,
                isOnline = true,
                lastSeen = now
            )
            
            // Save device to database
            deviceDao.insertDevice(deviceEntity)
            
            Logger.i(TAG, "Device paired successfully: ${pendingData.deviceName} (ID: $deviceId)")
            
            Result.success(deviceEntity.toDomain())
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to verify pairing code", e)
            Result.failure(e)
        }
    }
    
    override suspend fun removeDevice(deviceId: String) {
        Logger.i(TAG, "Removing device: $deviceId")
        deviceDao.deleteDeviceById(deviceId)
    }
    
    override suspend fun updateDeviceStatus(deviceId: String, isOnline: Boolean) {
        val lastSeen = System.currentTimeMillis()
        deviceDao.updateDeviceStatus(deviceId, isOnline, lastSeen)
        Logger.i(TAG, "Device $deviceId status updated: online=$isOnline")
    }
    
    /**
     * Generate a secure random pairing code
     */
    private fun generateSecurePairingCode(): String {
        val secureRandom = SecureRandom()
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Excluding confusing chars (0/O, 1/I)
        return (1..PAIRING_CODE_LENGTH)
            .map { chars[secureRandom.nextInt(chars.length)] }
            .joinToString("")
    }
    
    /**
     * Internal data class for pending pairing operations
     */
    private data class PendingPairingData(
        val deviceName: String,
        val publicKey: String,
        val expiresAt: Long
    )
    
    /**
     * Extension function to convert DeviceEntity to Device domain model
     */
    private fun DeviceEntity.toDomain(): Device {
        return Device(
            id = this.id,
            name = this.name,
            publicKey = this.publicKey,
            isOnline = this.isOnline,
            lastSeen = this.lastSeen
        )
    }
}
