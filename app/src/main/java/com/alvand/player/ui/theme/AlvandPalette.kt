package com.alvand.player.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * پالت تم‌دار الوند (روشن/تیره) — پایه «لیکویید گلس»:
 * سطوح شیشه‌ای نیمه‌شفاف + برق لبه + سایه، روی هر بکگراندی می‌نشینند.
 */
@Immutable
data class AlvandPalette(
    val bg: Color,
    val ink: Color,
    val sub: Color,
    val line: Color,
    val track: Color,
    /** کارت مات (دیالوگ‌ها) */
    val card: Color,
    /** بدنه شیت‌ها (تقریباً مات برای خوانایی) */
    val sheet: Color,
    /** سطح شیشه‌ای نیمه‌شفاف (کارت‌ها، چیپ‌ها) */
    val glass: Color,
    /** برق لبه شیشه */
    val glassBorder: Color,
    /** هایلایت بالای شیشه */
    val sheen: Color,
    /** حجاب روی بکگراند دلخواه برای خوانایی متن */
    val scrim: Color,
    val isDark: Boolean
)

val LightPalette = AlvandPalette(
    bg = Color(0xFFF4F4F6),
    ink = Color(0xFF111114),
    sub = Color(0xFF6E6E73),
    line = Color(0xFFE2E2E6),
    track = Color(0xFFD8D8DC),
    card = Color.White,
    sheet = Color.White.copy(alpha = 0.88f),
    glass = Color.White.copy(alpha = 0.55f),
    glassBorder = Color.White.copy(alpha = 0.75f),
    sheen = Color.White.copy(alpha = 0.55f),
    scrim = Color.Black.copy(alpha = 0.32f),
    isDark = false
)

val DarkPalette = AlvandPalette(
    bg = Color(0xFF0C0C10),
    ink = Color(0xFFF2F2F5),
    sub = Color(0xFFA8A8B2),
    line = Color(0x26FFFFFF),
    track = Color(0x30FFFFFF),
    card = Color(0xFF17171D),
    sheet = Color(0xE817171D),
    glass = Color(0x24FFFFFF),
    glassBorder = Color(0x38FFFFFF),
    sheen = Color(0x2EFFFFFF),
    scrim = Color.Black.copy(alpha = 0.55f),
    isDark = true
)

val LocalAP = staticCompositionLocalOf { LightPalette }
