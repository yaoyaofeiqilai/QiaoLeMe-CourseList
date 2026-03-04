package com.sb.courselist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    secondary = AccentGreen,
    onSecondary = Color.White,
    tertiary = AccentCoral,
    background = MintBg,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    outline = BorderMint,
)

private val DarkScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = AccentGreen,
    tertiary = AccentCoral,
)

@Composable
fun SBCourseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = AppTypography,
        shapes = androidx.compose.material3.Shapes(),
        content = content,
    )
}

