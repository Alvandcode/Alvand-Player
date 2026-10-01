package com.alvand.player.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
    val ctx = LocalContext.current
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
                                        // بازخورد صریح: آهنگ تکراری بی‌صدا رد نشود
                                        vm.addToPlaylist(pl.id, song) { added ->
                                            Toast.makeText(
                                                ctx,
                                                ctx.getString(
                                                    if (added) R.string.added_to_playlist
                                                    else R.string.already_in_playlist
                                                ),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            if (added) onDismiss()
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = pal.sub, modifier = Modifier.size(20.dp))
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
fun PlaylistsTab(vm: AppViewModel, onRequestNotificationPermission: () -> Unit = {}) {
    val playlists by vm.playlistList.collectAsState()
    val selectedId by vm.selectedPlaylistId.collectAsState()
    val selectedSongs by vm.selectedPlaylistSongs.collectAsState()
    val pal = LocalAP.current
    val ctx = LocalContext.current
    var showCreate by remember { mutableStateOf(false) }
    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }
    // ترتیب خوش‌بینانه پس از جابه‌جایی؛ با تغییر محتوای پلی‌لیست بی‌اثر می‌شود
    var localOrder by remember { mutableStateOf<List<Long>?>(null) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
        // عنوان تب در نوار تب‌های شیت کتابخانه است، پس اینجا فقط کنش‌ها
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
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
                        .clickable {
                            // باز کردن پلی‌لیست دیگر یعنی ترتیب قبلی دیگر ربطی ندارد
                            localOrder = null
                            vm.selectPlaylist(if (expanded) null else pl.id)
                        }
                        .padding(vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = pal.ink, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(pl.name, color = pal.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                // مثل ردیف‌های صف، اجازهٔ اعلان هم لازم است
                                onRequestNotificationPermission()
                                vm.playPlaylistSongs(pl.id) { ok ->
                                    if (!ok) {
                                        Toast.makeText(
                                            ctx,
                                            ctx.getString(R.string.playlist_empty),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
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
                            // ترتیب خوش‌بینانه: تا وقتی جریان دیتابیس به ترتیب جدید
                            // نرسیده، همان چیزی را نشان می‌دهیم که کاربر جابه‌جا کرده
                            val flowIds = selectedSongs.map { it.songId }
                            val shownIds = localOrder
                                ?.takeIf { it.toSet() == flowIds.toSet() }
                                ?: flowIds
                            val rows = shownIds.mapNotNull { id ->
                                selectedSongs.firstOrNull { it.songId == id }
                            }

                            fun move(from: Int, to: Int) {
                                if (to !in shownIds.indices) return
                                localOrder = shownIds.toMutableList().apply { add(to, removeAt(from)) }
                                vm.movePlaylistSong(pl.id, from, to)
                            }

                            Column(
                                Modifier.padding(start = 32.dp, top = 4.dp)
                                    // ارتفاع محدود: یک پلی‌لیست بلند نباید از شیت بیرون بزند
                                    .heightIn(max = 240.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(stringResource(R.string.songs_n, rows.size), color = pal.sub, fontSize = 12.sp)
                                Spacer(Modifier.height(4.dp))
                                rows.forEachIndexed { index, e ->
                                    Row(
                                        Modifier.fillMaxWidth()
                                            .clickable {
                                                // تپ روی آهنگ: پخش پلی‌لیست از همین‌جا
                                                onRequestNotificationPermission()
                                                vm.playPlaylistSongs(pl.id, index) { ok ->
                                                    if (!ok) {
                                                        Toast.makeText(
                                                            ctx,
                                                            ctx.getString(R.string.playlist_empty),
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(e.title, color = pal.ink, fontSize = 13.sp, maxLines = 1, modifier = Modifier.weight(1f))
                                        // بالا/پایین: در RTL هم معنی خودش را نگه می‌دارد
                                        IconButton(
                                            onClick = { move(index, index - 1) },
                                            enabled = index > 0,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowUp,
                                                contentDescription = stringResource(R.string.move_up),
                                                tint = if (index > 0) pal.sub else pal.line,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { move(index, index + 1) },
                                            enabled = index < rows.size - 1,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowDown,
                                                contentDescription = stringResource(R.string.move_down),
                                                tint = if (index < rows.size - 1) pal.sub else pal.line,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                localOrder = null
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
fun RecentTab(vm: AppViewModel, onRequestNotificationPermission: () -> Unit = {}) {
    val recent by vm.recentHistory.collectAsState()
    val pal = LocalAP.current
    val ctx = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
        // عنوان تب در نوار تب‌های شیت کتابخانه است، پس اینجا فقط کنش‌ها
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            if (recent.isNotEmpty()) {
                TextButton(onClick = { vm.clearHistory() }) {
                    Text(stringResource(R.string.clear_history), fontSize = 12.sp)
                }
            }
        }
        if (recent.isEmpty()) {
            Text(stringResource(R.string.no_history), color = pal.sub, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
        } else {
            // ارتفاع محدود تا تاریخچهٔ بلند از شیت بیرون نزند
            Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                recent.take(30).forEach { h ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable {
                                // اگر فایل آهنگ هنوز در کتابخانه نباشد، بی‌صدا رد می‌شود
                                if (vm.playHistorySong(h.songId)) {
                                    onRequestNotificationPermission()
                                } else {
                                    Toast.makeText(
                                        ctx,
                                        ctx.getString(R.string.song_gone),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .padding(vertical = 7.dp),
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
}
