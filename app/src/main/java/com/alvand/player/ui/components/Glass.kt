package com.alvand.player.ui.components

import android.graphics.PathMeasure
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.data.Artwork
import com.alvand.player.data.Song
import com.alvand.player.ui.theme.*
import kotlin.math.*

fun fmtTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val s = ms / 1000
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

/** کاور آهنگ: عکس امبدد، وگرنه جای‌خالی تیره */
@Composable
fun ArtImage(song: Song?, modifier: Modifier = Modifier, corners: Shape) {
    val ctx = LocalContext.current
    var bmp by remember(song?.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(song?.id) {
        bmp = null
        song?.let { bmp = Artwork.load(it, ctx) }
    }
    Box(
        modifier
            .clip(corners)
            .background(Brush.linearGradient(listOf(ArtDark1, ArtDark2)))
    ) {
        if (bmp != null) {
            Image(
                bmp!!.asImageBitmap(), null, Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "♪", color = Color.White.copy(0.85f),
                    fontSize = 64.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** میله‌های کوچک مشکیِ در حال پخش */
@Composable
fun MiniBars(
    playing: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MonoInk
) {
    val inf = rememberInfiniteTransition(label = "mb")
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(4) { i ->
            val h by inf.animateFloat(
                4f, (8 + (i * 7 % 10)).toFloat(),
                infiniteRepeatable(tween(350 + (i * 53 % 300), easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "m$i"
            )
            Box(
                Modifier.width(3.dp).height(if (playing) h.dp else 4.dp)
                    .background(color)
            )
        }
    }
}

/**
 * ردیف کنترل‌ها: شافل | قبلی | پلی بزرگ | بعدی | تکرار (۳ حالت: خاموش/همه/تک‌آهنگ)
 *
 * جهت ردیف عمداً همیشه چپ‌به‌راست است تا با عوض شدن زبان (راست‌به‌چپ)
 * جای دکمه‌ها عوض نشود و حافظه عضلانی کاربر به‌هم نریزد.
 */
@Composable
fun ControlsRow(
    playing: Boolean,
    shuffle: Boolean,
    repeatMode: Int,
    onShuffle: () -> Unit,
    onPrev: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    big: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = MonoInk
) {
    val pal = LocalAP.current
    val main = if (big) 76.dp else 58.dp
    val sub = if (big) 30.dp else 26.dp
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onShuffle, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Shuffle, null, tint = if (shuffle) pal.ink else pal.sub.copy(alpha = 0.5f), modifier = Modifier.size(sub))
        }
        IconButton(onClick = onPrev, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.SkipPrevious, null, tint = pal.ink, modifier = Modifier.size(34.dp))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            FilledIconButton(
                onClick = onToggle,
                modifier = Modifier.size(main),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = accent, contentColor = pal.bg)
            ) {
                Icon(
                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null, modifier = Modifier.size(main * 0.45f)
                )
            }
        }
        IconButton(onClick = onNext, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.SkipNext, null, tint = pal.ink, modifier = Modifier.size(34.dp))
        }
        IconButton(onClick = onRepeat, modifier = Modifier.weight(1f)) {
            Icon(
                if (repeatMode == 2) Icons.Default.RepeatOne else Icons.Default.Repeat,
                null, tint = if (repeatMode == 0) pal.sub.copy(alpha = 0.5f) else pal.ink,
                modifier = Modifier.size(sub)
            )
        }
    }
    }
}

/** پنل آرت کشیده با عنوان ماسک‌شده داخل کادر (با فاصله از لبه‌ها)
 *  فرم U: بالا کاملاً چسبیده (گوشه صفر)، پایین خیلی گرد — مثل ماکت */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtPanel(
    song: Song?,
    modifier: Modifier = Modifier,
    glow: Color = MonoInk
) {
    val artShape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 190.dp, bottomEnd = 190.dp)
    Box(modifier) {
        ArtImage(
            song, Modifier.fillMaxSize(),
            artShape
        )
        // هاله هم‌رنگ کاور دور آرت (پالت داینامیک)
        Box(
            Modifier.fillMaxSize()
                .border(
                    1.5.dp,
                    glow.copy(alpha = 0.35f),
                    artShape
                )
        )
        // محدوده نامرئی متن: داخل کادر + فاصله از لبه‌ها + برش اضافه
        Column(
            Modifier.fillMaxSize().padding(bottom = 40.dp, start = 30.dp, end = 30.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                song?.title ?: "Alvand Player", color = Color.White,
                fontWeight = FontWeight.Bold, fontSize = 19.sp,
                maxLines = 1, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
                    .clipToBounds()
                    .basicMarquee(iterations = Int.MAX_VALUE, spacing = MarqueeSpacing(16.dp))
            )
            Text(
                song?.artist ?: "", color = Color.White.copy(0.75f),
                fontSize = 13.sp, maxLines = 1, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** قوس لبخندی مسیر نوار پیشرفت زیر کادر */
private fun smilePath(w: Float, h: Float): android.graphics.Path {
    val y0 = h * 0.16f
    val yBot = h * 0.88f
    return android.graphics.Path().apply {
        moveTo(0f, y0)
        quadTo(w * 0.5f, 2f * yBot - y0, w, y0)
    }
}

/** نزدیک‌ترین نقطه مسیر به لمس (برای seek) */
private fun scrubFraction(aPath: android.graphics.Path, touch: Offset, maxDistPx: Float): Float? {
    val pm = PathMeasure(aPath, false)
    val len = pm.length
    if (len <= 0f) return null
    val pos = FloatArray(2)
    var best = 0f
    var bestD = Float.MAX_VALUE
    val n = 120
    for (i in 0..n) {
        pm.getPosTan(len * i / n, pos, null)
        val dx = pos[0] - touch.x
        val dy = pos[1] - touch.y
        val d = dx * dx + dy * dy
        if (d < bestD) {
            bestD = d
            best = i.toFloat() / n
        }
    }
    return if (bestD <= maxDistPx * maxDistPx) best else null
}

/**
 * نوار پیشرفت جدا زیر کادر (هم‌رنگ دکمه‌ها) — تپ و درگ با یک تشخیص‌دهنده واحد
 * تا هیچ ژستی گم نشود. درگ افقی = seek نسبی، تپ روی نوار = seek مطلق.
 */
@Composable
fun ProgressArc(
    progress: Float,
    durationMs: Long,
    onSeekMs: (Long) -> Unit,
    modifier: Modifier = Modifier,
    progColor: Color = MonoInk
) {
    val latestSeek by rememberUpdatedState(onSeekMs)
    val latestDur by rememberUpdatedState(durationMs)
    // بیرون از DrawScope خوانده می‌شود (داخل Canvas کامپوزبل نیست)
    val pal = LocalAP.current
    Canvas(
        modifier
            .height(86.dp)
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val wPx = size.width.toFloat()
                    val hPx = size.height.toFloat()
                    fun fracAt(off: Offset): Float? =
                        scrubFraction(
                            smilePath(wPx, hPx), off,
                            64.dp.toPx()
                        )
                    // تپ: seek مطلق روی نوار
                    fracAt(down.position)?.let { latestSeek((it * latestDur).toLong()) }
                    var dragging = false
                    var lastFrac: Float? = null
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull() ?: break
                        if (!ch.pressed) break
                        if (!dragging &&
                            (ch.position - down.position).getDistance() > viewConfiguration.touchSlop
                        ) dragging = true
                        if (dragging) {
                            // درگ: اگر روی نوار است مطلق، وگرنه افقی نسبی
                            val f = fracAt(ch.position)
                            if (f != null) {
                                lastFrac = f
                                latestSeek((f * latestDur).toLong())
                            } else if (lastFrac != null) {
                                val dxFrac = (ch.position.x - ch.previousPosition.x) / wPx
                                val nf = (lastFrac!! + dxFrac).coerceIn(0f, 1f)
                                lastFrac = nf
                                latestSeek((nf * latestDur).toLong())
                            }
                        }
                    }
                }
            }
    ) {
        val path = smilePath(size.width, size.height)
        val sw = 5.dp.toPx()
        drawPath(path.asComposePath(), color = pal.track, style = Stroke(sw, cap = StrokeCap.Round))
        val p = progress.coerceIn(0f, 1f)
        if (p > 0.001f) {
            val pm = PathMeasure(path, false)
            val len = pm.length
            if (len > 0f) {
                val seg = android.graphics.Path()
                pm.getSegment(0f, len * p, seg, true)
                drawPath(seg.asComposePath(), color = progColor, style = Stroke(sw, cap = StrokeCap.Round))
                val pos = FloatArray(2)
                pm.getPosTan((len * p).coerceAtMost(len), pos, null)
                val kc = Offset(pos[0], pos[1])
                drawCircle(pal.bg, radius = 11.dp.toPx(), center = kc)
                drawCircle(progColor, radius = 11.dp.toPx(), center = kc, style = Stroke(3.dp.toPx()))
                drawCircle(progColor, radius = 3.5.dp.toPx(), center = kc)
            }
        }
    }
}

/**
 * نور ملایم متحرک زیر کادر و کنار دکمه‌ها — یک هاله که آرام چپ‌به‌راست می‌لغزد
 * تا صفحه از خشکی دربیاید. خیلی کم‌رنگ است و روی تم روشن/تیره جواب می‌دهد.
 */
@Composable
fun MovingGlow(
    accent: Color,
    modifier: Modifier = Modifier,
    alpha: Float = 0.35f
) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val hPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val inf = rememberInfiniteTransition(label = "glow")
        val off by inf.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
            label = "glowOff"
        )
        val cx = wPx * off
        Canvas(Modifier.fillMaxSize().blur(14.dp)) {
            drawRect(
                Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = alpha),
                        accent.copy(alpha = alpha * 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(cx, hPx / 2f),
                    radius = wPx * 0.38f
                )
            )
        }
    }
}

/** شکل نوار پایینی با قوس وسط (^^) — تپ روی آن لیست را بالا می‌آورد */
class BottomArcHandleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): Outline {
        val bumpW = with(density) { 150.dp.toPx() }
        val bumpH = with(density) { 30.dp.toPx() }
        val smooth = with(density) { 26.dp.toPx() }
        val cx = size.width / 2f
        val path = Path().apply {
            moveTo(0f, bumpH)
            lineTo(cx - bumpW / 2f - smooth, bumpH)
            cubicTo(
                cx - bumpW / 4f, bumpH, cx - bumpW / 4f, 0f, cx, 0f
            )
            cubicTo(
                cx + bumpW / 4f, 0f, cx + bumpW / 4f, bumpH, cx + bumpW / 2f + smooth, bumpH
            )
            lineTo(size.width, bumpH)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

/** نوار سفید پایینی با برآمدگی وسط و آیکون دو فلش — مثل ماکت */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BottomArcHandle(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = Color.White,
    contentColor: Color = MonoInk
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(BottomArcHandleShape())
            .background(container)
            .clickable { onClick() },
        contentAlignment = Alignment.TopCenter
    ) {
        Icon(
            Icons.Default.KeyboardDoubleArrowUp, null,
            tint = contentColor,
            modifier = Modifier.padding(top = 4.dp).size(28.dp)
        )
    }
}
