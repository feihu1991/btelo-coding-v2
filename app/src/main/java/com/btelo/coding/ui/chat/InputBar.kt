package com.btelo.coding.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.btelo.coding.ui.theme.BubbleGradientEnd
import com.btelo.coding.ui.theme.BubbleGradientStart
import com.btelo.coding.ui.theme.CardSurface
import com.btelo.coding.ui.theme.InputSurface
import com.btelo.coding.ui.theme.TextOnBubble
import com.btelo.coding.ui.theme.TextPrimary
import com.btelo.coding.ui.theme.TextSecondary

@Composable
fun InputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceClick: () -> Unit,
    onAttachClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasText = text.isNotBlank()
    val inputShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(InputSurface, inputShape)
            .border(1.dp, Color(0x14FFFFFF), inputShape)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Plus/Attach button
        IconButton(onClick = onAttachClick, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(2.dp))

        // Text input
        TextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1C2128), RoundedCornerShape(20.dp)),
            placeholder = {
                Text(
                    "给 Claude 发消息...",
                    color = TextSecondary.copy(alpha = 0.5f)
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1C2128),
                unfocusedContainerColor = Color(0xFF1C2128),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color(0xFFE6EDF3),
                unfocusedTextColor = Color(0xFFE6EDF3),
                cursorColor = BubbleGradientStart
            ),
            maxLines = 3,
            textStyle = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Mic button
        IconButton(onClick = onVoiceClick, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Default.Mic,
                contentDescription = "Voice",
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        // Send button - blue/purple gradient circle when text is entered
        val sendBg = if (hasText) {
            Brush.linearGradient(listOf(BubbleGradientStart, BubbleGradientEnd))
        } else {
            Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x33FFFFFF)))
        }

        val sendIconTint = if (hasText) TextOnBubble else TextSecondary

        IconButton(
            onClick = onSend,
            enabled = hasText,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(brush = sendBg)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = sendIconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
