package com.btelo.coding.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.sp
import com.btelo.coding.ui.theme.BorderSubtle
import com.btelo.coding.ui.theme.BubbleGradientEnd
import com.btelo.coding.ui.theme.BubbleGradientStart
import com.btelo.coding.ui.theme.InputSurface
import com.btelo.coding.ui.theme.TextOnBubble
import com.btelo.coding.ui.theme.TextPrimary
import com.btelo.coding.ui.theme.TextSecondary

@Composable
fun InputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasText = text.isNotBlank()
    val inputShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(InputSurface, inputShape)
            .border(1.dp, BorderSubtle.copy(alpha = 0.3f), inputShape)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Attachment button (left)
        IconButton(
            onClick = onAttachClick,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Attach",
                tint = TextSecondary,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Text input
        TextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(InputSurface, RoundedCornerShape(20.dp)),
            placeholder = {
                Text(
                    "输入消息...",
                    color = TextSecondary.copy(alpha = 0.5f),
                    fontSize = 15.sp
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
            textStyle = TextStyle(fontSize = 15.sp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Send button
        val sendBg = if (hasText) {
            Brush.linearGradient(listOf(BubbleGradientStart, BubbleGradientEnd))
        } else {
            Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x33FFFFFF)))
        }

        val sendIconTint = if (hasText) TextOnBubble else TextSecondary

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(brush = sendBg),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onSend,
                enabled = hasText,
                modifier = Modifier.size(40.dp)
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
}
