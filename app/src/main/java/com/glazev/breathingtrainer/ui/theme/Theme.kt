package com.glazev.breathingtrainer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = LightCyan,
    secondary = MainTeal,
    tertiary = WaveCyan,
    background = DeepBlue,
    surface = DarkBlueBg,
    onPrimary = DeepBlue,
    onSecondary = White,
    onTertiary = DeepBlue,
    onBackground = White,
    onSurface = White
)

@Composable
fun BreathingTrainerTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
