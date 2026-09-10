package com.alvand.player.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.ui.theme.*
import kotlin.math.*

/** بک‌گراند گرادیانی متحرک + هلال شناور */
@Composable
fun AlvandBackground(modifier: Modifier = Modifier, dark: Boolean = false) {
    val t by rememberInfiniteTransition(label = "bg").animateFloat(
        0f, 1f, infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse), label = "t"
    )
    Box(
        modifier.background(
            Brush.verticalGradient(
                if (dark) listOf(Color(0xFF150A33), Color(0xFF241547), Color(0xFF3A2070))
                else listOf(Color(0xFF3A2066), Color(0xFF241547), Color(0xFF120A2A))
            )
        )
    ) {
        // هلال کوچک و کم‌رنگ بالا تا زیر متن‌ها نیاید
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
            val floatY = (sin(t * 2 * PI) * 12).toFloat()
            val crescentCut = if (dark) Color(0xFF1A0E38) else Color(0xFF3A2066)
            Canvas(Modifier.size(140.dp).offset(x = (-14).dp, y = (40 + floatY).dp)) {
                drawCircle(Color(0xFFFFF3D6).copy(alpha = 0.85f), radius = size.minDimension / 2)
                drawCircle(
                    crescentCut, radius = size.minDimension / 2.2f,
                    center = Offset(size.width * 0.68f, size.height * 0.32f)
                )
            }
        }
        // ابرهای خیلی کم‌رنگ پایین
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                val w = size.width; val h = size.height
                drawOval(Color.White.copy(alpha = 0.10f), Offset(w * 0.1f, h * 0.3f), androidx.compose.ui.geometry.Size(w * 0.8f, h))
                drawOval(Color.White.copy(alpha = 0.07f), Offset(w * -0.1f, h * 0.5f), androidx.compose.ui.geometry.Size(w * 0.7f, h))
            }
        }
        // سایه تیره برای خوانایی متن‌ها
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.22f)))
    }
}

/** کارت شیشه‌ای */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: androidx.compose.ui.unit.Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .border(1.dp, AlvandCardBorder, RoundedCornerShape(corner)),
        color = AlvandCard,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(corner)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

/** کارت با تیلت سه‌بعدی (drag → rotateX/Y) */
@Composable
fun TiltGlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var rx by remember { mutableFloatStateOf(0f) }
    var ry by remember { mutableFloatStateOf(0f) }
    val arx by animateFloatAsState(rx, tween(300), label = "rx")
    val ary by animateFloatAsState(ry, tween(300), label = "ry")
    Box(
        modifier
            .graphicsLayer {
                rotationX = arx; rotationY = ary
                cameraDistance = 16 * density
            }
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, AlvandCardBorder, RoundedCornerShape(28.dp))
            .pointerInput(Unit) {
                detectDragGestures(onDragEnd = { rx = 0f; ry = 0f }) { c, drag ->
                    ry = (ry + drag.x * 0.05f).coerceIn(-14f, 14f)
                    rx = (rx - drag.y * 0.05f).coerceIn(-14f, 14f)
                    c.consume()
                }
            }
            .padding(18.dp)
    ) { content() }
}

/** وینیل/دیسک چرخان سه‌بعدی صفحه پلیر */
@Composable
fun Vinyl3D(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val rot = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            rot.animateTo(rot.value + 360f, tween(6000, easing = LinearEasing))
            rot.snapTo(0f)
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        // هاله نئونی
        Box(
            Modifier.size(300.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(AlvandPink.copy(0.55f), Color.Transparent)))
        )
        // دیسک
        Box(
            Modifier.size(250.dp)
                .rotate(rot.value)
                .clip(CircleShape)
                .background(Brush.sweepGradient(listOf(Color(0xFF2B1B52), Color(0xFF8E7BFF), Color(0xFFF3A8FF), Color(0xFF2B1B52))))
                .border(1.dp, Color.White.copy(0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(84.dp).clip(CircleShape).background(AlvandDeep), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Text("☾", color = AlvandCream, fontSize = 34.sp)
            }
        }
        // سوزن گرامافون (ثابت، با کمی چرخش سه‌بعدی)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
            Canvas(Modifier.size(120.dp)) {
                drawLine(Color.White.copy(0.85f), Offset(size.width * 0.7f, 0f), Offset(size.width * 0.35f, size.height), strokeWidth = 8f)
                drawCircle(Color.White, radius = 14f, center = Offset(size.width * 0.7f, 10f))
            }
        }
    }
}

/** میله‌های اکولایزر متحرک */
@Composable
fun EqBars(isPlaying: Boolean, modifier: Modifier = Modifier, count: Int = 24) {
    val inf = rememberInfiniteTransition(label = "eq")
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(count) { i ->
            val h by inf.animateFloat(
                4f, (14 + (i * 37 % 28)).toFloat(),
                infiniteRepeatable(tween(400 + (i * 53 % 500), easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "b$i"
            )
            Box(
                Modifier.width(4.dp).height(if (isPlaying) h.dp else 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.verticalGradient(listOf(AlvandPink, AlvandPurple)))
            )
        }
    }
}
