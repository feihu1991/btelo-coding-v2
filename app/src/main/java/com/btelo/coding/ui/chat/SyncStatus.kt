package com.btelo.coding.ui.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.btelo.coding.data.sync.SyncState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 同步状态颜色
 */
object SyncColors {
    val Idle = Color(0xFF9E9E9E)       // 灰色
    val Syncing = Color(0xFF2196F3)    // 蓝色
    val Success = Color(0xFF4CAF50)    // 绿色
    val Error = Color(0xFFF44336)      // 红色
    val Offline = Color(0xFFFF9800)    // 橙色
}

/**
 * 同步状态指示器
 */
@Composable
fun SyncStatusIndicator(
    syncState: SyncState,
    lastSyncTime: Long,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (syncState) {
            SyncState.IDLE -> SyncColors.Idle.copy(alpha = 0.15f)
            SyncState.SYNCING -> SyncColors.Syncing.copy(alpha = 0.15f)
            SyncState.SUCCESS -> SyncColors.Success.copy(alpha = 0.15f)
            SyncState.ERROR -> SyncColors.Error.copy(alpha = 0.15f)
            SyncState.OFFLINE -> SyncColors.Offline.copy(alpha = 0.15f)
        },
        label = "syncColor"
    )

    val iconColor by animateColorAsState(
        targetValue = when (syncState) {
            SyncState.IDLE -> SyncColors.Idle
            SyncState.SYNCING -> SyncColors.Syncing
            SyncState.SUCCESS -> SyncColors.Success
            SyncState.ERROR -> SyncColors.Error
            SyncState.OFFLINE -> SyncColors.Offline
        },
        label = "syncIconColor"
    )

    val statusText = when (syncState) {
        SyncState.IDLE -> "未同步"
        SyncState.SYNCING -> "同步中"
        SyncState.SUCCESS -> "已同步"
        SyncState.ERROR -> "同步失败"
        SyncState.OFFLINE -> "离线"
    }

    Row(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = MaterialTheme.shapes.small
            )
            .clickable(onClick = onSyncClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (syncState == SyncState.SYNCING) {
            // 同步中显示旋转图标
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = iconColor
            )
        } else {
            Icon(
                imageVector = when (syncState) {
                    SyncState.IDLE -> Icons.Default.SyncDisabled
                    SyncState.SYNCING -> Icons.Default.Sync
                    SyncState.SUCCESS -> Icons.Default.Sync
                    SyncState.ERROR -> Icons.Default.SyncProblem
                    SyncState.OFFLINE -> Icons.Default.SyncDisabled
                },
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = iconColor
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            color = iconColor
        )
    }
}

/**
 * 同步状态详情对话框
 */
@Composable
fun SyncStatusDialog(
    syncState: SyncState,
    lastSyncTime: Long,
    isAutoSyncEnabled: Boolean,
    onSyncNow: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    
    val statusDescription = when (syncState) {
        SyncState.IDLE -> "尚未执行同步"
        SyncState.SYNCING -> "正在与服务器同步消息..."
        SyncState.SUCCESS -> "同步成功完成"
        SyncState.ERROR -> "同步遇到错误"
        SyncState.OFFLINE -> "设备处于离线状态"
    }

    val statusColor = when (syncState) {
        SyncState.IDLE -> SyncColors.Idle
        SyncState.SYNCING -> SyncColors.Syncing
        SyncState.SUCCESS -> SyncColors.Success
        SyncState.ERROR -> SyncColors.Error
        SyncState.OFFLINE -> SyncColors.Offline
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("消息同步状态")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 状态
                DetailRow(label = "状态", value = statusDescription)
                
                // 最后同步时间
                if (lastSyncTime > 0) {
                    DetailRow(
                        label = "最后同步",
                        value = dateFormat.format(Date(lastSyncTime))
                    )
                } else {
                    DetailRow(label = "最后同步", value = "从未同步")
                }

                // 自动同步开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "自动同步",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = isAutoSyncEnabled,
                        onCheckedChange = onToggleAutoSync
                    )
                }

                // 同步说明
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "同步说明",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SyncLegendItem(color = SyncColors.Success, text = "已同步 - 消息已同步到服务器")
                        SyncLegendItem(color = SyncColors.Syncing, text = "同步中 - 正在同步消息")
                        SyncLegendItem(color = SyncColors.Offline, text = "离线 - 无法同步消息")
                        SyncLegendItem(color = SyncColors.Error, text = "失败 - 请检查网络后重试")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSyncNow()
                    onDismiss()
                },
                enabled = syncState != SyncState.SYNCING && syncState != SyncState.OFFLINE
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("立即同步")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SyncLegendItem(color: Color, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
