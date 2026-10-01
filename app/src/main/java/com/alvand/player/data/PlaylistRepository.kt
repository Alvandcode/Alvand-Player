package com.alvand.player.data

import android.net.Uri
import com.alvand.player.data.local.LibraryDao
import com.alvand.player.data.local.PlayHistoryEntity
import com.alvand.player.data.local.PlaylistEntity
import com.alvand.player.data.local.PlaylistSongEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** مدل دامنه پلی‌لیست برای UI */
data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int = 0
)

@Singleton
class PlaylistRepository @Inject constructor(
    private val dao: LibraryDao
) {
    fun observePlaylists(): Flow<List<PlaylistEntity>> = dao.observePlaylists()
    fun observePlaylistSongs(pid: Long): Flow<List<PlaylistSongEntity>> = dao.observePlaylistSongs(pid)
    fun observeRecent(limit: Int = 50): Flow<List<PlayHistoryEntity>> = dao.observeRecent(limit)

    suspend fun createPlaylist(name: String): Long {
        val clean = name.trim().take(60)
        require(clean.isNotBlank()) { "empty name" }
        return dao.insertPlaylist(PlaylistEntity(name = clean))
    }

    suspend fun renamePlaylist(id: Long, name: String) {
        dao.renamePlaylist(id, name.trim().take(60))
    }

    suspend fun deletePlaylist(id: Long) = dao.deletePlaylist(id)

    /** اضافه به پلی‌لیست بدون تکرار؛ position خودکار */
    suspend fun addToPlaylist(pid: Long, song: Song): Boolean {
        if (dao.countInPlaylist(pid, song.id) > 0) return false
        val pos = dao.nextPosition(pid)
        dao.insertPlaylistSong(
            PlaylistSongEntity(
                playlistId = pid,
                songId = song.id,
                uri = song.uri.toString(),
                title = song.title,
                artist = song.artist,
                album = song.album,
                durationMs = song.durationMs,
                isRemote = song.isRemote,
                position = pos
            )
        )
        return true
    }

    suspend fun removeFromPlaylist(pid: Long, songId: Long) =
        dao.removeFromPlaylist(pid, songId)

    /**
     * نوشتن ترتیب جدید پلی‌لیست.
     *
     * اگر فهرست ارسالی با محتوای فعلی دیتابیس هم‌خوان نباشد (کاربر همزمان
     * آهنگی اضافه یا حذف کرده) هیچ کاری نمی‌کنیم تا ترتیب خراب نشود.
     */
    suspend fun reorderPlaylistSongs(pid: Long, orderedSongIds: List<Long>) {
        val current = dao.observePlaylistSongs(pid).first()
        if (current.size != orderedSongIds.size) return
        if (current.map { it.songId }.toSet() != orderedSongIds.toSet()) return
        val currentPositions = current.associate { it.songId to it.position }
        orderedSongIds.forEachIndexed { index, songId ->
            if (currentPositions[songId] != index) dao.setSongPosition(pid, songId, index)
        }
    }

    /** جابه‌جایی یک آهنگ از جایی به جای دیگر؛ مرزها در خود DAO خوانده می‌شود */
    suspend fun movePlaylistSong(pid: Long, fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val order = dao.observePlaylistSongs(pid).first().map { it.songId }
        if (fromIndex !in order.indices || toIndex !in order.indices) return
        val moved = order.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        reorderPlaylistSongs(pid, moved)
    }

    suspend fun recordPlay(song: Song) {
        val prev = dao.historyFor(song.id)
        dao.upsertHistory(
            PlayHistoryEntity(
                id = prev?.id ?: 0,
                songId = song.id,
                title = song.title,
                artist = song.artist,
                playedAt = System.currentTimeMillis(),
                playCount = (prev?.playCount ?: 0) + 1
            )
        )
        // نگه‌داشت ۲۰۰ رکورد آخر
        dao.pruneHistory(System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000)
    }

    suspend fun clearHistory() = dao.clearHistory()

    fun entityToSong(e: PlaylistSongEntity): Song = Song(
        id = e.songId,
        title = e.title,
        artist = e.artist,
        album = e.album,
        uri = runCatching { Uri.parse(e.uri) }.getOrNull() ?: Uri.EMPTY,
        durationMs = e.durationMs,
        isRemote = e.isRemote
    )
}
