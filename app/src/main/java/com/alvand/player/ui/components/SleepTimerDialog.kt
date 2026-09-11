package com.alvand.player.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.AppViewModel
import com.alvand.player.R
import com.alvand.player.player.SleepTimer
import com.alvand.player.ui.theme.MonoInk
import com.alvand.player.ui.theme.MonoSub

/** پیش‌فرض‌های تایمر خواب (دقیقه) */
private val SLEEP_PRESETS = listOf(5, 10, 15, 30, 45, 60, 90)

/** دیالوگ تایمر خواب + fade-out */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val sleep by vm.manager.sleepState.collectAsState()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = androidx.compose.ui.graphics.Color.White) {
        Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 36.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bedtime, null, tint = MonoInk)
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.sleep_timer),
                    color = MonoInk, fontWeight = FontWeight.Bold, fontSize = 18.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            if (sleep.active) {
                Text(
                    stringResource(R.string.sleep_remaining, SleepTimer.formatRemaining(sleep.remainingMs)),
                    color = MonoSub, fontSize = 13.sp
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { 1f - sleep.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MonoInk
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { vm.manager.cancelSleepTimer(); onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) { Text(stringResource(R.string.sleep_cancel)) }
                Spacer(Modifier.height(6.dp))
            } else {
                Text(stringResource(R.string.sleep_desc), color = MonoSub, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
            }
            SLEEP_PRESETS.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { min ->
                        FilterChip(
                            selected = false,
                            onClick = { vm.manager.startSleepTimer(min); onDismiss() },
                            label = { Text("$min′") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // ردیف آخر اگر ۳تایی نشد، فضای خالی را پر کن
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }
            Button(
                onClick = { vm.manager.startSleepEndOfTrack(); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MonoInk,
                    contentColor = androidx.compose.ui.graphics.Color.White
                )
            ) { Text(stringResource(R.string.sleep_end_of_song)) }
        }
    }
}

/** چیپ کوچک نمایش تایمر فعال روی صفحه پلیر */
@Composable
fun SleepChip(vm: AppViewModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val sleep by vm.manager.sleepState.collectAsState()
    if (!sleep.active) return
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                "☾ ${SleepTimer.formatRemaining(sleep.remainingMs)}",
                fontSize = 12.sp, fontWeight = FontWeight.Bold
            )
        },
        leadingIcon = { Icon(Icons.Default.Bedtime, null, modifier = Modifier.size(16.dp)) },
        modifier = modifier
    )
}
