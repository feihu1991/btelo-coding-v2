package com.btelo.coding.ui.sync;

import com.btelo.coding.data.local.DataStoreManager;
import com.btelo.coding.data.remote.api.SyncApi;
import com.btelo.coding.data.sync.SyncManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class SyncSettingsViewModel_Factory implements Factory<SyncSettingsViewModel> {
  private final Provider<SyncManager> syncManagerProvider;

  private final Provider<DataStoreManager> dataStoreManagerProvider;

  private final Provider<SyncApi> syncApiProvider;

  public SyncSettingsViewModel_Factory(Provider<SyncManager> syncManagerProvider,
      Provider<DataStoreManager> dataStoreManagerProvider, Provider<SyncApi> syncApiProvider) {
    this.syncManagerProvider = syncManagerProvider;
    this.dataStoreManagerProvider = dataStoreManagerProvider;
    this.syncApiProvider = syncApiProvider;
  }

  @Override
  public SyncSettingsViewModel get() {
    return newInstance(syncManagerProvider.get(), dataStoreManagerProvider.get(), syncApiProvider.get());
  }

  public static SyncSettingsViewModel_Factory create(Provider<SyncManager> syncManagerProvider,
      Provider<DataStoreManager> dataStoreManagerProvider, Provider<SyncApi> syncApiProvider) {
    return new SyncSettingsViewModel_Factory(syncManagerProvider, dataStoreManagerProvider, syncApiProvider);
  }

  public static SyncSettingsViewModel newInstance(SyncManager syncManager,
      DataStoreManager dataStoreManager, SyncApi syncApi) {
    return new SyncSettingsViewModel(syncManager, dataStoreManager, syncApi);
  }
}
