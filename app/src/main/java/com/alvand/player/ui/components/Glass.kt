package com.alvand.player.ui.components

import android.graphics.Bitmap
import android.graphics.PathMeasure
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
    var bmp by remember(song?.id) { mutableStateOf<Bitmap?>(null) }
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
            Image(bmp!!.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Text(
                    "♪", color = Color.White.copy(0.85f),
                    fontSize = 64.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** میله‌های کوچک مشکیِ در حال پخش */
@Composable
fun MiniBars(playing: Boolean, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "mb")
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(4) { i ->
            val h by inf.animateFloat(
                4f, (8 + (i * 7 % 10)).toFloat(),
                infiniteRepeatable(tween(350 + (i * 90 % 300), easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "m$i"
            )
            Box(
                Modifier.width(3.dp).height(if (playing) h.dp else 4.dp)
                    .background(MonoInk)
            )
        }
    }
}

/** ردیف کنترل‌های پخش مثل تصویر: شافل | قبلی | پلی بزرگ | بعدی | تکرار */
@Composable
fun ControlsRow(
    playing: Boolean,
    shuffle: Boolean,
    repeatOne: Boolean,
    onShuffle: () -> Unit,
    onPrev: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    big: Boolean,
    modifier: Modifier = Modifier
) {
    val main = if (big) 76.dp else 58.dp
    val sub = if (big) 30.dp else 26.dp
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onShuffle, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Shuffle, null, tint = if (shuffle) MonoInk else MonoSub.copy(0.5f), modifier = Modifier.size(sub))
        }
        IconButton(onClick = onPrev, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.SkipPrevious, null, tint = MonoInk, modifier = Modifier.size(34.dp))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            FilledIconButton(
                onClick = onToggle,
                modifier = Modifier.size(main),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MonoInk, contentColor = Color.White)
            ) {
                Icon(
                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null, modifier = Modifier.size(main * 0.45f)
                )
            }
        }
        IconButton(onClick = onNext, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.SkipNext, null, tint = MonoInk, modifier = Modifier.size(34.dp))
        }
        IconButton(onClick = onRepeat, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Repeat, null, tint = if (repeatOne) MonoInk else MonoSub.copy(0.5f), modifier = Modifier.size(sub))
        }
    }
}

/** حلقه پیشرفت دایره‌ای با قابلیت لمس/درگ برای جلو-عقب */
@Composable
fun RingProgress(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    stroke: Dp = 5.dp
) {
    val p = progress.coerceIn(0f, 1f)
    Canvas(
        modifier
            .size(size)
            .pointerInput(Unit) {
                detectTapGestures { off -> fracOf(off, size.toPx())?.let(onSeek) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    fracOf(change.position, size.toPx())?.let(onSeek)
                    change.consume()
                }
            }
    ) {
        val sw = stroke.toPx()
        val r = (size.toPx() - sw) / 2f
        val tl = center - Offset(r, r)
        val arcSize = Size(r * 2f, r * 2f)
        drawArc(MonoTrack, -90f, 360f, false, tl, arcSize, style = Stroke(sw, cap = StrokeCap.Round))
        if (p > 0f) {
            drawArc(MonoInk, -90f, 360f * p, false, tl, arcSize, style = Stroke(sw, cap = StrokeCap.Round))
            val a = Math.toRadians((360.0 * p - 90.0))
            val kc = center + Offset((r * cos(a)).toFloat(), (r * sin(a)).toFloat())
            drawCircle(Color.White, radius = 11.dp.toPx(), center = kc)
            drawCircle(MonoInk, radius = 11.dp.toPx(), center = kc, style = Stroke(3.dp.toPx()))
            drawCircle(MonoInk, radius = 3.5.dp.toPx(), center = kc)
        }
    }
}

private fun fracOf(off: Offset, dimPx: Float): Float? {
    val dx = off.x - dimPx / 2f
    val dy = off.y - dimPx / 2f
    if (dx * dx + dy * dy < 900f) return null // وسط دایره: نادیده بگیر
    val ang = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 90.0 + 360.0) % 360.0
    return (ang / 360.0).toFloat()
}

/** مسیر حلقه دور نیم‌دایره پایین پنل آرت (مثل نمونه) */
private fun artTrackPath(wPx: Float, hPx: Float, insetPx: Float, topYPx: Float): android.graphics.Path {
    val cornerR = (wPx - insetPx * 2f) / 2f
    val cy = hPx - insetPx - cornerR
    return android.graphics.Path().apply {
        moveTo(insetPx, topYPx)
        lineTo(insetPx, cy)
        arcTo(android.graphics.RectF(insetPx, cy - cornerR, wPx - insetPx, cy + cornerR), 180f, 180f, false)
        lineTo(wPx - insetPx, topYPx)
    }
}

/** نزدیک‌ترین نقطه مسیر به لمس → کسر پیشرفت (برای seek روی حلقه) */
private fun nearestFrac(aPath: android.graphics.Path, off: Offset, maxDistPx: Float): Float? {
    val pm = PathMeasure(aPath, false)
    val len = pm.length
    if (len <= 0f) return null
    val pos = FloatArray(2)
    var best = 0f
    var bestD = Float.MAX_VALUE
    val n = 150
    for (i in 0..n) {
        pm.getPosTan(len * i / n, pos, null)
        val dx = pos[0] - off.x
        val dy = pos[1] - off.y
        val d = dx * dx + dy * dy
        if (d < bestD) {
            bestD = d
            best = i.toFloat() / n
        }
    }
    return if (bestD <= maxDistPx * maxDistPx) best else null
}

/**
 * پنل آرت کشیده با حلقه پیشرفت دور نیم‌دایره پایین + عنوان روی آرت.
 * لمس/درگ روی حلقه = جلو-عقب.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtPanel(
    song: Song?,
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier) {
        val wPx = with(LocalDensity.current) { maxWidth.toPx() }
        val hPx = with(LocalDensity.current) { maxHeight.toPx() }
        ArtImage(
            song, Modifier.fillMaxSize(),
            RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 190.dp, bottomEnd = 190.dp)
        )
        Column(
            Modifier.fillMaxSize().padding(bottom = 40.dp, start = 24.dp, end = 24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                song?.title ?: "Alvand Player", color = Color.White,
                fontWeight = FontWeight.Bold, fontSize = 19.sp,
                maxLines = 1, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
                    .basicMarquee(iterations = Int.MAX_VALUE, spacing = MarqueeSpacing(16.dp))
            )
            Text(
                song?.artist ?: "", color = Color.White.copy(0.75f),
                fontSize = 13.sp, maxLines = 1, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Canvas(
            Modifier.fillMaxSize()
                .pointerInput(wPx, hPx) {
                    detectTapGestures { off ->
                        nearestFrac(artTrackPath(wPx, hPx, 18.dp.toPx(), minOf(120.dp.toPx(), hPx * 0.35f)), off, 56.dp.toPx())?.let(onSeek)
                    }
                }
                .pointerInput(wPx, hPx) {
                    detectDragGestures { change, _ ->
                        nearestFrac(artTrackPath(wPx, hPx, 18.dp.toPx(), minOf(120.dp.toPx(), hPx * 0.35f)), change.position, 56.dp.toPx())?.let(onSeek)
                        change.consume()
                    }
                }
        ) {
            val inset = 18.dp.toPx()
            val topY = minOf(120.dp.toPx(), size.height * 0.35f)
            val aPath = artTrackPath(size.width, size.height, inset, topY)
            val sw = 5.dp.toPx()
            drawPath(aPath.asComposePath(), color = MonoTrack, style = Stroke(sw, cap = StrokeCap.Round))
            val p = progress.coerceIn(0f, 1f)
            if (p > 0.001f) {
                val pm = PathMeasure(aPath, false)
                val len = pm.length
                if (len > 0f) {
                    val seg = android.graphics.Path()
                    pm.getSegment(0f, len * p, seg, true)
                    drawPath(seg.asComposePath(), color = MonoInk, style = Stroke(sw, cap = StrokeCap.Round))
                    val pos = FloatArray(2)
                    pm.getPosTan((len * p).coerceAtMost(len), pos, null)
                    val kc = Offset(pos[0], pos[1])
                    drawCircle(Color.White, radius = 11.dp.toPx(), center = kc)
                    drawCircle(MonoInk, radius = 11.dp.toPx(), center = kc, style = Stroke(3.dp.toPx()))
                    drawCircle(MonoInk, radius = 3.5.dp.toPx(), center = kc)
                }
            }
        }
    }
}
