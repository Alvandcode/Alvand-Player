package com.alvand.player.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
