package com.btelo.coding.di;

import com.btelo.coding.data.local.dao.MessageDao;
import com.btelo.coding.data.remote.websocket.factory.WebSocketClientFactory;
import com.btelo.coding.domain.repository.MessageRepository;
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
public final class AppModule_ProvideMessageRepositoryFactory implements Factory<MessageRepository> {
  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<WebSocketClientFactory> webSocketFactoryProvider;

  public AppModule_ProvideMessageRepositoryFactory(Provider<MessageDao> messageDaoProvider,
      Provider<WebSocketClientFactory> webSocketFactoryProvider) {
    this.messageDaoProvider = messageDaoProvider;
    this.webSocketFactoryProvider = webSocketFactoryProvider;
  }

  @Override
  public MessageRepository get() {
    return provideMessageRepository(messageDaoProvider.get(), webSocketFactoryProvider.get());
  }

  public static AppModule_ProvideMessageRepositoryFactory create(
      Provider<MessageDao> messageDaoProvider,
      Provider<WebSocketClientFactory> webSocketFactoryProvider) {
    return new AppModule_ProvideMessageRepositoryFactory(messageDaoProvider, webSocketFactoryProvider);
  }

  public static MessageRepository provideMessageRepository(MessageDao messageDao,
      WebSocketClientFactory webSocketFactory) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideMessageRepository(messageDao, webSocketFactory));
  }
}
