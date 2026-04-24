package com.btelo.coding.di;

import com.btelo.coding.data.remote.api.SyncApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

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
public final class AppModule_ProvideSyncApiFactory implements Factory<SyncApi> {
  private final Provider<Retrofit> retrofitProvider;

  public AppModule_ProvideSyncApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public SyncApi get() {
    return provideSyncApi(retrofitProvider.get());
  }

  public static AppModule_ProvideSyncApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new AppModule_ProvideSyncApiFactory(retrofitProvider);
  }

  public static SyncApi provideSyncApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSyncApi(retrofit));
  }
}
