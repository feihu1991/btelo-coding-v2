package com.btelo.coding.ui.chat;

import com.btelo.coding.domain.repository.AuthRepository;
import com.btelo.coding.domain.repository.MessageRepository;
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
public final class ChatViewModel_Factory implements Factory<ChatViewModel> {
  private final Provider<MessageRepository> messageRepositoryProvider;

  private final Provider<SessionRepository> sessionRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  public ChatViewModel_Factory(Provider<MessageRepository> messageRepositoryProvider,
      Provider<SessionRepository> sessionRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    this.messageRepositoryProvider = messageRepositoryProvider;
    this.sessionRepositoryProvider = sessionRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public ChatViewModel get() {
    return newInstance(messageRepositoryProvider.get(), sessionRepositoryProvider.get(), authRepositoryProvider.get());
  }

  public static ChatViewModel_Factory create(Provider<MessageRepository> messageRepositoryProvider,
      Provider<SessionRepository> sessionRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    return new ChatViewModel_Factory(messageRepositoryProvider, sessionRepositoryProvider, authRepositoryProvider);
  }

  public static ChatViewModel newInstance(MessageRepository messageRepository,
      SessionRepository sessionRepository, AuthRepository authRepository) {
    return new ChatViewModel(messageRepository, sessionRepository, authRepository);
  }
}
