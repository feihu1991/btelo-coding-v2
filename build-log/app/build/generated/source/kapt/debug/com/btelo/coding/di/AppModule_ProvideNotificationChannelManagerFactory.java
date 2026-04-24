package com.btelo.coding.di;

import android.content.Context;
import com.btelo.coding.notification.NotificationChannelManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AppModule_ProvideNotificationChannelManagerFactory implements Factory<NotificationChannelManager> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideNotificationChannelManagerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public NotificationChannelManager get() {
    return provideNotificationChannelManager(contextProvider.get());
  }

  public static AppModule_ProvideNotificationChannelManagerFactory create(
      Provider<Context> contextProvider) {
    return new AppModule_ProvideNotificationChannelManagerFactory(contextProvider);
  }

  public static NotificationChannelManager provideNotificationChannelManager(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideNotificationChannelManager(context));
  }
}
