package com.btelo.coding.data.remote.websocket.factory;

import com.btelo.coding.data.remote.encryption.CryptoManager;
import com.btelo.coding.data.remote.encryption.SecureKeyStore;
import com.btelo.coding.data.remote.network.NetworkMonitor;
import com.google.gson.Gson;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class WebSocketClientFactory_Factory implements Factory<WebSocketClientFactory> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<Gson> gsonProvider;

  private final Provider<CryptoManager> cryptoManagerProvider;

  private final Provider<NetworkMonitor> networkMonitorProvider;

  private final Provider<SecureKeyStore> secureKeyStoreProvider;

  public WebSocketClientFactory_Factory(Provider<OkHttpClient> okHttpClientProvider,
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
    return newInstance(okHttpClientProvider.get(), gsonProvider.get(), cryptoManagerProvider.get(), networkMonitorProvider.get(), secureKeyStoreProvider.get());
  }

  public static WebSocketClientFactory_Factory create(Provider<OkHttpClient> okHttpClientProvider,
      Provider<Gson> gsonProvider, Provider<CryptoManager> cryptoManagerProvider,
      Provider<NetworkMonitor> networkMonitorProvider,
      Provider<SecureKeyStore> secureKeyStoreProvider) {
    return new WebSocketClientFactory_Factory(okHttpClientProvider, gsonProvider, cryptoManagerProvider, networkMonitorProvider, secureKeyStoreProvider);
  }

  public static WebSocketClientFactory newInstance(OkHttpClient okHttpClient, Gson gson,
      CryptoManager cryptoManager, NetworkMonitor networkMonitor, SecureKeyStore secureKeyStore) {
    return new WebSocketClientFactory(okHttpClient, gson, cryptoManager, networkMonitor, secureKeyStore);
  }
}
