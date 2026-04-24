package com.btelo.coding.ui.notification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    // Permission state for Android 13+
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            viewModel.setNotificationEnabled(true)
        }
    }
    
    // Time picker dialogs
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("通知设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Permission warning for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "通知权限未开启",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "请在设置中开启通知权限以接收新消息通知",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        ) {
                            Text("开启通知权限")
                        }
                    }
                }
            }
            
            // Main notification toggle
            SettingsSwitchItem(
                title = "启用通知",
                subtitle = "接收新消息推送通知",
                checked = state.notificationEnabled && hasNotificationPermission,
                onCheckedChange = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.setNotificationEnabled(enabled)
                    }
                }
            )
            
            HorizontalDivider()
            
            // Sound setting
            SettingsSwitchItem(
                title = "声音",
                subtitle = "收到通知时播放提示音",
                checked = state.soundEnabled,
                onCheckedChange = { viewModel.setSoundEnabled(it) },
                enabled = state.notificationEnabled
            )
            
            // Vibration setting
            SettingsSwitchItem(
                title = "震动",
                subtitle = "收到通知时震动提示",
                checked = state.vibrationEnabled,
                onCheckedChange = { viewModel.setVibrationEnabled(it) },
                enabled = state.notificationEnabled
            )
            
            // LED setting
            SettingsSwitchItem(
                title = "呼吸灯",
                subtitle = "收到通知时LED灯闪烁",
                checked = state.ledEnabled,
                onCheckedChange = { viewModel.setLedEnabled(it) },
                enabled = state.notificationEnabled
            )
            
            HorizontalDivider()
            
            // Do Not Disturb section
            SettingsSwitchItem(
                title = "免打扰时段",
                subtitle = "设定时间段内不发送通知",
                checked = state.dndEnabled,
                onCheckedChange = { viewModel.setDndEnabled(it) },
                enabled = state.notificationEnabled
            )
            
            if (state.dndEnabled) {
                // Start time
                SettingsClickableItem(
                    title = "开始时间",
                    value = formatTime(state.dndStartHour, state.dndStartMinute),
                    onClick = { showStartTimePicker = true },
                    enabled = state.notificationEnabled
                )
                
                // End time
                SettingsClickableItem(
                    title = "结束时间",
                    value = formatTime(state.dndEndHour, state.dndEndMinute),
                    onClick = { showEndTimePicker = true },
                    enabled = state.notificationEnabled
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Time picker dialogs
    if (showStartTimePicker) {
        TimePickerDialog(
            initialHour = state.dndStartHour,
            initialMinute = state.dndStartMinute,
            onConfirm = { hour, minute ->
                viewModel.setDndStartTime(hour, minute)
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false }
        )
    }
    
    if (showEndTimePicker) {
        TimePickerDialog(
            initialHour = state.dndEndHour,
            initialMinute = state.dndEndMinute,
            onConfirm = { hour, minute ->
                viewModel.setDndEndTime(hour, minute)
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun SettingsClickableItem(
    title: String,
    value: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}

fun formatTime(hour: Int, minute: Int): String {
    return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
}
