package com.btelo.coding.ui.scan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.btelo.coding.data.remote.AppUpdateInfo
import com.btelo.coding.data.remote.DownloadState
import com.btelo.coding.ui.theme.AccentBlue
import com.btelo.coding.ui.theme.AppBackground
import com.btelo.coding.ui.theme.BubbleGradientEnd
import com.btelo.coding.ui.theme.BubbleGradientStart
import com.btelo.coding.ui.theme.CardSurface
import com.btelo.coding.ui.theme.GreenSuccess
import com.btelo.coding.ui.theme.RedError
import com.btelo.coding.ui.theme.TextOnBubble
import com.btelo.coding.ui.theme.TextPrimary
import com.btelo.coding.ui.theme.TextSecondary
import com.btelo.coding.ui.theme.TextTertiary

@Composable
fun ScanScreen(
    onConnected: (String) -> Unit,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAuthDialog by remember { mutableStateOf(false) }
    var authBridgeId by remember { mutableStateOf("") }
    var authBridgeName by remember { mutableStateOf("") }

    val installLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onInstallResult()
    }

    LaunchedEffect(Unit) {
        viewModel.installIntent.collect { intent ->
            installLauncher.launch(intent)
        }
    }

    LaunchedEffect(uiState.isConnected, uiState.sessionId) {
        if (uiState.isConnected && uiState.sessionId != null) {
            onConnected(uiState.sessionId!!)
        }
    }

    uiState.updateInfo?.let { update ->
        UpdateDialog(
            updateInfo = update,
            downloadState = uiState.downloadState,
            onDismiss = { viewModel.dismissUpdatePrompt() },
            onDownload = { viewModel.startDownload() },
            onCancel = { viewModel.cancelDownload() },
            onInstall = { viewModel.installApk() }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                BubbleGradientStart.copy(alpha = 0.2f),
                                BubbleGradientEnd.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(BubbleGradientStart, BubbleGradientEnd)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "B",
                        color = TextOnBubble,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "BTELO Coding",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 26.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "连接到你的开发机器",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Server address input
            OutlinedTextField(
                value = uiState.serverAddress,
                onValueChange = { viewModel.setServerAddress(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("http://192.168.x.x:8080", color = TextTertiary) },
                label = { Text("服务器地址") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Dns, contentDescription = null, tint = TextSecondary)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = BubbleGradientStart,
                    unfocusedBorderColor = TextTertiary.copy(alpha = 0.3f),
                    focusedLabelColor = BubbleGradientStart,
                    unfocusedLabelColor = TextSecondary
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Search
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Discover button
            if (uiState.isDiscovering) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BubbleGradientStart,
                            disabledContainerColor = BubbleGradientStart.copy(alpha = 0.6f)
                        ),
                        enabled = false
                    ) {
                        CircularProgressIndicator(
                            color = TextOnBubble,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("搜索中...", color = TextOnBubble, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = { viewModel.stopDiscovery() },
                        modifier = Modifier.height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedError)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "停止", tint = TextOnBubble, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("停止", color = TextOnBubble, fontSize = 15.sp)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.discoverBridges() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BubbleGradientStart)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextOnBubble)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("发现设备", color = TextOnBubble, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = { viewModel.checkForUpdate() },
                        modifier = Modifier.height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = "检查更新", tint = TextOnBubble, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("更新", color = TextOnBubble, fontSize = 15.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Error message
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = RedError.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = RedError,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Default.Close, contentDescription = "关闭", tint = RedError, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Bridge list
            if (uiState.bridges.isNotEmpty()) {
                Text(
                    text = "可用设备 (${uiState.bridges.size})",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    uiState.bridges.forEach { bridge ->
                        BridgeCard(
                            bridge = bridge,
                            isConnecting = uiState.isConnecting && uiState.selectedBridgeId == bridge.id,
                            onClick = {
                                authBridgeId = bridge.id
                                authBridgeName = bridge.deviceName
                                showAuthDialog = true
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else if (!uiState.isDiscovering && uiState.error == null && uiState.serverAddress.isNotBlank()) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "输入服务器地址后点击\"发现设备\"",
                    color = TextTertiary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Auth code dialog
        AnimatedVisibility(
            visible = showAuthDialog,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            AuthCodeDialog(
                bridgeName = authBridgeName,
                isConnecting = uiState.isConnecting,
                onConfirm = { code ->
                    viewModel.connectToBridge(authBridgeId, code)
                },
                onDismiss = {
                    showAuthDialog = false
                    viewModel.clearError()
                }
            )
        }
    }
}

@Composable
private fun BridgeCard(
    bridge: BridgeInfo,
    isConnecting: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !isConnecting) { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BubbleGradientStart.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Computer,
                    contentDescription = null,
                    tint = BubbleGradientStart,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = bridge.deviceName,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    if (bridge.bridgeConnected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GreenSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = bridge.workDir,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Text(
                    text = bridge.mode.uppercase(),
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }

            if (isConnecting) {
                CircularProgressIndicator(
                    color = BubbleGradientStart,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "认证",
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AuthCodeDialog(
    bridgeName: String,
    isConnecting: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() }
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "输入认证码",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "连接至: $bridgeName",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("000000", color = TextTertiary, fontSize = 24.sp, textAlign = TextAlign.Center) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                        textAlign = TextAlign.Center,
                        letterSpacing = 8.sp,
                        fontSize = 28.sp,
                        color = TextPrimary
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BubbleGradientStart,
                        unfocusedBorderColor = TextTertiary.copy(alpha = 0.3f),
                        focusedTextColor = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { if (code.length == 6) onConfirm(code) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BubbleGradientStart),
                    enabled = code.length == 6 && !isConnecting
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            color = TextOnBubble,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("验证中...", color = TextOnBubble, fontSize = 15.sp)
                    } else {
                        Text("连接", color = TextOnBubble, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateDialog(
    updateInfo: AppUpdateInfo,
    downloadState: DownloadState?,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (downloadState !is DownloadState.Progress) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = if (downloadState is DownloadState.Completed) "下载完成" else "发现新版本",
                color = TextPrimary
            )
        },
        text = {
            Column {
                when (downloadState) {
                    is DownloadState.Progress -> {
                        Text(
                            text = "正在下载 ${updateInfo.versionName}...",
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { downloadState.percent / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = BubbleGradientStart,
                            trackColor = TextTertiary.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${downloadState.percent}%",
                            color = TextTertiary,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                    is DownloadState.Completed -> {
                        Text(
                            text = "新版本 ${updateInfo.versionName} 已下载完成，点击安装按钮开始更新。",
                            color = TextSecondary
                        )
                    }
                    is DownloadState.Failed -> {
                        Text(
                            text = "下载失败：${downloadState.error}",
                            color = RedError
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "版本：${updateInfo.versionName}",
                            color = TextSecondary
                        )
                    }
                    null -> {
                        Text(
                            text = "新版本 ${updateInfo.versionName} 已发布，是否下载更新？",
                            color = TextSecondary
                        )
                        if (updateInfo.sizeBytes > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "大小：${formatFileSize(updateInfo.sizeBytes)}",
                                color = TextTertiary,
                                fontSize = 12.sp
                            )
                        }
                        if (!updateInfo.releaseNotes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "更新说明：",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = updateInfo.releaseNotes,
                                color = TextTertiary,
                                fontSize = 11.sp,
                                maxLines = 5
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (downloadState) {
                is DownloadState.Progress -> {
                    TextButton(onClick = onCancel) {
                        Text("取消", color = RedError)
                    }
                }
                is DownloadState.Completed -> {
                    TextButton(onClick = onInstall) {
                        Text("安装", color = BubbleGradientStart)
                    }
                }
                is DownloadState.Failed -> {
                    TextButton(onClick = onDownload) {
                        Text("重试", color = BubbleGradientStart)
                    }
                }
                null -> {
                    TextButton(onClick = onDownload) {
                        Text("下载更新", color = BubbleGradientStart)
                    }
                }
            }
        },
        dismissButton = {
            when (downloadState) {
                is DownloadState.Progress -> {}
                is DownloadState.Completed -> {
                    TextButton(onClick = onDismiss) {
                        Text("稍后", color = TextSecondary)
                    }
                }
                is DownloadState.Failed -> {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = TextSecondary)
                    }
                }
                null -> {
                    TextButton(onClick = onDismiss) {
                        Text("稍后", color = TextSecondary)
                    }
                }
            }
        },
        containerColor = CardSurface
    )
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / 1024 / 1024} MB"
        else -> "${bytes / 1024 / 1024 / 1024} GB"
    }
}
