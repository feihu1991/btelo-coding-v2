package com.btelo.coding.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BteloDarkColorScheme = darkColorScheme(
    primary = BubbleGradientStart,
    onPrimary = TextOnBubble,
    primaryContainer = BubbleGradientEnd,
    onPrimaryContainer = TextOnBubble,
    secondary = GreenSuccess,
    onSecondary = TextOnBubble,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = RedError,
    onError = TextOnBubble
)

@Composable
fun BteloCodingTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = BteloDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
