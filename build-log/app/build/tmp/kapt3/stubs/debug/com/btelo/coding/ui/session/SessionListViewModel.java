package com.btelo.coding.ui.session;

import androidx.lifecycle.ViewModel;
import com.btelo.coding.domain.model.Session;
import com.btelo.coding.domain.repository.AuthRepository;
import com.btelo.coding.domain.repository.SessionRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0005\b\u0007\u0018\u00002\u00020\u0001B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0010\u0010\u000e\u001a\u00020\u000f2\b\b\u0002\u0010\u0010\u001a\u00020\u0011J\u000e\u0010\u0012\u001a\u00020\u000f2\u0006\u0010\u0013\u001a\u00020\u0011J\b\u0010\u0014\u001a\u00020\u000fH\u0002J\u0006\u0010\u0015\u001a\u00020\u000fR\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00020\t0\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\r\u00a8\u0006\u0016"}, d2 = {"Lcom/btelo/coding/ui/session/SessionListViewModel;", "Landroidx/lifecycle/ViewModel;", "sessionRepository", "Lcom/btelo/coding/domain/repository/SessionRepository;", "authRepository", "Lcom/btelo/coding/domain/repository/AuthRepository;", "(Lcom/btelo/coding/domain/repository/SessionRepository;Lcom/btelo/coding/domain/repository/AuthRepository;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/btelo/coding/ui/session/SessionListUiState;", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "createSession", "", "tool", "", "deleteSession", "sessionId", "loadSessions", "logout", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class SessionListViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.domain.repository.SessionRepository sessionRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.domain.repository.AuthRepository authRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.btelo.coding.ui.session.SessionListUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.btelo.coding.ui.session.SessionListUiState> uiState = null;
    
    @javax.inject.Inject()
    public SessionListViewModel(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.domain.repository.SessionRepository sessionRepository, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.domain.repository.AuthRepository authRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.btelo.coding.ui.session.SessionListUiState> getUiState() {
        return null;
    }
    
    private final void loadSessions() {
    }
    
    public final void createSession(@org.jetbrains.annotations.NotNull()
    java.lang.String tool) {
    }
    
    public final void deleteSession(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId) {
    }
    
    public final void logout() {
    }
}