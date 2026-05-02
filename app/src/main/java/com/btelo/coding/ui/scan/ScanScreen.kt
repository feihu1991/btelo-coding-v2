package com.btelo.coding.ui.scan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.btelo.coding.data.remote.AppUpdateInfo
import com.btelo.coding.data.remote.DownloadState
import com.btelo.coding.ui.theme.AccentBlue
import com.btelo.coding.ui.theme.AppBackground
import com.btelo.coding.ui.theme.BorderDefault
import com.btelo.coding.ui.theme.BorderSubtle
import com.btelo.coding.ui.theme.BubbleGradientStart
import com.btelo.coding.ui.theme.CardSurface
import com.btelo.coding.ui.theme.GreenSuccess
import com.btelo.coding.ui.theme.RedError
import com.btelo.coding.ui.theme.TextOnBubble
import com.btelo.coding.ui.theme.TextPrimary
import com.btelo.coding.ui.theme.TextSecondary
import com.btelo.coding.ui.theme.TextTertiary
import com.btelo.coding.ui.theme.ThinkingPurple
import com.btelo.coding.ui.theme.WarningAmber
import com.btelo.coding.ui.workbench.StatusDot
import com.btelo.coding.ui.workbench.WorkbenchBottomBar
import com.btelo.coding.ui.workbench.WorkbenchTab

@Composable
fun ScanScreen(
    onConnected: (String) -> Unit,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(WorkbenchTab.Agents) }
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
            .statusBarsPadding()
    ) {
        when (selectedTab) {
            WorkbenchTab.Agents -> AgentsPage(
                uiState = uiState,
                onServerChange = viewModel::setServerAddress,
                onDiscover = viewModel::discoverBridges,
                onStopDiscover = viewModel::stopDiscovery,
                onClearError = viewModel::clearError,
                onBridgeClick = { bridge ->
                    authBridgeId = bridge.id
                    authBridgeName = bridge.deviceName
                    showAuthDialog = true
                }
            )
            WorkbenchTab.Files -> FilesPage()
            WorkbenchTab.Browser -> BrowserPage()
            WorkbenchTab.Devices -> DevicesPage(
                uiState = uiState,
                onCheckUpdate = viewModel::checkForUpdate,
                onDiscover = viewModel::discoverBridges
            )
        }

        WorkbenchBottomBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 12.dp)
        )

        AnimatedVisibility(
            visible = showAuthDialog,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            AuthCodeDialog(
                bridgeName = authBridgeName,
                isConnecting = uiState.isConnecting,
                onConfirm = { code -> viewModel.connectToBridge(authBridgeId, code) },
                onDismiss = {
                    showAuthDialog = false
                    viewModel.clearError()
                }
            )
        }
    }
}

@Composable
private fun AgentsPage(
    uiState: ScanUiState,
    onServerChange: (String) -> Unit,
    onDiscover: () -> Unit,
    onStopDiscover: () -> Unit,
    onClearError: () -> Unit,
    onBridgeClick: (BridgeInfo) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 112.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            OnboardingCard()
            Spacer(modifier = Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sessions",
                    color = TextPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                RoundAction(Icons.Default.Refresh, "Refresh", onDiscover)
                Spacer(modifier = Modifier.width(12.dp))
                RoundAction(Icons.Default.Add, "Add", onDiscover)
            }
            Spacer(modifier = Modifier.height(14.dp))
            ServerPanel(
                address = uiState.serverAddress,
                isDiscovering = uiState.isDiscovering,
                onServerChange = onServerChange,
                onDiscover = onDiscover,
                onStopDiscover = onStopDiscover
            )
            uiState.error?.let {
                Spacer(modifier = Modifier.height(10.dp))
                ErrorPanel(message = it, onDismiss = onClearError)
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        if (uiState.bridges.isEmpty()) {
            item {
                EmptySessionsCard()
                Spacer(modifier = Modifier.height(26.dp))
            }
        } else {
            items(uiState.bridges, key = { it.id }) { bridge ->
                BridgeRow(
                    bridge = bridge,
                    isConnecting = uiState.isConnecting && uiState.selectedBridgeId == bridge.id,
                    onClick = { onBridgeClick(bridge) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            item { Spacer(modifier = Modifier.height(18.dp)) }
        }

        item {
            Text(
                text = "Automation",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            AutomationRow("build:apk", "Package debug APK and publish through relay", GreenSuccess)
            AutomationRow("restart:bridge", "Restart claudex terminal bridge remotely", GreenSuccess)
            AutomationRow("restart:relay", "Restart local relay service on desktop", WarningAmber)
        }
    }
}

@Composable
private fun OnboardingCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardSurface)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(AccentBlue.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = AccentBlue)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Meet Agent", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Remote Claude Code sessions from your phone", color = TextSecondary, fontSize = 14.sp)
        }
        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ServerPanel(
    address: String,
    isDiscovering: Boolean,
    onServerChange: (String) -> Unit,
    onDiscover: () -> Unit,
    onStopDiscover: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            OutlinedTextField(
                value = address,
                onValueChange = onServerChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("http://192.168.x.x:8080", color = TextTertiary) },
                leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null, tint = TextSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = BorderDefault,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = AccentBlue
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Search),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { if (isDiscovering) onStopDiscover() else onDiscover() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isDiscovering) RedError else AccentBlue)
            ) {
                if (isDiscovering) {
                    CircularProgressIndicator(color = TextOnBubble, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop discovery", color = TextOnBubble)
                } else {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextOnBubble)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Discover desktop", color = TextOnBubble)
                }
            }
        }
    }
}

@Composable
private fun BridgeRow(
    bridge: BridgeInfo,
    isConnecting: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardSurface)
            .clickable(enabled = !isConnecting) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(ThinkingPurple.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Terminal, contentDescription = null, tint = AccentBlue)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = bridge.deviceName.ifBlank { "Claude Code" },
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (bridge.bridgeConnected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusDot(GreenSuccess)
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = bridge.workDir,
                color = TextSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isConnecting) {
            CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun EmptySessionsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderDefault, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurface.copy(alpha = 0.45f))
            .padding(18.dp)
    ) {
        Text("No active desktops", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Start claudex on your computer, then discover it here.", color = TextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun AutomationRow(title: String, subtitle: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(subtitle, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun FilesPage() {
    WorkbenchPlaceholder(
        title = "Files",
        introIcon = Icons.Default.Info,
        introTitle = "Browse from Path",
        introSubtitle = "Open a repository and start a new session there.",
        rows = listOf(
            Icons.Default.Folder to "app",
            Icons.Default.Folder to "server",
            Icons.Default.Code to "build.gradle.kts",
            Icons.Default.Code to "README.md"
        )
    )
}

@Composable
private fun BrowserPage() {
    WorkbenchPlaceholder(
        title = "Web Proxy",
        introIcon = Icons.Default.Search,
        introTitle = "How Browser Works",
        introSubtitle = "Access local dev servers and websites remotely.",
        rows = listOf(
            Icons.Default.Storage to "Next.js Dev  localhost:3000",
            Icons.Default.Storage to "Go API  localhost:8080",
            Icons.Default.Storage to "Storybook  localhost:6006"
        )
    )
}

@Composable
private fun DevicesPage(
    uiState: ScanUiState,
    onCheckUpdate: () -> Unit,
    onDiscover: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 112.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(80.dp))
            Text("Devices", color = TextPrimary, fontSize = 42.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(28.dp))
            DeviceAction("Relay server", uiState.serverAddress, AccentBlue, Icons.Default.Dns, onDiscover)
            DeviceAction("Check app update", "Download APK from release or relay", GreenSuccess, Icons.Default.CloudDownload, onCheckUpdate)
            DeviceAction("Connected bridges", "${uiState.bridges.size} desktop session(s)", ThinkingPurple, Icons.Default.Computer, onDiscover)
        }
    }
}

@Composable
private fun WorkbenchPlaceholder(
    title: String,
    introIcon: ImageVector,
    introTitle: String,
    introSubtitle: String,
    rows: List<Pair<ImageVector, String>>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 112.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(80.dp))
            Text(title, color = TextPrimary, fontSize = 42.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(28.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(CardSurface)
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(introIcon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(34.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(introTitle, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(introSubtitle, color = TextSecondary, fontSize = 14.sp)
                }
                Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(26.dp))
        }
        items(rows) { row ->
            DeviceAction(row.second, "Planned workspace feature", AccentBlue, row.first, {})
        }
    }
}

@Composable
private fun DeviceAction(title: String, subtitle: String, color: Color, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(CardSurface)
            .clickable { onClick() }
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(subtitle, color = TextSecondary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun RoundAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(CardSurface)
    ) {
        Icon(icon, contentDescription = label, tint = TextPrimary)
    }
}

@Composable
private fun ErrorPanel(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RedError.copy(alpha = 0.12f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(message, color = RedError, modifier = Modifier.weight(1f), fontSize = 13.sp)
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = null, tint = RedError, modifier = Modifier.size(18.dp))
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
            .background(Color.Black.copy(alpha = 0.82f)),
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
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface)
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text("Enter auth code", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Connect to $bridgeName", color = TextSecondary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(18.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { value -> code = value.filter { it.isDigit() }.take(6) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("000000", color = TextTertiary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderDefault,
                        cursorColor = AccentBlue
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(18.dp)
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = { onConfirm(code) },
                    enabled = code.length == 6 && !isConnecting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(color = TextOnBubble, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Connect", color = TextOnBubble, fontWeight = FontWeight.Bold)
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
        onDismissRequest = { if (downloadState !is DownloadState.Progress) onDismiss() },
        title = {
            Text(
                text = if (downloadState is DownloadState.Completed) "Download ready" else "Update available",
                color = TextPrimary
            )
        },
        text = {
            Column {
                when (downloadState) {
                    is DownloadState.Progress -> {
                        Text("Downloading ${updateInfo.versionName}", color = TextSecondary)
                        Spacer(modifier = Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { downloadState.percent / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = AccentBlue,
                            trackColor = TextTertiary.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${downloadState.percent}%", color = TextTertiary, fontSize = 12.sp)
                    }
                    is DownloadState.Completed -> Text("The APK has been downloaded and is ready to install.", color = TextSecondary)
                    is DownloadState.Failed -> Text("Download failed: ${downloadState.error}", color = RedError)
                    null -> {
                        Text("Version ${updateInfo.versionName} is available.", color = TextSecondary)
                        if (updateInfo.sizeBytes > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Size: ${formatFileSize(updateInfo.sizeBytes)}", color = TextTertiary, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (downloadState) {
                is DownloadState.Progress -> TextButton(onClick = onCancel) { Text("Cancel", color = RedError) }
                is DownloadState.Completed -> TextButton(onClick = onInstall) { Text("Install", color = AccentBlue) }
                is DownloadState.Failed -> TextButton(onClick = onDownload) { Text("Retry", color = AccentBlue) }
                null -> TextButton(onClick = onDownload) { Text("Download", color = AccentBlue) }
            }
        },
        dismissButton = {
            if (downloadState !is DownloadState.Progress) {
                TextButton(onClick = onDismiss) { Text("Later", color = TextSecondary) }
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
