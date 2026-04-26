package com.btelo.coding.ui.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.btelo.coding.data.remote.websocket.factory.ConnectionState
import com.btelo.coding.ui.theme.AppBackground
import com.btelo.coding.ui.theme.BubbleGradientStart
import com.btelo.coding.ui.theme.GreenSuccess
import com.btelo.coding.ui.theme.RedError
import com.btelo.coding.ui.theme.TextPrimary
import com.btelo.coding.ui.theme.TextSecondary
import com.btelo.coding.ui.theme.CardSurface

object ConnectionColors {
    val Connected = GreenSuccess
    val Connecting = Color(0xFFFFC107)
    val Disconnected = Color(0xFF6E7681)
    val Error = RedError
    val Reconnecting = Color(0xFFFF9800)
}

@Composable
fun ConnectionStatusDot(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val dotColor by animateColorAsState(
        targetValue = when (connectionState) {
            is ConnectionState.Connected -> ConnectionColors.Connected
            is ConnectionState.Connecting -> ConnectionColors.Connecting
            is ConnectionState.Disconnected -> ConnectionColors.Disconnected
            is ConnectionState.Error -> ConnectionColors.Error
            is ConnectionState.Reconnecting -> ConnectionColors.Reconnecting
        },
        label = "dotColor"
    )
    val ringColor by animateColorAsState(
        targetValue = when (connectionState) {
            is ConnectionState.Connected -> ConnectionColors.Connected.copy(alpha = 0.25f)
            is ConnectionState.Connecting -> ConnectionColors.Connecting.copy(alpha = 0.25f)
            is ConnectionState.Disconnected -> ConnectionColors.Disconnected.copy(alpha = 0.15f)
            is ConnectionState.Error -> ConnectionColors.Error.copy(alpha = 0.25f)
            is ConnectionState.Reconnecting -> ConnectionColors.Reconnecting.copy(alpha = 0.25f)
        },
        label = "ringColor"
    )
    val statusText by animateColorAsState(
        targetValue = when (connectionState) {
            is ConnectionState.Connected -> GreenSuccess
            is ConnectionState.Connecting -> ConnectionColors.Connecting
            is ConnectionState.Disconnected -> ConnectionColors.Disconnected
            is ConnectionState.Error -> ConnectionColors.Error
            is ConnectionState.Reconnecting -> ConnectionColors.Reconnecting
        },
        label = "statusTextColor"
    )
    val statusLabel = when (connectionState) {
        is ConnectionState.Connected -> "在线"
        is ConnectionState.Connecting -> "连接中"
        is ConnectionState.Disconnected -> "已断开"
        is ConnectionState.Error -> "错误"
        is ConnectionState.Reconnecting -> "重连中"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // Status dot with ring
        Box(
            modifier = Modifier.size(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(ringColor)
            )
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = statusLabel,
            fontSize = 11.sp,
            color = statusText,
            fontWeight = FontWeight.Medium
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

    LaunchedEffect(sessionId) {
        viewModel.setSessionId(sessionId)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar("Connection error: $error")
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.sessionName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        ConnectionStatusDot(connectionState = uiState.connectionState)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppBackground)
        ) {
            MessageList(
                messages = uiState.messages,
                streamingContent = uiState.streamingContent,
                isStreaming = uiState.isStreaming,
                modifier = Modifier.weight(1f)
            )

            InputBar(
                text = uiState.inputText,
                onTextChange = viewModel::updateInputText,
                onSend = viewModel::sendMessage,
                onVoiceClick = { },
                onAttachClick = { },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
