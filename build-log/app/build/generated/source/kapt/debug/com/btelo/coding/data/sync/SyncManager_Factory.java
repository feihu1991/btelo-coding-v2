package com.btelo.coding.data.sync;

import com.btelo.coding.data.local.DataStoreManager;
import com.btelo.coding.data.local.dao.MessageDao;
import com.btelo.coding.data.remote.api.SyncApi;
import com.btelo.coding.data.remote.network.NetworkMonitor;
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
public final class SyncManager_Factory implements Factory<SyncManager> {
  private final Provider<SyncApi> syncApiProvider;

  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<DataStoreManager> dataStoreManagerProvider;

  private final Provider<NetworkMonitor> networkMonitorProvider;

  public SyncManager_Factory(Provider<SyncApi> syncApiProvider,
      Provider<MessageDao> messageDaoProvider, Provider<DataStoreManager> dataStoreManagerProvider,
      Provider<NetworkMonitor> networkMonitorProvider) {
    this.syncApiProvider = syncApiProvider;
    this.messageDaoProvider = messageDaoProvider;
    this.dataStoreManagerProvider = dataStoreManagerProvider;
    this.networkMonitorProvider = networkMonitorProvider;
  }

  @Override
  public SyncManager get() {
    return newInstance(syncApiProvider.get(), messageDaoProvider.get(), dataStoreManagerProvider.get(), networkMonitorProvider.get());
  }

  public static SyncManager_Factory create(Provider<SyncApi> syncApiProvider,
      Provider<MessageDao> messageDaoProvider, Provider<DataStoreManager> dataStoreManagerProvider,
      Provider<NetworkMonitor> networkMonitorProvider) {
    return new SyncManager_Factory(syncApiProvider, messageDaoProvider, dataStoreManagerProvider, networkMonitorProvider);
  }

  public static SyncManager newInstance(SyncApi syncApi, MessageDao messageDao,
      DataStoreManager dataStoreManager, NetworkMonitor networkMonitor) {
    return new SyncManager(syncApi, messageDao, dataStoreManager, networkMonitor);
  }
}
