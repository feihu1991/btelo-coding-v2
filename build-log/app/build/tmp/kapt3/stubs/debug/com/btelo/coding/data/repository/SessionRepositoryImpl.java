package com.btelo.coding.data.repository;

import com.btelo.coding.data.local.dao.SessionDao;
import com.btelo.coding.domain.model.Session;
import com.btelo.coding.domain.repository.SessionRepository;
import com.btelo.coding.util.Logger;
import kotlinx.coroutines.flow.Flow;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\u0006H\u0096@\u00a2\u0006\u0002\u0010\nJ\u0016\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u0006H\u0096@\u00a2\u0006\u0002\u0010\nJ\u0018\u0010\u000e\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\b0\u000f2\u0006\u0010\r\u001a\u00020\u0006H\u0016J\u0014\u0010\u0010\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00110\u000fH\u0016J\u001e\u0010\u0012\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u00062\u0006\u0010\u0013\u001a\u00020\u0014H\u0096@\u00a2\u0006\u0002\u0010\u0015J\u0016\u0010\u0016\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u0006H\u0096@\u00a2\u0006\u0002\u0010\nR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082D\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0017"}, d2 = {"Lcom/btelo/coding/data/repository/SessionRepositoryImpl;", "Lcom/btelo/coding/domain/repository/SessionRepository;", "sessionDao", "Lcom/btelo/coding/data/local/dao/SessionDao;", "(Lcom/btelo/coding/data/local/dao/SessionDao;)V", "tag", "", "createSession", "Lcom/btelo/coding/domain/model/Session;", "tool", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteSession", "", "sessionId", "getSession", "Lkotlinx/coroutines/flow/Flow;", "getSessions", "", "updateSessionConnection", "isConnected", "", "(Ljava/lang/String;ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateSessionLastActive", "app_debug"})
public final class SessionRepositoryImpl implements com.btelo.coding.domain.repository.SessionRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.data.local.dao.SessionDao sessionDao = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String tag = "SessionRepository";
    
    @javax.inject.Inject()
    public SessionRepositoryImpl(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.dao.SessionDao sessionDao) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<java.util.List<com.btelo.coding.domain.model.Session>> getSessions() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object createSession(@org.jetbrains.annotations.NotNull()
    java.lang.String tool, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.btelo.coding.domain.model.Session> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object deleteSession(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<com.btelo.coding.domain.model.Session> getSession(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object updateSessionConnection(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId, boolean isConnected, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object updateSessionLastActive(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}