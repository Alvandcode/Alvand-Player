package com.alvand.player.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import kotlinx.coroutines.launch

/** صفحه اصلی: کادر U چسبیده به بالا + قوس لبخندی + قوس پایینی لیست + نور متحرک */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    vm: AppViewModel,
    onPickFile: () -> Unit,
    onPickBackground: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val state by vm.playerState.collectAsState()
    val songs by vm.songs.collectAsState()
    val bgUri by vm.backgroundUri.collectAsState()
    val pal = LocalAP.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()
    var showMenu by remember { mutableStateOf(false) }
    var showLyricsFull by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    val current = state.current
    val dur = state.durationMs.coerceAtLeast(1L)
    val shownPos = if (dur > 1L) minOf(state.positionMs, dur) else state.positionMs
    // پالت داینامیک از کاور آهنگ فعلی (فالبک: سیاه‌سفید / یاسی در تیره)
    val dynRaw by rememberDynamicAccent(current, pal.isDark)
    val dyn = dynRaw.animated()

    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(ctx, ctx.getString(R.string.play_error), Toast.LENGTH_LONG).show()
            vm.manager.clearError()
        }
    }

    // نتیجه اسکن دستی کتابخانه
    val scanAdded by vm.lastScanAdded.collectAsState()
    LaunchedEffect(scanAdded) {
        scanAdded?.let { added ->
            val msg = if (added > 0) ctx.getString(R.string.scan_new, added)
            else ctx.getString(R.string.scan_none, songs.size)
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            vm.consumeScanMessage()
        }
    }

    if (showMenu) MenuSheet(vm, onPickFile, onPickBackground, onOpenAbout,
        onPlayLink = {}, onDismiss = { showMenu = false })
    if (showLyricsFull) LyricsSheet(vm, onDismiss = { showLyricsFull = false })
    if (showSleep) SleepTimerDialog(vm, onDismiss = { showSleep = false })

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        // قوس پایینی اختصاصی خودمان را داریم؛ پیک پیش‌فرض صفر تا فقط با تپ باز شود
        sheetPeekHeight = 0.dp,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetContainerColor = pal.sheet,
        sheetDragHandle = null,
        sheetContent = {
            // دستگیره جمع‌کردن داخل شیت بازشده
            Row(
                Modifier.fillMaxWidth()
                    .clickable { scope.launch { scaffoldState.bottomSheetState.partialExpand() } }
                    .padding(top = 8.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.KeyboardDoubleArrowDown, null,
                    tint = dyn.accent, modifier = Modifier.size(28.dp)
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${stringResource(R.string.playlist)} (${songs.size})",
                    color = pal.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                itemsIndexed(songs) { i, s ->
                    val active = state.current?.id == s.id
                    val d = if (active) state.durationMs else s.durationMs
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.playList(songs, i) }
                            .padding(horizontal = 22.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            s.title, color = pal.ink,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp, maxLines = 1, modifier = Modifier.weight(1f)
                        )
                        if (active && state.isPlaying) {
                            MiniBars(true, color = dyn.accent)
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(fmtTime(d), color = pal.sub, fontSize = 13.sp)
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
        // بکگراند چندلایه همیشه زیر همه‌چیز است؛ کادر روی آن می‌نشیند
        Box(Modifier.fillMaxSize()) {
            PlayerBackground(accent = dyn, backgroundUri = bgUri)
            // ریسپانسیو: روی صفحه‌های پهن (تبلت) محتوا وسط‌چین با عرض محدود
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = maxWidth > 600.dp
                // کادر بلندتر مثل ماکت (حدود ۵۶٪ ارتفاع)
                val artH = (maxHeight * 0.56f).coerceIn(280.dp, 540.dp)
                Column(
                    Modifier
                        .then(if (wide) Modifier.width(560.dp).align(Alignment.TopCenter) else Modifier.fillMaxSize())
                        .verticalScroll(rememberScrollState())
                ) {
                    // کادر آرت: کامل چسبیده به بالا + کمی باریک‌تر (حاشیه ۳۸)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (wide) 60.dp else 38.dp)
                    ) {
                        ArtPanel(
                            song = current,
                            modifier = Modifier.fillMaxWidth().height(artH),
                            glow = dyn.accent
                        )
                        // نوار سه‌خط + تایمر خواب روی کاور (بالا)
                        Row(
                            Modifier.fillMaxWidth().padding(top = 44.dp, start = 6.dp, end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(pal.glass.copy(alpha = 0.65f), CircleShape)
                            ) { Icon(Icons.Default.Menu, null, tint = pal.ink) }
                            Spacer(Modifier.weight(1f))
                            SleepChip(vm, onClick = { showSleep = true })
                        }
                    }
                    // نور ملایم متحرک زیر کادر
                    MovingGlow(
                        accent = dyn.accent,
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 48.dp)
                            .height(22.dp),
                        alpha = 0.32f
                    )
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
                        Text(fmtTime(shownPos), color = pal.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(" / ${fmtTime(state.durationMs)}", color = pal.sub, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    // دکمه‌ها + نور ملایم کنارشان
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        MovingGlow(
                            accent = dyn.accent,
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            alpha = 0.20f
                        )
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
                    }
                    // فضای قوس پایینی
                    Spacer(Modifier.height(76.dp))
                }
                // قوس پایینی (^^) — تپ لیست را بالا می‌آورد
                Box(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    BottomArcHandle(
                        onClick = { scope.launch { scaffoldState.bottomSheetState.expand() } },
                        container = pal.sheet,
                        contentColor = dyn.accent
                    )
                }
            }
        }
    }
}
