package com.btelo.coding.ui.chat

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

// AST Node types
sealed class MarkdownNode {
    data class Heading(val level: Int, val text: String) : MarkdownNode()
    data class Paragraph(val parts: List<InlineNode>) : MarkdownNode()
    data class BulletList(val items: List<ListItem>) : MarkdownNode()
    data class OrderedList(val items: List<ListItem>) : MarkdownNode()
    data class BlockQuote(val lines: List<String>) : MarkdownNode()
    data class CodeBlock(val code: String, val language: String) : MarkdownNode()
    data class HorizontalRule(val text: String) : MarkdownNode()
}

data class ListItem(val parts: List<InlineNode>)

sealed class InlineNode {
    data class Text(val text: String) : InlineNode()
    data class Bold(val text: String) : InlineNode()
    data class Italic(val text: String) : InlineNode()
    data class BoldItalic(val text: String) : InlineNode()
    data class Code(val code: String) : InlineNode()
    data class Link(val text: String, val url: String) : InlineNode()
    data class Strikethrough(val text: String) : InlineNode()
}

/**
 * Parse markdown content into block-level nodes
 */
fun parseMarkdown(content: String): List<MarkdownNode> {
    if (content.isBlank()) return emptyList()

    val nodes = mutableListOf<MarkdownNode>()
    val lines = content.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // Empty line
        if (line.isBlank()) {
            i++
            continue
        }

        // Code block (fenced)
        if (line.trimStart().startsWith("```")) {
            val language = line.trimStart().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++ // skip closing ```
            nodes.add(MarkdownNode.CodeBlock(codeLines.joinToString("\n"), language))
            continue
        }

        // Horizontal rule
        if (line.matches(Regex("^[-*_]{3,}\\s*$"))) {
            nodes.add(MarkdownNode.HorizontalRule(line))
            i++
            continue
        }

        // Heading
        val headingMatch = Regex("^(#{1,6})\\s+(.+)$").matchEntire(line)
        if (headingMatch != null) {
            val level = headingMatch.groupValues[1].length
            val text = headingMatch.groupValues[2].trim()
            nodes.add(MarkdownNode.Heading(level, text))
            i++
            continue
        }

        // Blockquote
        if (line.startsWith("> ") || line == ">") {
            val quoteLines = mutableListOf<String>()
            while (i < lines.size && (lines[i].startsWith("> ") || lines[i] == ">")) {
                quoteLines.add(lines[i].removePrefix("> ").removePrefix(">"))
                i++
            }
            nodes.add(MarkdownNode.BlockQuote(quoteLines))
            continue
        }

        // Bullet list
        if (line.trimStart().matches(Regex("^[-*+]\\s+.*"))) {
            val items = mutableListOf<ListItem>()
            while (i < lines.size && lines[i].trimStart().matches(Regex("^[-*+]\\s+.*"))) {
                val itemText = lines[i].trimStart().replaceFirst(Regex("^[-*+]\\s+"), "")
                items.add(ListItem(parseInline(itemText)))
                i++
            }
            nodes.add(MarkdownNode.BulletList(items))
            continue
        }

        // Ordered list
        if (line.trimStart().matches(Regex("^\\d+\\.\\s+.*"))) {
            val items = mutableListOf<ListItem>()
            while (i < lines.size && lines[i].trimStart().matches(Regex("^\\d+\\.\\s+.*"))) {
                val itemText = lines[i].trimStart().replaceFirst(Regex("^\\d+\\.\\s+"), "")
                items.add(ListItem(parseInline(itemText)))
                i++
            }
            nodes.add(MarkdownNode.OrderedList(items))
            continue
        }

        // Paragraph (collect consecutive non-empty lines)
        val paragraphLines = mutableListOf<String>()
        while (i < lines.size && lines[i].isNotBlank() &&
            !lines[i].trimStart().startsWith("```") &&
            !lines[i].matches(Regex("^[-*_]{3,}\\s*$")) &&
            !Regex("^#{1,6}\\s+").containsMatchIn(lines[i]) &&
            !lines[i].startsWith("> ") &&
            !lines[i].trimStart().matches(Regex("^[-*+]\\s+.*")) &&
            !lines[i].trimStart().matches(Regex("^\\d+\\.\\s+.*"))
        ) {
            paragraphLines.add(lines[i])
            i++
        }
        if (paragraphLines.isNotEmpty()) {
            nodes.add(MarkdownNode.Paragraph(parseInline(paragraphLines.joinToString("\n"))))
        }
    }

    return nodes
}

/**
 * Parse inline formatting
 */
fun parseInline(text: String): List<InlineNode> {
    val nodes = mutableListOf<InlineNode>()
    var remaining = text

    while (remaining.isNotEmpty()) {
        // Inline code
        val codeMatch = Regex("^`([^`]+)`").find(remaining)
        if (codeMatch != null) {
            nodes.add(InlineNode.Code(codeMatch.groupValues[1]))
            remaining = remaining.substring(codeMatch.range.last + 1)
            continue
        }

        // Bold + Italic
        val boldItalicMatch = Regex("^\\*\\*\\*(.+?)\\*\\*\\*").find(remaining)
        if (boldItalicMatch != null) {
            nodes.add(InlineNode.BoldItalic(boldItalicMatch.groupValues[1]))
            remaining = remaining.substring(boldItalicMatch.range.last + 1)
            continue
        }

        // Bold
        val boldMatch = Regex("^\\*\\*(.+?)\\*\\*").find(remaining)
        if (boldMatch != null) {
            nodes.add(InlineNode.Bold(boldMatch.groupValues[1]))
            remaining = remaining.substring(boldMatch.range.last + 1)
            continue
        }

        // Italic
        val italicMatch = Regex("^\\*(.+?)\\*").find(remaining)
        if (italicMatch != null) {
            nodes.add(InlineNode.Italic(italicMatch.groupValues[1]))
            remaining = remaining.substring(italicMatch.range.last + 1)
            continue
        }

        // Strikethrough
        val strikeMatch = Regex("^~~(.+?)~~").find(remaining)
        if (strikeMatch != null) {
            nodes.add(InlineNode.Strikethrough(strikeMatch.groupValues[1]))
            remaining = remaining.substring(strikeMatch.range.last + 1)
            continue
        }

        // Link
        val linkMatch = Regex("^\\[([^\\]]+)\\]\\(([^)]+)\\)").find(remaining)
        if (linkMatch != null) {
            nodes.add(InlineNode.Link(linkMatch.groupValues[1], linkMatch.groupValues[2]))
            remaining = remaining.substring(linkMatch.range.last + 1)
            continue
        }

        // Plain text (up to next special character)
        val nextSpecial = remaining.indexOfFirst { it in "`*~[" }
        if (nextSpecial > 0) {
            nodes.add(InlineNode.Text(remaining.substring(0, nextSpecial)))
            remaining = remaining.substring(nextSpecial)
        } else if (nextSpecial == 0) {
            // Special char that didn't match any pattern, treat as text
            nodes.add(InlineNode.Text(remaining.first().toString()))
            remaining = remaining.substring(1)
        } else {
            nodes.add(InlineNode.Text(remaining))
            remaining = ""
        }
    }

    return nodes
}

/**
 * Convert inline nodes to AnnotatedString
 */
fun inlineToAnnotatedString(
    parts: List<InlineNode>,
    baseColor: androidx.compose.ui.graphics.Color,
    linkColor: androidx.compose.ui.graphics.Color
): AnnotatedString {
    return buildAnnotatedString {
        parts.forEach { part ->
            when (part) {
                is InlineNode.Text -> append(part.text)
                is InlineNode.Bold -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(part.text)
                    pop()
                }
                is InlineNode.Italic -> {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(part.text)
                    pop()
                }
                is InlineNode.BoldItalic -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                    append(part.text)
                    pop()
                }
                is InlineNode.Code -> {
                    pushStyle(SpanStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 13.sp,
                        background = androidx.compose.ui.graphics.Color(0xFF1E1E1E)
                    ))
                    append(" ${part.code} ")
                    pop()
                }
                is InlineNode.Link -> {
                    pushStyle(SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    ))
                    pushStringAnnotation("URL", part.url)
                    append(part.text)
                    pop()
                    pop()
                }
                is InlineNode.Strikethrough -> {
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    append(part.text)
                    pop()
                }
            }
        }
    }
}
