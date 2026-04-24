package com.btelo.coding.di;

import com.btelo.coding.data.remote.encryption.CryptoManager;
import com.btelo.coding.data.remote.encryption.SecureKeyStore;
import com.btelo.coding.data.remote.network.NetworkMonitor;
import com.btelo.coding.data.remote.websocket.factory.WebSocketClientFactory;
import com.google.gson.Gson;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class AppModule_ProvideWebSocketClientFactoryFactory implements Factory<WebSocketClientFactory> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<Gson> gsonProvider;

  private final Provider<CryptoManager> cryptoManagerProvider;

  private final Provider<NetworkMonitor> networkMonitorProvider;

  private final Provider<SecureKeyStore> secureKeyStoreProvider;

  public AppModule_ProvideWebSocketClientFactoryFactory(Provider<OkHttpClient> okHttpClientProvider,
      Provider<Gson> gsonProvider, Provider<CryptoManager> cryptoManagerProvider,
      Provider<NetworkMonitor> networkMonitorProvider,
      Provider<SecureKeyStore> secureKeyStoreProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.gsonProvider = gsonProvider;
    this.cryptoManagerProvider = cryptoManagerProvider;
    this.networkMonitorProvider = networkMonitorProvider;
    this.secureKeyStoreProvider = secureKeyStoreProvider;
  }

  @Override
  public WebSocketClientFactory get() {
    return provideWebSocketClientFactory(okHttpClientProvider.get(), gsonProvider.get(), cryptoManagerProvider.get(), networkMonitorProvider.get(), secureKeyStoreProvider.get());
  }

  public static AppModule_ProvideWebSocketClientFactoryFactory create(
      Provider<OkHttpClient> okHttpClientProvider, Provider<Gson> gsonProvider,
      Provider<CryptoManager> cryptoManagerProvider,
      Provider<NetworkMonitor> networkMonitorProvider,
      Provider<SecureKeyStore> secureKeyStoreProvider) {
    return new AppModule_ProvideWebSocketClientFactoryFactory(okHttpClientProvider, gsonProvider, cryptoManagerProvider, networkMonitorProvider, secureKeyStoreProvider);
  }

  public static WebSocketClientFactory provideWebSocketClientFactory(OkHttpClient okHttpClient,
      Gson gson, CryptoManager cryptoManager, NetworkMonitor networkMonitor,
      SecureKeyStore secureKeyStore) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideWebSocketClientFactory(okHttpClient, gson, cryptoManager, networkMonitor, secureKeyStore));
  }
}
