package com.btelo.coding.ui.scan

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.btelo.coding.domain.model.SessionAttentionType
import com.btelo.coding.ui.theme.AccentBlue
import com.btelo.coding.ui.theme.AppBackground
import com.btelo.coding.ui.theme.BubbleGradientStart
import com.btelo.coding.ui.theme.CardSurface
import com.btelo.coding.ui.theme.GreenSuccess
import com.btelo.coding.ui.theme.RedError
import com.btelo.coding.ui.theme.TextOnBubble
import com.btelo.coding.ui.theme.TextPrimary
import com.btelo.coding.ui.theme.TextSecondary
import com.btelo.coding.ui.theme.TextTertiary
import com.btelo.coding.ui.theme.WarningAmber
import com.btelo.coding.ui.update.UpdateDialog
import com.btelo.coding.ui.update.UpdateViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScanScreen(
    onConnected: (String) -> Unit,
    viewModel: ScanViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val updateState by updateViewModel.uiState.collectAsState()
    val availableUpdate = updateState.updateInfo

    var showAuthDialog by remember { mutableStateOf(false) }
    var authBridgeId by remember { mutableStateOf("") }
    var authBridgeName by remember { mutableStateOf("") }
    var quickReplySessionId by remember { mutableStateOf<String?>(null) }
    var quickReplyTitle by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isConnected, uiState.sessionId) {
        if (uiState.isConnected && uiState.sessionId != null) {
            onConnected(uiState.sessionId!!)
        }
    }

    LaunchedEffect(uiState.isSendingQuickReply, uiState.quickReplySessionId) {
        if (!uiState.isSendingQuickReply && uiState.quickReplySessionId == null && quickReplySessionId != null) {
            quickReplySessionId = null
        }
    }

    LaunchedEffect(Unit) {
        updateViewModel.checkOnLaunch()
    }

    UpdateDialog(
        state = updateState,
        onInstall = updateViewModel::downloadAndInstall,
        onRetryInstall = updateViewModel::installDownloaded,
        onDismiss = updateViewModel::dismissUpdate
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DashboardHeader(
                    liveCount = uiState.dashboardItems.count { it.isLive },
                    snapshotCount = uiState.dashboardItems.count { it.hasSnapshot }
                )
            }

            item {
                ConnectPanel(
                    serverAddress = uiState.serverAddress,
                    isDiscovering = uiState.isDiscovering,
                    onServerAddressChange = viewModel::setServerAddress,
                    onRefresh = { viewModel.discoverBridges() },
                    onStop = viewModel::stopDiscovery
                )
            }

            item {
                Button(
                    onClick = { updateViewModel.checkForUpdate(showNoUpdateMessage = true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CardSurface)
                ) {
                    Icon(
                        Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = if (availableUpdate != null) BubbleGradientStart else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            updateState.isChecking -> "Checking for updates..."
                            availableUpdate != null -> "Update ${availableUpdate.versionName} available"
                            else -> "Check updates"
                        },
                        color = TextPrimary
                    )
                }
            }

            uiState.error?.let { error ->
                item {
                    ErrorCard(
                        error = error,
                        onDismiss = viewModel::clearError
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Continue Working",
                    subtitle = if (uiState.dashboardItems.isEmpty()) {
                        "Your desktop sessions and saved snapshots will show up here."
                    } else {
                        "Pick up the same work from your phone without losing context."
                    }
                )
            }

            if (uiState.dashboardItems.isEmpty()) {
                item {
                    EmptyDashboardCard()
                }
            } else {
                items(uiState.dashboardItems, key = { it.key }) { item ->
                    SessionDashboardCard(
                        item = item,
                        isConnecting = uiState.isConnecting && uiState.selectedBridgeId == item.bridgeId,
                        isSendingQuickReply = uiState.isSendingQuickReply &&
                            uiState.quickReplySessionId == item.sessionId,
                        isSendingPermissionDecision = uiState.isSendingPermissionDecision &&
                            uiState.permissionDecisionSessionId == item.sessionId,
                        canDirectRespond = item.sessionId != null && item.isCurrentSession,
                        onResumeLive = {
                            authBridgeId = item.bridgeId.orEmpty()
                            authBridgeName = item.title
                            showAuthDialog = true
                        },
                        onOpenSnapshot = {
                            item.sessionId?.let(viewModel::reopenSnapshot)
                        },
                        onAllowPermission = {
                            item.sessionId?.let { viewModel.respondToPermissionRequest(it, "allow") }
                        },
                        onDenyPermission = {
                            item.sessionId?.let { viewModel.respondToPermissionRequest(it, "deny") }
                        },
                        onReply = {
                            quickReplySessionId = item.sessionId
                            quickReplyTitle = item.title
                        },
                        onViewResult = {
                            if (item.isLive) {
                                authBridgeId = item.bridgeId.orEmpty()
                                authBridgeName = item.title
                                showAuthDialog = true
                            } else {
                                item.sessionId?.let(viewModel::reopenSnapshot)
                            }
                        }
                    )
                }
            }
        }

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

        if (quickReplySessionId != null) {
            QuickReplyDialog(
                sessionTitle = quickReplyTitle,
                isSending = uiState.isSendingQuickReply,
                onDismiss = {
                    if (!uiState.isSendingQuickReply) {
                        quickReplySessionId = null
                    }
                },
                onSend = { content ->
                    quickReplySessionId?.let { sessionId ->
                        viewModel.sendQuickReply(sessionId, content)
                    }
                }
            )
        }
    }
}

@Composable
private fun DashboardHeader(
    liveCount: Int,
    snapshotCount: Int
) {
    Column {
        Text(
            text = "Work Relay",
            color = TextPrimary,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "See what's running on your desktop, then jump back into the same session from your phone.",
            color = TextSecondary,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusPill(
                text = "$liveCount live",
                background = GreenSuccess.copy(alpha = 0.14f),
                contentColor = GreenSuccess
            )
            StatusPill(
                text = "$snapshotCount snapshots",
                background = AccentBlue.copy(alpha = 0.14f),
                contentColor = AccentBlue
            )
        }
    }
}

@Composable
private fun ConnectPanel(
    serverAddress: String,
    isDiscovering: Boolean,
    onServerAddressChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Desktop Discovery",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Keep your relay address here so the app can continuously rediscover live sessions.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = serverAddress,
                onValueChange = onServerAddressChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://your-relay.example.com", color = TextTertiary) },
                label = { Text("Relay server") },
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (isDiscovering) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BubbleGradientStart,
                            disabledContainerColor = BubbleGradientStart.copy(alpha = 0.65f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        CircularProgressIndicator(
                            color = TextOnBubble,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Discovering...", color = TextOnBubble)
                    }
                    Button(
                        onClick = onStop,
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedError),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Stop", color = TextOnBubble)
                    }
                }
            } else {
                Button(
                    onClick = onRefresh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BubbleGradientStart),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = TextOnBubble)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh desktop sessions", color = TextOnBubble)
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RedError.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = error,
                color = RedError,
                modifier = Modifier.weight(1f),
                lineHeight = 18.sp
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = RedError)
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            text = title,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun EmptyDashboardCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(BubbleGradientStart.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Sync,
                    contentDescription = null,
                    tint = BubbleGradientStart,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "No sessions yet",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Start your bridge on desktop, then tap refresh. Once you connect, the app will keep a snapshot so you can reopen the same work later.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun SessionDashboardCard(
    item: DashboardSessionItem,
    isConnecting: Boolean,
    isSendingQuickReply: Boolean,
    isSendingPermissionDecision: Boolean,
    canDirectRespond: Boolean,
    onResumeLive: () -> Unit,
    onOpenSnapshot: () -> Unit,
    onAllowPermission: () -> Unit,
    onDenyPermission: () -> Unit,
    onReply: () -> Unit,
    onViewResult: () -> Unit
) {
    val primaryActionLabel = when {
        item.isLive -> "Resume on phone"
        item.hasSnapshot -> "Open snapshot"
        else -> "Unavailable"
    }
    val supportingLabel = when {
        item.isLive && item.isConnectedOnPhone -> "Live and already paired"
        item.isLive -> "Desktop is currently online"
        item.hasSnapshot -> "Saved from your previous session"
        else -> "Waiting for desktop to come online"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                item.isLive -> GreenSuccess.copy(alpha = 0.14f)
                                item.hasSnapshot -> AccentBlue.copy(alpha = 0.14f)
                                else -> WarningAmber.copy(alpha = 0.14f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isLive) Icons.Default.Computer else Icons.Default.History,
                        contentDescription = null,
                        tint = when {
                            item.isLive -> GreenSuccess
                            item.hasSnapshot -> AccentBlue
                            else -> WarningAmber
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.title,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        if (item.isCurrentSession) {
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusPill(
                                text = "Current",
                                background = BubbleGradientStart.copy(alpha = 0.18f),
                                contentColor = BubbleGradientStart
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = supportingLabel,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                Icon(
                    imageVector = if (item.isLive) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = if (item.isLive) GreenSuccess else TextTertiary
                )
            }

            if (item.workDir.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.workDir,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(
                    text = item.mode.uppercase(Locale.US),
                    background = TextTertiary.copy(alpha = 0.12f),
                    contentColor = TextSecondary
                )
                if (item.messageCount > 0) {
                    StatusPill(
                        text = "${item.messageCount} msgs",
                        background = AccentBlue.copy(alpha = 0.12f),
                        contentColor = AccentBlue
                    )
                }
                item.lastActiveAt?.let {
                    StatusPill(
                        text = formatLastActive(it),
                        background = TextTertiary.copy(alpha = 0.12f),
                        contentColor = TextSecondary
                    )
                }
            }

            if (item.attentionType != null) {
                Spacer(modifier = Modifier.height(14.dp))
                AttentionCard(
                    attentionType = item.attentionType,
                    title = item.attentionTitle,
                    body = item.attentionBody,
                    updatedAt = item.attentionUpdatedAt,
                    canDirectRespond = canDirectRespond,
                    isSendingQuickReply = isSendingQuickReply,
                    isSendingPermissionDecision = isSendingPermissionDecision,
                    onAllowPermission = onAllowPermission,
                    onDenyPermission = onDenyPermission,
                    onReply = onReply,
                    onViewResult = onViewResult
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (item.isLive) {
                            onResumeLive()
                        } else if (item.hasSnapshot) {
                            onOpenSnapshot()
                        }
                    },
                    enabled = (item.isLive || item.hasSnapshot) && !isConnecting,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BubbleGradientStart)
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            color = TextOnBubble,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(primaryActionLabel, color = TextOnBubble, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (item.hasSnapshot && item.isLive) {
                    TextButton(
                        onClick = onOpenSnapshot,
                        modifier = Modifier.height(46.dp)
                    ) {
                        Text("Snapshot", color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun AttentionCard(
    attentionType: SessionAttentionType,
    title: String,
    body: String,
    updatedAt: Long?,
    canDirectRespond: Boolean,
    isSendingQuickReply: Boolean,
    isSendingPermissionDecision: Boolean,
    onAllowPermission: () -> Unit,
    onDenyPermission: () -> Unit,
    onReply: () -> Unit,
    onViewResult: () -> Unit
) {
    val icon = when (attentionType) {
        SessionAttentionType.WAITING_INPUT -> Icons.Default.Timer
        SessionAttentionType.PERMISSION_REQUEST -> Icons.Default.Warning
        SessionAttentionType.TASK_COMPLETE -> Icons.Default.TaskAlt
    }
    val accent = when (attentionType) {
        SessionAttentionType.WAITING_INPUT -> WarningAmber
        SessionAttentionType.PERMISSION_REQUEST -> RedError
        SessionAttentionType.TASK_COMPLETE -> GreenSuccess
    }
    val label = when (attentionType) {
        SessionAttentionType.WAITING_INPUT -> "Waiting Input"
        SessionAttentionType.PERMISSION_REQUEST -> "Permission Request"
        SessionAttentionType.TASK_COMPLETE -> "Task Completed"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                updatedAt?.let {
                    Text(
                        text = formatLastActive(it),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (body.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = body,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (attentionType == SessionAttentionType.PERMISSION_REQUEST) {
                Spacer(modifier = Modifier.height(12.dp))
                if (canDirectRespond) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onAllowPermission,
                            enabled = !isSendingPermissionDecision,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess)
                        ) {
                            if (isSendingPermissionDecision) {
                                CircularProgressIndicator(
                                    color = TextOnBubble,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Allow", color = TextOnBubble, fontSize = 13.sp)
                            }
                        }
                        Button(
                            onClick = onDenyPermission,
                            enabled = !isSendingPermissionDecision,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedError)
                        ) {
                            Text("Deny", color = TextOnBubble, fontSize = 13.sp)
                        }
                    }
                } else {
                    Text(
                        text = "Resume this session first to approve or deny the request.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
            if (attentionType == SessionAttentionType.WAITING_INPUT) {
                Spacer(modifier = Modifier.height(12.dp))
                if (canDirectRespond) {
                    Button(
                        onClick = onReply,
                        enabled = !isSendingQuickReply,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        if (isSendingQuickReply) {
                            CircularProgressIndicator(
                                color = TextOnBubble,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Reply from dashboard", color = TextOnBubble, fontSize = 13.sp)
                        }
                    }
                } else {
                    Text(
                        text = "Resume this session first to send the next instruction.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
            if (attentionType == SessionAttentionType.TASK_COMPLETE) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onViewResult,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("View session", color = TextOnBubble, fontSize = 13.sp)
                    }
                    if (canDirectRespond) {
                        OutlinedButton(
                            onClick = onReply,
                            enabled = !isSendingQuickReply,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            if (isSendingQuickReply) {
                                CircularProgressIndicator(
                                    color = TextPrimary,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Continue", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickReplyDialog(
    sessionTitle: String,
    isSending: Boolean,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var reply by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = !isSending) { onDismiss() }
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Send next instruction",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss, enabled = !isSending) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Reply directly to $sessionTitle without leaving the dashboard.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 6,
                    placeholder = {
                        Text("Tell the agent what to do next...", color = TextTertiary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = TextTertiary.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSending,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Button(
                        onClick = { onSend(reply.trim()) },
                        enabled = reply.isNotBlank() && !isSending,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                color = TextOnBubble,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Send", color = TextOnBubble, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    background: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
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
                .padding(28.dp),
            shape = RoundedCornerShape(22.dp),
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
                        text = "Authenticate desktop",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter the 6-digit code shown on $bridgeName.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { next ->
                        if (next.length <= 6) {
                            code = next.filter { it.isDigit() }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "000000",
                            color = TextTertiary,
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center
                        )
                    },
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
                        Text("Connecting...", color = TextOnBubble)
                    } else {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = TextOnBubble)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Resume live session", color = TextOnBubble, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun formatLastActive(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val deltaMinutes = ((now - timestamp) / 60000L).coerceAtLeast(0L)
    return when {
        deltaMinutes < 1L -> "Just now"
        deltaMinutes < 60L -> "${deltaMinutes}m ago"
        deltaMinutes < 24L * 60L -> "${deltaMinutes / 60L}h ago"
        else -> SimpleDateFormat("MMM d", Locale.US).format(Date(timestamp))
    }
}
