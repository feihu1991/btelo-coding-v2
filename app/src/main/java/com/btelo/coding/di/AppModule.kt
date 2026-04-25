package com.btelo.coding.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.btelo.coding.data.local.AppDatabase
import com.btelo.coding.data.local.DataStoreManager
import com.btelo.coding.data.local.dao.DeviceDao
import com.btelo.coding.data.local.dao.MessageDao
import com.btelo.coding.data.local.dao.SessionDao
import com.btelo.coding.data.remote.api.AuthApi
import com.btelo.coding.data.remote.api.SyncApi
import com.btelo.coding.data.remote.encryption.CryptoManager
import com.btelo.coding.data.remote.encryption.SecureKeyStore
import com.btelo.coding.data.remote.network.NetworkMonitor
import com.btelo.coding.data.remote.websocket.factory.WebSocketClientFactory
import com.btelo.coding.data.repository.AuthRepositoryImpl
import com.btelo.coding.data.sync.ApplicationScope
import com.btelo.coding.data.repository.DeviceRepositoryImpl
import com.btelo.coding.data.repository.MessageRepositoryImpl
import com.btelo.coding.data.repository.SessionRepositoryImpl
import com.btelo.coding.domain.repository.AuthRepository
import com.btelo.coding.domain.repository.DeviceRepository
import com.btelo.coding.domain.repository.MessageRepository
import com.btelo.coding.domain.repository.SessionRepository
import com.btelo.coding.notification.NotificationChannelManager
import com.btelo.coding.notification.NotificationHelper
import com.btelo.coding.push.FcmTokenManager
import com.btelo.coding.util.Logger
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    init {
        Logger.init()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS) // WebSocket ping
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(okHttpClient: OkHttpClient, gson: Gson): AuthApi {
        return AuthApi(okHttpClient, gson)
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://localhost:8080/") // Base URL placeholder
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideSyncApi(retrofit: Retrofit): SyncApi {
        return retrofit.create(SyncApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDataStoreManager(@ApplicationContext context: Context): DataStoreManager {
        return DataStoreManager(context)
    }

    @Provides
    @Singleton
    fun provideCryptoManager(): CryptoManager {
        return CryptoManager()
    }
    
    @Provides
    @Singleton
    fun provideSecureKeyStore(@ApplicationContext context: Context): SecureKeyStore {
        return SecureKeyStore(context)
    }
    
    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitor(context)
    }
    
    @Provides
    @Singleton
    fun provideWebSocketClientFactory(
        okHttpClient: OkHttpClient,
        gson: Gson,
        cryptoManager: CryptoManager,
        networkMonitor: NetworkMonitor,
        secureKeyStore: SecureKeyStore
    ): WebSocketClientFactory {
        return WebSocketClientFactory(okHttpClient, gson, cryptoManager, networkMonitor, secureKeyStore)
    }
    
    // 数据库迁移策略：从 v1 升级到 v2
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 为 sessions 表添加密钥版本相关字段
            database.execSQL("ALTER TABLE sessions ADD COLUMN currentKeyVersion INTEGER NOT NULL DEFAULT 1")
            database.execSQL("ALTER TABLE sessions ADD COLUMN lastKeyRotation INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE sessions ADD COLUMN rotationIntervalDays INTEGER NOT NULL DEFAULT 7")
            
            // 为 messages 表添加密钥版本字段
            database.execSQL("ALTER TABLE messages ADD COLUMN keyVersion INTEGER NOT NULL DEFAULT 1")
        }
    }
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    @Singleton
    fun provideSessionDao(database: AppDatabase): SessionDao {
        return database.sessionDao()
    }
    
    @Provides
    @Singleton
    fun provideMessageDao(database: AppDatabase): MessageDao {
        return database.messageDao()
    }
    
    @Provides
    @Singleton
    fun provideDeviceDao(database: AppDatabase): DeviceDao {
        return database.deviceDao()
    }
    
    @Provides
    @Singleton
    fun provideDeviceRepository(
        deviceDao: DeviceDao,
        cryptoManager: CryptoManager
    ): DeviceRepository {
        return DeviceRepositoryImpl(deviceDao, cryptoManager)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        authApi: AuthApi,
        dataStoreManager: DataStoreManager
    ): AuthRepository {
        return AuthRepositoryImpl(authApi, dataStoreManager)
    }

    @Provides
    @Singleton
    fun provideSessionRepository(
        sessionDao: SessionDao
    ): SessionRepository {
        return SessionRepositoryImpl(sessionDao)
    }

    @Provides
    @Singleton
    fun provideMessageRepository(
        messageDao: MessageDao,
        webSocketFactory: WebSocketClientFactory
    ): MessageRepository {
        return MessageRepositoryImpl(messageDao, webSocketFactory)
    }

    @Provides
    @Singleton
    fun provideNotificationChannelManager(@ApplicationContext context: Context): NotificationChannelManager {
        return NotificationChannelManager(context)
    }

    @Provides
    @Singleton
    fun provideNotificationHelper(
        @ApplicationContext context: Context,
        dataStoreManager: DataStoreManager
    ): NotificationHelper {
        return NotificationHelper(context, dataStoreManager)
    }

    @Provides
    @Singleton
    fun provideFcmTokenManager(@ApplicationContext context: Context): FcmTokenManager {
        return FcmTokenManager(context)
    }
    
    @Provides
    @Singleton
    fun provideKeyRotationManager(
        @ApplicationContext context: Context,
        secureKeyStore: SecureKeyStore,
        cryptoManager: CryptoManager,
        gson: Gson
    ): com.btelo.coding.data.remote.encryption.KeyRotationManager {
        return com.btelo.coding.data.remote.encryption.KeyRotationManager(
            context, secureKeyStore, cryptoManager, gson
        )
    }
}
