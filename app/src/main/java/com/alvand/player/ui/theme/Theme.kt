package com.alvand.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = darkColorScheme(
    primary = AlvandPurple,
    secondary = AlvandPink,
    tertiary = AlvandPeach,
    background = AlvandDeep,
    surface = Color(0xFF241547),
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun AlvandTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
