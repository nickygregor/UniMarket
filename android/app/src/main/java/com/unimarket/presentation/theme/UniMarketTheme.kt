package com.unimarket.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val UniOrange = Color(0xFFF58025)
private val UniNavy = Color(0xFF0B1F33)
private val UniBlue = Color(0xFF60A5FA)

private val LightColors = lightColorScheme(
    primary = UniOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0C5),
    onPrimaryContainer = Color(0xFF321200),
    secondary = UniBlue,
    onSecondary = Color.White,
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF132033),
    surface = Color.White,
    onSurface = Color(0xFF132033),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFD7DFEA),
    error = Color(0xFFDC2626)
)

private val DarkColors = darkColorScheme(
    primary = UniOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF6B320B),
    onPrimaryContainer = Color(0xFFFFD9BD),
    secondary = UniBlue,
    onSecondary = UniNavy,
    background = Color(0xFF07111D),
    onBackground = Color(0xFFE7EDF5),
    surface = Color(0xFF0F1B2A),
    onSurface = Color(0xFFE7EDF5),
    surfaceVariant = Color(0xFF18283A),
    onSurfaceVariant = Color(0xFFB8C4D4),
    outline = Color(0xFF34465B),
    error = Color(0xFFFF6B6B)
)

@Composable
fun UniMarketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
