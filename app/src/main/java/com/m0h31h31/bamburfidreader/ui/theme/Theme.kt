package com.m0h31h31.bamburfidreader.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ColorError = Coral.copy(alpha = 0.95f)

private val LightColorScheme = lightColorScheme(
    primary = Ocean,
    onPrimary = Mist,
    secondary = Mint,
    tertiary = Coral,
    background = Mist,
    surface = Frost,
    surfaceVariant = Cloud,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Steel,
    outline = Cloud,
    outlineVariant = Steel.copy(alpha = 0.45f),
    error = ColorError,
    errorContainer = Coral.copy(alpha = 0.18f),
    onErrorContainer = Ink
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkOcean,
    onPrimary = DarkMist,
    secondary = Mint,
    background = DarkMist,
    surface = DarkFrost,
    surfaceVariant = DarkCloud,
    onBackground = DarkInk,
    onSurface = DarkInk,
    onSurfaceVariant = DarkSteel,
    outline = DarkCloud,
    outlineVariant = DarkSteel.copy(alpha = 0.35f),
    error = ColorError
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun BambuRfidReaderTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
