package com.alvand.player.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.AppViewModel
import com.alvand.player.R
import com.alvand.player.ui.components.*
import com.alvand.player.ui.theme.*
import kotlinx.coroutines.launch

/** صفحه اصلی: آرت + نوار پیشرفت زیر کادر + لیست کشویی */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    vm: AppViewModel,
    onPickFile: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val state by vm.playerState.collectAsState()
    val songs by vm.songs.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()
    var showMenu by remember { mutableStateOf(false) }
    var showLyricsFull by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    val current = state.current
    val dur = state.durationMs.coerceAtLeast(1L)
    val shownPos = if (dur > 1L) minOf(state.positionMs, dur) else state.positionMs
    // پالت داینامیک از کاور آهنگ فعلی (فالبک: سیاه‌سفید)
    val dynRaw by rememberDynamicAccent(current)
    val dyn = dynRaw.animated()

    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(ctx, ctx.getString(R.string.play_error), Toast.LENGTH_LONG).show()
            vm.manager.clearError()
        }
    }

    if (showMenu) MenuSheet(vm, onPickFile, onOpenAbout,
        onPlayLink = {}, onDismiss = { showMenu = false })
    if (showLyricsFull) LyricsSheet(vm, onDismiss = { showLyricsFull = false })
    if (showSleep) SleepTimerDialog(vm, onDismiss = { showSleep = false })

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 104.dp,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetContainerColor = Color.White,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            LazyColumn(Modifier.fillMaxWidth()) {
                // نوار کوچک: با تپ باز می‌شود، با درگ لیست می‌آید بالا
                item {
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { scope.launch { scaffoldState.bottomSheetState.expand() } }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ArtImage(current, Modifier.size(52.dp), RoundedCornerShape(14.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(current?.title ?: "Alvand Player", color = MonoInk,
                                fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                            Text(current?.artist ?: "", color = MonoSub, fontSize = 12.sp, maxLines = 1)
                        }
                        IconButton(onClick = { vm.manager.togglePlayPause() }) {
                            Icon(
                                if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                null, tint = MonoInk, modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
                item {
                    Text(
                        "${stringResource(R.string.playlist)} (${songs.size})",
                        color = MonoInk, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp)
                    )
                }
                itemsIndexed(songs) { i, s ->
                    val active = state.current?.id == s.id
                    val d = if (active) state.durationMs else s.durationMs
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.playList(songs, i) }
                            .padding(horizontal = 22.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            s.title, color = MonoInk,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp, maxLines = 1, modifier = Modifier.weight(1f)
                        )
                        if (active && state.isPlaying) {
                            MiniBars(true, color = dyn.accent)
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(fmtTime(d), color = MonoSub, fontSize = 13.sp)
                    }
                }
                item { LyricsPreviewCard(vm) { showLyricsFull = true } }
                item {
                    ControlsRow(
                        playing = state.isPlaying,
                        shuffle = state.shuffle,
                        repeatMode = state.repeatMode,
                        onShuffle = { vm.manager.toggleShuffle() },
                        onPrev = { vm.manager.prev() },
                        onToggle = { vm.manager.togglePlayPause() },
                        onNext = { vm.manager.next() },
                        onRepeat = { vm.manager.cycleRepeat() },
                        big = false,
                        accent = dyn.accent,
                        modifier = Modifier.padding(top = 6.dp, bottom = 28.dp)
                    )
                }
            }
        }
    ) {
        // ریسپانسیو: روی صفحه‌های پهن (تبلت) محتوا وسط‌چین با عرض محدود
        BoxWithConstraints(Modifier.fillMaxSize().dynamicBackground(dyn)) {
            val wide = maxWidth > 600.dp
            val artH = (maxHeight * 0.44f).coerceIn(230.dp, 460.dp)
            Column(
                Modifier
                    .then(if (wide) Modifier.width(560.dp).align(Alignment.TopCenter) else Modifier.fillMaxSize())
                    .verticalScroll(rememberScrollState())
            ) {
                // نوار بالا (بدون بک — صفحه اصلی ریشه است)
                Row(
                    Modifier.fillMaxWidth().padding(top = 40.dp, start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // چیپ تایمر خواب (فقط وقتی فعال است دیده می‌شود)
                    SleepChip(vm, onClick = { showSleep = true })
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.Menu, null, tint = MonoInk) }
                }
                // کادر آرت
                ArtPanel(
                    song = current,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp).height(artH),
                    glow = dyn.accent
                )
                Spacer(Modifier.height(4.dp))
                // نوار پرشونده جدا زیر کادر (هم‌رنگ کاور)
                ProgressArc(
                    progress = if (dur > 0) shownPos.toFloat() / dur else 0f,
                    durationMs = dur,
                    onSeekMs = { vm.manager.seekTo(it) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 34.dp),
                    progColor = dyn.accent
                )
                Row(
                    Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(fmtTime(shownPos), color = MonoInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(" / ${fmtTime(state.durationMs)}", color = MonoSub, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                ControlsRow(
                    playing = state.isPlaying,
                    shuffle = state.shuffle,
                    repeatMode = state.repeatMode,
                    onShuffle = { vm.manager.toggleShuffle() },
                    onPrev = { vm.manager.prev() },
                    onToggle = { vm.manager.togglePlayPause() },
                    onNext = { vm.manager.next() },
                    onRepeat = { vm.manager.cycleRepeat() },
                    big = true,
                    accent = dyn.accent
                )
                // فضای نوار کشویی پایین
                Spacer(Modifier.height(118.dp))
            }
        }
    }
}
