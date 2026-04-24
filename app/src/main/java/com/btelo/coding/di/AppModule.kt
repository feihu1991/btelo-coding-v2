package com.btelo.coding.di

import android.content.Context
import com.btelo.coding.data.local.DataStoreManager
import com.btelo.coding.data.remote.api.AuthApi
import com.btelo.coding.data.remote.encryption.CryptoManager
import com.btelo.coding.data.remote.websocket.BteloWebSocketClient
import com.btelo.coding.data.repository.AuthRepositoryImpl
import com.btelo.coding.data.repository.MessageRepositoryImpl
import com.btelo.coding.data.repository.SessionRepositoryImpl
import com.btelo.coding.domain.repository.AuthRepository
import com.btelo.coding.domain.repository.MessageRepository
import com.btelo.coding.domain.repository.SessionRepository
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
    fun provideBteloWebSocketClient(
        okHttpClient: OkHttpClient,
        gson: Gson,
        cryptoManager: CryptoManager
    ): BteloWebSocketClient {
        return BteloWebSocketClient(okHttpClient, gson, cryptoManager)
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
    fun provideSessionRepository(): SessionRepository {
        return SessionRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideMessageRepository(
        webSocketClient: BteloWebSocketClient
    ): MessageRepository {
        return MessageRepositoryImpl(webSocketClient)
    }
}
