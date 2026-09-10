package com.alvand.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.AppViewModel
import com.alvand.player.R
import com.alvand.player.audio.AudioSettings
import com.alvand.player.ui.components.*
import com.alvand.player.ui.theme.AlvandPurple

/** صفحه Now Playing: وینیل سه‌بعدی + لیریک + اکولایزر */
@Composable
fun PlayerScreen(vm: AppViewModel, onBack: () -> Unit) {
    val state by vm.playerState.collectAsState()
    val lyrics by vm.lyrics.collectAsState()
    val loading by vm.lyricsLoading.collectAsState()
    val eq by vm.eqSettings.collectAsState()
    var tab by remember { mutableIntStateOf(0) } // 0=player 1=lyrics 2=EQ
    var manual by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val ctx = LocalContext.current
    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(ctx, ctx.getString(R.string.play_error), Toast.LENGTH_LONG).show()
            vm.manager.clearError()
        }
    }
    val tabs = listOf(
        stringResource(R.string.tab_player),
        stringResource(R.string.tab_lyrics),
        stringResource(R.string.tab_eq)
    )

    val current = state.current
    val activeIdx = remember(lyrics, state.positionMs) {
        lyrics.lines.indexOfLast { it.timeMs <= state.positionMs }.coerceAtLeast(0)
    }
    LaunchedEffect(activeIdx) {
        if (tab == 1 && lyrics.lines.isNotEmpty())
            runCatching { listState.animateScrollToItem((activeIdx - 2).coerceAtLeast(0)) }
    }

    Box(Modifier.fillMaxSize()) {
        AlvandBackground(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(top = 44.dp, start = 18.dp, end = 18.dp, bottom = 18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ExpandMore, null, tint = Color.White) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.playing_from), color = Color.White.copy(0.6f), fontSize = 10.sp)
                    Text("Dreamy Night ✦", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.MoreHoriz, null, tint = Color.White)
            }
            // تب‌ها
            TabRow(selectedTabIndex = tab, containerColor = Color.Transparent, contentColor = Color.White,
                divider = {}, modifier = Modifier.clip(RoundedCornerShape(16.dp))) {
                tabs.forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i },
                        text = { Text(t, fontSize = 12.sp, fontWeight = FontWeight.Bold) })
                }
            }
            Spacer(Modifier.height(10.dp))

            when (tab) {
                0 -> {
                    Vinyl3D(state.isPlaying, Modifier.fillMaxWidth().height(300.dp))
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(current?.title ?: "Night Changes", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            Text(current?.artist ?: "One Direction", color = Color.White.copy(0.7f), fontSize = 13.sp)
                        }
                        val likedSongs by vm.liked.collectAsState()
                        val isLiked = likedSongs.contains(current?.id)
                        IconButton(onClick = { current?.let { vm.toggleLike(it.id) } }) {
                            Icon(
                                if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                null, tint = if (isLiked) Color(0xFFF3A8FF) else Color.White
                            )
                        }
                    }
                    Slider(
                        value = state.positionMs.toFloat(),
                        onValueChange = { vm.manager.seekTo(it.toLong()) },
                        valueRange = 0f..(state.durationMs.coerceAtLeast(1L).toFloat()),
                        colors = SliderDefaults.colors(thumbColor = AlvandPurple, activeTrackColor = AlvandPurple, inactiveTrackColor = Color.White.copy(alpha = 0.25f))
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(fmt(state.positionMs), color = Color.White.copy(0.6f), fontSize = 11.sp)
                        Text(fmt(state.durationMs), color = Color.White.copy(0.6f), fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { vm.manager.toggleShuffle() }) { Icon(Icons.Default.Shuffle, null, tint = if (state.shuffle) AlvandPurple else Color.White) }
                        IconButton(onClick = { vm.manager.prev() }) { Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(34.dp)) }
                        FilledIconButton(onClick = { vm.manager.togglePlayPause() }, modifier = Modifier.size(64.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = AlvandPurple)) {
                            Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, modifier = Modifier.size(32.dp))
                        }
                        IconButton(onClick = { vm.manager.next() }) { Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(34.dp)) }
                        IconButton(onClick = { vm.manager.toggleRepeatOne() }) { Icon(Icons.Default.Repeat, null, tint = if (state.repeatOne) AlvandPurple else Color.White) }
                    }
                    EqBars(state.isPlaying, Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp))
                }
                1 -> {
                    GlassCard(Modifier.fillMaxWidth().weight(1f)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${stringResource(R.string.tab_lyrics)} • ${lyrics.source}", color = Color.White, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { vm.refreshLyricsOnline() }) { Text(stringResource(R.string.get_lyrics)) }
                        }
                        if (loading) { LinearProgressIndicator(Modifier.fillMaxWidth()); Spacer(Modifier.height(8.dp)) }
                        if (lyrics.lines.isEmpty() && !loading) {
                            Text(stringResource(R.string.no_lyrics), color = Color.White.copy(0.7f), fontSize = 13.sp)
                        } else {
                            LazyColumn(state = listState, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(lyrics.lines.withIndex().toList()) { (i, l) ->
                                    val active = i == activeIdx
                                    Text(l.text, color = if (active) Color.White else Color.White.copy(0.55f),
                                        fontWeight = if (active) FontWeight.Black else FontWeight.Normal,
                                        fontSize = if (active) 17.sp else 14.sp, textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (active) Color.White.copy(0.18f) else Color.Transparent)
                                            .padding(6.dp))
                                }
                            }
                        }
                        OutlinedTextField(manual, { manual = it }, label = { Text(stringResource(R.string.manual_hint)) },
                            modifier = Modifier.fillMaxWidth().height(90.dp))
                        Button(onClick = { vm.saveLyricsManual(manual); manual = "" }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.save_lyrics))
                        }
                    }
                }
                2 -> {
                    EqPanel(eq, presets = vm.manager.eqManager.presets,
                        bandCount = vm.manager.eqManager.bandCount,
                        bandRange = vm.manager.eqManager.bandRange,
                        freqOf = { vm.manager.eqManager.bandFreqHz(it) },
                        onChange = { vm.updateAudio(it) })
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EqPanel(
    s: AudioSettings, presets: List<String>, bandCount: Int, bandRange: IntRange,
    freqOf: (Int) -> Int, onChange: (AudioSettings) -> Unit
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.eq_title), color = Color.White, fontWeight = FontWeight.Bold)
            Switch(checked = s.eqEnabled, onCheckedChange = { onChange(s.copy(eqEnabled = it)) })
        }
        // پریست‌ها
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.take(8).forEachIndexed { i, p ->
                FilterChip(selected = s.preset == i, onClick = { onChange(s.copy(preset = i)) }, label = { Text(p, fontSize = 11.sp) })
            }
        }
        // باندها
        val levels = remember(s.bandLevels, bandCount) {
            MutableList(bandCount) { s.bandLevels.getOrNull(it) ?: 0 }.toMutableStateList()
        }
        Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            repeat(bandCount) { i ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Slider(
                        value = levels[i].toFloat(),
                        onValueChange = {
                            levels[i] = it.toInt()
                            onChange(s.copy(preset = -1, bandLevels = levels.toList()))
                        },
                        valueRange = bandRange.first.toFloat()..bandRange.last.toFloat(),
                        modifier = Modifier.height(110.dp).width(36.dp)
                    )
                    Text("${freqOf(i) / 1000}k", color = Color.White.copy(0.6f), fontSize = 10.sp)
                }
            }
        }
        Divider(color = Color.White.copy(0.15f))
        Text(stringResource(R.string.bass_boost), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Slider(s.bassStrength.toFloat(), { onChange(s.copy(bassStrength = it.toInt())) }, valueRange = 0f..1000f)
        Text(stringResource(R.string.volume_boost, s.volumeBoostDb), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Slider(s.volumeBoostDb.toFloat(), { onChange(s.copy(volumeBoostDb = it.toInt())) }, valueRange = 0f..10f, steps = 9)
        Divider(color = Color.White.copy(0.15f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.denoise), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Switch(checked = s.noiseReduction, onCheckedChange = { onChange(s.copy(noiseReduction = it)) })
        }
        if (s.noiseReduction) {
            Text(stringResource(R.string.denoise_level, s.noiseLevel), color = Color.White.copy(0.7f), fontSize = 12.sp)
            Slider(s.noiseLevel.toFloat(), { onChange(s.copy(noiseLevel = it.toInt())) }, valueRange = 0f..100f)
            Text(stringResource(R.string.denoise_desc),
                color = Color.White.copy(0.55f), fontSize = 11.sp)
        }
        // نشانگر نئونی
        Box(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF6C4CF1), Color(0xFFF3A8FF)))),
            contentAlignment = Alignment.Center) {
            Text("☾ Alvand Audio Engine", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

private fun fmt(ms: Long): String {
    if (ms <= 0) return "0:00"
    val s = ms / 1000; return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}
