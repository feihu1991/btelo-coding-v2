package com.btelo.coding.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.btelo.coding.data.local.dao.DeviceDao
import com.btelo.coding.data.local.dao.MessageDao
import com.btelo.coding.data.local.dao.SessionDao
import com.btelo.coding.data.local.entity.DeviceEntity
import com.btelo.coding.data.local.entity.MessageEntity
import com.btelo.coding.data.local.entity.SessionEntity

@Database(
    entities = [
        SessionEntity::class,
        MessageEntity::class,
        DeviceEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
    abstract fun deviceDao(): DeviceDao
    
    companion object {
        const val DATABASE_NAME = "btelo_coding_db"
        const val DATABASE_VERSION = 2
    }
}
