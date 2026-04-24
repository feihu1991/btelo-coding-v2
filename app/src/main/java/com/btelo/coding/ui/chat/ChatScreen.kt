package com.btelo.coding.ui.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.btelo.coding.data.remote.websocket.factory.ConnectionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 连接状态颜色
object ConnectionColors {
    val Connected = Color(0xFF4CAF50)      // 绿色
    val Connecting = Color(0xFFFFC107)     // 黄色
    val Disconnected = Color(0xFF9E9E9E)   // 灰色
    val Error = Color(0xFFF44336)          // 红色
    val Reconnecting = Color(0xFFFF9800)  // 橙色
}

@Composable
fun ConnectionStatusIndicator(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (connectionState) {
            is ConnectionState.Connected -> ConnectionColors.Connected
            is ConnectionState.Connecting -> ConnectionColors.Connecting
            is ConnectionState.Disconnected -> ConnectionColors.Disconnected
            is ConnectionState.Error -> ConnectionColors.Error
            is ConnectionState.Reconnecting -> ConnectionColors.Reconnecting
        },
        label = "connectionColor"
    )

    val statusText = when (connectionState) {
        is ConnectionState.Connected -> "已连接"
        is ConnectionState.Connecting -> "连接中"
        is ConnectionState.Disconnected -> "未连接"
        is ConnectionState.Error -> "连接错误"
        is ConnectionState.Reconnecting -> "重连中 (${connectionState.attempt})"
    }

    Row(
        modifier = modifier
            .background(
                color = backgroundColor.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(backgroundColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelMedium,
            color = backgroundColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 显示错误消息
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar("连接错误: $error")
        }
    }

    // 连接详情弹窗
    if (uiState.showConnectionDetails) {
        ConnectionDetailsDialog(
            connectionState = uiState.connectionState,
            reconnectAttempts = uiState.reconnectAttempts,
            lastConnectedTime = uiState.lastConnectedTime,
            errorMessage = uiState.errorMessage,
            onDismiss = viewModel::dismissConnectionDetails
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 对话") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 连接状态指示器
                    ConnectionStatusIndicator(
                        connectionState = uiState.connectionState,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    // 连接详情按钮
                    IconButton(onClick = viewModel::toggleConnectionDetails) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "连接详情",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MessageList(
                messages = uiState.messages,
                modifier = Modifier.weight(1f)
            )

            InputBar(
                text = uiState.inputText,
                onTextChange = viewModel::updateInputText,
                onSend = viewModel::sendMessage,
                onVoiceClick = { /* TODO: Implement voice input */ },
                onAttachClick = { /* TODO: Implement file attachment */ },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ConnectionDetailsDialog(
    connectionState: ConnectionState,
    reconnectAttempts: Int,
    lastConnectedTime: Long?,
    errorMessage: String?,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    
    val statusDescription = when (connectionState) {
        is ConnectionState.Connected -> "已成功连接到服务器"
        is ConnectionState.Connecting -> "正在建立连接..."
        is ConnectionState.Disconnected -> "未连接到服务器"
        is ConnectionState.Error -> "连接遇到错误"
        is ConnectionState.Reconnecting -> "正在尝试重新连接"
    }

    val statusColor = when (connectionState) {
        is ConnectionState.Connected -> ConnectionColors.Connected
        is ConnectionState.Connecting -> ConnectionColors.Connecting
        is ConnectionState.Disconnected -> ConnectionColors.Disconnected
        is ConnectionState.Error -> ConnectionColors.Error
        is ConnectionState.Reconnecting -> ConnectionColors.Reconnecting
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
                Text("连接详情")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailRow(label = "状态", value = statusDescription)
                
                if (reconnectAttempts > 0) {
                    DetailRow(label = "重连次数", value = "$reconnectAttempts 次")
                }
                
                lastConnectedTime?.let { time ->
                    DetailRow(
                        label = "最后连接时间",
                        value = dateFormat.format(Date(time))
                    )
                }
                
                errorMessage?.let { error ->
                    DetailRow(label = "错误信息", value = error)
                }

                // 状态说明
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
                            text = "状态说明",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        StatusLegendItem(color = ConnectionColors.Connected, text = "🟢 已连接 - 正常通信")
                        StatusLegendItem(color = ConnectionColors.Connecting, text = "🟡 连接中 - 正在建立连接")
                        StatusLegendItem(color = ConnectionColors.Reconnecting, text = "🟠 重连中 - 自动重试连接")
                        StatusLegendItem(color = ConnectionColors.Error, text = "🔴 连接错误 - 需要检查网络")
                        StatusLegendItem(color = ConnectionColors.Disconnected, text = "⚪ 未连接 - 请检查设置")
                    }
                }
            }
        },
        confirmButton = {
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
private fun StatusLegendItem(color: Color, text: String) {
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
