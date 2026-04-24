package com.btelo.coding.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import com.btelo.coding.data.local.dao.DeviceDao;
import com.btelo.coding.data.local.dao.MessageDao;
import com.btelo.coding.data.local.dao.SessionDao;
import com.btelo.coding.data.local.entity.DeviceEntity;
import com.btelo.coding.data.local.entity.MessageEntity;
import com.btelo.coding.data.local.entity.SessionEntity;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\'\u0018\u0000 \t2\u00020\u0001:\u0001\tB\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H&J\b\u0010\u0005\u001a\u00020\u0006H&J\b\u0010\u0007\u001a\u00020\bH&\u00a8\u0006\n"}, d2 = {"Lcom/btelo/coding/data/local/AppDatabase;", "Landroidx/room/RoomDatabase;", "()V", "deviceDao", "Lcom/btelo/coding/data/local/dao/DeviceDao;", "messageDao", "Lcom/btelo/coding/data/local/dao/MessageDao;", "sessionDao", "Lcom/btelo/coding/data/local/dao/SessionDao;", "Companion", "app_debug"})
@androidx.room.Database(entities = {com.btelo.coding.data.local.entity.SessionEntity.class, com.btelo.coding.data.local.entity.MessageEntity.class, com.btelo.coding.data.local.entity.DeviceEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends androidx.room.RoomDatabase {
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String DATABASE_NAME = "btelo_coding_db";
    public static final int DATABASE_VERSION = 2;
    @org.jetbrains.annotations.NotNull()
    public static final com.btelo.coding.data.local.AppDatabase.Companion Companion = null;
    
    public AppDatabase() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.btelo.coding.data.local.dao.SessionDao sessionDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.btelo.coding.data.local.dao.MessageDao messageDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.btelo.coding.data.local.dao.DeviceDao deviceDao();
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/btelo/coding/data/local/AppDatabase$Companion;", "", "()V", "DATABASE_NAME", "", "DATABASE_VERSION", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}