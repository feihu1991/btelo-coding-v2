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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.btelo.coding.ui.theme.AccentBlue
import com.btelo.coding.ui.theme.CodeBlockBorder
import com.btelo.coding.ui.theme.TextPrimary
import com.btelo.coding.ui.theme.TextSecondary
import com.btelo.coding.ui.theme.TextTertiary

/**
 * Main markdown content renderer
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
                    is MarkdownNode.Heading -> MarkdownHeading(node, baseStyle, textColor)
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
    baseStyle: TextStyle,
    textColor: androidx.compose.ui.graphics.Color
) {
    val (fontSize, fontWeight) = when (node.level) {
        1 -> 24.sp to FontWeight.Bold
        2 -> 20.sp to FontWeight.Bold
        3 -> 18.sp to FontWeight.SemiBold
        4 -> 16.sp to FontWeight.SemiBold
        5 -> 14.sp to FontWeight.Medium
        6 -> 13.sp to FontWeight.Medium
        else -> 14.sp to FontWeight.Normal
    }

    Text(
        text = node.text,
        color = textColor,
        fontSize = fontSize,
        fontWeight = fontWeight,
        lineHeight = (fontSize.value * 1.4).sp,
        modifier = Modifier.padding(
            top = if (node.level <= 2) 12.dp else 8.dp,
            bottom = 4.dp
        )
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
        style = baseStyle.copy(color = textColor),
        onClick = { offset ->
            annotatedString.getStringAnnotations("URL", offset, offset)
                .firstOrNull()?.let { annotation ->
                    try {
                        uriHandler.openUri(annotation.item)
                    } catch (_: Exception) {}
                }
        },
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun MarkdownBulletList(
    node: MarkdownNode.BulletList,
    baseStyle: TextStyle,
    textColor: androidx.compose.ui.graphics.Color
) {
    Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
        node.items.forEach { item ->
            Row {
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
                    style = baseStyle.copy(color = textColor),
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
    Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
        node.items.forEachIndexed { index, item ->
            Row {
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
                    style = baseStyle.copy(color = textColor),
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
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
            .background(TextTertiary.copy(alpha = 0.1f))
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp)
    ) {
        Column {
            node.lines.forEach { line ->
                Text(
                    text = line,
                    color = TextSecondary,
                    style = baseStyle.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun MarkdownHorizontalRule() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = CodeBlockBorder,
        thickness = 0.5.dp
    )
}
