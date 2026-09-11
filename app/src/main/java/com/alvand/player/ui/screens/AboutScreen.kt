package com.alvand.player.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.alvand.player.ui.theme.*

private const val URL_GITHUB = "https://github.com/Alvandcode"
private const val URL_SITE = "https://alvandcode.github.io/"
private const val URL_TELEGRAM = "https://t.me/a_c_official"
private const val TON_ADDRESS = "UQCB9rzvwmq0FJDaBkHVdBgbfZPb06FWdKco3woAHH6AXuUt"

/** درباره ما — تم‌دار (روشن/تیره) */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val uri = LocalUriHandler.current
    val clip = LocalClipboardManager.current
    val version = remember { appVersion(ctx) }
    val pal = LocalAP.current

    Column(
        Modifier.fillMaxSize().background(pal.bg)
            .verticalScroll(rememberScrollState())
            .padding(top = 40.dp, start = 18.dp, end = 18.dp, bottom = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = pal.ink) }
            Text(stringResource(R.string.about), color = pal.ink, fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
            color = pal.card, border = androidx.compose.foundation.BorderStroke(1.dp, pal.line)
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painterResource(R.drawable.alvand_mark),
                    contentDescription = null,
                    modifier = Modifier.size(110.dp).clip(RoundedCornerShape(26.dp))
                )
                Spacer(Modifier.height(10.dp))
                Text("Alvand Player", color = pal.ink, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text(stringResource(R.string.version_fmt, version), color = pal.sub, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.about_desc), color = pal.sub,
                    fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
            color = pal.card, border = androidx.compose.foundation.BorderStroke(1.dp, pal.line)
        ) {
            Column(Modifier.padding(vertical = 6.dp)) {
                AboutLink(Icons.Default.Code, "GitHub", URL_GITHUB, pal) { uri.openUri(URL_GITHUB) }
                AboutLink(Icons.Default.Public, stringResource(R.string.link_website), URL_SITE, pal) { uri.openUri(URL_SITE) }
                AboutLink(Icons.Default.Send, "Telegram", URL_TELEGRAM, pal) { uri.openUri(URL_TELEGRAM) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
            color = pal.card, border = androidx.compose.foundation.BorderStroke(1.dp, pal.line)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, null, tint = pal.ink)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.donate_title), color = pal.ink,
                        fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text("TON", color = pal.sub, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(TON_ADDRESS, color = pal.ink, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        clip.setText(AnnotatedString(TON_ADDRESS))
                        Toast.makeText(ctx, ctx.getString(R.string.copied), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = pal.ink, contentColor = pal.bg)
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
private fun AboutLink(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    pal: AlvandPalette,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = pal.ink)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = pal.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, color = pal.sub, fontSize = 11.sp)
            }
            Icon(Icons.Default.OpenInNew, null, tint = pal.sub)
        }
    }
}

@Suppress("DEPRECATION")
private fun appVersion(ctx: android.content.Context): String = runCatching {
    ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0.0"
}.getOrNull() ?: "1.0.0"
