package com.btelo.coding.push;

import com.btelo.coding.notification.NotificationChannelManager;
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
public final class BootReceiver_MembersInjector implements MembersInjector<BootReceiver> {
  private final Provider<NotificationChannelManager> notificationChannelManagerProvider;

  public BootReceiver_MembersInjector(
      Provider<NotificationChannelManager> notificationChannelManagerProvider) {
    this.notificationChannelManagerProvider = notificationChannelManagerProvider;
  }

  public static MembersInjector<BootReceiver> create(
      Provider<NotificationChannelManager> notificationChannelManagerProvider) {
    return new BootReceiver_MembersInjector(notificationChannelManagerProvider);
  }

  @Override
  public void injectMembers(BootReceiver instance) {
    injectNotificationChannelManager(instance, notificationChannelManagerProvider.get());
  }

  @InjectedFieldSignature("com.btelo.coding.push.BootReceiver.notificationChannelManager")
  public static void injectNotificationChannelManager(BootReceiver instance,
      NotificationChannelManager notificationChannelManager) {
    instance.notificationChannelManager = notificationChannelManager;
  }
}
