package com.vincent.voicedrop.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

private val VoiceDropColors = ColorScheme(
    primary = BalticBlue,
    onPrimary = Color.White,
    primaryContainer = SteelAzure,
    onPrimaryContainer = Color.White,
    secondary = AmberGold,
    onSecondary = InkDark,
    tertiary = VibrantCoral,
    onTertiary = Color.White,
    background = Color.Black,
    onBackground = Platinum,
)

@Composable
fun VoicedropTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VoiceDropColors,
        content = content
    )
}
