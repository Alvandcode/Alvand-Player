package com.alvand.player.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.R
import com.alvand.player.ui.components.AlvandBackground
import com.alvand.player.ui.components.GlassCard

private const val URL_GITHUB = "https://github.com/Alvandcode"
private const val URL_SITE = "https://alvandcode.github.io/"
private const val URL_TELEGRAM = "https://t.me/a_c_official"
private const val TON_ADDRESS = "UQCB9rzvwmq0FJDaBkHVdBgbfZPb06FWdKco3woAHH6AXuUt"

/** صفحه درباره ما: لینک‌ها + حمایت مالی TON */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val uri = LocalUriHandler.current
    val clip = LocalClipboardManager.current
    val version = remember { appVersion(ctx) }

    Box(Modifier.fillMaxSize()) {
        AlvandBackground(Modifier.fillMaxSize(), dark = true)
        Column(Modifier.fillMaxSize().padding(top = 44.dp, start = 18.dp, end = 18.dp, bottom = 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                Text(stringResource(R.string.about), color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
            Spacer(Modifier.height(12.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painterResource(R.drawable.alvand_mark),
                        contentDescription = null,
                        modifier = Modifier.size(120.dp).clip(RoundedCornerShape(28.dp))
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Alvand Player", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Text(stringResource(R.string.version_fmt, version), color = Color.White.copy(0.65f), fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.about_desc),
                        color = Color.White.copy(0.75f), fontSize = 13.sp, textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                LinkRow(Icons.Default.Code, "GitHub", URL_GITHUB) { uri.openUri(URL_GITHUB) }
                LinkRow(Icons.Default.Public, stringResource(R.string.link_website), URL_SITE) { uri.openUri(URL_SITE) }
                LinkRow(Icons.Default.Send, "Telegram", URL_TELEGRAM) { uri.openUri(URL_TELEGRAM) }
            }
            Spacer(Modifier.height(12.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, null, tint = Color(0xFFF3A8FF))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.donate_title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text("TON", color = Color.White.copy(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(TON_ADDRESS, color = Color.White, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        clip.setText(AnnotatedString(TON_ADDRESS))
                        Toast.makeText(ctx, ctx.getString(R.string.copied), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.copy))
                }
            }
        }
    }
}

@Composable
private fun LinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, color = Color.White.copy(0.55f), fontSize = 11.sp)
            }
            Icon(Icons.Default.OpenInNew, null, tint = Color.White.copy(0.6f))
        }
    }
}

@Suppress("DEPRECATION")
private fun appVersion(ctx: Context): String = runCatching {
    ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0.0"
}.getOrNull() ?: "1.0.0"
