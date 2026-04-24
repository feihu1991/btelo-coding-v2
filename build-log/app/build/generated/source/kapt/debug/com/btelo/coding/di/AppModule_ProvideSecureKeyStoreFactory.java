package com.btelo.coding.di;

import android.content.Context;
import com.btelo.coding.data.remote.encryption.SecureKeyStore;
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
public final class AppModule_ProvideSecureKeyStoreFactory implements Factory<SecureKeyStore> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideSecureKeyStoreFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SecureKeyStore get() {
    return provideSecureKeyStore(contextProvider.get());
  }

  public static AppModule_ProvideSecureKeyStoreFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideSecureKeyStoreFactory(contextProvider);
  }

  public static SecureKeyStore provideSecureKeyStore(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSecureKeyStore(context));
  }
}
