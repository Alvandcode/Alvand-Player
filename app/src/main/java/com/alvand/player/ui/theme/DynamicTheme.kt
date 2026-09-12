package com.alvand.player.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
fun dynamicBackgroundBrush(accent: DynamicAccent, base: Color = MonoBg): Brush =
    Brush.verticalGradient(
        listOf(
            accent.accentDark.copy(alpha = 0.28f),
            accent.accent.copy(alpha = 0.10f),
            base
        )
    )

/** رنگ کاور → accent قابل‌استفاده در UI (روشن/تیره) */
fun sanitizeAccent(raw: Color, dark: Boolean = false): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(raw.toArgb(), hsl)
    // اشباع و روشنایی را در بازه خوشگل نگه دار
    hsl[1] = hsl[1].coerceIn(0.45f, 0.95f)
    hsl[2] = if (dark) hsl[2].coerceIn(0.55f, 0.80f) else hsl[2].coerceIn(0.28f, 0.55f)
    return Color(ColorUtils.HSLToColor(hsl))
}

/**
 * accent را از کاور آهنگ فعلی می‌خواند (با کش داخل [Artwork]).
 * تغییر آهنگ → تغییر نرم رنگ‌ها در UI (با animateColorAsState در محل مصرف).
 */
@Composable
fun rememberDynamicAccent(song: Song?, dark: Boolean = false): State<DynamicAccent> {
    val ctx = LocalContext.current
    // فالبک را با کلید dark نگه دار تا موقع سوییچ تم فلش نزند
    val fallback = remember(dark) {
        if (dark) DynamicAccent(AlvandLavender, ArtDark2, fromArtwork = false)
        else DynamicAccent.Fallback
    }
    // state پایدار (نه ساخت State جدید در هر recompose که stability را می‌شکست)
    val holder = remember(dark) { mutableStateOf(fallback) }
    LaunchedEffect(song?.id, dark) {
        val id = song?.id
        if (song == null || id == null) {
            holder.value = fallback
        } else {
            // Artwork.colors خودش از کش load استفاده می‌کند — دوباره دیکد نمی‌شود
            val c = Artwork.colors(song, ctx)
            holder.value = if (c == null || c.dominant == android.graphics.Color.BLACK) {
                fallback
            } else {
                val raw = Color(c.dominant)
                val vib = sanitizeAccent(raw, dark)
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
    return holder
}

/** رنگ متحرک نرم بین دو accent (برای تعویض آهنگ بدون پرش) */
@Composable
fun DynamicAccent.animated(): DynamicAccent {
    // اگر از کاور نیامده، انیمیشن بیهوده اجرا نکن (جلوگیری از لوپ recompose)
    if (!fromArtwork) return this
    val a by animateColorAsState(accent, label = "dynAccent")
    val d by animateColorAsState(accentDark, label = "dynAccentDark")
    return remember(a, d) { DynamicAccent(a, d, fromArtwork = true) }
}

/**
 * تم الوند با accent داینامیک + حالت روشن/تیره.
 * اسکیم روشن هویت مینیمال را نگه می‌دارد؛ تیره نسخه OLED-دوست همان است.
 */
@Composable
fun AlvandTheme(
    dynamic: DynamicAccent? = null,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val pal = if (darkTheme) DarkPalette else LightPalette
    val accent = dynamic?.accent ?: if (darkTheme) AlvandLavender else MonoInk
    val scheme = if (darkTheme) darkColorScheme(
        primary = accent,
        onPrimary = Color(0xFF0C0C10),
        secondary = accent,
        onSecondary = Color(0xFF0C0C10),
        background = pal.bg,
        onBackground = pal.ink,
        surface = pal.card,
        onSurface = pal.ink,
        surfaceVariant = pal.card,
        onSurfaceVariant = pal.sub,
        outline = pal.line
    ) else lightColorScheme(
        primary = accent,
        onPrimary = Color.White,
        secondary = accent,
        onSecondary = Color.White,
        background = pal.bg,
        onBackground = pal.ink,
        surface = pal.card,
        onSurface = pal.ink,
        surfaceVariant = pal.card,
        onSurfaceVariant = pal.sub,
        outline = pal.line
    )
    CompositionLocalProvider(LocalAP provides pal) {
        MaterialTheme(
            colorScheme = scheme,
            typography = MaterialTheme.typography,
            content = content
        )
    }
}

/** مودیفایر پس‌زمینه گرادیانی داینامیک */
@Composable
fun Modifier.dynamicBackground(accent: DynamicAccent, base: Color = LocalAP.current.bg): Modifier =
    background(dynamicBackgroundBrush(accent, base))
