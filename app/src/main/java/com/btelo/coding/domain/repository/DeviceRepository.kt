package com.btelo.coding.domain.repository

import com.btelo.coding.domain.model.Device
import kotlinx.coroutines.flow.Flow

/**
 * 设备配对和管理的仓库接口
 */
interface DeviceRepository {
    /**
     * 获取所有已配对设备列表
     */
    fun getAllDevices(): Flow<List<Device>>
    
    /**
     * 获取在线设备列表
     */
    fun getOnlineDevices(): Flow<List<Device>>
    
    /**
     * 通过设备ID获取设备信息
     */
    fun getDeviceById(deviceId: String): Flow<Device?>
    
    /**
     * 生成配对码
     * @param deviceName 要配对的设备名称
     * @return 配对码和相关信息
     */
    suspend fun generatePairingCode(deviceName: String): Result<PairingCodeInfo>
    
    /**
     * 使用配对码验证并绑定设备
     * @param pairingCode 配对码
     * @return 配对结果
     */
    suspend fun verifyPairingCode(pairingCode: String): Result<Device>
    
    /**
     * 移除设备配对
     * @param deviceId 要移除的设备ID
     */
    suspend fun removeDevice(deviceId: String)
    
    /**
     * 更新设备在线状态
     * @param deviceId 设备ID
     * @param isOnline 是否在线
     */
    suspend fun updateDeviceStatus(deviceId: String, isOnline: Boolean)
}

/**
 * 配对码信息
 */
data class PairingCodeInfo(
    val code: String,
    val deviceName: String,
    val expiresAt: Long,
    val publicKey: String
)
