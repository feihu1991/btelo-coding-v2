package com.btelo.coding.di;

import android.content.Context;
import com.btelo.coding.data.remote.encryption.CryptoManager;
import com.btelo.coding.data.remote.encryption.KeyRotationManager;
import com.btelo.coding.data.remote.encryption.SecureKeyStore;
import com.google.gson.Gson;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideKeyRotationManagerFactory implements Factory<KeyRotationManager> {
  private final Provider<Context> contextProvider;

  private final Provider<SecureKeyStore> secureKeyStoreProvider;

  private final Provider<CryptoManager> cryptoManagerProvider;

  private final Provider<Gson> gsonProvider;

  public AppModule_ProvideKeyRotationManagerFactory(Provider<Context> contextProvider,
      Provider<SecureKeyStore> secureKeyStoreProvider,
      Provider<CryptoManager> cryptoManagerProvider, Provider<Gson> gsonProvider) {
    this.contextProvider = contextProvider;
    this.secureKeyStoreProvider = secureKeyStoreProvider;
    this.cryptoManagerProvider = cryptoManagerProvider;
    this.gsonProvider = gsonProvider;
  }

  @Override
  public KeyRotationManager get() {
    return provideKeyRotationManager(contextProvider.get(), secureKeyStoreProvider.get(), cryptoManagerProvider.get(), gsonProvider.get());
  }

  public static AppModule_ProvideKeyRotationManagerFactory create(Provider<Context> contextProvider,
      Provider<SecureKeyStore> secureKeyStoreProvider,
      Provider<CryptoManager> cryptoManagerProvider, Provider<Gson> gsonProvider) {
    return new AppModule_ProvideKeyRotationManagerFactory(contextProvider, secureKeyStoreProvider, cryptoManagerProvider, gsonProvider);
  }

  public static KeyRotationManager provideKeyRotationManager(Context context,
      SecureKeyStore secureKeyStore, CryptoManager cryptoManager, Gson gson) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideKeyRotationManager(context, secureKeyStore, cryptoManager, gson));
  }
}
