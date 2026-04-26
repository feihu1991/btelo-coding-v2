package com.btelo.coding.ui.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// 语音活跃颜色
private val VoiceActiveColor = Color(0xFFE91E63)

@Composable
fun InputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceClick: () -> Unit,
    onAttachClick: () -> Unit,
    modifier: Modifier = Modifier,
    isVoiceListening: Boolean = false
) {
    // 录音动画
    val infiniteTransition = rememberInfiniteTransition(label = "voiceAnimation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val micColor by animateColorAsState(
        targetValue = if (isVoiceListening) VoiceActiveColor else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "micColor"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAttachClick) {
            Icon(
                Icons.Default.AttachFile,
                contentDescription = "附件",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 语音按钮（带录音动画）
        IconButton(
            onClick = onVoiceClick,
            modifier = if (isVoiceListening) Modifier.scale(pulseScale) else Modifier
        ) {
            Icon(
                imageVector = if (isVoiceListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isVoiceListening) "停止录音" else "语音",
                tint = micColor
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { 
                Text(
                    if (isVoiceListening) "正在识别..." else "输入命令..."
                )
            },
            maxLines = 3,
            enabled = !isVoiceListening // 录音时禁用输入
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onSend,
            enabled = text.isNotBlank() && !isVoiceListening
        ) {
            Icon(
                Icons.Default.Send,
                contentDescription = "发送",
                tint = if (text.isNotBlank() && !isVoiceListening)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
