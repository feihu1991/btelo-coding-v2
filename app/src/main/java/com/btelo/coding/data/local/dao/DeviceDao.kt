package com.btelo.coding.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.btelo.coding.data.local.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    
    @Query("SELECT * FROM devices ORDER BY lastSeen DESC")
    fun getAllDevices(): Flow<List<DeviceEntity>>
    
    @Query("SELECT * FROM devices WHERE id = :deviceId")
    fun getDeviceById(deviceId: String): Flow<DeviceEntity?>
    
    @Query("SELECT * FROM devices WHERE id = :deviceId")
    suspend fun getDeviceByIdSync(deviceId: String): DeviceEntity?
    
    @Query("SELECT * FROM devices WHERE isOnline = 1")
    fun getOnlineDevices(): Flow<List<DeviceEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity)
    
    @Update
    suspend fun updateDevice(device: DeviceEntity)
    
    @Delete
    suspend fun deleteDevice(device: DeviceEntity)
    
    @Query("DELETE FROM devices WHERE id = :deviceId")
    suspend fun deleteDeviceById(deviceId: String)
    
    @Query("UPDATE devices SET isOnline = :isOnline, lastSeen = :lastSeen WHERE id = :deviceId")
    suspend fun updateDeviceStatus(deviceId: String, isOnline: Boolean, lastSeen: Long)
}
