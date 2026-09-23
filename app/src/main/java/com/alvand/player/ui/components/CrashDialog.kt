package com.alvand.player.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.AppViewModel
import com.alvand.player.R
import com.alvand.player.data.CrashInfo
import com.alvand.player.ui.theme.LocalAP

/** اشتراک گزارش کرش با اپ‌های دیگر */
fun shareCrash(ctx: android.content.Context, info: CrashInfo) {
    runCatching {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, info.fullText.take(60_000))
        }
        ctx.startActivity(
            Intent.createChooser(send, ctx.getString(R.string.crash_share_title))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/** دیالوگ شروع: کرش دیده‌نشده را نشان می‌دهد تا کاربر بفرستد */
@Composable
fun CrashReportDialog(vm: AppViewModel, info: CrashInfo, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val pal = LocalAP.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.crash_title), fontSize = 16.sp) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(info.headline, color = pal.ink, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 3)
                Text(info.timeLabel(), color = pal.sub, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.crash_desc), color = pal.sub, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().heightIn(max = 240.dp)
                        .background(pal.track.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    SelectionContainer {
                        Text(
                            info.fullText.take(8_000),
                            fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                            color = pal.ink, lineHeight = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                clip.setText(AnnotatedString(info.fullText.take(60_000)))
                Toast.makeText(ctx, ctx.getString(R.string.copied), Toast.LENGTH_SHORT).show()
            }) { Text(stringResource(R.string.crash_copy), fontSize = 13.sp) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { shareCrash(ctx, info) }) {
                    Text(stringResource(R.string.crash_share), fontSize = 13.sp)
                }
                TextButton(onClick = { vm.markCrashSeen(); onDismiss() }) {
                    Text(stringResource(R.string.crash_close), fontSize = 13.sp)
                }
            }
        }
    )
}

/** کارت گزارش خطا در صفحه درباره ما */
@Composable
fun CrashCard(vm: AppViewModel) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val pal = LocalAP.current
    val info by vm.crashReport.collectAsState()
    Surface(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = pal.card, border = androidx.compose.foundation.BorderStroke(1.dp, pal.line)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BugReport, null, tint = pal.ink)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.crash_report), color = pal.ink,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            if (info == null) {
                Text(stringResource(R.string.crash_none), color = pal.sub, fontSize = 12.sp)
            } else {
                val c = info!!
                Text(c.headline, color = pal.ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 3)
                Text(c.timeLabel(), color = pal.sub, fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            clip.setText(AnnotatedString(c.fullText.take(60_000)))
                            Toast.makeText(ctx, ctx.getString(R.string.copied), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.crash_copy), fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = { shareCrash(ctx, c) },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.crash_share), fontSize = 12.sp) }
                    TextButton(onClick = { vm.clearCrashReport() }) {
                        Text(stringResource(R.string.crash_clear), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
