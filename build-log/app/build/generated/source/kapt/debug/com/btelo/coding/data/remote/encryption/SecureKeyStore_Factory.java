package com.btelo.coding.data.remote.encryption;

import android.content.Context;
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
public final class SecureKeyStore_Factory implements Factory<SecureKeyStore> {
  private final Provider<Context> contextProvider;

  public SecureKeyStore_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SecureKeyStore get() {
    return newInstance(contextProvider.get());
  }

  public static SecureKeyStore_Factory create(Provider<Context> contextProvider) {
    return new SecureKeyStore_Factory(contextProvider);
  }

  public static SecureKeyStore newInstance(Context context) {
    return new SecureKeyStore(context);
  }
}
