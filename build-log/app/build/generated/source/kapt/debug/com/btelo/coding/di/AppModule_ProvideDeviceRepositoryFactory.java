package com.btelo.coding.di;

import com.btelo.coding.data.local.dao.DeviceDao;
import com.btelo.coding.data.remote.encryption.CryptoManager;
import com.btelo.coding.domain.repository.DeviceRepository;
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
public final class AppModule_ProvideDeviceRepositoryFactory implements Factory<DeviceRepository> {
  private final Provider<DeviceDao> deviceDaoProvider;

  private final Provider<CryptoManager> cryptoManagerProvider;

  public AppModule_ProvideDeviceRepositoryFactory(Provider<DeviceDao> deviceDaoProvider,
      Provider<CryptoManager> cryptoManagerProvider) {
    this.deviceDaoProvider = deviceDaoProvider;
    this.cryptoManagerProvider = cryptoManagerProvider;
  }

  @Override
  public DeviceRepository get() {
    return provideDeviceRepository(deviceDaoProvider.get(), cryptoManagerProvider.get());
  }

  public static AppModule_ProvideDeviceRepositoryFactory create(
      Provider<DeviceDao> deviceDaoProvider, Provider<CryptoManager> cryptoManagerProvider) {
    return new AppModule_ProvideDeviceRepositoryFactory(deviceDaoProvider, cryptoManagerProvider);
  }

  public static DeviceRepository provideDeviceRepository(DeviceDao deviceDao,
      CryptoManager cryptoManager) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideDeviceRepository(deviceDao, cryptoManager));
  }
}
