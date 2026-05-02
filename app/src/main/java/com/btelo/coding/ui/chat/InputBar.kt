package com.btelo.coding.ui.chat

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Terminal
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.btelo.coding.ui.theme.AccentBlue
import com.btelo.coding.ui.theme.BorderDefault
import com.btelo.coding.ui.theme.BubbleGradientEnd
import com.btelo.coding.ui.theme.BubbleGradientStart
import com.btelo.coding.ui.theme.CardSurface
import com.btelo.coding.ui.theme.GreenSuccess
import com.btelo.coding.ui.theme.InputSurface
import com.btelo.coding.ui.theme.TextOnBubble
import com.btelo.coding.ui.theme.TextPrimary
import com.btelo.coding.ui.theme.TextSecondary
import com.btelo.coding.ui.theme.TextTertiary
import com.btelo.coding.ui.workbench.StatusDot

@Composable
fun InputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedImageUri: Uri? = null,
    onClearImage: () -> Unit = {},
    projectName: String = "workspace"
) {
    val hasText = text.isNotBlank() || selectedImageUri != null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(CardSurface.copy(alpha = 0.98f))
            .border(1.dp, BorderDefault, RoundedCornerShape(26.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WorkspaceChip(projectName, selected = true)
            Spacer(modifier = Modifier.width(10.dp))
            WorkspaceChip("Add SSO login", selected = false)
            Spacer(modifier = Modifier.width(10.dp))
            Icon(Icons.Default.Add, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Icon(Icons.Default.KeyboardCommandKey, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedImageUri != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected attachment",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = onClearImage,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.7f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("Attachment ready", color = TextSecondary, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        TextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(InputSurface),
            placeholder = {
                Text(
                    "Message, / commands, @ history, $ files",
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = AccentBlue
            ),
            maxLines = 4,
            textStyle = TextStyle(fontSize = 17.sp, lineHeight = 23.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() })
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ToolbarIcon(Icons.Default.AutoAwesome, AccentBlue, "Agent")
                ToolbarIcon(Icons.Default.AttachFile, TextSecondary, "Attach", onAttachClick)
                ToolbarIcon(Icons.Default.Terminal, TextSecondary, "Command")
                ToolbarIcon(Icons.Default.Folder, TextSecondary, "Files")
                ToolbarIcon(Icons.Default.Mic, TextSecondary, "Voice")
                ToolbarIcon(Icons.Default.MoreHoriz, TextSecondary, "More")
            }
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasText) {
                            Brush.linearGradient(listOf(BubbleGradientStart, BubbleGradientEnd))
                        } else {
                            Brush.linearGradient(listOf(Color(0xFF3A3A3C), Color(0xFF3A3A3C)))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onSend, enabled = hasText, modifier = Modifier.size(46.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = if (hasText) TextOnBubble else TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun WorkspaceChip(label: String, selected: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) TextPrimary.copy(alpha = 0.12f) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            StatusDot(GreenSuccess)
            Spacer(modifier = Modifier.width(7.dp))
            Icon(Icons.Default.Folder, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(7.dp))
        } else {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(7.dp))
        }
        Text(label, color = if (selected) TextPrimary else TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ToolbarIcon(
    icon: ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit = {}
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
    }
}
