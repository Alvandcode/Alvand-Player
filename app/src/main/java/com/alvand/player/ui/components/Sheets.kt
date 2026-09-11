package com.alvand.player.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.AppViewModel
import com.alvand.player.R
import com.alvand.player.ui.theme.*

/** منوی اصلی (≡): همه امکانات اضافی اینجاست */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuSheet(
    vm: AppViewModel,
    onPickFile: () -> Unit,
    onOpenAbout: () -> Unit,
    onPlayLink: () -> Unit,
    onDismiss: () -> Unit
) {
    var showLink by remember { mutableStateOf(false) }
    var showLang by remember { mutableStateOf(false) }
    var showEq by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(Modifier.padding(horizontal = 8.dp).padding(bottom = 36.dp)) {
            MenuItem(Icons.Default.Link, stringResource(R.string.direct_link)) { showLink = true }
            MenuItem(Icons.Default.FolderOpen, stringResource(R.string.audio_file)) { onPickFile(); onDismiss() }
            MenuItem(Icons.Default.Tune, stringResource(R.string.tab_eq)) { showEq = true }
            MenuItem(Icons.Default.Mic, stringResource(R.string.tab_lyrics)) { showLyrics = true }
            MenuItem(Icons.Default.Language, stringResource(R.string.language)) { showLang = true }
            MenuItem(Icons.Default.Info, stringResource(R.string.about)) { onOpenAbout(); onDismiss() }
        }
    }

    if (showLink) DirectLinkDialog(vm, onDone = { showLink = false; onPlayLink() }, onDismiss = { showLink = false })
    if (showLang) LanguageDialog(onDismiss = { showLang = false })
    if (showEq) EqSheet(vm, onDismiss = { showEq = false })
    if (showLyrics) LyricsSheet(vm, onDismiss = { showLyrics = false })
}

@Composable
private fun MenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MonoInk)
        Spacer(Modifier.width(14.dp))
        Text(title, color = MonoInk, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

/** دیالوگ لینک مستقیم */
@Composable
fun DirectLinkDialog(vm: AppViewModel, onDone: () -> Unit, onDismiss: () -> Unit) {
    var link by remember { mutableStateOf("") }
    var err by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.direct_link_title)) },
        text = {
            Column {
                Text(stringResource(R.string.direct_link_msg), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(link, { link = it; err = false },
                    placeholder = { Text("https://…/song.mp3") }, singleLine = true, isError = err)
                if (err) Text(stringResource(R.string.invalid_link),
                    color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (vm.playDirectLink(link)) onDone() else err = true
            }) { Text(stringResource(R.string.play)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}

/** شیت اکولایزر / تقویت / نویز (سیاه‌سفید) */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EqSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    val eq by vm.eqSettings.collectAsState()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 36.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.eq_title), color = MonoInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Switch(checked = eq.eqEnabled, onCheckedChange = { vm.updateAudio(eq.copy(eqEnabled = it)) })
            }
            // بازخورد اتصال: تا سشن صوتی نیاید، تغییر بی‌اثر است
            vm.playerState.collectAsState().value
            if (!vm.manager.eqManager.isAttached()) {
                Text(
                    if (eq.eqEnabled) stringResource(R.string.eq_need_play)
                    else stringResource(R.string.eq_off),
                    color = MonoSub, fontSize = 12.sp
                )
                Spacer(Modifier.height(4.dp))
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vm.manager.eqManager.presets.take(8).withIndex().toList()) { (i, p) ->
                    FilterChip(
                        selected = eq.preset == i,
                        onClick = { vm.updateAudio(eq.copy(preset = i)) },
                        label = { Text(p, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MonoInk, selectedLabelColor = Color.White
                        )
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            val mgr = vm.manager.eqManager
            repeat(mgr.bandCount) { i ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${mgr.bandFreqHz(i) / 1000}k", color = MonoSub, fontSize = 11.sp, modifier = Modifier.width(38.dp))
                    Slider(
                        value = (eq.bandLevels.getOrNull(i) ?: 0).toFloat(),
                        onValueChange = {
                            val lv = MutableList(mgr.bandCount) { k -> eq.bandLevels.getOrNull(k) ?: 0 }
                            lv[i] = it.toInt()
                            vm.updateAudio(eq.copy(preset = -1, bandLevels = lv))
                        },
                        valueRange = mgr.bandRange.first.toFloat()..mgr.bandRange.last.toFloat(),
                        colors = SliderDefaults.colors(thumbColor = MonoInk, activeTrackColor = MonoInk, inactiveTrackColor = MonoTrack),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Text(stringResource(R.string.bass_boost), color = MonoInk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Slider(eq.bassStrength.toFloat(), { vm.updateAudio(eq.copy(bassStrength = it.toInt())) },
                valueRange = 0f..1000f,
                colors = SliderDefaults.colors(thumbColor = MonoInk, activeTrackColor = MonoInk, inactiveTrackColor = MonoTrack))
            Text(stringResource(R.string.volume_boost, eq.volumeBoostDb), color = MonoInk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Slider(eq.volumeBoostDb.toFloat(), { vm.updateAudio(eq.copy(volumeBoostDb = it.toInt())) },
                valueRange = 0f..10f, steps = 9,
                colors = SliderDefaults.colors(thumbColor = MonoInk, activeTrackColor = MonoInk, inactiveTrackColor = MonoTrack))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.denoise), color = MonoInk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Switch(checked = eq.noiseReduction, onCheckedChange = { vm.updateAudio(eq.copy(noiseReduction = it)) })
            }
            if (eq.noiseReduction) {
                Text(stringResource(R.string.denoise_level, eq.noiseLevel), color = MonoSub, fontSize = 12.sp)
                Slider(eq.noiseLevel.toFloat(), { vm.updateAudio(eq.copy(noiseLevel = it.toInt())) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = MonoInk, activeTrackColor = MonoInk, inactiveTrackColor = MonoTrack))
                Text(stringResource(R.string.denoise_desc), color = MonoSub, fontSize = 11.sp)
            }
        }
    }
}

/** شیت لیریک (سیاه‌سفید) */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LyricsSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    val lyrics by vm.lyrics.collectAsState()
    val loading by vm.lyricsLoading.collectAsState()
    val pos = vm.playerState.collectAsState().value.positionMs
    var manual by remember { mutableStateOf("") }
    val activeIdx = remember(lyrics, pos) {
        lyrics.lines.indexOfLast { it.timeMs <= pos }.coerceAtLeast(0)
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 36.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${stringResource(R.string.tab_lyrics)} • ${lyrics.source}",
                    color = MonoInk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                TextButton(onClick = { vm.refreshLyricsOnline() }) { Text(stringResource(R.string.get_lyrics)) }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = MonoInk)
            if (lyrics.lines.isEmpty() && !loading) {
                Text(stringResource(R.string.no_lyrics), color = MonoSub, fontSize = 13.sp)
            } else {
                LazyColumn(Modifier.heightIn(max = 340.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(lyrics.lines.withIndex().toList()) { (i, l) ->
                                    val active = i == activeIdx
                                    Text(
                                        l.text,
                                        color = if (active) MonoInk else MonoSub.copy(0.6f),
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = if (active) 16.sp else 14.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        modifier = Modifier.fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .padding(4.dp)
                                            .then(if (active) Modifier.basicMarquee(
                                                iterations = Int.MAX_VALUE,
                                                spacing = MarqueeSpacing(16.dp)
                                            ) else Modifier)
                                    )
                                }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(manual, { manual = it }, label = { Text(stringResource(R.string.manual_hint)) },
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.saveLyricsManual(manual); manual = "" },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MonoInk, contentColor = Color.White)
            ) { Text(stringResource(R.string.save_lyrics)) }
        }
    }
}

/** متن متحرک (فقط وقتی از کادر بزند بیرون حرکت می‌کند) */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarqueeLine(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    bold: Boolean,
    modifier: Modifier = Modifier
) {
    Text(
        text,
        color = color,
        fontSize = fontSize,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        maxLines = 1,
        modifier = modifier.basicMarquee(
            iterations = Int.MAX_VALUE,
            spacing = MarqueeSpacing(16.dp)
        )
    )
}

/** کارت لیریک با فاصله از اطراف + خط فعال متحرک */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LyricsPreviewCard(vm: AppViewModel, onOpenFull: () -> Unit) {
    val lyrics by vm.lyrics.collectAsState()
    val pos = vm.playerState.collectAsState().value.positionMs
    val activeIdx = remember(lyrics, pos) {
        lyrics.lines.indexOfLast { it.timeMs <= pos }.coerceAtLeast(0)
    }
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)
            .clickable { onOpenFull() },
        shape = RoundedCornerShape(20.dp),
        color = MonoBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, MonoLine)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${stringResource(R.string.tab_lyrics)} • ${lyrics.source}",
                    color = MonoInk, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.OpenInNew, null, tint = MonoSub, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(6.dp))
            if (lyrics.lines.isEmpty()) {
                Text(stringResource(R.string.no_lyrics), color = MonoSub, fontSize = 12.sp, maxLines = 2)
            } else {
                MarqueeLine(
                    lyrics.lines[activeIdx.coerceIn(lyrics.lines.indices)].text,
                    MonoInk, 15.sp, true, Modifier.fillMaxWidth()
                )
                lyrics.lines.getOrNull(activeIdx + 1)?.let {
                    Text(it.text, color = MonoSub, fontSize = 12.sp, maxLines = 1, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
