package com.alvand.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.AppViewModel
import com.alvand.player.R
import com.alvand.player.data.Song
import com.alvand.player.ui.components.*
import com.alvand.player.ui.theme.AlvandCardBorder

/** خانه */
@Composable
fun HomeScreen(
    vm: AppViewModel,
    onOpenPlaylist: () -> Unit,
    onOpenPlayer: () -> Unit,
    onPickFile: () -> Unit
) {
    val songs by vm.songs.collectAsState()
    val state by vm.playerState.collectAsState()
    var showLink by remember { mutableStateOf(false) }
    var showLang by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf("") }
    var linkError by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        AlvandBackground(Modifier.fillMaxSize())
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 48.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Menu, null, tint = Color.White)
                    Text("Alvand", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Row {
                        IconButton(onClick = { showLang = true }) {
                            Icon(Icons.Default.Language, null, tint = Color.White)
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Notifications, null, tint = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.good_evening), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(stringResource(R.string.vibe_subtitle), color = Color.White.copy(0.75f), fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                // سرچ + دکمه لینک مستقیم و فایل
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, tint = Color.White.copy(0.7f))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.search_hint), color = Color.White.copy(0.6f), fontSize = 13.sp)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = { showLink = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Link, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.direct_link))
                    }
                    FilledTonalButton(onClick = onPickFile, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.audio_file))
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.for_you), color = Color.White, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.see_all), color = Color.White.copy(0.6f), fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val cards = listOf(
                        "Dreamy Night" to Color(0xFFB9A7FF), "Happy Hits" to Color(0xFFFFC9A8),
                        "Chill Vibes" to Color(0xFFF3A8FF)
                    )
                    itemsIndexed(cards) { i, (t, c) ->
                        TiltGlassCard(Modifier.width(130.dp).clickable { onOpenPlaylist() }) {
                            Box(Modifier.height(86.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(c, Color.White.copy(0.4f))))) {
                                EqBars(state.isPlaying && i == 0, Modifier.align(Alignment.BottomStart).padding(8.dp), 12)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(t, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(stringResource(R.string.playlist), color = Color.White.copy(0.65f), fontSize = 11.sp)
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.recently_played), color = Color.White, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.see_all), color = Color.White.copy(0.6f), fontSize = 12.sp)
                }
            }
            itemsIndexed(songs.take(12)) { i, s ->
                SongRow(s, i, playing = state.current?.id == s.id && state.isPlaying,
                    onClick = { vm.playList(songs, i); onOpenPlayer() })
            }
        }
        // مینی‌پلیر شیشه‌ای پایین
        state.current?.let { c ->
            Box(Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                GlassCard(Modifier.fillMaxWidth().clickable { onOpenPlayer() }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFF3A8FF), Color(0xFF6C4CF1)))), contentAlignment = Alignment.Center) {
                            Text("☾", color = Color.White)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                            Text(c.artist, color = Color.White.copy(0.65f), fontSize = 11.sp, maxLines = 1)
                        }
                        IconButton(onClick = { vm.manager.togglePlayPause() }) {
                            Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showLang) LanguageDialog(onDismiss = { showLang = false })

    if (showLink) AlertDialog(
        onDismissRequest = { showLink = false },
        title = { Text(stringResource(R.string.direct_link_title)) },
        text = {
            Column {
                Text(stringResource(R.string.direct_link_msg), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(link, { link = it; linkError = false }, placeholder = { Text("https://…/song.mp3") }, singleLine = true, isError = linkError)
                if (linkError) Text(stringResource(R.string.invalid_link), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (vm.playDirectLink(link)) { showLink = false; link = ""; onOpenPlayer() }
                else linkError = true
            }) { Text(stringResource(R.string.play)) }
        },
        dismissButton = { TextButton(onClick = { showLink = false }) { Text(stringResource(R.string.close)) } }
    )
}

@Composable
private fun SongRow(s: Song, index: Int, playing: Boolean, onClick: () -> Unit) {
    val bg = listOf(Color(0xFFB9A7FF), Color(0xFFFFC9A8), Color(0xFF6C4CF1), Color(0xFFF3A8FF))[index % 4]
    Surface(
        Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, AlvandCardBorder)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(bg), contentAlignment = Alignment.Center) {
                Text("♪", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(s.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                Text(s.artist, color = Color.White.copy(0.65f), fontSize = 11.sp, maxLines = 1)
            }
            if (playing) EqBars(true, count = 8)
            Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White)
        }
    }
}
