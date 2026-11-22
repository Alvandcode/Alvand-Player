package com.alvand.player.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.AppViewModel
import com.alvand.player.R
import com.alvand.player.audio.AudioSettings
import com.alvand.player.ui.theme.*
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

/** منوی اصلی (≡): همه امکانات اضافی اینجاست */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuSheet(
    vm: AppViewModel,
    onPickFile: () -> Unit,
    onPickBackground: () -> Unit,
    hasAudioPermission: Boolean,
    onRequestAudioPermission: () -> Unit,
    onOpenAbout: () -> Unit,
    onPlayLink: () -> Unit,
    onDismiss: () -> Unit
) {
    var showLink by remember { mutableStateOf(false) }
    var showLang by remember { mutableStateOf(false) }
    var showEq by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var showBg by remember { mutableStateOf(false) }
    val sleep by vm.manager.sleepState.collectAsState()
    val scanning by vm.isScanning.collectAsState()
    val pal = LocalAP.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = pal.sheet
    ) {
        Column(Modifier.padding(horizontal = 8.dp).padding(bottom = 36.dp)) {
            MenuItem(Icons.Default.Link, stringResource(R.string.direct_link)) { showLink = true }
            MenuItem(Icons.Default.FolderOpen, stringResource(R.string.audio_file)) { onPickFile(); onDismiss() }
            // اسکنر کتابخانه: آهنگ‌های جدید گوشی را پیدا و به پلی‌لیست اضافه می‌کند
            MenuItem(
                Icons.Default.Refresh,
                if (scanning) stringResource(R.string.scanning)
                else stringResource(R.string.scan_songs)
            ) {
                if (hasAudioPermission) vm.scanDeviceSongs()
                else onRequestAudioPermission()
                onDismiss()
            }
            MenuItem(
                Icons.Default.Bedtime,
                if (sleep.active) {
                    "${stringResource(R.string.sleep_timer)} • " +
                        com.alvand.player.player.SleepTimer.formatRemaining(sleep.remainingMs)
                } else stringResource(R.string.sleep_timer)
            ) { showSleep = true }
            MenuItem(Icons.Default.Tune, stringResource(R.string.tab_eq)) { showEq = true }
            MenuItem(Icons.Default.Mic, stringResource(R.string.tab_lyrics)) { showLyrics = true }
            MenuItem(Icons.Default.DarkMode, stringResource(R.string.theme)) { showTheme = true }
            MenuItem(Icons.Default.Image, stringResource(R.string.background)) { showBg = true }
            MenuItem(Icons.Default.Language, stringResource(R.string.language)) { showLang = true }
            MenuItem(Icons.Default.Info, stringResource(R.string.about)) { onOpenAbout(); onDismiss() }
        }
    }

    if (showLink) DirectLinkDialog(vm, onDone = { showLink = false; onPlayLink() }, onDismiss = { showLink = false })
    if (showLang) LanguageDialog(onDismiss = { showLang = false })
    if (showEq) EqSheet(vm, onDismiss = { showEq = false })
    if (showLyrics) LyricsSheet(vm, onDismiss = { showLyrics = false })
    if (showSleep) SleepTimerDialog(vm, onDismiss = { showSleep = false })
    if (showTheme) ThemeDialog(vm, onDismiss = { showTheme = false })
    if (showBg) BackgroundDialog(vm, onPickImage = onPickBackground, onDismiss = { showBg = false })
}

@Composable
private fun MenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    val pal = LocalAP.current
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = pal.ink)
        Spacer(Modifier.width(14.dp))
        Text(title, color = pal.ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
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
    val pal = LocalAP.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = pal.sheet) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 36.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.eq_title), color = pal.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Switch(checked = eq.eqEnabled, onCheckedChange = { vm.updateAudio(eq.copy(eqEnabled = it)) })
            }
            // بازخورد اتصال: فقط isPlaying را کالکت کن تا position هر ۵۰۰ms کل شیت را نسوزاند
            val isPlayingFlow = remember(vm) { vm.playerState.map { it.isPlaying } }
            val isPlaying by isPlayingFlow.collectAsState(initial = false)
            @Suppress("UNUSED_EXPRESSION")
            isPlaying
            if (!vm.manager.eqManager.isAttached()) {
                Text(
                    if (eq.eqEnabled) stringResource(R.string.eq_need_play)
                    else stringResource(R.string.eq_off),
                    color = pal.sub, fontSize = 12.sp
                )
                Spacer(Modifier.height(4.dp))
            }
            // ردیف پریست‌ها + دکمهٔ ریست تا کاربر بتواند هر تنظیمی را به حالت پیش‌فرض برگرداند
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LazyRow(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(vm.manager.eqManager.presets.take(8).withIndex().toList()) { (i, p) ->
                        FilterChip(
                            selected = eq.preset == i,
                            onClick = { vm.updateAudio(eq.copy(preset = i)) },
                            label = { Text(p, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = pal.ink, selectedLabelColor = pal.bg
                            )
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                val isDefault = eq == AudioSettings()
                // ریست باید واقعاً همه‌چیز را برگرداند، ولی وقتی همه‌چیز پیش‌فرض
                // است دکمه غیرفعاله تا بی‌فایده نزنی
                TextButton(
                    onClick = { vm.updateAudio(AudioSettings()) },
                    enabled = !isDefault,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh, null,
                        tint = if (isDefault) pal.line else pal.ink,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.eq_reset),
                        color = if (isDefault) pal.line else pal.ink,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            val mgr = vm.manager.eqManager
            // درگ روی اسلایدرها فقط حالت محلی را عوض می‌کند؛ افکت صوتی (binder)
            // فقط در پایان درگ یک‌بار کامیت می‌شود تا صد تنظیم در ثانیه نفرستیم
            var bandsDraft by remember(eq.bandLevels) {
                mutableStateOf(eq.bandLevels.toMutableList())
            }
            repeat(mgr.bandCount) { i ->
                val hz = mgr.bandFreqHz(i)
                val label = when {
                    hz <= 0 -> ""
                    hz < 1000 -> "${hz}Hz"
                    hz % 1000 == 0 -> "${hz / 1000}k"
                    else -> String.format(java.util.Locale.US, "%.1fk", hz / 1000f)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = pal.sub, fontSize = 11.sp, modifier = Modifier.width(44.dp))
                    Slider(
                        value = (bandsDraft.getOrNull(i) ?: 0).toFloat(),
                        onValueChange = { v ->
                            bandsDraft = bandsDraft.toMutableList().apply {
                                if (i < size) this[i] = v.roundToInt()
                            }
                        },
                        onValueChangeFinished = {
                            vm.updateAudio(eq.copy(preset = -1, bandLevels = bandsDraft.toList()))
                        },
                        valueRange = mgr.bandRange.first.toFloat()..mgr.bandRange.last.toFloat(),
                        colors = SliderDefaults.colors(thumbColor = pal.ink, activeTrackColor = pal.ink, inactiveTrackColor = pal.track),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Text(stringResource(R.string.bass_boost), color = pal.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            var bassDraft by remember(eq.bassStrength) { mutableStateOf(eq.bassStrength.toFloat()) }
            Slider(bassDraft, { bassDraft = it },
                onValueChangeFinished = { vm.updateAudio(eq.copy(bassStrength = bassDraft.roundToInt())) },
                valueRange = 0f..1000f,
                colors = SliderDefaults.colors(thumbColor = pal.ink, activeTrackColor = pal.ink, inactiveTrackColor = pal.track))
            var volDraft by remember(eq.volumeBoostDb) { mutableStateOf(eq.volumeBoostDb.toFloat()) }
            Text(stringResource(R.string.volume_boost, volDraft.roundToInt()), color = pal.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Slider(volDraft, { volDraft = it },
                onValueChangeFinished = { vm.updateAudio(eq.copy(volumeBoostDb = volDraft.roundToInt())) },
                valueRange = 0f..10f, steps = 9,
                colors = SliderDefaults.colors(thumbColor = pal.ink, activeTrackColor = pal.ink, inactiveTrackColor = pal.track))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.denoise), color = pal.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Switch(checked = eq.noiseReduction, onCheckedChange = { vm.updateAudio(eq.copy(noiseReduction = it)) })
            }
            if (eq.noiseReduction) {
                var noiseDraft by remember(eq.noiseLevel) { mutableStateOf(eq.noiseLevel.toFloat()) }
                Text(stringResource(R.string.denoise_level, noiseDraft.roundToInt()), color = pal.sub, fontSize = 12.sp)
                Slider(noiseDraft, { noiseDraft = it },
                    onValueChangeFinished = { vm.updateAudio(eq.copy(noiseLevel = noiseDraft.roundToInt())) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = pal.ink, activeTrackColor = pal.ink, inactiveTrackColor = pal.track))
                Text(stringResource(R.string.denoise_desc), color = pal.sub, fontSize = 11.sp)
            }
        }
    }
}

/** نمایش ترجمه‌شدهٔ سورس لیریک به‌جای کد خام داخلی (lrc-file و امثال آن) */
@Composable
fun lyricsSourceLabel(source: String): String = stringResource(
    when (source) {
        "manual" -> R.string.lyrics_source_manual
        "online" -> R.string.lyrics_source_online
        "lrc-file", "embedded", "lyrics-cache" -> R.string.lyrics_source_local
        else -> R.string.lyrics_source_none
    }
)

/** شیت لیریک (سیاه‌سفید) */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LyricsSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    val lyrics by vm.lyrics.collectAsState()
    val loading by vm.lyricsLoading.collectAsState()
    val onlineEnabled by vm.onlineLyricsEnabled.collectAsState()
    // فقط position را کالکت کن (نه کل PlayerUiState) + جستجوی دودویی به‌جای اسکن خطی O(n)
    val positionFlow = remember(vm) { vm.playerState.map { it.positionMs } }
    val pos by positionFlow.collectAsState(initial = 0L)
    var manual by remember { mutableStateOf("") }
    val activeIdx by remember(lyrics, pos) {
        derivedStateOf {
            val lines = lyrics.lines
            if (lines.isEmpty()) 0
            else {
                var lo = 0
                var hi = lines.size - 1
                var ans = 0
                while (lo <= hi) {
                    val mid = (lo + hi) ushr 1
                    if (lines[mid].timeMs <= pos) { ans = mid; lo = mid + 1 } else hi = mid - 1
                }
                ans
            }
        }
    }
    val pal = LocalAP.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = pal.sheet) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 36.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${stringResource(R.string.tab_lyrics)} • ${lyricsSourceLabel(lyrics.source)}",
                    color = pal.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                TextButton(onClick = { vm.refreshLyricsOnline() }) { Text(stringResource(R.string.get_lyrics)) }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = pal.ink)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.online_lyrics), color = pal.ink, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.online_lyrics_desc), color = pal.sub, fontSize = 11.sp)
                }
                Switch(
                    checked = onlineEnabled,
                    onCheckedChange = vm::setOnlineLyricsEnabled
                )
            }
            if (lyrics.lines.isEmpty() && !loading) {
                Text(stringResource(R.string.no_lyrics), color = pal.sub, fontSize = 13.sp)
            } else {
                // اسکرول خودکار به خط فعال: خط فعلی وسط کادر می‌ماند و کاربر
                // لازم نیست دنبالش اسکرول کند. اسپیسر بالا/پایین ارتفاع نصف کادر را
                // جبران می‌کند تا اسکرول‌تُ‌آیتِم واقعاً وسط‌چین شود.
                val listState = rememberLazyListState()
                LaunchedEffect(activeIdx, lyrics.lines.size) {
                    if (lyrics.lines.isNotEmpty()) {
                        // اگر کاربر همین حالا خودش اسکرول می‌کند، او را نکش
                        if (!listState.isScrollInProgress) {
                            listState.animateScrollToItem(activeIdx + 1)
                        }
                    }
                }
                LazyColumn(
                    Modifier.heightIn(max = 340.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(Modifier.height(150.dp)) }
                    itemsIndexed(lyrics.lines) { i, l ->
                        val active = i == activeIdx
                        Text(
                            l.text,
                            color = if (active) pal.ink else pal.sub.copy(alpha = 0.75f),
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            fontSize = if (active) 16.sp else 14.sp,
                            textAlign = TextAlign.Center,
                            // لیریک برای خواندن است؛ دو خط پیچش می‌خورد نه marquee تک‌خطی
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .padding(4.dp)
                        )
                    }
                    item { Spacer(Modifier.height(150.dp)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(manual, { manual = it }, label = { Text(stringResource(R.string.manual_hint)) },
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.saveLyricsManual(manual); manual = "" },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = pal.ink, contentColor = pal.bg)
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

/** کارت لیریک شیشه‌ای با فاصله از اطراف + خط فعال متحرک */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LyricsPreviewCard(vm: AppViewModel, onOpenFull: () -> Unit) {
    val lyrics by vm.lyrics.collectAsState()
    val positionFlow = remember(vm) { vm.playerState.map { it.positionMs } }
    val pos by positionFlow.collectAsState(initial = 0L)
    val pal = LocalAP.current
    val activeIdx by remember(lyrics, pos) {
        derivedStateOf {
            val lines = lyrics.lines
            if (lines.isEmpty()) 0
            else {
                var lo = 0
                var hi = lines.size - 1
                var ans = 0
                while (lo <= hi) {
                    val mid = (lo + hi) ushr 1
                    if (lines[mid].timeMs <= pos) { ans = mid; lo = mid + 1 } else hi = mid - 1
                }
                ans
            }
        }
    }
    GlassCard(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        onClick = onOpenFull
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${stringResource(R.string.tab_lyrics)} • ${lyricsSourceLabel(lyrics.source)}",
                    color = pal.ink, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = pal.sub, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(6.dp))
            if (lyrics.lines.isEmpty()) {
                Text(stringResource(R.string.no_lyrics), color = pal.sub, fontSize = 12.sp, maxLines = 2)
            } else {
                MarqueeLine(
                    lyrics.lines[activeIdx.coerceIn(lyrics.lines.indices)].text,
                    pal.ink, 15.sp, true, Modifier.fillMaxWidth()
                )
                lyrics.lines.getOrNull(activeIdx + 1)?.let {
                    Text(it.text, color = pal.sub, fontSize = 12.sp, maxLines = 1, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
