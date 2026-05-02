package com.btelo.coding.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.btelo.coding.ui.theme.AppBackground
import com.btelo.coding.ui.theme.GreenSuccess
import com.btelo.coding.ui.theme.RedError
import com.btelo.coding.ui.theme.TextPrimary
import com.btelo.coding.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: String,
    onDisconnect: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showBridgeMenu by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedImageUri = uri
        if (uri != null) viewModel.onImageSelected(uri.toString())
    }

    LaunchedEffect(sessionId) {
        viewModel.setSessionId(sessionId)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar("Connection error: $error")
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    LaunchedEffect(uiState.controlMessage) {
        uiState.controlMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissControlMessage()
        }
    }

    // Disconnect confirmation dialog
    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("断开连接", color = TextPrimary) },
            text = { Text("确定要断开与服务器的连接吗？", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDisconnectDialog = false
                    viewModel.disconnect()
                    onDisconnect()
                }) {
                    Text("断开", color = RedError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("取消", color = TextSecondary)
                }
            },
            containerColor = AppBackground
        )
    }

    uiState.updateInfo?.let { update ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdatePrompt() },
            title = { Text("发现新版本", color = TextPrimary) },
            text = {
                Column {
                    Text(
                        text = "电脑已打包 ${update.versionName}，可以下载到手机更新。",
                        color = TextSecondary
                    )
                    if (update.sizeBytes > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "大小：${update.sizeBytes / 1024 / 1024} MB",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    update.downloadUrl?.let { uriHandler.openUri(it) }
                    viewModel.dismissUpdatePrompt()
                }) {
                    Text("下载更新", color = GreenSuccess)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdatePrompt() }) {
                    Text("稍后", color = TextSecondary)
                }
            },
            containerColor = AppBackground
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Claude Code",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { showDisconnectDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Disconnect",
                            tint = TextSecondary
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showBridgeMenu = true },
                            enabled = uiState.isConnected
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Bridge actions",
                                tint = TextSecondary
                            )
                        }
                        DropdownMenu(
                            expanded = showBridgeMenu,
                            onDismissRequest = { showBridgeMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("打包 APK") },
                                leadingIcon = { Icon(Icons.Default.Build, contentDescription = null) },
                                onClick = {
                                    showBridgeMenu = false
                                    viewModel.sendBridgeControl("build_apk")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("重启桥接") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                onClick = {
                                    showBridgeMenu = false
                                    viewModel.sendBridgeControl("restart_bridge")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("重启中继") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                onClick = {
                                    showBridgeMenu = false
                                    viewModel.sendBridgeControl("restart_relay")
                                }
                            )
                        }
                    }

                    // Connection status indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isConnected) GreenSuccess else RedError)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isConnected) "已连接" else "未连接",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .background(AppBackground)
        ) {
            MessageList(
                messages = uiState.messages,
                streamingContent = uiState.streamingContent,
                isStreaming = uiState.isStreaming,
                thinkingSession = uiState.thinkingSession,
                modifier = Modifier.weight(1f)
            )

            InputBar(
                text = uiState.inputText,
                onTextChange = viewModel::updateInputText,
                onSend = viewModel::sendMessage,
                onAttachClick = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                selectedImageUri = selectedImageUri,
                onClearImage = { selectedImageUri = null; viewModel.onImageSelected("") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
