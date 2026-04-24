package com.btelo.coding.push;

import com.btelo.coding.data.local.DataStoreManager;
import com.btelo.coding.notification.NotificationHelper;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class BteloFirebaseMessagingService_MembersInjector implements MembersInjector<BteloFirebaseMessagingService> {
  private final Provider<DataStoreManager> dataStoreManagerProvider;

  private final Provider<NotificationHelper> notificationHelperProvider;

  public BteloFirebaseMessagingService_MembersInjector(
      Provider<DataStoreManager> dataStoreManagerProvider,
      Provider<NotificationHelper> notificationHelperProvider) {
    this.dataStoreManagerProvider = dataStoreManagerProvider;
    this.notificationHelperProvider = notificationHelperProvider;
  }

  public static MembersInjector<BteloFirebaseMessagingService> create(
      Provider<DataStoreManager> dataStoreManagerProvider,
      Provider<NotificationHelper> notificationHelperProvider) {
    return new BteloFirebaseMessagingService_MembersInjector(dataStoreManagerProvider, notificationHelperProvider);
  }

  @Override
  public void injectMembers(BteloFirebaseMessagingService instance) {
    injectDataStoreManager(instance, dataStoreManagerProvider.get());
    injectNotificationHelper(instance, notificationHelperProvider.get());
  }

  @InjectedFieldSignature("com.btelo.coding.push.BteloFirebaseMessagingService.dataStoreManager")
  public static void injectDataStoreManager(BteloFirebaseMessagingService instance,
      DataStoreManager dataStoreManager) {
    instance.dataStoreManager = dataStoreManager;
  }

  @InjectedFieldSignature("com.btelo.coding.push.BteloFirebaseMessagingService.notificationHelper")
  public static void injectNotificationHelper(BteloFirebaseMessagingService instance,
      NotificationHelper notificationHelper) {
    instance.notificationHelper = notificationHelper;
  }
}
