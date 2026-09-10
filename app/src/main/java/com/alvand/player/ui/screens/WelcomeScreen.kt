package com.alvand.player.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.R
import com.alvand.player.ui.components.AlvandBackground

/** اسپلش: لوگو + نام اپ + دکمه شروع */
@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "w")
    val scale by inf.animateFloat(1f, 1.06f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "s")
    Box(Modifier.fillMaxSize()) {
        AlvandBackground(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Spacer(Modifier.weight(1f))
            Image(
                painterResource(R.drawable.alvand_mark),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(0.82f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(36.dp))
                    .graphicsLayer { scaleX = scale; scaleY = scale }
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.tagline), color = Color.White.copy(0.8f), fontSize = 13.sp)
            Spacer(Modifier.height(26.dp))
            Button(
                onClick = onStart,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF6C4CF1)),
                modifier = Modifier.fillMaxWidth(0.75f).height(54.dp)
            ) { Text(stringResource(R.string.lets_play), fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.sign_in), color = Color.White.copy(0.75f), fontSize = 13.sp)
            Spacer(Modifier.height(28.dp))
        }
    }
}
