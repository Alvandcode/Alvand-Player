package com.alvand.player.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.AppViewModel
import com.alvand.player.R
import com.alvand.player.data.Song
import com.alvand.player.ui.theme.LocalAP

/** دیالوگ ساخت پلی‌لیست جدید */
@Composable
fun CreatePlaylistDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_playlist)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(stringResource(R.string.playlist_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        vm.createPlaylistAsync(name.trim()) { onDismiss() }
                    }
                }
            ) { Text(stringResource(R.string.create)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}

/** دیالوگ افزودن آهنگ به پلی‌لیست */
@Composable
fun AddToPlaylistDialog(vm: AppViewModel, song: Song, onDismiss: () -> Unit) {
    val playlists by vm.playlistList.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    val pal = LocalAP.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_playlist), fontSize = 16.sp) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (playlists.isEmpty()) {
                    Text(stringResource(R.string.no_playlists), color = pal.sub, fontSize = 13.sp)
                } else {
                    LazyColumn(Modifier.heightIn(max = 300.dp)) {
                        items(playlists, key = { it.id }) { pl ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable {
                                        vm.addToPlaylist(pl.id, song) { onDismiss() }
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.QueueMusic, null, tint = pal.sub, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(pl.name, color = pal.ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { showCreate = true }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.new_playlist))
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
    if (showCreate) CreatePlaylistDialog(vm, onDismiss = { showCreate = false })
}

/** تب پلی‌لیست‌ها + اخیراً پخش‌شده — داخل شیت کتابخانه */
@Composable
fun PlaylistsTab(vm: AppViewModel) {
    val playlists by vm.playlistList.collectAsState()
    val selectedId by vm.selectedPlaylistId.collectAsState()
    val selectedSongs by vm.selectedPlaylistSongs.collectAsState()
    val pal = LocalAP.current
    var showCreate by remember { mutableStateOf(false) }
    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.tab_playlists),
                color = pal.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(2.dp))
                Text(stringResource(R.string.new_playlist), fontSize = 12.sp)
            }
        }
        if (playlists.isEmpty()) {
            Text(stringResource(R.string.no_playlists), color = pal.sub, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
        } else {
            playlists.forEach { pl ->
                val expanded = selectedId == pl.id
                Column(
                    Modifier.fillMaxWidth()
                        .clickable { vm.selectPlaylist(if (expanded) null else pl.id) }
                        .padding(vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QueueMusic, null, tint = pal.ink, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(pl.name, color = pal.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { vm.playPlaylistSongs(pl.id) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.PlayArrow, null, tint = pal.sub, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { confirmDeleteId = pl.id }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.DeleteOutline, null, tint = pal.sub, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (expanded) {
                        if (selectedSongs.isEmpty()) {
                            Text("— 0", color = pal.sub, fontSize = 12.sp, modifier = Modifier.padding(start = 32.dp, top = 4.dp))
                        } else {
                            Column(Modifier.padding(start = 32.dp, top = 4.dp)) {
                                Text(stringResource(R.string.songs_n, selectedSongs.size), color = pal.sub, fontSize = 12.sp)
                                Spacer(Modifier.height(4.dp))
                                selectedSongs.take(50).forEach { e ->
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(e.title, color = pal.ink, fontSize = 13.sp, maxLines = 1, modifier = Modifier.weight(1f))
                                        IconButton(
                                            onClick = {
                                                vm.removeFromPlaylist(pl.id, e.songId)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = pal.sub, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = pal.track.copy(alpha = 0.5f))
            }
        }
    }
    if (showCreate) CreatePlaylistDialog(vm, onDismiss = { showCreate = false })
    confirmDeleteId?.let { pid ->
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text(stringResource(R.string.delete_playlist)) },
            confirmButton = {
                TextButton(onClick = { vm.deletePlaylist(pid); confirmDeleteId = null }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteId = null }) { Text(stringResource(R.string.close)) } }
        )
    }
}

/** تب اخیراً پخش‌شده */
@Composable
fun RecentTab(vm: AppViewModel) {
    val recent by vm.recentHistory.collectAsState()
    val pal = LocalAP.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.tab_recent),
                color = pal.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            if (recent.isNotEmpty()) {
                TextButton(onClick = { vm.clearHistory() }) {
                    Text(stringResource(R.string.clear_history), fontSize = 12.sp)
                }
            }
        }
        if (recent.isEmpty()) {
            Text(stringResource(R.string.no_history), color = pal.sub, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
        } else {
            recent.take(30).forEach { h ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.History, null, tint = pal.sub, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(h.title, color = pal.ink, fontSize = 13.5.sp, maxLines = 1)
                        Text(h.artist, color = pal.sub, fontSize = 12.sp, maxLines = 1)
                    }
                    Text("×${h.playCount}", color = pal.sub, fontSize = 11.sp)
                }
            }
        }
    }
}
