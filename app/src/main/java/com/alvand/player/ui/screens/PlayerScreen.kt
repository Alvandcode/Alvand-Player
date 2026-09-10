package com.alvand.player.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.AppViewModel
import com.alvand.player.R
import com.alvand.player.ui.components.*
import com.alvand.player.ui.theme.*

/** صفحه در حال پخش — قاب راست تصویر: آرت کشیده + حلقه پیشرفت + کنترل‌ها */
@Composable
fun PlayerScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onPickFile: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val state by vm.playerState.collectAsState()
    val ctx = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val current = state.current

    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(ctx, ctx.getString(R.string.play_error), Toast.LENGTH_LONG).show()
            vm.manager.clearError()
        }
    }

    if (showMenu) MenuSheet(vm, onPickFile, onOpenAbout,
        onPlayLink = {}, onDismiss = { showMenu = false })

    Column(
        Modifier.fillMaxSize().background(MonoBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 30.dp)
    ) {
        // نوار بالا
        Row(
            Modifier.fillMaxWidth().padding(top = 40.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = MonoInk) }
            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.Menu, null, tint = MonoInk) }
        }
        // آرت کشیده گرد
        ArtImage(
            current,
            Modifier.fillMaxWidth(0.78f).aspectRatio(0.72f).align(Alignment.CenterHorizontally),
            RoundedCornerShape(90.dp)
        )
        Spacer(Modifier.height(18.dp))
        Text(
            current?.title ?: "—", color = MonoInk,
            fontWeight = FontWeight.Bold, fontSize = 20.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            current?.artist ?: "", color = MonoSub, fontSize = 13.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(14.dp))
        // حلقه پیشرفت
        val dur = state.durationMs.coerceAtLeast(1L)
        RingProgress(
            progress = if (dur > 0) state.positionMs.toFloat() / dur else 0f,
            onSeek = { f -> vm.manager.seekTo((f * dur).toLong()) },
            modifier = Modifier.align(Alignment.CenterHorizontally),
            size = 210.dp
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.align(Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(fmtTime(state.positionMs), color = MonoInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(" / ${fmtTime(state.durationMs)}", color = MonoSub, fontSize = 12.sp)
        }
        Spacer(Modifier.height(14.dp))
        ControlsRow(
            playing = state.isPlaying,
            shuffle = state.shuffle,
            repeatOne = state.repeatOne,
            onShuffle = { vm.manager.toggleShuffle() },
            onPrev = { vm.manager.prev() },
            onToggle = { vm.manager.togglePlayPause() },
            onNext = { vm.manager.next() },
            onRepeat = { vm.manager.toggleRepeatOne() },
            big = true
        )
    }
}
