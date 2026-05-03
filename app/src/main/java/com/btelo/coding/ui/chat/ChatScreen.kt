package com.btelo.coding.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.btelo.coding.ui.theme.AppBackground
import com.btelo.coding.ui.theme.BubbleGradientStart
import com.btelo.coding.ui.theme.CardSurface
import com.btelo.coding.ui.theme.GreenSuccess
import com.btelo.coding.ui.theme.RedError
import com.btelo.coding.ui.theme.TextPrimary
import com.btelo.coding.ui.theme.TextSecondary
import com.btelo.coding.ui.update.UpdateDialog
import com.btelo.coding.ui.update.UpdateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: String,
    onDisconnect: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val updateState by updateViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showImagePanel by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedImageUri = uri
        showImagePanel = false
        viewModel.onImageSelected(uri?.toString().orEmpty())
    }

    LaunchedEffect(sessionId) {
        viewModel.setSessionId(sessionId)
    }

    LaunchedEffect(Unit) {
        updateViewModel.checkOnLaunch()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar("Connection error: $error")
        }
    }

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("Disconnect", color = TextPrimary) },
            text = { Text("End the mobile bridge session?", color = TextSecondary) },
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
            containerColor = AppBackground
        )
    }

    UpdateDialog(
        state = updateState,
        onInstall = updateViewModel::downloadAndInstall,
        onOpenInstallSettings = updateViewModel::openInstallSettings,
        onRetryInstall = updateViewModel::installDownloaded,
        onDismiss = updateViewModel::dismissUpdate
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.sessionName.ifBlank { "Claude Code" },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (uiState.isConnected) "connected" else "offline",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
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
                    IconButton(onClick = { updateViewModel.checkForUpdate(showNoUpdateMessage = true) }) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Check updates",
                            tint = if (updateState.updateInfo != null) BubbleGradientStart else TextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isConnected) GreenSuccess else RedError)
                    )
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

            AnimatedVisibility(visible = showImagePanel) {
                CompactImagePanel(
                    onOpenPicker = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onDismiss = { showImagePanel = false }
                )
            }

            InputBar(
                text = uiState.inputText,
                onTextChange = viewModel::updateInputText,
                onSend = viewModel::sendMessage,
                onAttachClick = { showImagePanel = !showImagePanel },
                selectedImageUri = selectedImageUri,
                onClearImage = {
                    selectedImageUri = null
                    viewModel.onImageSelected("")
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CompactImagePanel(
    onOpenPicker: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(286.dp)
            .background(AppBackground)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Images", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = "Close",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardSurface)
                .clickable(onClick = onOpenPicker)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(BubbleGradientStart.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = BubbleGradientStart,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Choose from gallery", color = TextPrimary, fontSize = 15.sp)
                Text("Compact panel, then Android picker", color = TextSecondary, fontSize = 12.sp)
            }
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
