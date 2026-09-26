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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.AppViewModel
import com.alvand.player.R
import com.alvand.player.ui.components.*
import com.alvand.player.ui.theme.*
import kotlinx.coroutines.launch

/** صفحه اصلی Mono+Aura: کاور سینمایی + بک‌گراند بلر + کنترل‌های تمیز */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    vm: AppViewModel,
    onPickFile: () -> Unit,
    onPickBackground: () -> Unit,
    onRequestAudioPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val state by vm.playerState.collectAsState()
    val songs by vm.songs.collectAsState()
    val audioPermissionGranted by vm.audioPermissionGranted.collectAsState()
    val isScanning by vm.isScanning.collectAsState()
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

    // اگر دفعه قبل کرش کرده، دیالوگ گزارش را نشان بده (نامرئی در حالت عادی)
    val crash by vm.crashReport.collectAsState()
    LaunchedEffect(Unit) { vm.loadUnseenCrash() }
    if (crash != null) {
        CrashReportDialog(vm, crash!!, onDismiss = { vm.markCrashSeen() })
    }

    if (showMenu) MenuSheet(
        vm = vm,
        onPickFile = onPickFile,
        onPickBackground = onPickBackground,
        hasAudioPermission = audioPermissionGranted,
        onRequestAudioPermission = onRequestAudioPermission,
        onOpenAbout = onOpenAbout,
        onPlayLink = {},
        onDismiss = { showMenu = false }
    )
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
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                if (songs.isEmpty()) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(stringResource(R.string.cover_fallback), color = pal.sub, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${stringResource(R.string.playlist)} (0)",
                                color = pal.ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(
                                    if (audioPermissionGranted) R.string.scan_songs
                                    else R.string.allow_access
                                ),
                                color = pal.sub, fontSize = 13.sp
                            )
                        }
                    }
                }
                itemsIndexed(songs, key = { _, s -> s.id }) { i, s ->
                    val active = state.current?.id == s.id
                    val d = if (active) state.durationMs else s.durationMs
                    Row(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 3.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (active) pal.glass else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable {
                                onRequestNotificationPermission()
                                vm.playList(songs, i)
                            }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ArtImage(
                            s, Modifier.size(44.dp),
                            RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                s.title, color = pal.ink,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 14.5.sp, maxLines = 1
                            )
                            Text(
                                s.artist.takeIf { it.isNotBlank() && it != "Unknown Artist" && it != "Unknown" }
                                    ?: stringResource(R.string.unknown_artist),
                                color = pal.sub, fontSize = 12.sp, maxLines = 1
                            )
                        }
                        if (active && state.isPlaying) {
                            MiniBars(true, color = dyn.accent)
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(fmtTime(d), color = pal.sub, fontSize = 12.5.sp)
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
        // بکگراند سینمایی همیشه زیر همه‌چیز است؛ کاور بلرشده + تک‌هاله
        Box(Modifier.fillMaxSize()) {
            PlayerBackground(accent = dyn, backgroundUri = bgUri, song = current)
            // ریسپانسیو: روی صفحه‌های پهن (تبلت) محتوا وسط‌چین با عرض محدود
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = maxWidth > 600.dp
                // قطر دایره کاور: ۸۰٪ عرض صفحه (تبلت: ثابت ۳۶۰)
                val circleD = if (wide) 360.dp else (maxWidth * 0.80f).coerceIn(240.dp, 400.dp)
                Column(
                    Modifier
                        .then(if (wide) Modifier.width(560.dp).align(Alignment.TopCenter) else Modifier.fillMaxSize())
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.statusBarsPadding().height(8.dp))
                    // نوار بالا: منو + تایمر خواب
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = if (wide) 60.dp else 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(44.dp)
                                .background(pal.card.copy(alpha = 0.92f), CircleShape)
                        ) { Icon(Icons.Default.Menu, null, tint = pal.ink) }
                        Spacer(Modifier.weight(1f))
                        SleepChip(vm, onClick = { showSleep = true })
                    }
                    Spacer(Modifier.height(16.dp))
                    // کاور دایره‌ای وسط‌چین با هاله نور نرم (نفس‌کشیدن موقع پخش)
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(contentAlignment = Alignment.Center) {
                            CoverHalo(
                                playing = state.isPlaying,
                                accent = dyn.accent,
                                diameter = circleD + 72.dp
                            )
                            ArtImage(
                                current,
                                Modifier.size(circleD).shadow(28.dp, CircleShape),
                                CircleShape
                            )
                        }
                    }
                    // عنوان و خواننده زیر دایره (وسط‌چین)
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            current?.title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.app_name),
                            color = pal.ink, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp,
                            letterSpacing = 0.2.sp, maxLines = 1, textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            current?.artist?.takeIf { it.isNotBlank() } ?: "—",
                            color = pal.sub, fontSize = 13.sp, letterSpacing = 0.4.sp,
                            maxLines = 1, textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth()
                        )
                    }
                    // نوار پیشرفت لبخندی جدا زیر کادر (هم‌رنگ کاور)
                    ProgressArc(
                        progress = if (dur > 0) shownPos.toFloat() / dur else 0f,
                        durationMs = dur,
                        onSeekMs = { vm.manager.seekTo(it) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                        progColor = dyn.accent
                    )
                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalLayoutDirection provides LayoutDirection.Ltr
                    ) {
                        Row(
                            Modifier.align(Alignment.CenterHorizontally).padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                fmtTime(shownPos), color = pal.ink,
                                fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                                letterSpacing = 0.3.sp, maxLines = 1
                            )
                            Text(
                                " / ${fmtTime(state.durationMs)}", color = pal.sub,
                                fontSize = 12.5.sp, letterSpacing = 0.2.sp, maxLines = 1
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    // دکمه‌ها + هاله بسیار ملایم پشتشان
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
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
                    if (songs.isEmpty()) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                stringResource(
                                    if (audioPermissionGranted) R.string.no_music
                                    else R.string.allow_access
                                ),
                                color = pal.sub,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(10.dp))
                            if (audioPermissionGranted) {
                                Button(
                                    onClick = { vm.scanDeviceSongs() },
                                    enabled = !isScanning,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = pal.ink,
                                        contentColor = pal.bg
                                    )
                                ) {
                                    Text(stringResource(if (isScanning) R.string.scanning else R.string.scan_songs))
                                }
                            } else {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = onRequestAudioPermission,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = pal.ink,
                                            contentColor = pal.bg
                                        )
                                    ) { Text(stringResource(R.string.allow)) }
                                    OutlinedButton(
                                        onClick = onOpenAppSettings,
                                        modifier = Modifier.weight(1f)
                                    ) { Text(stringResource(R.string.open_settings)) }
                                }
                            }
                        }
                    }
                    // فضای دسته پایینی
                    Spacer(Modifier.height(84.dp))
                }
                // دسته پایینی با لیبل قابل‌کشف — تپ لیست را بالا می‌آورد
                Box(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    BottomArcHandle(
                        onClick = { scope.launch { scaffoldState.bottomSheetState.expand() } },
                        container = pal.sheet,
                        contentColor = dyn.accent,
                        label = "${stringResource(R.string.playlist)} • ${songs.size}"
                    )
                }
            }
        }
    }
}
