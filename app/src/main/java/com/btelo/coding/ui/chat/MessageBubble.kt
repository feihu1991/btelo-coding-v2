package com.btelo.coding.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.btelo.coding.domain.model.Message
import com.btelo.coding.domain.model.OutputType
import com.btelo.coding.ui.theme.AiBubbleDark
import com.btelo.coding.ui.theme.BubbleGradientEnd
import com.btelo.coding.ui.theme.BubbleGradientStart
import com.btelo.coding.ui.theme.CardSurface
import com.btelo.coding.ui.theme.CodeBlockBg
import com.btelo.coding.ui.theme.RedError
import com.btelo.coding.ui.theme.TextOnBubble
import com.btelo.coding.ui.theme.TextPrimary
import com.btelo.coding.ui.theme.TextSecondary
import com.btelo.coding.ui.theme.TextTertiary
import com.btelo.coding.ui.theme.ThinkingPurple
import com.btelo.coding.ui.theme.WarningAmber

private val UserBubbleShape = RoundedCornerShape(14.dp, 14.dp, 3.dp, 14.dp)
private val AiBubbleShape = RoundedCornerShape(14.dp, 14.dp, 14.dp, 3.dp)

private val UserBubbleGradient = Brush.linearGradient(
    colors = listOf(BubbleGradientStart, BubbleGradientEnd)
)

@Composable
fun MessageBubble(message: Message) {
    val isUser = message.isFromUser
    val clipboardManager = LocalClipboardManager.current
    
    // BTELO Coding v2: Handle different output types
    val outputType = message.outputType
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Render based on output type
        when {
            isUser -> UserBubble(message, clipboardManager)
            outputType == OutputType.TOOL_CALL -> ToolCallBubble(message, clipboardManager)
            outputType == OutputType.FILE_OP -> FileOpBubble(message, clipboardManager)
            outputType == OutputType.THINKING -> ThinkingBubble(message, clipboardManager)
            outputType == OutputType.ERROR -> ErrorBubble(message, clipboardManager)
            outputType == OutputType.SYSTEM -> SystemBubble(message, clipboardManager)
            else -> AiResponseBubble(message, clipboardManager)
        }

        // Action buttons row
        if (!isUser) {
            ActionButtonsRow(message, clipboardManager)
        }
    }
}

@Composable
private fun UserBubble(message: Message, clipboardManager: androidx.compose.ui.platform.ClipboardManager) {
    Row(
        modifier = Modifier
            .padding(
                start = 60.dp,
                end = 12.dp,
                top = 4.dp,
                bottom = 2.dp
            ),
        horizontalArrangement = Arrangement.End
    ) {
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
    }
}

@Composable
private fun AiResponseBubble(message: Message, clipboardManager: androidx.compose.ui.platform.ClipboardManager) {
    Row(
        modifier = Modifier
            .padding(
                start = 12.dp,
                end = 60.dp,
                top = 4.dp,
                bottom = 2.dp
            ),
        horizontalArrangement = Arrangement.Start
    ) {
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

@Composable
private fun ToolCallBubble(message: Message, clipboardManager: androidx.compose.ui.platform.ClipboardManager) {
    var isExpanded by remember { mutableStateOf(false) }
    val metadata = message.metadata
    
    Row(
        modifier = Modifier
            .padding(
                start = 12.dp,
                end = 60.dp,
                top = 4.dp,
                bottom = 2.dp
            ),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(CardSurface)
                .border(1.dp, BubbleGradientStart.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                .clickable { isExpanded = !isExpanded }
                .padding(12.dp)
        ) {
            // Tool call header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Tool",
                    tint = BubbleGradientStart,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = metadata?.toolName ?: "Tool Call",
                    color = BubbleGradientStart,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.Refresh else Icons.Default.PlayArrow,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            // Command or summary
            if (!message.content.isNullOrBlank()) {
                Text(
                    text = message.content,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Expandable details
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = TextTertiary.copy(alpha = 0.2f))
                    
                    metadata?.command?.let { cmd ->
                        DetailRow("Command:", cmd)
                    }
                    metadata?.filePath?.let { path ->
                        DetailRow("File:", path)
                    }
                    metadata?.toolType?.let { type ->
                        DetailRow("Type:", type)
                    }
                }
            }
        }
    }
}

@Composable
private fun FileOpBubble(message: Message, clipboardManager: androidx.compose.ui.platform.ClipboardManager) {
    var isExpanded by remember { mutableStateOf(false) }
    val metadata = message.metadata
    val fileOpType = metadata?.fileOpType ?: "file"
    
    val (icon, color) = when (fileOpType.lowercase()) {
        "read" -> Icons.Default.Description to TextSecondary
        "write" -> Icons.Default.Description to BubbleGradientStart
        "edit" -> Icons.Default.Code to WarningAmber
        "delete" -> Icons.Default.Error to RedError
        else -> Icons.Default.Description to TextSecondary
    }
    
    Row(
        modifier = Modifier
            .padding(
                start = 12.dp,
                end = 60.dp,
                top = 4.dp,
                bottom = 2.dp
            ),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(CardSurface.copy(alpha = 0.5f))
                .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                .clickable { isExpanded = !isExpanded }
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = "File",
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${fileOpType.replaceFirstChar { it.uppercase() }}: ${metadata?.filePath ?: "Unknown"}",
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
            
            if (isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = TextTertiary.copy(alpha = 0.2f))
                    Text(
                        text = message.content,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThinkingBubble(message: Message, clipboardManager: androidx.compose.ui.platform.ClipboardManager) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .padding(
                start = 12.dp,
                end = 60.dp,
                top = 4.dp,
                bottom = 2.dp
            ),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(ThinkingPurple.copy(alpha = 0.1f))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(ThinkingPurple.copy(alpha = 0.5f), ThinkingPurple.copy(alpha = 0.2f))
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable { isExpanded = !isExpanded }
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Thinking",
                    tint = ThinkingPurple,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Thinking...",
                    color = ThinkingPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (isExpanded) "Hide" else "Show",
                    color = TextTertiary,
                    fontSize = 10.sp
                )
            }
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                SelectionContainer {
                    Text(
                        text = message.content,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorBubble(message: Message, clipboardManager: androidx.compose.ui.platform.ClipboardManager) {
    Row(
        modifier = Modifier
            .padding(
                start = 12.dp,
                end = 60.dp,
                top = 4.dp,
                bottom = 2.dp
            ),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(RedError.copy(alpha = 0.1f))
                .border(1.dp, RedError.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    tint = RedError,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Error",
                    color = RedError,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
            Text(
                text = message.content,
                color = TextPrimary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun SystemBubble(message: Message, clipboardManager: androidx.compose.ui.platform.ClipboardManager) {
    Row(
        modifier = Modifier
            .padding(
                start = 12.dp,
                end = 60.dp,
                top = 4.dp,
                bottom = 2.dp
            ),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(TextTertiary.copy(alpha = 0.1f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = message.content,
                color = TextTertiary,
                fontSize = 12.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = TextTertiary,
            fontSize = 11.sp,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = value,
            color = TextSecondary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ActionButtonsRow(message: Message, clipboardManager: androidx.compose.ui.platform.ClipboardManager) {
    Row(
        modifier = Modifier.padding(
            start = 18.dp,
            end = 12.dp,
            top = 2.dp,
            bottom = 4.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        IconButton(
            onClick = { clipboardManager.setText(AnnotatedString(message.content)) },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "复制",
                tint = TextTertiary,
                modifier = Modifier.size(14.dp)
            )
        }
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
    ) {
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
                onClick = { clipboardManager.setText(AnnotatedString(code)) },
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

        SelectionContainer {
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
}

@Composable
fun AiStreamingBubble(partialContent: String) {
    val infiniteTransition = rememberInfiniteTransition()
    val dotAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse)
    )
    val dotAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400, 150), RepeatMode.Reverse)
    )
    val dotAlpha3 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400, 300), RepeatMode.Reverse)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(modifier = Modifier.fillMaxWidth(0.85f)) {
            Column(
                modifier = Modifier
                    .clip(AiBubbleShape)
                    .background(AiBubbleDark)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (partialContent == "…") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val rotationAngle by infiniteTransition.animateFloat(
                            initialValue = 0f, targetValue = 360f,
                            animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Restart)
                        )
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = "Thinking",
                            tint = ThinkingPurple,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(rotationAngle)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "深度思考中…",
                            color = ThinkingPurple,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text(
                        text = partialContent,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
                if (partialContent != "…") {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "▋",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "● Claude 正在回复...",
                        color = BubbleGradientStart,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                }
            }
        }
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
