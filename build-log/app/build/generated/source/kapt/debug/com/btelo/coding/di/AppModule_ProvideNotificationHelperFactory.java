package com.btelo.coding.di;

import android.content.Context;
import com.btelo.coding.data.local.DataStoreManager;
import com.btelo.coding.notification.NotificationHelper;
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
public final class AppModule_ProvideNotificationHelperFactory implements Factory<NotificationHelper> {
  private final Provider<Context> contextProvider;

  private final Provider<DataStoreManager> dataStoreManagerProvider;

  public AppModule_ProvideNotificationHelperFactory(Provider<Context> contextProvider,
      Provider<DataStoreManager> dataStoreManagerProvider) {
    this.contextProvider = contextProvider;
    this.dataStoreManagerProvider = dataStoreManagerProvider;
  }

  @Override
  public NotificationHelper get() {
    return provideNotificationHelper(contextProvider.get(), dataStoreManagerProvider.get());
  }

  public static AppModule_ProvideNotificationHelperFactory create(Provider<Context> contextProvider,
      Provider<DataStoreManager> dataStoreManagerProvider) {
    return new AppModule_ProvideNotificationHelperFactory(contextProvider, dataStoreManagerProvider);
  }

  public static NotificationHelper provideNotificationHelper(Context context,
      DataStoreManager dataStoreManager) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideNotificationHelper(context, dataStoreManager));
  }
}
