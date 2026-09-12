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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.R
import com.alvand.player.ui.theme.LocalAP

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    val pal = LocalAP.current
    Column(
        Modifier.fillMaxSize().background(pal.bg).padding(horizontal = 28.dp).statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1.1f))
        Image(
            painterResource(R.drawable.alvand_mark),
            contentDescription = "Alvand Player logo",
            modifier = Modifier.size(148.dp).clip(RoundedCornerShape(34.dp))
        )
        Spacer(Modifier.height(22.dp))
        Text(
            "Alvand Player", color = pal.ink, fontSize = 30.sp,
            fontWeight = FontWeight.Black, letterSpacing = (-0.3).sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.tagline), color = pal.sub, fontSize = 14.sp,
            letterSpacing = 0.2.sp
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onStart,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = pal.ink, contentColor = pal.bg),
            modifier = Modifier.fillMaxWidth(0.78f).height(58.dp)
        ) { Text(stringResource(R.string.lets_play), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        Spacer(Modifier.height(12.dp))
        Text(
            "Mono + Aura • Minimal",
            color = pal.sub.copy(alpha = 0.7f), fontSize = 11.sp, letterSpacing = 1.2.sp
        )
        Spacer(Modifier.navigationBarsPadding().height(28.dp))
    }
}
