package com.btelo.coding.domain.repository;

import com.btelo.coding.domain.model.Device;
import kotlinx.coroutines.flow.Flow;

/**
 * 设备配对和管理的仓库接口
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0005\bf\u0018\u00002\u00020\u0001J$\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\u0006\u0010\u0005\u001a\u00020\u0006H\u00a6@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u0007\u0010\bJ\u0014\u0010\t\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u000b0\nH&J\u0018\u0010\r\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\f0\n2\u0006\u0010\u000e\u001a\u00020\u0006H&J\u0014\u0010\u000f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u000b0\nH&J\u0016\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u000e\u001a\u00020\u0006H\u00a6@\u00a2\u0006\u0002\u0010\bJ\u001e\u0010\u0012\u001a\u00020\u00112\u0006\u0010\u000e\u001a\u00020\u00062\u0006\u0010\u0013\u001a\u00020\u0014H\u00a6@\u00a2\u0006\u0002\u0010\u0015J$\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\f0\u00032\u0006\u0010\u0017\u001a\u00020\u0006H\u00a6@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u0018\u0010\b\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006\u0019"}, d2 = {"Lcom/btelo/coding/domain/repository/DeviceRepository;", "", "generatePairingCode", "Lkotlin/Result;", "Lcom/btelo/coding/domain/repository/PairingCodeInfo;", "deviceName", "", "generatePairingCode-gIAlu-s", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAllDevices", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/btelo/coding/domain/model/Device;", "getDeviceById", "deviceId", "getOnlineDevices", "removeDevice", "", "updateDeviceStatus", "isOnline", "", "(Ljava/lang/String;ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "verifyPairingCode", "pairingCode", "verifyPairingCode-gIAlu-s", "app_debug"})
public abstract interface DeviceRepository {
    
    /**
     * 获取所有已配对设备列表
     */
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.btelo.coding.domain.model.Device>> getAllDevices();
    
    /**
     * 获取在线设备列表
     */
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.btelo.coding.domain.model.Device>> getOnlineDevices();
    
    /**
     * 通过设备ID获取设备信息
     */
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<com.btelo.coding.domain.model.Device> getDeviceById(@org.jetbrains.annotations.NotNull()
    java.lang.String deviceId);
    
    /**
     * 移除设备配对
     * @param deviceId 要移除的设备ID
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object removeDevice(@org.jetbrains.annotations.NotNull()
    java.lang.String deviceId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * 更新设备在线状态
     * @param deviceId 设备ID
     * @param isOnline 是否在线
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateDeviceStatus(@org.jetbrains.annotations.NotNull()
    java.lang.String deviceId, boolean isOnline, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}