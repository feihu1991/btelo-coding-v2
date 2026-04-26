package com.btelo.coding.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.btelo.coding.ui.theme.AppBackground
import com.btelo.coding.ui.theme.BubbleGradientEnd
import com.btelo.coding.ui.theme.BubbleGradientStart
import com.btelo.coding.ui.theme.CardSurface
import com.btelo.coding.ui.theme.CardElevated
import com.btelo.coding.ui.theme.TextOnBubble
import com.btelo.coding.ui.theme.TextPrimary
import com.btelo.coding.ui.theme.TextSecondary
import com.btelo.coding.ui.theme.TextTertiary
import com.btelo.coding.ui.theme.BorderSubtle
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var isRegisterMode by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = CardSurface,
                contentColor = TextPrimary,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "对话"
                        )
                    },
                    label = { Text("对话", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BubbleGradientStart,
                        selectedTextColor = BubbleGradientStart,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = Color(0x143B82F6)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Work,
                            contentDescription = "工作"
                        )
                    },
                    label = { Text("工作", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BubbleGradientStart,
                        selectedTextColor = BubbleGradientStart,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = Color(0x143B82F6)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "我的"
                        )
                    },
                    label = { Text("我的", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BubbleGradientStart,
                        selectedTextColor = BubbleGradientStart,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = Color(0x143B82F6)
                    )
                )
            }
        },
        containerColor = AppBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(paddingValues)
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Decorative illustration - brain/neural network
                HubIllustration(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Fixed title and subtitle
                Text(
                    text = "BTELO Coding",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    fontSize = 30.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "口袋开发工作站",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Tab row: 登录 / 注册
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardSurface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (!isRegisterMode) BubbleGradientStart.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .clickable { isRegisterMode = false; viewModel.clearError() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "登录",
                            fontWeight = if (!isRegisterMode) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (!isRegisterMode) BubbleGradientStart else TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isRegisterMode) BubbleGradientStart.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .clickable { isRegisterMode = true; viewModel.clearError() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "注册",
                            fontWeight = if (isRegisterMode) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isRegisterMode) BubbleGradientStart else TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Server address field - always visible
                OutlinedTextField(
                    value = uiState.serverAddress,
                    onValueChange = viewModel::updateServerAddress,
                    label = { Text("服务器地址", color = TextSecondary) },
                    placeholder = { Text("http://10.0.2.2:8080", color = TextTertiary) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Work,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = uiState.connectionStatus == ConnectionStatus.DISCONNECTED,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Register mode: show name field
                AnimatedVisibility(visible = isRegisterMode) {
                    Column {
                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = viewModel::updateName,
                            label = { Text("昵称", color = TextSecondary) },
                            placeholder = { Text("输入您的昵称", color = TextTertiary) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = uiState.connectionStatus == ConnectionStatus.DISCONNECTED,
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Email field
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = viewModel::updateEmail,
                    label = { Text("账号", color = TextSecondary) },
                    placeholder = { Text("输入您的账号", color = TextTertiary) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    enabled = uiState.connectionStatus == ConnectionStatus.DISCONNECTED,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password field with visibility toggle
                var passwordVisible by rememberSaveable { mutableStateOf(false) }
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = viewModel::updatePassword,
                    label = { Text("密码", color = TextSecondary) },
                    placeholder = { Text("输入您的密码", color = TextTertiary) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (isRegisterMode) ImeAction.Next else ImeAction.Done
                    ),
                    enabled = uiState.connectionStatus == ConnectionStatus.DISCONNECTED,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors()
                )

                // Register mode: show confirm password field
                AnimatedVisibility(visible = isRegisterMode) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = uiState.confirmPassword,
                            onValueChange = viewModel::updateConfirmPassword,
                            label = { Text("确认密码", color = TextSecondary) },
                            placeholder = { Text("再次输入密码", color = TextTertiary) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            enabled = uiState.connectionStatus == ConnectionStatus.DISCONNECTED,
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors()
                        )
                    }
                }

                if (uiState.error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = uiState.error!!,
                        color = Color(0xFFEF4444),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Login / Register button with gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(BubbleGradientStart, BubbleGradientEnd)
                            )
                        )
                        .clickable(enabled = !uiState.isLoading) {
                            if (isRegisterMode) {
                                viewModel.register()
                            } else {
                                viewModel.login()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoading && uiState.connectionStatus != ConnectionStatus.WAITING_FOR_PAIRING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = TextOnBubble,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isRegisterMode) "注册并登录" else "连接",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = TextOnBubble
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle login/register link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRegisterMode) "已有账号？" else "还没有账号？",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isRegisterMode) "去登录" else "去注册",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = BubbleGradientStart,
                        modifier = Modifier.clickable {
                            isRegisterMode = !isRegisterMode
                            viewModel.clearError()
                        }
                    )
                }

                // Connection status states
                AnimatedVisibility(visible = uiState.connectionStatus == ConnectionStatus.WAITING_FOR_PAIRING) {
                    Column {
                        Spacer(modifier = Modifier.height(20.dp))
                        DevicePairingCard(
                            deviceId = uiState.deviceId,
                            pairingCode = uiState.pairingCode,
                            expiresAt = uiState.expiresAt,
                            isLoading = uiState.isLoading,
                            onRefreshCode = viewModel::refreshPairingCode,
                            onPaired = viewModel::onDevicePaired
                        )
                    }
                }

                AnimatedVisibility(visible = uiState.connectionStatus == ConnectionStatus.PAIRED) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(20.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(44.dp),
                            color = BubbleGradientStart
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "配对成功! 正在进入...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BubbleGradientStart
                        )
                    }
                }

                // Extra space to ensure button is above nav bar
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BubbleGradientStart,
    unfocusedBorderColor = BorderSubtle,
    cursorColor = BubbleGradientStart,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = BubbleGradientStart,
    unfocusedLabelColor = TextSecondary,
    focusedContainerColor = CardSurface,
    unfocusedContainerColor = CardSurface,
    focusedLeadingIconColor = BubbleGradientStart,
    unfocusedLeadingIconColor = TextSecondary,
    focusedTrailingIconColor = TextSecondary,
    unfocusedTrailingIconColor = TextSecondary
)

@Composable
private fun HubIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2
        val nodeRadius = size.minDimension * 0.055f

        data class Node(val x: Float, val y: Float, val r: Float, val color: Color, val glow: Boolean)

        val nodes = listOf(
            Node(cx, cy - h * 0.30f, nodeRadius * 1.4f, Color(0xFF3B82F6), true),
            Node(cx + w * 0.16f, cy - h * 0.14f, nodeRadius * 1.1f, Color(0xFF60A5FA), false),
            Node(cx - w * 0.18f, cy - h * 0.16f, nodeRadius, Color(0xFF8B5CF6), true),
            Node(cx + w * 0.22f, cy + h * 0.04f, nodeRadius * 1.2f, Color(0xFFA78BFA), true),
            Node(cx - w * 0.20f, cy + h * 0.06f, nodeRadius * 0.9f, Color(0xFF3B82F6), false),
            Node(cx + w * 0.04f, cy + h * 0.20f, nodeRadius * 1.3f, Color(0xFF6D28D9), true),
            Node(cx - w * 0.06f, cy + h * 0.22f, nodeRadius, Color(0xFF60A5FA), false),
            Node(cx + w * 0.26f, cy - h * 0.24f, nodeRadius * 0.8f, Color(0xFF8B5CF6), false),
            Node(cx - w * 0.28f, cy - h * 0.06f, nodeRadius * 0.7f, Color(0xFF3B82F6), false),
            Node(cx - w * 0.28f, cy + h * 0.18f, nodeRadius * 0.9f, Color(0xFFA78BFA), false),
            Node(cx + w * 0.28f, cy + h * 0.16f, nodeRadius * 0.75f, Color(0xFF60A5FA), false),
            Node(cx, cy - h * 0.04f, nodeRadius * 1.5f, Color(0xFF7C3AED), true),
        )

        data class Edge(val a: Int, val b: Int, val alpha: Float, val width: Float)
        val edges = listOf(
            Edge(0, 1, 0.4f, 2f), Edge(0, 2, 0.35f, 1.5f), Edge(0, 3, 0.3f, 1.5f),
            Edge(1, 3, 0.4f, 2f), Edge(1, 5, 0.3f, 1.5f), Edge(2, 4, 0.35f, 2f),
            Edge(3, 5, 0.4f, 2f), Edge(3, 10, 0.25f, 1f), Edge(4, 6, 0.35f, 1.5f),
            Edge(4, 9, 0.25f, 1f), Edge(5, 6, 0.3f, 1.5f), Edge(5, 11, 0.4f, 2.5f),
            Edge(6, 11, 0.35f, 2f), Edge(7, 0, 0.2f, 1f), Edge(7, 1, 0.25f, 1.5f),
            Edge(8, 2, 0.2f, 1f), Edge(8, 9, 0.3f, 1.5f), Edge(9, 4, 0.25f, 1.5f),
            Edge(10, 3, 0.2f, 1f), Edge(11, 0, 0.35f, 2f), Edge(11, 2, 0.25f, 1.5f),
        )

        // Background glow at key nodes
        for (node in nodes.filter { it.glow }) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        node.color.copy(alpha = 0.3f),
                        node.color.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(node.x, node.y),
                    radius = node.r * 4f
                ),
                radius = node.r * 4f,
                center = Offset(node.x, node.y)
            )
        }

        // Draw edges
        for ((a, b, alpha, width) in edges) {
            val na = nodes[a]
            val nb = nodes[b]
            val midColor = Color(
                red = (na.color.red + nb.color.red) / 2,
                green = (na.color.green + nb.color.green) / 2,
                blue = (na.color.blue + nb.color.blue) / 2,
                alpha = alpha
            )
            drawLine(
                color = midColor,
                start = Offset(na.x, na.y),
                end = Offset(nb.x, nb.y),
                strokeWidth = width,
                cap = StrokeCap.Round
            )
        }

        // Draw nodes
        for (node in nodes) {
            drawCircle(
                color = node.color.copy(alpha = 0.3f),
                radius = node.r * 1.6f,
                center = Offset(node.x, node.y)
            )
            drawCircle(
                color = node.color.copy(alpha = 0.85f),
                radius = node.r,
                center = Offset(node.x, node.y)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = node.r * 0.45f,
                center = Offset(node.x - node.r * 0.15f, node.y - node.r * 0.15f)
            )
        }

        // Scattered small dots
        val smallDots = listOf(
            Pair(cx + w * 0.33f, cy - h * 0.15f),
            Pair(cx - w * 0.35f, cy + h * 0.10f),
            Pair(cx + w * 0.13f, cy - h * 0.33f),
            Pair(cx - w * 0.10f, cy + h * 0.30f),
            Pair(cx + w * 0.35f, cy + h * 0.22f),
            Pair(cx - w * 0.33f, cy - h * 0.26f),
            Pair(cx + w * 0.30f, cy + h * 0.32f),
            Pair(cx - w * 0.26f, cy + h * 0.28f),
        )
        for ((dx, dy) in smallDots) {
            drawCircle(
                color = Color(0x50FFFFFF),
                radius = 2.5f,
                center = Offset(dx, dy)
            )
        }

        // Central highlight glow
        val centerNode = nodes[11]
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x407C3AED),
                    Color(0x103B82F6),
                    Color.Transparent
                ),
                center = Offset(centerNode.x, centerNode.y),
                radius = nodeRadius * 7f
            ),
            radius = nodeRadius * 7f,
            center = Offset(centerNode.x, centerNode.y)
        )
    }
}

@Composable
private fun DevicePairingCard(
    deviceId: String,
    pairingCode: String,
    expiresAt: String?,
    isLoading: Boolean,
    onRefreshCode: () -> Unit,
    onPaired: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .background(CardSurface, RoundedCornerShape(20.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "设备配对",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "在终端中输入配对码完成连接",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "设备 ID",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = deviceId,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "配对码",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0D1117))
                .padding(horizontal = 32.dp, vertical = 14.dp)
        ) {
            Text(
                text = pairingCode,
                style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = 6.sp),
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        if (expiresAt != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "过期时间: $expiresAt",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRefreshCode,
                enabled = !isLoading,
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("刷新", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = onPaired,
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = TextOnBubble
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(BubbleGradientStart, BubbleGradientEnd)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("已配对", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
