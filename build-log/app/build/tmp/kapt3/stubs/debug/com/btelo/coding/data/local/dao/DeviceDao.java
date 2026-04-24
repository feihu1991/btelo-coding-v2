package com.btelo.coding.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.btelo.coding.data.local.entity.DeviceEntity;
import kotlinx.coroutines.flow.Flow;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\bg\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u0007\u001a\u00020\u00032\u0006\u0010\b\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u0014\u0010\u000b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\r0\fH\'J\u0018\u0010\u000e\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00050\f2\u0006\u0010\b\u001a\u00020\tH\'J\u0018\u0010\u000f\u001a\u0004\u0018\u00010\u00052\u0006\u0010\b\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u0014\u0010\u0010\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\r0\fH\'J\u0016\u0010\u0011\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u0012\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J&\u0010\u0013\u001a\u00020\u00032\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0017H\u00a7@\u00a2\u0006\u0002\u0010\u0018\u00a8\u0006\u0019"}, d2 = {"Lcom/btelo/coding/data/local/dao/DeviceDao;", "", "deleteDevice", "", "device", "Lcom/btelo/coding/data/local/entity/DeviceEntity;", "(Lcom/btelo/coding/data/local/entity/DeviceEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteDeviceById", "deviceId", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAllDevices", "Lkotlinx/coroutines/flow/Flow;", "", "getDeviceById", "getDeviceByIdSync", "getOnlineDevices", "insertDevice", "updateDevice", "updateDeviceStatus", "isOnline", "", "lastSeen", "", "(Ljava/lang/String;ZJLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
@androidx.room.Dao()
public abstract interface DeviceDao {
    
    @androidx.room.Query(value = "SELECT * FROM devices ORDER BY lastSeen DESC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.btelo.coding.data.local.entity.DeviceEntity>> getAllDevices();
    
    @androidx.room.Query(value = "SELECT * FROM devices WHERE id = :deviceId")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<com.btelo.coding.data.local.entity.DeviceEntity> getDeviceById(@org.jetbrains.annotations.NotNull()
    java.lang.String deviceId);
    
    @androidx.room.Query(value = "SELECT * FROM devices WHERE id = :deviceId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getDeviceByIdSync(@org.jetbrains.annotations.NotNull()
    java.lang.String deviceId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.btelo.coding.data.local.entity.DeviceEntity> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM devices WHERE isOnline = 1")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.btelo.coding.data.local.entity.DeviceEntity>> getOnlineDevices();
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertDevice(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.entity.DeviceEntity device, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Update()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateDevice(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.entity.DeviceEntity device, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Delete()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteDevice(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.entity.DeviceEntity device, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "DELETE FROM devices WHERE id = :deviceId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteDeviceById(@org.jetbrains.annotations.NotNull()
    java.lang.String deviceId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "UPDATE devices SET isOnline = :isOnline, lastSeen = :lastSeen WHERE id = :deviceId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateDeviceStatus(@org.jetbrains.annotations.NotNull()
    java.lang.String deviceId, boolean isOnline, long lastSeen, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}