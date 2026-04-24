package com.btelo.coding.ui.sync

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.btelo.coding.ui.chat.SyncStatusIndicator
import java.text.SimpleDateFormat
import java.util.*

/**
 * 同步设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SyncSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("同步设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 同步状态卡片
            SyncStatusCard(
                uiState = uiState,
                onSyncClick = { viewModel.triggerSync() }
            )
            
            // 同步设置
            SyncSettingsCard(
                syncEnabled = uiState.syncEnabled,
                autoSyncEnabled = uiState.autoSyncEnabled,
                onSyncEnabledChange = { viewModel.setSyncEnabled(it) },
                onAutoSyncEnabledChange = { viewModel.setAutoSyncEnabled(it) }
            )
            
            // 同步信息
            SyncInfoCard(
                lastSyncTime = uiState.lastSyncTime,
                unsyncedCount = uiState.unsyncedCount
            )
        }
    }
}

@Composable
private fun SyncStatusCard(
    uiState: SyncSettingsUiState,
    onSyncClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SyncStatusIndicator(
                        syncState = uiState.syncState,
                        lastSyncTime = uiState.lastSyncTime,
                        onSyncClick = onSyncClick
                    )
                }
            }
            
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SyncSettingsCard(
    syncEnabled: Boolean,
    autoSyncEnabled: Boolean,
    onSyncEnabledChange: (Boolean) -> Unit,
    onAutoSyncEnabledChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "同步选项",
                style = MaterialTheme.typography.titleMedium
            )
            
            HorizontalDivider()
            
            // 同步总开关
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "启用消息同步",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "允许在不同设备间同步聊天记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Switch(
                    checked = syncEnabled,
                    onCheckedChange = onSyncEnabledChange
                )
            }
            
            // 自动同步开关
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "自动同步",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "在网络可用时自动同步消息",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Switch(
                    checked = autoSyncEnabled,
                    onCheckedChange = onAutoSyncEnabledChange,
                    enabled = syncEnabled
                )
            }
        }
    }
}

@Composable
private fun SyncInfoCard(
    lastSyncTime: Long,
    unsyncedCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "同步信息",
                style = MaterialTheme.typography.titleMedium
            )
            
            HorizontalDivider()
            
            // 上次同步时间
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "上次同步时间",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = if (lastSyncTime > 0) {
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(Date(lastSyncTime))
                    } else {
                        "从未同步"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // 未同步消息数
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "待同步消息",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "$unsyncedCount 条",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // 同步说明
            Text(
                text = "消息同步采用增量同步方式，仅同步自上次同步后的新消息，确保数据传输的高效性。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
