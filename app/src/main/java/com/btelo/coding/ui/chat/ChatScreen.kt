package com.btelo.coding.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.btelo.coding.ui.theme.AccentBlue
import com.btelo.coding.ui.theme.AppBackground
import com.btelo.coding.ui.theme.BorderDefault
import com.btelo.coding.ui.theme.CardSurface
import com.btelo.coding.ui.theme.GreenSuccess
import com.btelo.coding.ui.theme.RedError
import com.btelo.coding.ui.theme.TextOnBubble
import com.btelo.coding.ui.theme.TextPrimary
import com.btelo.coding.ui.theme.TextSecondary
import com.btelo.coding.ui.theme.TextTertiary
import com.btelo.coding.ui.workbench.WorkbenchBottomBar
import com.btelo.coding.ui.workbench.WorkbenchTab

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
        uiState.errorMessage?.let { snackbarHostState.showSnackbar("Connection error: $it") }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(uiState.controlMessage) {
        uiState.controlMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissControlMessage()
        }
    }

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("Disconnect", color = TextPrimary) },
            text = { Text("Disconnect this phone from the desktop bridge?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDisconnectDialog = false
                    viewModel.disconnect()
                    onDisconnect()
                }) {
                    Text("Disconnect", color = RedError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardSurface
        )
    }

    uiState.updateInfo?.let { update ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdatePrompt() },
            title = { Text("Update available", color = TextPrimary) },
            text = {
                Column {
                    Text("Desktop packaged ${update.versionName}. Download it to update this phone.", color = TextSecondary)
                    if (update.sizeBytes > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Size: ${update.sizeBytes / 1024 / 1024} MB", fontSize = 12.sp, color = TextTertiary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    update.downloadUrl.let { uriHandler.openUri(it) }
                    viewModel.dismissUpdatePrompt()
                }) {
                    Text("Download", color = GreenSuccess)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdatePrompt() }) {
                    Text("Later", color = TextSecondary)
                }
            },
            containerColor = CardSurface
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppBackground)
                .statusBarsPadding()
                .imePadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ChatHeader(
                    projectName = uiState.sessionName,
                    isConnected = uiState.isConnected,
                    onDisconnect = { showDisconnectDialog = true },
                    onMenu = { showBridgeMenu = true }
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    DropdownMenu(
                        expanded = showBridgeMenu,
                        onDismissRequest = { showBridgeMenu = false },
                        modifier = Modifier.background(CardSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Build APK") },
                            leadingIcon = { Icon(Icons.Default.Build, contentDescription = null) },
                            onClick = {
                                showBridgeMenu = false
                                viewModel.sendBridgeControl("build_apk")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Restart bridge") },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                            onClick = {
                                showBridgeMenu = false
                                viewModel.sendBridgeControl("restart_bridge")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Restart relay") },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                            onClick = {
                                showBridgeMenu = false
                                viewModel.sendBridgeControl("restart_relay")
                            }
                        )
                    }
                }

                MessageList(
                    messages = uiState.messages,
                    streamingContent = uiState.streamingContent,
                    isStreaming = uiState.isStreaming,
                    thinkingSession = uiState.thinkingSession,
                    modifier = Modifier.weight(1f)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                InputBar(
                    text = uiState.inputText,
                    onTextChange = viewModel::updateInputText,
                    onSend = viewModel::sendMessage,
                    onAttachClick = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    selectedImageUri = selectedImageUri,
                    onClearImage = {
                        selectedImageUri = null
                        viewModel.onImageSelected("")
                    },
                    projectName = uiState.sessionName,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                WorkbenchBottomBar(
                    selectedTab = WorkbenchTab.Agents,
                    onTabSelected = {},
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatHeader(
    projectName: String,
    isConnected: Boolean,
    onDisconnect: () -> Unit,
    onMenu: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onDisconnect,
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(CardSurface)
                .border(1.dp, BorderDefault, CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Disconnect", tint = TextPrimary)
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(CardSurface.copy(alpha = 0.9f))
                .border(1.dp, BorderDefault, RoundedCornerShape(28.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = if (isConnected) GreenSuccess else RedError, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = projectName.ifBlank { "Claude Code" },
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isConnected) "connected" else "not connected",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(CardSurface)
                .border(1.dp, BorderDefault, RoundedCornerShape(28.dp))
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = TextPrimary)
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(AccentBlue.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = TextOnBubble, modifier = Modifier.size(30.dp))
            }
            IconButton(onClick = onMenu, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "Bridge actions", tint = TextPrimary)
            }
        }
    }
}
