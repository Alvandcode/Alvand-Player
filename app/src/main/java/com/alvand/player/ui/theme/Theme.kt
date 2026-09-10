package com.alvand.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = lightColorScheme(
    primary = MonoInk,
    onPrimary = Color.White,
    secondary = MonoInk,
    onSecondary = Color.White,
    background = MonoBg,
    onBackground = MonoInk,
    surface = Color.White,
    onSurface = MonoInk,
    surfaceVariant = Color.White,
    onSurfaceVariant = MonoSub,
    outline = MonoLine
)

@Composable
fun AlvandTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
