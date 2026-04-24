package com.btelo.coding.ui.notification;

import androidx.lifecycle.ViewModel;
import com.btelo.coding.data.local.DataStoreManager;
import com.btelo.coding.notification.NotificationChannelManager;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\b\b\u0007\u0018\u00002\u00020\u0001B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\b\u0010\u000e\u001a\u00020\u000fH\u0002J\u000e\u0010\u0010\u001a\u00020\u000f2\u0006\u0010\u0011\u001a\u00020\u0012J\u0016\u0010\u0013\u001a\u00020\u000f2\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0015J\u0016\u0010\u0017\u001a\u00020\u000f2\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0015J\u000e\u0010\u0018\u001a\u00020\u000f2\u0006\u0010\u0011\u001a\u00020\u0012J\u000e\u0010\u0019\u001a\u00020\u000f2\u0006\u0010\u0011\u001a\u00020\u0012J\u000e\u0010\u001a\u001a\u00020\u000f2\u0006\u0010\u0011\u001a\u00020\u0012J\u000e\u0010\u001b\u001a\u00020\u000f2\u0006\u0010\u0011\u001a\u00020\u0012J\b\u0010\u001c\u001a\u00020\u000fH\u0002R\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00020\t0\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\r\u00a8\u0006\u001d"}, d2 = {"Lcom/btelo/coding/ui/notification/NotificationSettingsViewModel;", "Landroidx/lifecycle/ViewModel;", "dataStoreManager", "Lcom/btelo/coding/data/local/DataStoreManager;", "notificationChannelManager", "Lcom/btelo/coding/notification/NotificationChannelManager;", "(Lcom/btelo/coding/data/local/DataStoreManager;Lcom/btelo/coding/notification/NotificationChannelManager;)V", "_state", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/btelo/coding/ui/notification/NotificationSettingsState;", "state", "Lkotlinx/coroutines/flow/StateFlow;", "getState", "()Lkotlinx/coroutines/flow/StateFlow;", "loadSettings", "", "setDndEnabled", "enabled", "", "setDndEndTime", "hour", "", "minute", "setDndStartTime", "setLedEnabled", "setNotificationEnabled", "setSoundEnabled", "setVibrationEnabled", "updateChatChannel", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class NotificationSettingsViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.data.local.DataStoreManager dataStoreManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.notification.NotificationChannelManager notificationChannelManager = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.btelo.coding.ui.notification.NotificationSettingsState> _state = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.btelo.coding.ui.notification.NotificationSettingsState> state = null;
    
    @javax.inject.Inject()
    public NotificationSettingsViewModel(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.DataStoreManager dataStoreManager, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.notification.NotificationChannelManager notificationChannelManager) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.btelo.coding.ui.notification.NotificationSettingsState> getState() {
        return null;
    }
    
    private final void loadSettings() {
    }
    
    public final void setNotificationEnabled(boolean enabled) {
    }
    
    public final void setSoundEnabled(boolean enabled) {
    }
    
    public final void setVibrationEnabled(boolean enabled) {
    }
    
    public final void setLedEnabled(boolean enabled) {
    }
    
    public final void setDndEnabled(boolean enabled) {
    }
    
    public final void setDndStartTime(int hour, int minute) {
    }
    
    public final void setDndEndTime(int hour, int minute) {
    }
    
    private final void updateChatChannel() {
    }
}