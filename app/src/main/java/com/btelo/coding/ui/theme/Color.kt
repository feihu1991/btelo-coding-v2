package com.btelo.coding.ui.theme

import androidx.compose.ui.graphics.Color

// Dark theme (PRD Appendix A — video frame analysis spec)
val AppBackground = Color(0xFF1A1A1A)
val CardSurface = Color(0xFF2A2A2A)
val CardElevated = Color(0xFF2A2A2A)
val InputSurface = Color(0xFF2A2A2A)

// User bubble gradient (green, darker for better white text contrast)
val BubbleGradientStart = Color(0xFF1A9E48)  // green-700
val BubbleGradientEnd = Color(0xFF15803D)    // green-800

// AI bubble
val AiBubbleDark = Color(0xFF2A2A2A)

// Send button gradient (purple, per PRD 3.2.5.3)
val SendGradientStart = Color(0xFF8B5CF6)
val SendGradientEnd = Color(0xFF6366F1)

// Text
val TextPrimary = Color(0xFFF5F5F7)
val TextSecondary = Color(0xFF888888)          // PRD Appendix A.3
val TextTertiary = Color(0x8CF5F5F7)           // rgba(245,245,247,0.55) - WCAG AA
val TextOnBubble = Color(0xFFFFFFFF)

// Status colors
val GreenSuccess = Color(0xFF22C55E)
val RedError = Color(0xFFEF4444)
val WarningAmber = Color(0xFFF59E0B)

// Accent colors
val AccentBlue = Color(0xFF3B82F6)             // PRD: selected tab, links
val ThinkingPurple = Color(0xFF8B5CF6)         // PRD: AI thinking state
val SkillTagBorder = Color(0xFFF97316)         // PRD: skill tag border

// Borders
val BorderSubtle = Color(0x14FFFFFF)  // rgba(255,255,255,0.08)
val BorderDefault = Color(0x1AFFFFFF) // rgba(255,255,255,0.10)

// Session dot colors (for session tabs)
val AvatarColors = listOf(
    Color(0xFF8B5CF6), // purple
    Color(0xFF3B82F6), // blue
    Color(0xFF22C55E), // green
    Color(0xFFF59E0B), // amber
    Color(0xFFEF4444), // red
    Color(0xFFEC4899), // pink
    Color(0xFF06B6D4), // cyan
    Color(0xFF6366F1), // indigo
)

// Code block
val CodeBlockBg = Color(0xFF0D1117)
val CodeBlockBorder = Color(0xFF30363D)

// Syntax highlighting (GitHub Dark theme)
val SyntaxKeyword = Color(0xFFFF7B72)    // coral red
val SyntaxString = Color(0xFFA5D6FF)     // light blue
val SyntaxComment = Color(0xFF8B949E)    // muted gray
val SyntaxNumber = Color(0xFF79C0FF)     // cyan
val SyntaxType = Color(0xFFFFA657)       // orange
val SyntaxFunction = Color(0xFFD2A8FF)   // purple
val SyntaxProperty = Color(0xFF7EE787)   // green
