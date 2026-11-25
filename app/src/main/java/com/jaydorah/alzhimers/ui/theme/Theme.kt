package com.jaydorah.alzhimers.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.CompositionLocalProvider

private const val MAX_COMBINED_FONT_SCALE = 2f

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkPrimaryForeground,
    secondary = DarkSecondary,
    onSecondary = DarkSecondaryForeground,
    background = DarkBackground,
    onBackground = DarkForeground,
    surface = DarkCard,
    onSurface = DarkCardForeground,
    surfaceVariant = DarkMuted,
    onSurfaceVariant = DarkMutedForeground,
    outline = DarkBorder,
    error = DarkDestructive,
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightPrimaryForeground,
    secondary = LightSecondary,
    onSecondary = LightSecondaryForeground,
    background = LightBackground,
    onBackground = LightForeground,
    surface = LightCard,
    onSurface = LightCardForeground,
    surfaceVariant = LightMuted,
    onSurfaceVariant = LightMutedForeground,
    outline = LightBorder,
    error = LightDestructive,
)

@Composable
fun YoAlzhimersTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        // The in-app slider multiplies the system font size instead of replacing it, so users who
        // enlarged text in Android accessibility settings keep it. Capped so game boards still fit.
        LocalDensity provides Density(
            density = density.density,
            fontScale = (density.fontScale * fontScale).coerceAtMost(MAX_COMBINED_FONT_SCALE),
        ),
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content,
        )
    }
}
