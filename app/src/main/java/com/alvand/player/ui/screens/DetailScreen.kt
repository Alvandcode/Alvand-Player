package com.alvand.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.alvand.player.ui.components.*

/** صفحه پلی‌لیست (تم تیره بنفش) */
@Composable
fun DetailScreen(vm: AppViewModel, onBack: () -> Unit, onOpenPlayer: () -> Unit) {
    val songs by vm.songs.collectAsState()
    val state by vm.playerState.collectAsState()
    Box(Modifier.fillMaxSize()) {
        AlvandBackground(Modifier.fillMaxSize(), dark = true)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 130.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 44.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                    Text(stringResource(R.string.playing_from), color = Color.White.copy(0.6f), fontSize = 11.sp)
                    Icon(Icons.Default.MoreHoriz, null, tint = Color.White)
                }
                Text("Dreamy Night", color = Color.White.copy(0.6f), fontSize = 11.sp, modifier = Modifier.padding(start = 16.dp))
                Spacer(Modifier.height(10.dp))
                TiltGlassCard(Modifier.padding(horizontal = 18.dp)) {
                    Text("Dreamy Night ✦", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
                    Text("Feel the calm. Chase the stars.", color = Color.White.copy(0.7f), fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(Color.White.copy(0.3f)), contentAlignment = Alignment.Center) {
                            Text("☾", fontSize = 14.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.songs_count, songs.size), color = Color.White.copy(0.7f), fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { vm.playList(songs, 0); onOpenPlayer() }, shape = RoundedCornerShape(20.dp)) {
                            Icon(Icons.Default.PlayArrow, null); Text(stringResource(R.string.play_all))
                        }
                        IconButton(onClick = {}) { Icon(Icons.Default.FavoriteBorder, null, tint = Color.White) }
                        IconButton(onClick = {}) { Icon(Icons.Default.Download, null, tint = Color.White) }
                    }
                    EqBars(state.isPlaying, Modifier.padding(top = 8.dp))
                }
            }
            itemsIndexed(songs) { i, s ->
                val active = state.current?.id == s.id
                ListItem(
                    headlineContent = { Text(s.title, color = Color.White, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal) },
                    supportingContent = { Text(s.artist, color = Color.White.copy(0.6f)) },
                    leadingContent = {
                        Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF6C4CF1), Color(0xFFF3A8FF)))),
                            contentAlignment = Alignment.Center) {
                            Text("♪", color = Color.White)
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = { vm.playList(songs, i); onOpenPlayer() }) {
                            Icon(if (active && state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White)
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { vm.playList(songs, i); onOpenPlayer() }
                )
            }
        }
        state.current?.let { c ->
            Box(Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                GlassCard(Modifier.fillMaxWidth().clickable { onOpenPlayer() }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
}
