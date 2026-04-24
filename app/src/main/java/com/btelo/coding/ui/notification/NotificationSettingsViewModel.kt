package com.btelo.coding.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btelo.coding.data.local.DataStoreManager
import com.btelo.coding.notification.NotificationChannelManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationSettingsState(
    val notificationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val ledEnabled: Boolean = true,
    val dndEnabled: Boolean = false,
    val dndStartHour: Int = 22,
    val dndStartMinute: Int = 0,
    val dndEndHour: Int = 7,
    val dndEndMinute: Int = 0
)

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val notificationChannelManager: NotificationChannelManager
) : ViewModel() {
    
    private val _state = MutableStateFlow(NotificationSettingsState())
    val state: StateFlow<NotificationSettingsState> = _state.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        _state.value = NotificationSettingsState(
            notificationEnabled = dataStoreManager.isNotificationEnabled(),
            soundEnabled = dataStoreManager.isNotificationSoundEnabled(),
            vibrationEnabled = dataStoreManager.isNotificationVibrationEnabled(),
            ledEnabled = dataStoreManager.isNotificationLedEnabled(),
            dndEnabled = dataStoreManager.isDndEnabled(),
            dndStartHour = dataStoreManager.getDndStartHour(),
            dndStartMinute = dataStoreManager.getDndStartMinute(),
            dndEndHour = dataStoreManager.getDndEndHour(),
            dndEndMinute = dataStoreManager.getDndEndMinute()
        )
    }
    
    fun setNotificationEnabled(enabled: Boolean) {
        dataStoreManager.setNotificationEnabled(enabled)
        _state.value = _state.value.copy(notificationEnabled = enabled)
    }
    
    fun setSoundEnabled(enabled: Boolean) {
        dataStoreManager.setNotificationSoundEnabled(enabled)
        _state.value = _state.value.copy(soundEnabled = enabled)
        updateChatChannel()
    }
    
    fun setVibrationEnabled(enabled: Boolean) {
        dataStoreManager.setNotificationVibrationEnabled(enabled)
        _state.value = _state.value.copy(vibrationEnabled = enabled)
        updateChatChannel()
    }
    
    fun setLedEnabled(enabled: Boolean) {
        dataStoreManager.setNotificationLedEnabled(enabled)
        _state.value = _state.value.copy(ledEnabled = enabled)
        updateChatChannel()
    }
    
    fun setDndEnabled(enabled: Boolean) {
        dataStoreManager.setDndEnabled(enabled)
        _state.value = _state.value.copy(dndEnabled = enabled)
    }
    
    fun setDndStartTime(hour: Int, minute: Int) {
        dataStoreManager.setDndStartTime(hour, minute)
        _state.value = _state.value.copy(dndStartHour = hour, dndStartMinute = minute)
    }
    
    fun setDndEndTime(hour: Int, minute: Int) {
        dataStoreManager.setDndEndTime(hour, minute)
        _state.value = _state.value.copy(dndEndHour = hour, dndEndMinute = minute)
    }
    
    private fun updateChatChannel() {
        viewModelScope.launch {
            notificationChannelManager.updateChatChannelSettings(
                soundEnabled = _state.value.soundEnabled,
                vibrationEnabled = _state.value.vibrationEnabled,
                ledEnabled = _state.value.ledEnabled
            )
        }
    }
}
