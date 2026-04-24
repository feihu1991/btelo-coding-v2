package com.btelo.coding.di;

import com.btelo.coding.data.remote.api.AuthApi;
import com.google.gson.Gson;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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
public final class AppModule_ProvideAuthApiFactory implements Factory<AuthApi> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<Gson> gsonProvider;

  public AppModule_ProvideAuthApiFactory(Provider<OkHttpClient> okHttpClientProvider,
      Provider<Gson> gsonProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.gsonProvider = gsonProvider;
  }

  @Override
  public AuthApi get() {
    return provideAuthApi(okHttpClientProvider.get(), gsonProvider.get());
  }

  public static AppModule_ProvideAuthApiFactory create(Provider<OkHttpClient> okHttpClientProvider,
      Provider<Gson> gsonProvider) {
    return new AppModule_ProvideAuthApiFactory(okHttpClientProvider, gsonProvider);
  }

  public static AuthApi provideAuthApi(OkHttpClient okHttpClient, Gson gson) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAuthApi(okHttpClient, gson));
  }
}
