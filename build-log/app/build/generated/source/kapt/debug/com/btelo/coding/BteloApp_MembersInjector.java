package com.btelo.coding;

import com.btelo.coding.notification.NotificationChannelManager;
import com.btelo.coding.push.FcmTokenManager;
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
public final class BteloApp_MembersInjector implements MembersInjector<BteloApp> {
  private final Provider<NotificationChannelManager> notificationChannelManagerProvider;

  private final Provider<FcmTokenManager> fcmTokenManagerProvider;

  public BteloApp_MembersInjector(
      Provider<NotificationChannelManager> notificationChannelManagerProvider,
      Provider<FcmTokenManager> fcmTokenManagerProvider) {
    this.notificationChannelManagerProvider = notificationChannelManagerProvider;
    this.fcmTokenManagerProvider = fcmTokenManagerProvider;
  }

  public static MembersInjector<BteloApp> create(
      Provider<NotificationChannelManager> notificationChannelManagerProvider,
      Provider<FcmTokenManager> fcmTokenManagerProvider) {
    return new BteloApp_MembersInjector(notificationChannelManagerProvider, fcmTokenManagerProvider);
  }

  @Override
  public void injectMembers(BteloApp instance) {
    injectNotificationChannelManager(instance, notificationChannelManagerProvider.get());
    injectFcmTokenManager(instance, fcmTokenManagerProvider.get());
  }

  @InjectedFieldSignature("com.btelo.coding.BteloApp.notificationChannelManager")
  public static void injectNotificationChannelManager(BteloApp instance,
      NotificationChannelManager notificationChannelManager) {
    instance.notificationChannelManager = notificationChannelManager;
  }

  @InjectedFieldSignature("com.btelo.coding.BteloApp.fcmTokenManager")
  public static void injectFcmTokenManager(BteloApp instance, FcmTokenManager fcmTokenManager) {
    instance.fcmTokenManager = fcmTokenManager;
  }
}
