package com.btelo.coding.di;

import com.btelo.coding.data.local.DataStoreManager;
import com.btelo.coding.data.remote.api.AuthApi;
import com.btelo.coding.domain.repository.AuthRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideAuthRepositoryFactory implements Factory<AuthRepository> {
  private final Provider<AuthApi> authApiProvider;

  private final Provider<DataStoreManager> dataStoreManagerProvider;

  public AppModule_ProvideAuthRepositoryFactory(Provider<AuthApi> authApiProvider,
      Provider<DataStoreManager> dataStoreManagerProvider) {
    this.authApiProvider = authApiProvider;
    this.dataStoreManagerProvider = dataStoreManagerProvider;
  }

  @Override
  public AuthRepository get() {
    return provideAuthRepository(authApiProvider.get(), dataStoreManagerProvider.get());
  }

  public static AppModule_ProvideAuthRepositoryFactory create(Provider<AuthApi> authApiProvider,
      Provider<DataStoreManager> dataStoreManagerProvider) {
    return new AppModule_ProvideAuthRepositoryFactory(authApiProvider, dataStoreManagerProvider);
  }

  public static AuthRepository provideAuthRepository(AuthApi authApi,
      DataStoreManager dataStoreManager) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAuthRepository(authApi, dataStoreManager));
  }
}
