package com.alvand.player.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.alvand.player.ui.theme.DynamicAccent
import com.alvand.player.ui.theme.LocalAP

/**
 * کارت «لیکویید گلس»: سطح نیمه‌شفاف + برق لبه + هایلایت بالا + سایه نرم.
 * روی هر بکگراندی (گرادیان، عکس، accent) شیشه‌ای دیده می‌شود و در همه APIها کار می‌کند.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val pal = LocalAP.current
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = shape,
        color = pal.glass,
        border = androidx.compose.foundation.BorderStroke(1.dp, pal.glassBorder),
        shadowElevation = 10.dp
    ) {
        Box {
            // برق شیشه از بالا
            Box(
                Modifier.fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(pal.sheen, Color.Transparent.copy(alpha = 0f)),
                            startY = 0f,
                            endY = 320f
                        )
                    )
            )
            content()
        }
    }
}

/**
 * بکگراند چندلایه صفحه پلیر — همیشه زیر کادر قرار می‌گیرد:
 * ۱) رنگ پایه تم ۲) عکس دلخواه کاربر (اختیاری) + حجاب ۳) حباب‌های accent (تار، API 31+)
 */
@Composable
fun PlayerBackground(
    accent: DynamicAccent,
    backgroundUri: String?,
    modifier: Modifier = Modifier
) {
    val pal = LocalAP.current
    Box(modifier.fillMaxSize().background(pal.bg)) {
        // لایه عکس دلخواه
        if (backgroundUri != null) {
            AsyncImage(
                model = backgroundUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // حجاب برای خوانایی متن و یکدستی با تم
            Box(
                Modifier.fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                pal.scrim,
                                pal.scrim.copy(alpha = pal.scrim.alpha * 0.55f),
                                pal.bg.copy(alpha = 0.86f)
                            )
                        )
                    )
            )
        } else {
            // گرادیان accent وقتی عکسی نیست
            Box(
                Modifier.fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accent.accentDark.copy(alpha = if (pal.isDark) 0.55f else 0.28f),
                                accent.accent.copy(alpha = if (pal.isDark) 0.22f else 0.10f),
                                pal.bg
                            )
                        )
                    )
            )
        }
        // حباب‌های نورانی (روی عکس هم می‌نشینند، کم‌رنگ‌تر)
        AmbientBlob(
            color = accent.accent,
            centerFraction = Offset(0.85f, 0.12f),
            radiusFraction = 0.55f,
            alpha = if (backgroundUri != null) 0.20f else 0.30f
        )
        AmbientBlob(
            color = accent.accentDark,
            centerFraction = Offset(0.10f, 0.55f),
            radiusFraction = 0.60f,
            alpha = if (backgroundUri != null) 0.16f else 0.24f
        )
    }
}

/** حباب گرادیانی تار — روی API 31+ واقعاً blur می‌شود، پایین‌تر همان گرادیان نرم است */
@Composable
private fun BoxScope.AmbientBlob(
    color: Color,
    centerFraction: Offset,
    radiusFraction: Float,
    alpha: Float
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val cx = with(density) { (maxWidth * centerFraction.x).toPx() }
        val cy = with(density) { (maxHeight * centerFraction.y).toPx() }
        val r = with(density) { ((maxWidth.coerceAtLeast(maxHeight)) * radiusFraction).toPx() }
        Box(
            Modifier.fillMaxSize()
                .then(if (Build.VERSION.SDK_INT >= 31) Modifier.blur(90.dp) else Modifier)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = alpha),
                            color.copy(alpha = alpha * 0.35f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = r
                    )
                )
        )
    }
}

/** شعاع پیش‌فرض گوشه‌های شیشه */
val GlassCorners: Shape = RoundedCornerShape(24.dp)

/** اسکیریم پایین برای خوانایی متن روی بکگراند */
@Composable
fun BottomScrim(modifier: Modifier = Modifier, height: Dp = 220.dp) {
    val pal = LocalAP.current
    Box(
        modifier
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, pal.bg.copy(alpha = 0.9f))
                )
            )
    )
}
