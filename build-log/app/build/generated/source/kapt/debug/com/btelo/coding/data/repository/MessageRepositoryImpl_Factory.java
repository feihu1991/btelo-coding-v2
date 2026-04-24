package com.btelo.coding.data.repository;

import com.btelo.coding.data.local.dao.MessageDao;
import com.btelo.coding.data.remote.websocket.factory.WebSocketClientFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class MessageRepositoryImpl_Factory implements Factory<MessageRepositoryImpl> {
  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<WebSocketClientFactory> webSocketFactoryProvider;

  public MessageRepositoryImpl_Factory(Provider<MessageDao> messageDaoProvider,
      Provider<WebSocketClientFactory> webSocketFactoryProvider) {
    this.messageDaoProvider = messageDaoProvider;
    this.webSocketFactoryProvider = webSocketFactoryProvider;
  }

  @Override
  public MessageRepositoryImpl get() {
    return newInstance(messageDaoProvider.get(), webSocketFactoryProvider.get());
  }

  public static MessageRepositoryImpl_Factory create(Provider<MessageDao> messageDaoProvider,
      Provider<WebSocketClientFactory> webSocketFactoryProvider) {
    return new MessageRepositoryImpl_Factory(messageDaoProvider, webSocketFactoryProvider);
  }

  public static MessageRepositoryImpl newInstance(MessageDao messageDao,
      WebSocketClientFactory webSocketFactory) {
    return new MessageRepositoryImpl(messageDao, webSocketFactory);
  }
}
