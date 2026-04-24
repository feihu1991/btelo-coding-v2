package com.btelo.coding.di;

import com.btelo.coding.data.local.AppDatabase;
import com.btelo.coding.data.local.dao.DeviceDao;
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
public final class AppModule_ProvideDeviceDaoFactory implements Factory<DeviceDao> {
  private final Provider<AppDatabase> databaseProvider;

  public AppModule_ProvideDeviceDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public DeviceDao get() {
    return provideDeviceDao(databaseProvider.get());
  }

  public static AppModule_ProvideDeviceDaoFactory create(Provider<AppDatabase> databaseProvider) {
    return new AppModule_ProvideDeviceDaoFactory(databaseProvider);
  }

  public static DeviceDao provideDeviceDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideDeviceDao(database));
  }
}
