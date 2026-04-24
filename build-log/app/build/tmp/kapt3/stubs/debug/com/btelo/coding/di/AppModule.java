package com.btelo.coding.di;

import android.content.Context;
import androidx.room.Room;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.btelo.coding.data.local.AppDatabase;
import com.btelo.coding.data.local.DataStoreManager;
import com.btelo.coding.data.local.dao.DeviceDao;
import com.btelo.coding.data.local.dao.MessageDao;
import com.btelo.coding.data.local.dao.SessionDao;
import com.btelo.coding.data.remote.api.AuthApi;
import com.btelo.coding.data.remote.api.SyncApi;
import com.btelo.coding.data.remote.encryption.CryptoManager;
import com.btelo.coding.data.remote.encryption.SecureKeyStore;
import com.btelo.coding.data.remote.network.NetworkMonitor;
import com.btelo.coding.data.remote.websocket.factory.WebSocketClientFactory;
import com.btelo.coding.data.repository.AuthRepositoryImpl;
import com.btelo.coding.data.repository.DeviceRepositoryImpl;
import com.btelo.coding.data.repository.MessageRepositoryImpl;
import com.btelo.coding.data.repository.SessionRepositoryImpl;
import com.btelo.coding.domain.repository.AuthRepository;
import com.btelo.coding.domain.repository.DeviceRepository;
import com.btelo.coding.domain.repository.MessageRepository;
import com.btelo.coding.domain.repository.SessionRepository;
import com.btelo.coding.notification.NotificationChannelManager;
import com.btelo.coding.notification.NotificationHelper;
import com.btelo.coding.push.FcmTokenManager;
import com.btelo.coding.util.Logger;
import com.google.gson.Gson;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;

@dagger.Module()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u00b0\u0001\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u00c7\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0005\u001a\u00020\u00062\b\b\u0001\u0010\u0007\u001a\u00020\bH\u0007J\u0018\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eH\u0007J\u0018\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\n2\u0006\u0010\u0012\u001a\u00020\u0013H\u0007J\b\u0010\u0014\u001a\u00020\u0015H\u0007J\u0012\u0010\u0016\u001a\u00020\u00132\b\b\u0001\u0010\u0007\u001a\u00020\bH\u0007J\u0010\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u0006H\u0007J\u0018\u0010\u001a\u001a\u00020\u001b2\u0006\u0010\u001c\u001a\u00020\u00182\u0006\u0010\u001d\u001a\u00020\u0015H\u0007J\u0012\u0010\u001e\u001a\u00020\u001f2\b\b\u0001\u0010\u0007\u001a\u00020\bH\u0007J\b\u0010 \u001a\u00020\u000eH\u0007J*\u0010!\u001a\u00020\"2\b\b\u0001\u0010\u0007\u001a\u00020\b2\u0006\u0010#\u001a\u00020$2\u0006\u0010\u001d\u001a\u00020\u00152\u0006\u0010\r\u001a\u00020\u000eH\u0007J\u0010\u0010%\u001a\u00020&2\u0006\u0010\u0019\u001a\u00020\u0006H\u0007J\u0018\u0010\'\u001a\u00020(2\u0006\u0010)\u001a\u00020&2\u0006\u0010*\u001a\u00020+H\u0007J\u0012\u0010,\u001a\u00020-2\b\b\u0001\u0010\u0007\u001a\u00020\bH\u0007J\u0012\u0010.\u001a\u00020/2\b\b\u0001\u0010\u0007\u001a\u00020\bH\u0007J\u001a\u00100\u001a\u0002012\b\b\u0001\u0010\u0007\u001a\u00020\b2\u0006\u0010\u0012\u001a\u00020\u0013H\u0007J\b\u00102\u001a\u00020\fH\u0007J\u0018\u00103\u001a\u0002042\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eH\u0007J\u0012\u00105\u001a\u00020$2\b\b\u0001\u0010\u0007\u001a\u00020\bH\u0007J\u0010\u00106\u001a\u0002072\u0006\u0010\u0019\u001a\u00020\u0006H\u0007J\u0010\u00108\u001a\u0002092\u0006\u0010:\u001a\u000207H\u0007J\u0010\u0010;\u001a\u00020<2\u0006\u0010=\u001a\u000204H\u0007J0\u0010>\u001a\u00020+2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000e2\u0006\u0010\u001d\u001a\u00020\u00152\u0006\u0010?\u001a\u00020-2\u0006\u0010#\u001a\u00020$H\u0007R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006@"}, d2 = {"Lcom/btelo/coding/di/AppModule;", "", "()V", "MIGRATION_1_2", "Landroidx/room/migration/Migration;", "provideAppDatabase", "Lcom/btelo/coding/data/local/AppDatabase;", "context", "Landroid/content/Context;", "provideAuthApi", "Lcom/btelo/coding/data/remote/api/AuthApi;", "okHttpClient", "Lokhttp3/OkHttpClient;", "gson", "Lcom/google/gson/Gson;", "provideAuthRepository", "Lcom/btelo/coding/domain/repository/AuthRepository;", "authApi", "dataStoreManager", "Lcom/btelo/coding/data/local/DataStoreManager;", "provideCryptoManager", "Lcom/btelo/coding/data/remote/encryption/CryptoManager;", "provideDataStoreManager", "provideDeviceDao", "Lcom/btelo/coding/data/local/dao/DeviceDao;", "database", "provideDeviceRepository", "Lcom/btelo/coding/domain/repository/DeviceRepository;", "deviceDao", "cryptoManager", "provideFcmTokenManager", "Lcom/btelo/coding/push/FcmTokenManager;", "provideGson", "provideKeyRotationManager", "Lcom/btelo/coding/data/remote/encryption/KeyRotationManager;", "secureKeyStore", "Lcom/btelo/coding/data/remote/encryption/SecureKeyStore;", "provideMessageDao", "Lcom/btelo/coding/data/local/dao/MessageDao;", "provideMessageRepository", "Lcom/btelo/coding/domain/repository/MessageRepository;", "messageDao", "webSocketFactory", "Lcom/btelo/coding/data/remote/websocket/factory/WebSocketClientFactory;", "provideNetworkMonitor", "Lcom/btelo/coding/data/remote/network/NetworkMonitor;", "provideNotificationChannelManager", "Lcom/btelo/coding/notification/NotificationChannelManager;", "provideNotificationHelper", "Lcom/btelo/coding/notification/NotificationHelper;", "provideOkHttpClient", "provideRetrofit", "Lretrofit2/Retrofit;", "provideSecureKeyStore", "provideSessionDao", "Lcom/btelo/coding/data/local/dao/SessionDao;", "provideSessionRepository", "Lcom/btelo/coding/domain/repository/SessionRepository;", "sessionDao", "provideSyncApi", "Lcom/btelo/coding/data/remote/api/SyncApi;", "retrofit", "provideWebSocketClientFactory", "networkMonitor", "app_debug"})
@dagger.hilt.InstallIn(value = {dagger.hilt.components.SingletonComponent.class})
public final class AppModule {
    @org.jetbrains.annotations.NotNull()
    private static final androidx.room.migration.Migration MIGRATION_1_2 = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.btelo.coding.di.AppModule INSTANCE = null;
    
    private AppModule() {
        super();
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.google.gson.Gson provideGson() {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final okhttp3.OkHttpClient provideOkHttpClient() {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.remote.api.AuthApi provideAuthApi(@org.jetbrains.annotations.NotNull()
    okhttp3.OkHttpClient okHttpClient, @org.jetbrains.annotations.NotNull()
    com.google.gson.Gson gson) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final retrofit2.Retrofit provideRetrofit(@org.jetbrains.annotations.NotNull()
    okhttp3.OkHttpClient okHttpClient, @org.jetbrains.annotations.NotNull()
    com.google.gson.Gson gson) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.remote.api.SyncApi provideSyncApi(@org.jetbrains.annotations.NotNull()
    retrofit2.Retrofit retrofit) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.local.DataStoreManager provideDataStoreManager(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.remote.encryption.CryptoManager provideCryptoManager() {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.remote.encryption.SecureKeyStore provideSecureKeyStore(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.remote.network.NetworkMonitor provideNetworkMonitor(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.remote.websocket.factory.WebSocketClientFactory provideWebSocketClientFactory(@org.jetbrains.annotations.NotNull()
    okhttp3.OkHttpClient okHttpClient, @org.jetbrains.annotations.NotNull()
    com.google.gson.Gson gson, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.encryption.CryptoManager cryptoManager, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.network.NetworkMonitor networkMonitor, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.encryption.SecureKeyStore secureKeyStore) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.local.AppDatabase provideAppDatabase(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.local.dao.SessionDao provideSessionDao(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.AppDatabase database) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.local.dao.MessageDao provideMessageDao(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.AppDatabase database) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.local.dao.DeviceDao provideDeviceDao(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.AppDatabase database) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.domain.repository.DeviceRepository provideDeviceRepository(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.dao.DeviceDao deviceDao, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.encryption.CryptoManager cryptoManager) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.domain.repository.AuthRepository provideAuthRepository(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.api.AuthApi authApi, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.DataStoreManager dataStoreManager) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.domain.repository.SessionRepository provideSessionRepository(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.dao.SessionDao sessionDao) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.domain.repository.MessageRepository provideMessageRepository(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.dao.MessageDao messageDao, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.websocket.factory.WebSocketClientFactory webSocketFactory) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.notification.NotificationChannelManager provideNotificationChannelManager(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.notification.NotificationHelper provideNotificationHelper(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.DataStoreManager dataStoreManager) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.push.FcmTokenManager provideFcmTokenManager(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.remote.encryption.KeyRotationManager provideKeyRotationManager(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.encryption.SecureKeyStore secureKeyStore, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.remote.encryption.CryptoManager cryptoManager, @org.jetbrains.annotations.NotNull()
    com.google.gson.Gson gson) {
        return null;
    }
}