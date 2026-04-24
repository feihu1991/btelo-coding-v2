package com.btelo.coding.ui.login;

import android.os.Build;
import androidx.lifecycle.ViewModel;
import com.btelo.coding.domain.repository.AuthRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0006\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/btelo/coding/ui/login/ConnectionStatus;", "", "(Ljava/lang/String;I)V", "DISCONNECTED", "REGISTERING", "WAITING_FOR_PAIRING", "PAIRED", "app_debug"})
public enum ConnectionStatus {
    /*public static final*/ DISCONNECTED /* = new DISCONNECTED() */,
    /*public static final*/ REGISTERING /* = new REGISTERING() */,
    /*public static final*/ WAITING_FOR_PAIRING /* = new WAITING_FOR_PAIRING() */,
    /*public static final*/ PAIRED /* = new PAIRED() */;
    
    ConnectionStatus() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public static kotlin.enums.EnumEntries<com.btelo.coding.ui.login.ConnectionStatus> getEntries() {
        return null;
    }
}