package com.btelo.coding.notification;

import android.content.Context;
import com.btelo.coding.data.local.DataStoreManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class NotificationHelper_Factory implements Factory<NotificationHelper> {
  private final Provider<Context> contextProvider;

  private final Provider<DataStoreManager> dataStoreManagerProvider;

  public NotificationHelper_Factory(Provider<Context> contextProvider,
      Provider<DataStoreManager> dataStoreManagerProvider) {
    this.contextProvider = contextProvider;
    this.dataStoreManagerProvider = dataStoreManagerProvider;
  }

  @Override
  public NotificationHelper get() {
    return newInstance(contextProvider.get(), dataStoreManagerProvider.get());
  }

  public static NotificationHelper_Factory create(Provider<Context> contextProvider,
      Provider<DataStoreManager> dataStoreManagerProvider) {
    return new NotificationHelper_Factory(contextProvider, dataStoreManagerProvider);
  }

  public static NotificationHelper newInstance(Context context, DataStoreManager dataStoreManager) {
    return new NotificationHelper(context, dataStoreManager);
  }
}
