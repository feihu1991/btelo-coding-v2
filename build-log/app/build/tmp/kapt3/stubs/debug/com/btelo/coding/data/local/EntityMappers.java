package com.btelo.coding.data.local;

import com.btelo.coding.data.local.entity.DeviceEntity;
import com.btelo.coding.data.local.entity.MessageEntity;
import com.btelo.coding.data.local.entity.SessionEntity;
import com.btelo.coding.domain.model.Device;
import com.btelo.coding.domain.model.Message;
import com.btelo.coding.domain.model.MessageType;
import com.btelo.coding.domain.model.Session;

/**
 * 数据层与领域层之间的实体转换工具
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0016\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004*\b\u0012\u0004\u0012\u00020\u00060\u0004J\n\u0010\u0007\u001a\u00020\u0005*\u00020\u0006J\n\u0010\u0007\u001a\u00020\b*\u00020\tJ\n\u0010\u0007\u001a\u00020\n*\u00020\u000bJ\n\u0010\f\u001a\u00020\u0006*\u00020\u0005J\n\u0010\f\u001a\u00020\t*\u00020\bJ\n\u0010\f\u001a\u00020\u000b*\u00020\nJ.\u0010\r\u001a\u00020\t*\u00020\t2\u0006\u0010\u000e\u001a\u00020\u000f2\b\u0010\u0010\u001a\u0004\u0018\u00010\u00112\u0006\u0010\u0012\u001a\u00020\u00132\b\b\u0002\u0010\u0014\u001a\u00020\u0013J\u0016\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\b0\u0004*\b\u0012\u0004\u0012\u00020\t0\u0004J\u0016\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\n0\u0004*\b\u0012\u0004\u0012\u00020\u000b0\u0004\u00a8\u0006\u0017"}, d2 = {"Lcom/btelo/coding/data/local/EntityMappers;", "", "()V", "toDeviceList", "", "Lcom/btelo/coding/domain/model/Device;", "Lcom/btelo/coding/data/local/entity/DeviceEntity;", "toDomain", "Lcom/btelo/coding/domain/model/Message;", "Lcom/btelo/coding/data/local/entity/MessageEntity;", "Lcom/btelo/coding/domain/model/Session;", "Lcom/btelo/coding/data/local/entity/SessionEntity;", "toEntity", "toEntityWithSync", "version", "", "deviceId", "", "isSynced", "", "isDeleted", "toMessageList", "toSessionList", "app_debug"})
public final class EntityMappers {
    @org.jetbrains.annotations.NotNull()
    public static final com.btelo.coding.data.local.EntityMappers INSTANCE = null;
    
    private EntityMappers() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.domain.model.Session toDomain(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.entity.SessionEntity $this$toDomain) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.local.entity.SessionEntity toEntity(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.domain.model.Session $this$toEntity) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.domain.model.Message toDomain(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.entity.MessageEntity $this$toDomain) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.local.entity.MessageEntity toEntity(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.domain.model.Message $this$toEntity) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.local.entity.MessageEntity toEntityWithSync(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.entity.MessageEntity $this$toEntityWithSync, int version, @org.jetbrains.annotations.Nullable()
    java.lang.String deviceId, boolean isSynced, boolean isDeleted) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.domain.model.Device toDomain(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.entity.DeviceEntity $this$toDomain) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.local.entity.DeviceEntity toEntity(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.domain.model.Device $this$toEntity) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.btelo.coding.domain.model.Session> toSessionList(@org.jetbrains.annotations.NotNull()
    java.util.List<com.btelo.coding.data.local.entity.SessionEntity> $this$toSessionList) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.btelo.coding.domain.model.Message> toMessageList(@org.jetbrains.annotations.NotNull()
    java.util.List<com.btelo.coding.data.local.entity.MessageEntity> $this$toMessageList) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.btelo.coding.domain.model.Device> toDeviceList(@org.jetbrains.annotations.NotNull()
    java.util.List<com.btelo.coding.data.local.entity.DeviceEntity> $this$toDeviceList) {
        return null;
    }
}