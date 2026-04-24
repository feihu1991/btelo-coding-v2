package com.btelo.coding.data.remote.encryption;

import android.content.Context;
import com.google.gson.Gson;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class KeyRotationManager_Factory implements Factory<KeyRotationManager> {
  private final Provider<Context> contextProvider;

  private final Provider<SecureKeyStore> secureKeyStoreProvider;

  private final Provider<CryptoManager> cryptoManagerProvider;

  private final Provider<Gson> gsonProvider;

  public KeyRotationManager_Factory(Provider<Context> contextProvider,
      Provider<SecureKeyStore> secureKeyStoreProvider,
      Provider<CryptoManager> cryptoManagerProvider, Provider<Gson> gsonProvider) {
    this.contextProvider = contextProvider;
    this.secureKeyStoreProvider = secureKeyStoreProvider;
    this.cryptoManagerProvider = cryptoManagerProvider;
    this.gsonProvider = gsonProvider;
  }

  @Override
  public KeyRotationManager get() {
    return newInstance(contextProvider.get(), secureKeyStoreProvider.get(), cryptoManagerProvider.get(), gsonProvider.get());
  }

  public static KeyRotationManager_Factory create(Provider<Context> contextProvider,
      Provider<SecureKeyStore> secureKeyStoreProvider,
      Provider<CryptoManager> cryptoManagerProvider, Provider<Gson> gsonProvider) {
    return new KeyRotationManager_Factory(contextProvider, secureKeyStoreProvider, cryptoManagerProvider, gsonProvider);
  }

  public static KeyRotationManager newInstance(Context context, SecureKeyStore secureKeyStore,
      CryptoManager cryptoManager, Gson gson) {
    return new KeyRotationManager(context, secureKeyStore, cryptoManager, gson);
  }
}
