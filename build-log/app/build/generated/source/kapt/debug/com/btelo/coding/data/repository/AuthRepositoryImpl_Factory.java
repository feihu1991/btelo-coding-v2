package com.btelo.coding.data.repository;

import com.btelo.coding.data.local.DataStoreManager;
import com.btelo.coding.data.remote.api.AuthApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class AuthRepositoryImpl_Factory implements Factory<AuthRepositoryImpl> {
  private final Provider<AuthApi> authApiProvider;

  private final Provider<DataStoreManager> dataStoreManagerProvider;

  public AuthRepositoryImpl_Factory(Provider<AuthApi> authApiProvider,
      Provider<DataStoreManager> dataStoreManagerProvider) {
    this.authApiProvider = authApiProvider;
    this.dataStoreManagerProvider = dataStoreManagerProvider;
  }

  @Override
  public AuthRepositoryImpl get() {
    return newInstance(authApiProvider.get(), dataStoreManagerProvider.get());
  }

  public static AuthRepositoryImpl_Factory create(Provider<AuthApi> authApiProvider,
      Provider<DataStoreManager> dataStoreManagerProvider) {
    return new AuthRepositoryImpl_Factory(authApiProvider, dataStoreManagerProvider);
  }

  public static AuthRepositoryImpl newInstance(AuthApi authApi, DataStoreManager dataStoreManager) {
    return new AuthRepositoryImpl(authApi, dataStoreManager);
  }
}
