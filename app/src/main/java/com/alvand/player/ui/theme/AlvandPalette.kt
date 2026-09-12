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
    sub = Color(0xFF5E5E66),
    line = Color(0xFFDFDFE5),
    track = Color(0xFFCFCFD6),
    card = Color.White,
    sheet = Color.White.copy(alpha = 0.97f),
    glass = Color.White.copy(alpha = 0.72f),
    glassBorder = Color.White.copy(alpha = 0.85f),
    sheen = Color.White.copy(alpha = 0.55f),
    scrim = Color.Black.copy(alpha = 0.38f),
    isDark = false
)

val DarkPalette = AlvandPalette(
    bg = Color(0xFF0C0C10),
    ink = Color(0xFFF2F2F5),
    sub = Color(0xFFB4B4BE),
    line = Color(0x33FFFFFF),
    track = Color(0x44FFFFFF),
    card = Color(0xFF17171D),
    sheet = Color(0xF517171D),
    glass = Color(0x33FFFFFF),
    glassBorder = Color(0x40FFFFFF),
    sheen = Color(0x2EFFFFFF),
    scrim = Color.Black.copy(alpha = 0.60f),
    isDark = true
)

val LocalAP = staticCompositionLocalOf { LightPalette }
