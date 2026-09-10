package com.alvand.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.AppViewModel
import com.alvand.player.ui.components.*
import com.alvand.player.ui.theme.*

/** صفحه لیست آهنگ‌ها — قاب چپ تصویر */
@Composable
fun LibraryScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
    onPickFile: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val songs by vm.songs.collectAsState()
    val state by vm.playerState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    val current = state.current ?: songs.firstOrNull()

    if (showMenu) MenuSheet(vm, onPickFile, onOpenAbout,
        onPlayLink = { onOpenPlayer() }, onDismiss = { showMenu = false })

    Column(Modifier.fillMaxSize().background(MonoBg)) {
        // نوار بالا
        Row(
            Modifier.fillMaxWidth().padding(top = 40.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = MonoInk) }
            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.Menu, null, tint = MonoInk) }
        }
        // پنل آرت U-شکل
        Box(
            Modifier.padding(horizontal = 20.dp).height(370.dp)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 190.dp, bottomEnd = 190.dp))
                .clickable { if (current != null) onOpenPlayer() }
        ) {
            ArtImage(current, Modifier.fillMaxSize(), RoundedCornerShape(0.dp))
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.55f)))
                )
            )
            Column(
                Modifier.fillMaxSize().padding(bottom = 42.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(current?.title ?: "Alvand Player", color = Color.White,
                    fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1)
                Text(current?.artist ?: "", color = Color.White.copy(0.75f), fontSize = 13.sp, maxLines = 1)
            }
        }
        // لیست ترک‌ها: عنوان چپ، مدت راست
        LazyColumn(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            itemsIndexed(songs) { i, s ->
                val active = state.current?.id == s.id
                val dur = if (active) state.durationMs else s.durationMs
                Row(
                    Modifier.fillMaxWidth().clickable { vm.playList(songs, i) }
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        s.title, color = MonoInk,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 15.sp, maxLines = 1, modifier = Modifier.weight(1f)
                    )
                    if (active && state.isPlaying) {
                        MiniBars(true)
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(fmtTime(dur), color = MonoSub, fontSize = 13.sp)
                }
            }
        }
        // کنترل‌های پایین
        if (current != null) {
            ControlsRow(
                playing = state.isPlaying,
                shuffle = state.shuffle,
                repeatOne = state.repeatOne,
                onShuffle = { vm.manager.toggleShuffle() },
                onPrev = { vm.manager.prev() },
                onToggle = { vm.manager.togglePlayPause() },
                onNext = { vm.manager.next() },
                onRepeat = { vm.manager.toggleRepeatOne() },
                big = false,
                modifier = Modifier.padding(bottom = 30.dp, top = 6.dp)
            )
        } else Spacer(Modifier.height(30.dp))
    }
}
