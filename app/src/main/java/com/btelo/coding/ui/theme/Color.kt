package com.btelo.coding.ui.theme

import androidx.compose.ui.graphics.Color

// Dark theme (Btelo Coding product page spec)
val AppBackground = Color(0xFF0A0A0A)
val CardSurface = Color(0xFF111114)
val CardElevated = Color(0xFF1A1D21)
val InputSurface = Color(0xFF111114)

// User bubble gradient (green, per product page)
val BubbleGradientStart = Color(0xFF22C55E)  // green-500
val BubbleGradientEnd = Color(0xFF16A34A)    // green-600

// AI bubble
val AiBubbleDark = Color(0xFF1A1D21)

// Text
val TextPrimary = Color(0xFFF5F5F7)
val TextSecondary = Color(0x99F5F5F7)       // rgba(245,245,247,0.6)
val TextTertiary = Color(0x59F5F5F7)        // rgba(245,245,247,0.35)
val TextOnBubble = Color(0xFFFFFFFF)

// Status colors
val GreenSuccess = Color(0xFF22C55E)
val RedError = Color(0xFFEF4444)
val WarningAmber = Color(0xFFF59E0B)
val BlueInfo = Color(0xFF3B82F6)

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
