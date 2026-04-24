package com.btelo.coding.di;

import com.btelo.coding.data.local.dao.SessionDao;
import com.btelo.coding.domain.repository.SessionRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class AppModule_ProvideSessionRepositoryFactory implements Factory<SessionRepository> {
  private final Provider<SessionDao> sessionDaoProvider;

  public AppModule_ProvideSessionRepositoryFactory(Provider<SessionDao> sessionDaoProvider) {
    this.sessionDaoProvider = sessionDaoProvider;
  }

  @Override
  public SessionRepository get() {
    return provideSessionRepository(sessionDaoProvider.get());
  }

  public static AppModule_ProvideSessionRepositoryFactory create(
      Provider<SessionDao> sessionDaoProvider) {
    return new AppModule_ProvideSessionRepositoryFactory(sessionDaoProvider);
  }

  public static SessionRepository provideSessionRepository(SessionDao sessionDao) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSessionRepository(sessionDao));
  }
}
