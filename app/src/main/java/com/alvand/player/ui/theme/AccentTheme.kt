package com.alvand.player.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.alvand.player.data.AccentThemeMode

/**
 * یک تم رنگی: رنگ accent در حالت روشن/تیره به‌علاوهٔ نسخهٔ عمیق‌تر برای گرادیان پس‌زمینه.
 */
@Immutable
data class AccentTheme(
    val id: Int,
    val light: Color,
    val dark: Color,
    val lightDeep: Color,
    val darkDeep: Color
) {
    fun accent(isDark: Boolean): Color = if (isDark) dark else light
    fun deep(isDark: Boolean): Color = if (isDark) darkDeep else lightDeep

    /** آیا تم رنگی است یا مینیمال سیاه‌وسفید؟ */
    val isColored: Boolean get() = id != AccentThemeMode.MONO
}

/** فهرست تم‌های رنگی — ترتیب با [AccentThemeMode] یکی است. */
val AccentThemes: List<AccentTheme> = listOf(
    // مینیمال سیاه‌وسفید (پیش‌فرض) — همان رفتار قبلی
    AccentTheme(
        id = AccentThemeMode.MONO,
        light = MonoInk,
        dark = AlvandLavender,
        lightDeep = MonoInk,
        darkDeep = ArtDark2
    ),
    // سلطنتی — بنفش الوند
    AccentTheme(
        id = AccentThemeMode.ROYAL,
        light = Color(0xFF6C4CF1),
        dark = Color(0xFFB9A7FF),
        lightDeep = Color(0xFF1B1040),
        darkDeep = Color(0xFF160C33)
    ),
    // غروب — نارنجی/صورتی گرم
    AccentTheme(
        id = AccentThemeMode.SUNSET,
        light = Color(0xFFE8590C),
        dark = Color(0xFFFFB088),
        lightDeep = Color(0xFF3A1206),
        darkDeep = Color(0xFF2E0E04)
    ),
    // اقیانوس — فیروزه‌ای/آبی خنک
    AccentTheme(
        id = AccentThemeMode.OCEAN,
        light = Color(0xFF0E7490),
        dark = Color(0xFF67E8F9),
        lightDeep = Color(0xFF04222C),
        darkDeep = Color(0xFF031B23)
    )
)

fun accentThemeAt(id: Int): AccentTheme =
    AccentThemes.firstOrNull { it.id == id } ?: AccentThemes[0]

/** تم رنگی فعال در درخت Compose */
val LocalAccentTheme = staticCompositionLocalOf { AccentThemes[0] }

/**
 * پالت را با رنگ تم آراسته می‌کند تا حالت شیشه‌ای «رنگی» شود.
 * برای تم مینیمال بدون تغییر برمی‌گردد.
 */
fun AlvandPalette.tintedWith(accent: AccentTheme, isDark: Boolean): AlvandPalette {
    if (!accent.isColored) return this
    val a = accent.accent(isDark)
    return copy(
        bg = lerp(bg, a, 0.05f),
        card = lerp(card, a, 0.09f),
        sheet = lerp(sheet, a, 0.06f),
        glass = lerp(glass, a, 0.18f),
        glassBorder = lerp(glassBorder, a, 0.38f),
        sheen = lerp(sheen, a, 0.28f)
    )
}
