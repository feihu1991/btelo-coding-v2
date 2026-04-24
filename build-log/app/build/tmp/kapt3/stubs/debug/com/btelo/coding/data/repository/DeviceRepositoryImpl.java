package com.btelo.coding.data.repository;

import com.btelo.coding.data.local.dao.DeviceDao;
import com.btelo.coding.data.local.entity.DeviceEntity;
import com.btelo.coding.data.remote.encryption.CryptoManager;
import com.btelo.coding.domain.model.Device;
import com.btelo.coding.domain.repository.DeviceRepository;
import com.btelo.coding.domain.repository.PairingCodeInfo;
import com.btelo.coding.util.Logger;
import kotlinx.coroutines.flow.Flow;
import java.security.SecureRandom;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000R\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0007\u0018\u0000 !2\u00020\u0001:\u0002!\"B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J$\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\b2\u0006\u0010\n\u001a\u00020\u000bH\u0096@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\f\u0010\rJ\b\u0010\u000e\u001a\u00020\u000bH\u0002J\u0014\u0010\u000f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00120\u00110\u0010H\u0016J\u0018\u0010\u0013\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00120\u00102\u0006\u0010\u0014\u001a\u00020\u000bH\u0016J\u0014\u0010\u0015\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00120\u00110\u0010H\u0016J\u0016\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0014\u001a\u00020\u000bH\u0096@\u00a2\u0006\u0002\u0010\rJ\u001e\u0010\u0018\u001a\u00020\u00172\u0006\u0010\u0014\u001a\u00020\u000b2\u0006\u0010\u0019\u001a\u00020\u001aH\u0096@\u00a2\u0006\u0002\u0010\u001bJ$\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u00120\b2\u0006\u0010\u001d\u001a\u00020\u000bH\u0096@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u001e\u0010\rJ\f\u0010\u001f\u001a\u00020\u0012*\u00020 H\u0002R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006#"}, d2 = {"Lcom/btelo/coding/data/repository/DeviceRepositoryImpl;", "Lcom/btelo/coding/domain/repository/DeviceRepository;", "deviceDao", "Lcom/btelo/coding/data/local/dao/DeviceDao;", "cryptoManager", "Lcom/btelo/coding/data/remote/encryption/CryptoManager;", "(Lcom/btelo/coding/data/local/dao/DeviceDao;Lcom/btelo/coding/data/remote/encryption/CryptoManager;)V", "generatePairingCode", "Lkotlin/Result;", "Lcom/btelo/coding/domain/repository/PairingCodeInfo;", "deviceName", "", "generatePairingCode-gIAlu-s", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "generateSecurePairingCode", "getAllDevices", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/btelo/coding/domain/model/Device;", "getDeviceById", "deviceId", "getOnlineDevices", "removeDevice", "", "updateDeviceStatus", "isOnline", "", "(Ljava/lang/String;ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "verifyPairingCode", "pairingCode", "verifyPairingCode-gIAlu-s", "toDomain", "Lcom/btelo/coding/data/local/entity/DeviceEntity;", "Companion", "PendingPairingData", "app_debug"})
public final class DeviceRepositoryImpl implements com.btelo.coding.domain.repository.DeviceRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.data.local.dao.DeviceDao deviceDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.data.remote.encryption.CryptoManager cryptoManager = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "DeviceRepository";
    private static final int PAIRING_CODE_LENGTH = 6;
    private static final long PAIRING_CODE_VALIDITY_MS = 300000L;
    @org.jetbrains.annotations.NotNull()
    private static final java.util.Map<java.lang.String, com.btelo.coding.data.repository.DeviceRepositoryImpl.PendingPairingData> pendingPairingCodes = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.btelo.coding.data.repository.DeviceRepositoryImpl.Companion Companion = null;
    
    @javax.inject.Inject()
    public DeviceRepositoryImpl(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.dao.DeviceDao deviceDao, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.encryption.CryptoManager cryptoManager) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<java.util.List<com.btelo.coding.domain.model.Device>> getAllDevices() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<java.util.List<com.btelo.coding.domain.model.Device>> getOnlineDevices() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<com.btelo.coding.domain.model.Device> getDeviceById(@org.jetbrains.annotations.NotNull()
    java.lang.String deviceId) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object removeDevice(@org.jetbrains.annotations.NotNull()
    java.lang.String deviceId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object updateDeviceStatus(@org.jetbrains.annotations.NotNull()
    java.lang.String deviceId, boolean isOnline, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * Generate a secure random pairing code
     */
    private final java.lang.String generateSecurePairingCode() {
        return null;
    }
    
    /**
     * Extension function to convert DeviceEntity to Device domain model
     */
    private final com.btelo.coding.domain.model.Device toDomain(com.btelo.coding.data.local.entity.DeviceEntity $this$toDomain) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010%\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082T\u00a2\u0006\u0002\n\u0000R\u001a\u0010\t\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\f"}, d2 = {"Lcom/btelo/coding/data/repository/DeviceRepositoryImpl$Companion;", "", "()V", "PAIRING_CODE_LENGTH", "", "PAIRING_CODE_VALIDITY_MS", "", "TAG", "", "pendingPairingCodes", "", "Lcom/btelo/coding/data/repository/DeviceRepositoryImpl$PendingPairingData;", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
    
    /**
     * Internal data class for pending pairing operations
     */
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u000b\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0082\b\u0018\u00002\u00020\u0001B\u001d\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\u0002\u0010\u0007J\t\u0010\r\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u000e\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u000f\u001a\u00020\u0006H\u00c6\u0003J\'\u0010\u0010\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u0006H\u00c6\u0001J\u0013\u0010\u0011\u001a\u00020\u00122\b\u0010\u0013\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0014\u001a\u00020\u0015H\u00d6\u0001J\t\u0010\u0016\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\t\u00a8\u0006\u0017"}, d2 = {"Lcom/btelo/coding/data/repository/DeviceRepositoryImpl$PendingPairingData;", "", "deviceName", "", "publicKey", "expiresAt", "", "(Ljava/lang/String;Ljava/lang/String;J)V", "getDeviceName", "()Ljava/lang/String;", "getExpiresAt", "()J", "getPublicKey", "component1", "component2", "component3", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
    static final class PendingPairingData {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String deviceName = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String publicKey = null;
        private final long expiresAt = 0L;
        
        public PendingPairingData(@org.jetbrains.annotations.NotNull()
        java.lang.String deviceName, @org.jetbrains.annotations.NotNull()
        java.lang.String publicKey, long expiresAt) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getDeviceName() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getPublicKey() {
            return null;
        }
        
        public final long getExpiresAt() {
            return 0L;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component2() {
            return null;
        }
        
        public final long component3() {
            return 0L;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.btelo.coding.data.repository.DeviceRepositoryImpl.PendingPairingData copy(@org.jetbrains.annotations.NotNull()
        java.lang.String deviceName, @org.jetbrains.annotations.NotNull()
        java.lang.String publicKey, long expiresAt) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
}