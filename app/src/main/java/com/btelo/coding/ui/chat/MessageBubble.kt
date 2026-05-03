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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Terminal
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
import androidx.compose.ui.graphics.Color
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
import com.btelo.coding.ui.theme.AppBackground
import com.btelo.coding.ui.theme.BubbleGradientStart
import com.btelo.coding.ui.theme.CardSurface
import com.btelo.coding.ui.theme.CodeBlockBg
import com.btelo.coding.ui.theme.GreenSuccess
import com.btelo.coding.ui.theme.RedError
import com.btelo.coding.ui.theme.TextOnBubble
import com.btelo.coding.ui.theme.TextPrimary
import com.btelo.coding.ui.theme.TextSecondary
import com.btelo.coding.ui.theme.TextTertiary
import com.btelo.coding.ui.theme.ThinkingPurple
import com.btelo.coding.ui.theme.WarningAmber

private val UserBubbleShape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
private val AiBubbleShape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)

@Composable
fun MessageBubble(message: Message) {
    when {
        message.isFromUser -> UserBubble(message)
        message.outputType == OutputType.TOOL_CALL -> ToolEventBubble(message)
        message.outputType == OutputType.FILE_OP -> ToolEventBubble(message)
        message.outputType == OutputType.THINKING -> ThinkingEventBubble(message)
        message.outputType == OutputType.ERROR -> ErrorBubble(message)
        message.outputType == OutputType.SYSTEM -> SystemBubble(message)
        else -> AiBubble(message)
    }
}

@Composable
private fun UserBubble(message: Message) {
    val clipboard = LocalClipboardManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 64.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        SelectionContainer {
            Text(
                text = message.content,
                color = TextOnBubble,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 21.sp,
                modifier = Modifier
                    .clip(UserBubbleShape)
                    .background(GreenSuccess)
                    .clickable { clipboard.setText(AnnotatedString(message.content)) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun AiBubble(message: Message) {
    val clipboard = LocalClipboardManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 48.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .clip(AiBubbleShape)
                .background(AiBubbleDark)
                .padding(vertical = 2.dp)
        ) {
            parseCodeBlocks(message.content).forEach { part ->
                when (part) {
                    is CodePart -> CodeBlock(part.code, part.language)
                    is TextPart -> SelectionContainer {
                        Text(
                            text = part.text,
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp,
                            modifier = Modifier
                                .clickable { clipboard.setText(AnnotatedString(message.content)) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolEventBubble(message: Message) {
    var expanded by remember { mutableStateOf(false) }
    val metadata = message.metadata
    val isFile = message.outputType == OutputType.FILE_OP
    val color = if (isFile) WarningAmber else BubbleGradientStart
    val label = when {
        isFile -> metadata?.fileOpType?.replaceFirstChar { it.uppercase() } ?: "File"
        !metadata?.toolName.isNullOrBlank() -> metadata?.toolName ?: "Tool"
        else -> "Tool"
    }
    val detail = metadata?.command ?: metadata?.filePath ?: message.content

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 48.dp, top = 3.dp, bottom = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(CardSurface.copy(alpha = 0.72f))
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isFile) Icons.Default.Description else Icons.Default.Build,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = label,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = detail,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = TextTertiary.copy(alpha = 0.22f))
                    ToolDetail("Command", metadata?.command)
                    ToolDetail("File", metadata?.filePath)
                    ToolDetail("Type", metadata?.toolType ?: metadata?.fileOpType)
                    if (message.content.isNotBlank()) {
                        SelectionContainer {
                            Text(
                                text = message.content,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolDetail(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(modifier = Modifier.padding(top = 6.dp)) {
        Text(label, color = TextTertiary, fontSize = 11.sp, modifier = Modifier.width(56.dp))
        Text(value, color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ThinkingEventBubble(message: Message) {
    var expanded by remember { mutableStateOf(false) }
    val toolNames = message.metadata?.toolNames.orEmpty()
    val summary = if (toolNames.isNotEmpty()) {
        "${toolNames.size} tool call${if (toolNames.size == 1) "" else "s"}"
    } else {
        "Thinking"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(CardSurface.copy(alpha = 0.55f))
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = ThinkingPurple,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(summary, color = TextSecondary, fontSize = 12.sp)
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                SelectionContainer {
                    Text(
                        text = message.content,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorBubble(message: Message) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 48.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .clip(AiBubbleShape)
                .background(RedError.copy(alpha = 0.12f))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Error, contentDescription = null, tint = RedError, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Error", color = RedError, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(message.content, color = TextPrimary, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun SystemBubble(message: Message) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = message.content,
            color = TextTertiary,
            fontSize = 12.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(CardSurface.copy(alpha = 0.45f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun CodeBlock(code: String, language: String = "") {
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CodeBlockBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.ifBlank { "code" },
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { clipboard.setText(AnnotatedString(code)) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy code", tint = TextSecondary, modifier = Modifier.size(14.dp))
            }
        }
        SelectionContainer {
            Text(
                text = code,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun ThinkingBox(session: ThinkingSession) {
    var expanded by remember { mutableStateOf(false) }
    var expandedTypes by remember { mutableStateOf(setOf<ThinkingMessageType>()) }
    val transition = rememberInfiniteTransition(label = "tool-rotation")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
        label = "tool-rotation"
    )
    val statusAlpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "status-alpha"
    )
    val toolCount = session.messages.count {
        it.type == ThinkingMessageType.TOOL_CALL || it.type == ThinkingMessageType.FILE_OP
    }
    val isActive = session.isActive
    val isCompleted = session.isCompleted
    val latest = when {
        isCompleted -> "Completed - ${session.messages.size} steps"
        isActive -> session.currentMessage.ifBlank { "Waiting for Claude" }
        else -> "Waiting for Claude"
    }

    // Group messages by type for collapsible sections
    val groupedMessages = session.messages.groupBy { it.type }
    val typeOrder = listOf(
        ThinkingMessageType.THINKING,
        ThinkingMessageType.TOOL_CALL,
        ThinkingMessageType.FILE_OP,
        ThinkingMessageType.ERROR,
        ThinkingMessageType.SYSTEM
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(CardSurface.copy(alpha = 0.7f))
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Header row with Lightbulb icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(ThinkingPurple.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = if (isCompleted) GreenSuccess else ThinkingPurple,
                        modifier = Modifier
                            .size(15.dp)
                            .rotate(if (isActive) rotation else 0f)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (expanded) "Thinking Process" else latest,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (toolCount > 0) {
                    Text(
                        text = "$toolCount tools",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Status ticker outside the collapsed area (visible when active and not expanded)
            if (!expanded && isActive) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = latest,
                    color = TextSecondary.copy(alpha = statusAlpha),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Expanded view with per-type collapsible groups
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                SelectionContainer {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        HorizontalDivider(color = TextTertiary.copy(alpha = 0.22f))
                        typeOrder.forEach { type ->
                            val messages = groupedMessages[type] ?: return@forEach
                            val (icon, tint, label) = when (type) {
                                ThinkingMessageType.THINKING -> Triple(Icons.Default.Lightbulb, ThinkingPurple, "Thinking")
                                ThinkingMessageType.TOOL_CALL -> Triple(Icons.Default.Build, BubbleGradientStart, "Tool Calls")
                                ThinkingMessageType.FILE_OP -> Triple(Icons.Default.Description, WarningAmber, "File Operations")
                                ThinkingMessageType.ERROR -> Triple(Icons.Default.Error, RedError, "Errors")
                                ThinkingMessageType.SYSTEM -> Triple(Icons.Default.Terminal, TextSecondary, "System")
                            }
                            val isTypeExpanded = type in expandedTypes

                            // Type header (collapsible)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        expandedTypes = if (isTypeExpanded) {
                                            expandedTypes - type
                                        } else {
                                            expandedTypes + type
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = tint,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    color = tint,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${messages.size}",
                                    color = TextTertiary,
                                    fontSize = 11.sp
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (isTypeExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Type messages (shown when type is expanded)
                            AnimatedVisibility(
                                visible = isTypeExpanded,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Column(modifier = Modifier.padding(start = 20.dp)) {
                                    messages.forEach { msg ->
                                        Text(
                                            text = msg.content,
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiStreamingBubble(partialContent: String) {
    val transition = rememberInfiniteTransition(label = "streaming")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "streaming-alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 48.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .clip(AiBubbleShape)
                .background(AiBubbleDark)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (partialContent.isBlank() || partialContent == "...") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(BubbleGradientStart.copy(alpha = alpha))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Claude is working", color = TextSecondary, fontSize = 13.sp)
                }
            } else {
                Text(partialContent, color = TextPrimary, lineHeight = 22.sp)
            }
        }
    }
}

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
            if (textBefore.isNotBlank()) parts.add(TextPart(textBefore))
        }
        val language = match.groupValues[1].trim()
        val code = match.groupValues[2].trim()
        parts.add(CodePart(code, language))
        lastEnd = match.range.last + 1
    }
    if (lastEnd < content.length) {
        val remaining = content.substring(lastEnd).trim()
        if (remaining.isNotBlank()) parts.add(TextPart(remaining))
    }
    return parts.ifEmpty { listOf(TextPart(content)) }
}
