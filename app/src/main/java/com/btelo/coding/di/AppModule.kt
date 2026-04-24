package com.btelo.coding.di

import android.content.Context
import androidx.room.Room
import com.btelo.coding.data.local.AppDatabase
import com.btelo.coding.data.local.DataStoreManager
import com.btelo.coding.data.local.dao.DeviceDao
import com.btelo.coding.data.local.dao.MessageDao
import com.btelo.coding.data.local.dao.SessionDao
import com.btelo.coding.data.remote.api.AuthApi
import com.btelo.coding.data.remote.encryption.CryptoManager
import com.btelo.coding.data.remote.encryption.SecureKeyStore
import com.btelo.coding.data.remote.network.NetworkMonitor
import com.btelo.coding.data.remote.websocket.factory.WebSocketClientFactory
import com.btelo.coding.data.repository.AuthRepositoryImpl
import com.btelo.coding.data.repository.MessageRepositoryImpl
import com.btelo.coding.data.repository.SessionRepositoryImpl
import com.btelo.coding.domain.repository.AuthRepository
import com.btelo.coding.domain.repository.MessageRepository
import com.btelo.coding.domain.repository.SessionRepository
import com.btelo.coding.util.Logger
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
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
}
