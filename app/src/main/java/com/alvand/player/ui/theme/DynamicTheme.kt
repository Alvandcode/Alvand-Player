package com.alvand.player.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import com.alvand.player.data.Artwork
import com.alvand.player.data.Song

/**
 * پالت داینامیک استخراج‌شده از کاور آهنگ.
 *
 * - اگر کاور/رنگی نباشد، به تم مینیمال سیاه‌سفید ([MonoInk]) برمی‌گردیم.
 * - رنگ accent با HSL تنظیم می‌شود تا همیشه روی [MonoBg] خوانا و زنده باشد
 *   (نه خیلی روشن که دیده نشود، نه خاکستری مرده).
 */
data class DynamicAccent(
    val accent: Color,
    /** نسخه تیره‌تر برای گرادیان پس‌زمینه */
    val accentDark: Color,
    /** آیا accent از کاور واقعی آمده یا فالبک سیاه‌سفید است؟ */
    val fromArtwork: Boolean
) {
    companion object {
        val Fallback = DynamicAccent(MonoInk, ArtDark2, fromArtwork = false)
    }
}

/** گرادیان پس‌زمینه صفحه پلیر بر اساس accent */
fun dynamicBackgroundBrush(accent: DynamicAccent): Brush = Brush.verticalGradient(
    listOf(
        accent.accentDark.copy(alpha = 0.28f),
        accent.accent.copy(alpha = 0.10f),
        MonoBg
    )
)

/** رنگ کاور → accent قابل‌استفاده در UI روشن */
fun sanitizeAccent(raw: Color): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(raw.toArgb(), hsl)
    // اشباع و روشنایی را در بازه خوشگل نگه دار
    val s = hsl[1].coerceIn(0.45f, 0.95f)
    val l = hsl[2].coerceIn(0.28f, 0.55f)
    hsl[1] = s
    hsl[2] = l
    return Color(ColorUtils.HSLToColor(hsl))
}

/**
 * accent را از کاور آهنگ فعلی می‌خواند (با کش داخل [Artwork]).
 * تغییر آهنگ → تغییر نرم رنگ‌ها در UI (با animateColorAsState در محل مصرف).
 */
@Composable
fun rememberDynamicAccent(song: Song?): State<DynamicAccent> {
    val ctx = LocalContext.current
    var accent by remember { mutableStateOf(DynamicAccent.Fallback) }
    LaunchedEffect(song?.id) {
        if (song == null) {
            accent = DynamicAccent.Fallback
        } else {
            val c = Artwork.colors(song, ctx)
            accent = if (c == null || c.dominant == android.graphics.Color.BLACK) {
                DynamicAccent.Fallback
            } else {
                val raw = Color(c.dominant)
                val vib = sanitizeAccent(raw)
                val darkRaw = Color(c.dark)
                val darkHsl = FloatArray(3).also {
                    ColorUtils.colorToHSL(darkRaw.toArgb(), it)
                }
                darkHsl[2] = darkHsl[2].coerceIn(0.12f, 0.30f)
                DynamicAccent(
                    accent = vib,
                    accentDark = Color(ColorUtils.HSLToColor(darkHsl)),
                    fromArtwork = true
                )
            }
        }
    }
    return remember(accent) { mutableStateOf(accent) }
}

/** رنگ متحرک نرم بین دو accent (برای تعویض آهنگ بدون پرش) */
@Composable
fun DynamicAccent.animated(): DynamicAccent {
    val a by animateColorAsState(accent, label = "dynAccent")
    val d by animateColorAsState(accentDark, label = "dynAccentDark")
    return remember(a, d, fromArtwork) { DynamicAccent(a, d, fromArtwork) }
}

/**
 * تم الوند با accent داینامیک.
 * اسکیم روشن می‌ماند (هویت مینیمال) ولی primary/کنترل‌ها هم‌رنگ کاور می‌شوند.
 */
@Composable
fun AlvandTheme(
    dynamic: DynamicAccent? = null,
    content: @Composable () -> Unit
) {
    val accent = dynamic?.accent ?: MonoInk
    val scheme = lightColorScheme(
        primary = accent,
        onPrimary = Color.White,
        secondary = accent,
        onSecondary = Color.White,
        background = MonoBg,
        onBackground = MonoInk,
        surface = Color.White,
        onSurface = MonoInk,
        surfaceVariant = Color.White,
        onSurfaceVariant = MonoSub,
        outline = MonoLine
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

/** مودیفایر پس‌زمینه گرادیانی داینامیک */
@Composable
fun Modifier.dynamicBackground(accent: DynamicAccent): Modifier =
    background(dynamicBackgroundBrush(accent))
