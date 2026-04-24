package com.btelo.coding.data.repository;

import com.btelo.coding.data.local.dao.DeviceDao;
import com.btelo.coding.data.remote.encryption.CryptoManager;
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
public final class DeviceRepositoryImpl_Factory implements Factory<DeviceRepositoryImpl> {
  private final Provider<DeviceDao> deviceDaoProvider;

  private final Provider<CryptoManager> cryptoManagerProvider;

  public DeviceRepositoryImpl_Factory(Provider<DeviceDao> deviceDaoProvider,
      Provider<CryptoManager> cryptoManagerProvider) {
    this.deviceDaoProvider = deviceDaoProvider;
    this.cryptoManagerProvider = cryptoManagerProvider;
  }

  @Override
  public DeviceRepositoryImpl get() {
    return newInstance(deviceDaoProvider.get(), cryptoManagerProvider.get());
  }

  public static DeviceRepositoryImpl_Factory create(Provider<DeviceDao> deviceDaoProvider,
      Provider<CryptoManager> cryptoManagerProvider) {
    return new DeviceRepositoryImpl_Factory(deviceDaoProvider, cryptoManagerProvider);
  }

  public static DeviceRepositoryImpl newInstance(DeviceDao deviceDao, CryptoManager cryptoManager) {
    return new DeviceRepositoryImpl(deviceDao, cryptoManager);
  }
}
