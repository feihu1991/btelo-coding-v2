package com.btelo.coding.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.btelo.coding.domain.model.Message
import com.btelo.coding.ui.theme.AiBubbleDark
import com.btelo.coding.ui.theme.BubbleGradientEnd
import com.btelo.coding.ui.theme.BubbleGradientStart
import com.btelo.coding.ui.theme.CodeBlockBg
import com.btelo.coding.ui.theme.CodeBlockBorder
import com.btelo.coding.ui.theme.GreenSuccess
import com.btelo.coding.ui.theme.TextOnBubble
import com.btelo.coding.ui.theme.TextPrimary
import com.btelo.coding.ui.theme.TextSecondary
import com.btelo.coding.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val UserBubbleShape = RoundedCornerShape(14.dp, 14.dp, 3.dp, 14.dp)
private val AiBubbleShape = RoundedCornerShape(14.dp, 14.dp, 14.dp, 3.dp)

private val UserBubbleGradient = Brush.linearGradient(
    colors = listOf(BubbleGradientStart, BubbleGradientEnd)
)

@Composable
fun MessageBubble(message: Message) {
    val isUser = message.isFromUser

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier
                .padding(
                    start = if (isUser) 60.dp else 12.dp,
                    end = if (isUser) 12.dp else 60.dp,
                    top = 4.dp,
                    bottom = 2.dp
                ),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (isUser) {
                Box(
                    modifier = Modifier
                        .clip(UserBubbleShape)
                        .background(brush = UserBubbleGradient)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = message.content,
                        color = TextOnBubble,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            } else {
                val parts = parseCodeBlocks(message.content)
                if (parts.size == 1 && parts.first() is TextPart) {
                    Box(
                        modifier = Modifier
                            .clip(AiBubbleShape)
                            .background(AiBubbleDark)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = message.content,
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .clip(AiBubbleShape)
                            .background(AiBubbleDark)
                    ) {
                        parts.forEach { part ->
                            when (part) {
                                is CodePart -> CodeBlock(part.code, part.language)
                                is TextPart -> {
                                    if (part.text.isNotBlank()) {
                                        Text(
                                            text = part.text,
                                            color = TextPrimary,
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 22.sp,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Timestamp
        Text(
            text = formatTimestamp(message.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            modifier = Modifier.padding(
                start = if (isUser) 12.dp else 18.dp,
                end = if (isUser) 18.dp else 12.dp,
                top = 2.dp,
                bottom = 4.dp
            )
        )
    }
}

@Composable
fun CodeBlock(code: String, language: String = "") {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CodeBlockBg)
            .then(
                Modifier.background(CodeBlockBg, RoundedCornerShape(8.dp))
            )
    ) {
        // Code block header with language and copy button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.ifBlank { "code" },
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(code))
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Code content
        Text(
            text = code,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun AiStreamingBubble(partialContent: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(modifier = Modifier.fillMaxWidth(0.82f)) {
            Column(
                modifier = Modifier
                    .clip(AiBubbleShape)
                    .background(AiBubbleDark)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = partialContent,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Animated blinking cursor
                    Text(
                        text = "\u258B",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "\u25CF Claude 正在回复...",
                        color = BubbleGradientStart,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AskCard(
    question: String,
    options: List<String>,
    selectedIndex: Int = -1,
    onOptionSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(modifier = Modifier.fillMaxWidth(0.82f)) {
            Column(
                modifier = Modifier
                    .clip(AiBubbleShape)
                    .background(AiBubbleDark)
                    .padding(12.dp)
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                options.forEachIndexed { index, option ->
                    val isSelected = index == selectedIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) Color(0x173B82F6)
                                else Color.Transparent
                            )
                            .clickable { onOptionSelected(index) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) Color(0xFF60A5FA)
                            else TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Text(
                                text = "\u2713",
                                color = GreenSuccess,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PreviewLink(url: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x143B82F6))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = url,
            color = Color(0xFF60A5FA),
            style = MaterialTheme.typography.bodySmall,
            textDecoration = TextDecoration.Underline
        )
    }
}

// --- helpers ---

private sealed class ContentPart
private class TextPart(val text: String) : ContentPart()
private class CodePart(val code: String, val language: String = "") : ContentPart()

private fun parseCodeBlocks(content: String): List<ContentPart> {
    val parts = mutableListOf<ContentPart>()
    val regex = Regex("```(\\w*)\\n?([\\s\\S]*?)```")
    var lastEnd = 0
    regex.findAll(content).forEach { match ->
        if (match.range.first > lastEnd) {
            val textBefore = content.substring(lastEnd, match.range.first).trim()
            if (textBefore.isNotBlank()) {
                parts.add(TextPart(textBefore))
            }
        }
        val language = match.groupValues[1].trim()
        val code = match.groupValues[2].trim()
        parts.add(CodePart(code, language))
        lastEnd = match.range.last + 1
    }
    if (lastEnd < content.length) {
        val remaining = content.substring(lastEnd).trim()
        if (remaining.isNotBlank()) {
            parts.add(TextPart(remaining))
        }
    }
    return parts.ifEmpty { listOf(TextPart(content)) }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
