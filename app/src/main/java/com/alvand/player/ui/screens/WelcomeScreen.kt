package com.alvand.player.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.R
import com.alvand.player.ui.theme.MonoBg
import com.alvand.player.ui.theme.MonoInk
import com.alvand.player.ui.theme.MonoSub

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MonoBg).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1f))
        Image(
            painterResource(R.drawable.alvand_mark),
            contentDescription = null,
            modifier = Modifier.size(190.dp).clip(RoundedCornerShape(44.dp))
        )
        Spacer(Modifier.height(20.dp))
        Text("Alvand Player", color = MonoInk, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text(stringResource(R.string.tagline), color = MonoSub, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onStart,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MonoInk, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth(0.7f).height(56.dp)
        ) { Text(stringResource(R.string.lets_play), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        Spacer(Modifier.height(24.dp))
    }
}
