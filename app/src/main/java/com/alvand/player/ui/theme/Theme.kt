package com.alvand.player.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** اسکیم پایه مینیمال (فالبک وقتی کاور/رنگی در دسترس نیست) */
val BaseScheme = lightColorScheme(
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

// NOTE: تابع AlvandTheme به DynamicTheme.kt منتقل شد تا accent داینامیک کاور را بگیرد.
// امضای جدید: AlvandTheme(dynamic: DynamicAccent? = null, content: ...)
