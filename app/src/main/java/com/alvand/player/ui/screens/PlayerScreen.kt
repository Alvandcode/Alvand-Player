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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    onOpenAbout: () -> Unit
) {
    val state by vm.playerState.collectAsState()
    val songs by vm.songs.collectAsState()
    val filtered by vm.filteredSongs.collectAsState()
    val query by vm.searchQuery.collectAsState()
    val sortMode by vm.sortMode.collectAsState()
    val likedSet by vm.liked.collectAsState()
    val favOnly by vm.favoritesOnly.collectAsState()
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
                    "${stringResource(R.string.playlist)} (${filtered.size}/${songs.size})",
                    color = pal.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            // v1.4.0: جستجو + فیلتر علاقه‌مندی + سورت — بدون شکستن دیزاین مینیمال
            OutlinedTextField(
                value = query,
                onValueChange = { vm.setSearchQuery(it) },
                placeholder = { Text(stringResource(R.string.search_hint), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = pal.sub, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.clearSearch() }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Clear, null, tint = pal.sub, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !favOnly,
                    onClick = { if (favOnly) vm.toggleFavoritesOnly() },
                    label = { Text(stringResource(R.string.show_all), fontSize = 12.sp) }
                )
                FilterChip(
                    selected = favOnly,
                    onClick = { if (!favOnly) vm.toggleFavoritesOnly() },
                    label = { Text("♡ ${stringResource(R.string.favorites)}", fontSize = 12.sp) }
                )
                Spacer(Modifier.weight(1f))
                var sortOpen by remember { mutableStateOf(false) }
                TextButton(onClick = { sortOpen = true }) {
                    val sortLabel = when (sortMode) {
                        1 -> stringResource(R.string.sort_title)
                        2 -> stringResource(R.string.sort_artist)
                        3 -> stringResource(R.string.sort_longest)
                        else -> stringResource(R.string.sort_default)
                    }
                    Text("⇅ $sortLabel", color = dyn.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.sort_default)) }, onClick = { vm.setSortMode(0); sortOpen = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.sort_title)) }, onClick = { vm.setSortMode(1); sortOpen = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.sort_artist)) }, onClick = { vm.setSortMode(2); sortOpen = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.sort_longest)) }, onClick = { vm.setSortMode(3); sortOpen = false })
                }
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                if (filtered.isEmpty()) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(stringResource(R.string.cover_fallback), color = pal.sub, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (favOnly) stringResource(R.string.no_favorites)
                                else if (query.isNotBlank()) stringResource(R.string.no_results)
                                else "${stringResource(R.string.playlist)} (0)",
                                color = pal.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                            )
                            if (songs.isEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    stringResource(R.string.scan_songs),
                                    color = pal.sub, fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
                itemsIndexed(filtered, key = { _, s -> s.id }) { i, s ->
                    val active = state.current?.id == s.id
                    val d = if (active) state.durationMs else s.durationMs
                    val isFav = s.id in likedSet
                    Row(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 3.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (active) pal.glass else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable { vm.playList(filtered, i) }
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
                        IconButton(
                            onClick = { vm.toggleLike(s.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = stringResource(if (isFav) R.string.unlike else R.string.like),
                                tint = if (isFav) dyn.accent else pal.sub,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (active && state.isPlaying) {
                            MiniBars(true, color = dyn.accent)
                            Spacer(Modifier.width(6.dp))
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
                // کاور سینمایی با ارتفاع متعادل (قبلا ۵۶٪ و خیلی بلند بود)
                val artH = (maxHeight * 0.46f).coerceIn(260.dp, 480.dp)
                Column(
                    Modifier
                        .then(if (wide) Modifier.width(560.dp).align(Alignment.TopCenter) else Modifier.fillMaxSize())
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.statusBarsPadding().height(8.dp))
                    // کاور مدرن با حاشیه استاندارد و سایه نرم
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (wide) 60.dp else 20.dp)
                    ) {
                        ArtPanel(
                            song = current,
                            modifier = Modifier.fillMaxWidth().height(artH)
                                .shadow(24.dp, RoundedCornerShape(28.dp)),
                            glow = dyn.accent
                        )
                        // نوار سه‌خط + تایمر خواب روی کاور (بالا) — خوانا روی هر کاوری
                        Row(
                            Modifier.fillMaxWidth().padding(top = 12.dp, start = 10.dp, end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(pal.card.copy(alpha = 0.92f), CircleShape)
                            ) { Icon(Icons.Default.Menu, null, tint = pal.ink) }
                            Spacer(Modifier.weight(1f))
                            // v1.4.0: لایک آهنگ فعلی روی کاور — پایدار در DataStore
                            if (current != null) {
                                val curFav = current.id in likedSet
                                IconButton(
                                    onClick = { vm.toggleLike(current.id) },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(pal.card.copy(alpha = 0.92f), CircleShape)
                                ) {
                                    Icon(
                                        if (curFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = stringResource(if (curFav) R.string.unlike else R.string.like),
                                        tint = if (curFav) dyn.accent else pal.ink
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                            SleepChip(vm, onClick = { showSleep = true })
                        }
                    }
                    // هاله ظریف زیر کاور (تک‌لایه، کم‌رنگ برای پرفورمنس)
                    MovingGlow(
                        accent = dyn.accent,
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 60.dp)
                            .height(14.dp),
                        alpha = 0.16f
                    )
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
