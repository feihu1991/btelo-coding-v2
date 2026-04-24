package com.btelo.coding.ui.session;

import com.btelo.coding.domain.repository.AuthRepository;
import com.btelo.coding.domain.repository.SessionRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class SessionListViewModel_Factory implements Factory<SessionListViewModel> {
  private final Provider<SessionRepository> sessionRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  public SessionListViewModel_Factory(Provider<SessionRepository> sessionRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    this.sessionRepositoryProvider = sessionRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public SessionListViewModel get() {
    return newInstance(sessionRepositoryProvider.get(), authRepositoryProvider.get());
  }

  public static SessionListViewModel_Factory create(
      Provider<SessionRepository> sessionRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    return new SessionListViewModel_Factory(sessionRepositoryProvider, authRepositoryProvider);
  }

  public static SessionListViewModel newInstance(SessionRepository sessionRepository,
      AuthRepository authRepository) {
    return new SessionListViewModel(sessionRepository, authRepository);
  }
}
