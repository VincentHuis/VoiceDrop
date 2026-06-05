package com.vincent.voicedrop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = SteelAzure,
    onPrimary = Color.White,
    primaryContainer = BalticBlue,
    onPrimaryContainer = Color.White,
    secondary = BalticBlue,
    onSecondary = Color.White,
    tertiary = AmberGold,
    onTertiary = InkDark,
    error = VibrantCoral,
    onError = Color.White,
    background = Color.White,
    onBackground = InkDark,
    surface = Color.White,
    onSurface = InkDark,
    surfaceVariant = Platinum,
    onSurfaceVariant = InkDark,
)

private val DarkColors = darkColorScheme(
    primary = BalticBlue,
    onPrimary = Color.White,
    primaryContainer = SteelAzure,
    onPrimaryContainer = Color.White,
    secondary = AmberGold,
    onSecondary = InkDark,
    tertiary = VibrantCoral,
    onTertiary = Color.White,
    error = VibrantCoral,
    onError = Color.White,
    background = SurfaceDark,
    onBackground = Platinum,
    surface = SurfaceDark,
    onSurface = Platinum,
    surfaceVariant = InkDark,
    onSurfaceVariant = Platinum,
)

@Composable
fun VoicedropTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
