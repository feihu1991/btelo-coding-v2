package com.btelo.coding.di;

import android.content.Context;
import com.btelo.coding.push.FcmTokenManager;
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
public final class AppModule_ProvideFcmTokenManagerFactory implements Factory<FcmTokenManager> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideFcmTokenManagerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public FcmTokenManager get() {
    return provideFcmTokenManager(contextProvider.get());
  }

  public static AppModule_ProvideFcmTokenManagerFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideFcmTokenManagerFactory(contextProvider);
  }

  public static FcmTokenManager provideFcmTokenManager(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideFcmTokenManager(context));
  }
}
