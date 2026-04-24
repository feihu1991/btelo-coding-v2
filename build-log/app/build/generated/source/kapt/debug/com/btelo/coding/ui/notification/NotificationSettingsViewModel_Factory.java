package com.btelo.coding.ui.notification;

import com.btelo.coding.data.local.DataStoreManager;
import com.btelo.coding.notification.NotificationChannelManager;
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
public final class NotificationSettingsViewModel_Factory implements Factory<NotificationSettingsViewModel> {
  private final Provider<DataStoreManager> dataStoreManagerProvider;

  private final Provider<NotificationChannelManager> notificationChannelManagerProvider;

  public NotificationSettingsViewModel_Factory(Provider<DataStoreManager> dataStoreManagerProvider,
      Provider<NotificationChannelManager> notificationChannelManagerProvider) {
    this.dataStoreManagerProvider = dataStoreManagerProvider;
    this.notificationChannelManagerProvider = notificationChannelManagerProvider;
  }

  @Override
  public NotificationSettingsViewModel get() {
    return newInstance(dataStoreManagerProvider.get(), notificationChannelManagerProvider.get());
  }

  public static NotificationSettingsViewModel_Factory create(
      Provider<DataStoreManager> dataStoreManagerProvider,
      Provider<NotificationChannelManager> notificationChannelManagerProvider) {
    return new NotificationSettingsViewModel_Factory(dataStoreManagerProvider, notificationChannelManagerProvider);
  }

  public static NotificationSettingsViewModel newInstance(DataStoreManager dataStoreManager,
      NotificationChannelManager notificationChannelManager) {
    return new NotificationSettingsViewModel(dataStoreManager, notificationChannelManager);
  }
}
