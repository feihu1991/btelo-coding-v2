package com.btelo.coding.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.btelo.coding.ui.theme.AccentBlue
import com.btelo.coding.ui.theme.CodeBlockBg
import com.btelo.coding.ui.theme.CodeBlockBorder
import com.btelo.coding.ui.theme.TextPrimary
import com.btelo.coding.ui.theme.TextSecondary
import com.btelo.coding.ui.theme.TextTertiary

/**
 * Claude Code 终端风格的 Markdown 渲染器
 */
@Composable
fun MarkdownContent(
    content: String,
    modifier: Modifier = Modifier,
    textColor: androidx.compose.ui.graphics.Color = TextPrimary,
    baseStyle: TextStyle = MaterialTheme.typography.bodyMedium
) {
    val nodes = remember(content) { parseMarkdown(content) }

    SelectionContainer {
        Column(modifier = modifier) {
            nodes.forEach { node ->
                when (node) {
                    is MarkdownNode.Heading -> MarkdownHeading(node, textColor)
                    is MarkdownNode.Paragraph -> MarkdownParagraph(node, baseStyle, textColor)
                    is MarkdownNode.BulletList -> MarkdownBulletList(node, baseStyle, textColor)
                    is MarkdownNode.OrderedList -> MarkdownOrderedList(node, baseStyle, textColor)
                    is MarkdownNode.BlockQuote -> MarkdownBlockQuote(node, baseStyle, textColor)
                    is MarkdownNode.CodeBlock -> CodeBlock(node.code, node.language)
                    is MarkdownNode.HorizontalRule -> MarkdownHorizontalRule()
                }
            }
        }
    }
}

@Composable
private fun MarkdownHeading(
    node: MarkdownNode.Heading,
    textColor: androidx.compose.ui.graphics.Color
) {
    val (fontSize, fontWeight, topPadding) = when (node.level) {
        1 -> Triple(24.sp, FontWeight.Bold, 16.dp)
        2 -> Triple(20.sp, FontWeight.Bold, 14.dp)
        3 -> Triple(18.sp, FontWeight.SemiBold, 12.dp)
        4 -> Triple(16.sp, FontWeight.SemiBold, 10.dp)
        5 -> Triple(14.sp, FontWeight.Medium, 8.dp)
        6 -> Triple(13.sp, FontWeight.Medium, 6.dp)
        else -> Triple(14.sp, FontWeight.Normal, 6.dp)
    }

    Text(
        text = node.text,
        color = textColor,
        fontSize = fontSize,
        fontWeight = fontWeight,
        lineHeight = (fontSize.value * 1.4).sp,
        modifier = Modifier.padding(top = topPadding, bottom = 4.dp)
    )
}

@Composable
private fun MarkdownParagraph(
    node: MarkdownNode.Paragraph,
    baseStyle: TextStyle,
    textColor: androidx.compose.ui.graphics.Color
) {
    val uriHandler = LocalUriHandler.current
    val annotatedString = remember(node.parts) {
        inlineToAnnotatedString(node.parts, textColor, AccentBlue)
    }

    ClickableText(
        text = annotatedString,
        style = baseStyle.copy(color = textColor, lineHeight = 22.sp),
        onClick = { offset ->
            annotatedString.getStringAnnotations("URL", offset, offset)
                .firstOrNull()?.let { annotation ->
                    try {
                        uriHandler.openUri(annotation.item)
                    } catch (_: Exception) {}
                }
        },
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun MarkdownBulletList(
    node: MarkdownNode.BulletList,
    baseStyle: TextStyle,
    textColor: androidx.compose.ui.graphics.Color
) {
    Column(modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp)) {
        node.items.forEach { item ->
            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                Text(
                    text = "•",
                    color = textColor,
                    style = baseStyle,
                    modifier = Modifier.width(16.dp)
                )
                val annotatedString = remember(item.parts) {
                    inlineToAnnotatedString(item.parts, textColor, AccentBlue)
                }
                val uriHandler = LocalUriHandler.current
                ClickableText(
                    text = annotatedString,
                    style = baseStyle.copy(color = textColor, lineHeight = 22.sp),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations("URL", offset, offset)
                            .firstOrNull()?.let { annotation ->
                                try {
                                    uriHandler.openUri(annotation.item)
                                } catch (_: Exception) {}
                            }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MarkdownOrderedList(
    node: MarkdownNode.OrderedList,
    baseStyle: TextStyle,
    textColor: androidx.compose.ui.graphics.Color
) {
    Column(modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp)) {
        node.items.forEachIndexed { index, item ->
            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                Text(
                    text = "${index + 1}.",
                    color = textColor,
                    style = baseStyle,
                    modifier = Modifier.width(24.dp)
                )
                val annotatedString = remember(item.parts) {
                    inlineToAnnotatedString(item.parts, textColor, AccentBlue)
                }
                val uriHandler = LocalUriHandler.current
                ClickableText(
                    text = annotatedString,
                    style = baseStyle.copy(color = textColor, lineHeight = 22.sp),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations("URL", offset, offset)
                            .firstOrNull()?.let { annotation ->
                                try {
                                    uriHandler.openUri(annotation.item)
                                } catch (_: Exception) {}
                            }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MarkdownBlockQuote(
    node: MarkdownNode.BlockQuote,
    baseStyle: TextStyle,
    textColor: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
            .background(TextTertiary.copy(alpha = 0.1f))
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp)
    ) {
        Column {
            node.lines.forEach { line ->
                Text(
                    text = line,
                    color = TextSecondary,
                    style = baseStyle.copy(
                        fontStyle = FontStyle.Italic,
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun MarkdownHorizontalRule() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = CodeBlockBorder,
        thickness = 1.dp
    )
}

/**
 * 转换 InlineNode 到 AnnotatedString
 */
fun inlineToAnnotatedString(
    parts: List<InlineNode>,
    baseColor: androidx.compose.ui.graphics.Color,
    linkColor: androidx.compose.ui.graphics.Color
): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        parts.forEach { part ->
            when (part) {
                is InlineNode.Text -> append(part.text)
                is InlineNode.Bold -> {
                    pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
                    append(part.text)
                    pop()
                }
                is InlineNode.Italic -> {
                    pushStyle(androidx.compose.ui.text.SpanStyle(fontStyle = FontStyle.Italic))
                    append(part.text)
                    pop()
                }
                is InlineNode.BoldItalic -> {
                    pushStyle(androidx.compose.ui.text.SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic
                    ))
                    append(part.text)
                    pop()
                }
                is InlineNode.Code -> {
                    pushStyle(androidx.compose.ui.text.SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        background = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
                        color = TextPrimary
                    ))
                    append(" ${part.code} ")
                    pop()
                }
                is InlineNode.Link -> {
                    pushStyle(androidx.compose.ui.text.SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    ))
                    pushStringAnnotation("URL", part.url)
                    append(part.text)
                    pop()
                    pop()
                }
                is InlineNode.Strikethrough -> {
                    pushStyle(androidx.compose.ui.text.SpanStyle(
                        textDecoration = TextDecoration.LineThrough
                    ))
                    append(part.text)
                    pop()
                }
            }
        }
    }
}
