package com.btelo.coding.ui.agents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.btelo.coding.ui.chat.AiStreamingBubble
import com.btelo.coding.ui.chat.InputBar
import com.btelo.coding.ui.chat.MessageBubble
import com.btelo.coding.ui.theme.AppBackground
import com.btelo.coding.ui.theme.AvatarColors
import com.btelo.coding.ui.theme.BubbleGradientEnd
import com.btelo.coding.ui.theme.BubbleGradientStart
import com.btelo.coding.ui.theme.CardSurface
import com.btelo.coding.ui.theme.CardElevated
import com.btelo.coding.ui.theme.TextOnBubble
import com.btelo.coding.ui.theme.TextPrimary
import com.btelo.coding.ui.theme.TextSecondary
import com.btelo.coding.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentsScreen(
    onSessionListOpen: () -> Unit,
    onSessionClick: (String) -> Unit,
    viewModel: AgentsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val chatListState = rememberLazyListState()

    // Auto-scroll when new messages arrive or streaming
    LaunchedEffect(uiState.messages.size, uiState.isStreaming, uiState.streamingContent) {
        if (uiState.messages.isNotEmpty() || uiState.isStreaming) {
            chatListState.animateScrollToItem(
                if (uiState.isStreaming) uiState.messages.size else uiState.messages.size - 1
            )
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar("Error: $error")
        }
    }

    Scaffold(
        topBar = {
            AgentsTopBar(
                sessionName = uiState.currentSessionName,
                onMenuClick = onSessionListOpen,
                onNotificationClick = { }
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
            // Chat area
            if (uiState.messages.isEmpty() && !uiState.isStreaming) {
                // Empty state
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Start a conversation",
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Send a message to begin coding with Claude",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                // Messages list
                LazyColumn(
                    state = chatListState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.messages) { message ->
                        MessageBubble(message = message)
                    }
                    if (uiState.isStreaming && uiState.streamingContent.isNotBlank()) {
                        item {
                            AiStreamingBubble(partialContent = uiState.streamingContent)
                        }
                    }
                }
            }

            // Session tabs (horizontal scrollable)
            if (uiState.sessions.isNotEmpty()) {
                SessionTabsRow(
                    sessions = uiState.sessions,
                    currentSessionId = uiState.currentSessionId,
                    onSessionClick = { sessionId ->
                        viewModel.switchSession(sessionId)
                    },
                    onNewSession = { viewModel.createSession() }
                )
            }

            // Quick actions
            QuickActionsRow(
                actions = uiState.quickActions,
                onActionClick = { action ->
                    viewModel.updateInputText(action)
                    viewModel.sendMessage()
                }
            )

            // Input bar + toolbar
            AgentsInputBar(
                text = uiState.inputText,
                onTextChange = viewModel::updateInputText,
                onSend = viewModel::sendMessage,
                onVoiceClick = { },
                onAttachClick = { }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgentsTopBar(
    sessionName: String,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    TopAppBar(
        title = {
            // Session name pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardSurface)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sessionName.ifBlank { "BTELO Coding" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "0 tokens",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "Sessions",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        actions = {
            IconButton(onClick = onNotificationClick) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            // Avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BubbleGradientStart.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = BubbleGradientStart,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppBackground
        )
    )
}

@Composable
private fun SessionTabsRow(
    sessions: List<SessionInfo>,
    currentSessionId: String?,
    onSessionClick: (String) -> Unit,
    onNewSession: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(sessions) { session ->
                val dotColor = AvatarColors[session.name.hashCode().mod(AvatarColors.size)]
                val isSelected = session.id == currentSessionId

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.08f)
                            else Color.Transparent
                        )
                        .clickable { onSessionClick(session.id) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = session.name,
                            fontSize = 12.sp,
                            color = if (isSelected) TextPrimary else TextTertiary,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // New session button
        IconButton(
            onClick = onNewSession,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "New session",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun QuickActionsRow(
    actions: List<String>,
    onActionClick: (String) -> Unit
) {
    if (actions.isEmpty()) return

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(actions) { action ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardSurface)
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                    .clickable { onActionClick(action) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = action,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun AgentsInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceClick: () -> Unit,
    onAttachClick: () -> Unit
) {
    val hasText = text.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurface)
    ) {
        // Input field
        TextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            placeholder = {
                Text(
                    "Message, / commands, @ history, $ files",
                    color = TextTertiary,
                    fontSize = 14.sp
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = BubbleGradientStart
            ),
            maxLines = 4,
            textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium
        )

        // Toolbar row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // AI mode
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = "AI",
                tint = Color(0xFF8B5CF6),
                modifier = Modifier
                    .size(28.dp)
                    .padding(4.dp)
                    .clickable { }
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Attachment
            Icon(
                Icons.Default.Attachment,
                contentDescription = "Attach",
                tint = TextSecondary,
                modifier = Modifier
                    .size(28.dp)
                    .padding(4.dp)
                    .clickable(onClick = onAttachClick)
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Star
            Icon(
                Icons.Default.Star,
                contentDescription = "Star",
                tint = TextSecondary,
                modifier = Modifier
                    .size(28.dp)
                    .padding(4.dp)
                    .clickable { }
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Code
            Icon(
                Icons.Default.Code,
                contentDescription = "Code",
                tint = TextSecondary,
                modifier = Modifier
                    .size(28.dp)
                    .padding(4.dp)
                    .clickable { }
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Casino/Random
            Icon(
                Icons.Default.Casino,
                contentDescription = "Tools",
                tint = TextSecondary,
                modifier = Modifier
                    .size(28.dp)
                    .padding(4.dp)
                    .clickable { }
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Mic
            Icon(
                Icons.Default.Mic,
                contentDescription = "Voice",
                tint = TextSecondary,
                modifier = Modifier
                    .size(28.dp)
                    .padding(4.dp)
                    .clickable(onClick = onVoiceClick)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Send button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasText) Brush.linearGradient(
                            listOf(BubbleGradientStart, BubbleGradientEnd)
                        ) else Brush.linearGradient(
                            listOf(Color(0x33FFFFFF), Color(0x33FFFFFF))
                        )
                    )
                    .clickable(enabled = hasText, onClick = onSend),
                contentAlignment = Alignment.Center
            ) {
                if (false) { // TODO: loading state
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = TextOnBubble,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (hasText) TextOnBubble else TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Bottom safe area padding
        Spacer(modifier = Modifier.height(8.dp))
    }
}
