package com.alvand.player

import android.net.Uri
import com.alvand.player.data.PlaylistRepository
import com.alvand.player.data.Song
import com.alvand.player.data.local.LibraryDao
import com.alvand.player.data.local.PlayHistoryEntity
import com.alvand.player.data.local.PlaylistEntity
import com.alvand.player.data.local.PlaylistSongEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * قواعد دامنه‌ای پلی‌لیست و تاریخچه روی [PlaylistRepository] واقعی، با یک DAO
 * قلابی درون‌حافظه‌ای. این کار یک وابستگی جدید (mockk) لازم ندارد و منطق
 * برش نام، شمارهٔ ترتیب، جلوگیری از تکرار و شمارش پخش را واقعاً می‌آزماید.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlaylistRepositoryTest {

    /** DAO درون‌حافظه‌ای با همان ترتیب‌دهی که کوئری‌های واقعی دارند */
    private class FakeDao : LibraryDao {
        val playlists = MutableStateFlow<List<PlaylistEntity>>(emptyList())
        val playlistSongs = MutableStateFlow<List<PlaylistSongEntity>>(emptyList())
        val history = MutableStateFlow<List<PlayHistoryEntity>>(emptyList())

        var nextPlaylistId = 1L
        var nextRowId = 1L

        val prunedBefore = mutableListOf<Long>()

        override fun observePlaylists(): Flow<List<PlaylistEntity>> = playlists

        override suspend fun getPlaylists(): List<PlaylistEntity> = playlists.value

        override suspend fun insertPlaylist(e: PlaylistEntity): Long {
            val id = nextPlaylistId++
            playlists.value = playlists.value + e.copy(id = id)
            return id
        }

        override suspend fun deletePlaylist(id: Long) {
            playlists.value = playlists.value.filterNot { it.id == id }
            playlistSongs.value = playlistSongs.value.filterNot { it.playlistId == id }
        }

        override suspend fun renamePlaylist(id: Long, name: String) {
            playlists.value = playlists.value.map {
                if (it.id == id) it.copy(name = name) else it
            }
        }

        override fun observePlaylistSongs(pid: Long): Flow<List<PlaylistSongEntity>> =
            playlistSongs.map { rows ->
                rows.filter { it.playlistId == pid }
                    .sortedWith(compareBy({ it.position }, { it.addedAt }))
            }

        override suspend fun countInPlaylist(pid: Long, sid: Long): Int =
            playlistSongs.value.count { it.playlistId == pid && it.songId == sid }

        override suspend fun insertPlaylistSong(e: PlaylistSongEntity): Long {
            val id = nextRowId++
            playlistSongs.value = playlistSongs.value + e.copy(id = id)
            return id
        }

        override suspend fun removeFromPlaylist(pid: Long, sid: Long) {
            playlistSongs.value = playlistSongs.value
                .filterNot { it.playlistId == pid && it.songId == sid }
        }

        override suspend fun nextPosition(pid: Long): Int =
            (playlistSongs.value.filter { it.playlistId == pid }.maxOfOrNull { it.position } ?: -1) + 1

        override suspend fun setSongPosition(pid: Long, sid: Long, position: Int) {
            playlistSongs.value = playlistSongs.value.map {
                if (it.playlistId == pid && it.songId == sid) it.copy(position = position) else it
            }
        }

        /** کمک‌کار تست: شناسه‌ها به ترتیب فعلی پلی‌لیست */
        fun order(pid: Long): List<Long> =
            playlistSongs.value
                .filter { it.playlistId == pid }
                .sortedWith(compareBy({ it.position }, { it.addedAt }))
                .map { it.songId }

        /** کمک‌کار تست: شماره‌های ترتیب به ترتیب نمایش */
        fun positions(pid: Long): List<Int> =
            playlistSongs.value
                .filter { it.playlistId == pid }
                .sortedWith(compareBy({ it.position }, { it.addedAt }))
                .map { it.position }

        /** کمک‌کار تست: شمارهٔ ترتیب ذخیره‌شدهٔ یک آهنگ */
        fun positionOf(pid: Long, songId: Long): Int =
            playlistSongs.value.first { it.playlistId == pid && it.songId == songId }.position

        override fun observeRecent(limit: Int): Flow<List<PlayHistoryEntity>> =
            history.map { rows -> rows.sortedByDescending { it.playedAt }.take(limit) }

        override suspend fun historyFor(sid: Long): PlayHistoryEntity? =
            history.value.firstOrNull { it.songId == sid }

        override suspend fun upsertHistory(e: PlayHistoryEntity) {
            val rows = history.value.filterNot { it.id == e.id }
            history.value = rows + e.copy(id = if (e.id == 0L) nextRowId++ else e.id)
        }

        override suspend fun pruneHistory(before: Long) {
            prunedBefore += before
            history.value = history.value.filter { it.playedAt >= before }
        }

        override suspend fun clearHistory() {
            history.value = emptyList()
        }
    }

    private lateinit var dao: FakeDao
    private lateinit var repo: PlaylistRepository

    @Before
    fun setUp() {
        dao = FakeDao()
        repo = PlaylistRepository(dao)
    }

    private fun song(id: Long = 42L, remote: Boolean = false) = Song(
        id = id,
        title = "Track $id",
        artist = "Artist",
        album = "Album",
        uri = Uri.parse("https://example.com/track-$id.mp3"),
        durationMs = 180_000L,
        isRemote = remote
    )

    @Test
    fun `createPlaylist trims the name and caps it at 60 characters`() = runTest {
        repo.createPlaylist("   " + "x".repeat(80) + "   ")
        assertEquals(60, dao.playlists.value.single().name.length)
        assertEquals("x".repeat(60), dao.playlists.value.single().name)
    }

    @Test
    fun `createPlaylist keeps a short name trimmed`() = runTest {
        repo.createPlaylist("  My Mix  ")
        assertEquals("My Mix", dao.playlists.value.single().name)
    }

    @Test
    fun `createPlaylist rejects blank names`() = runTest {
        listOf("", "   ", "\t", "\n").forEach { bad ->
            val failed = runCatching { repo.createPlaylist(bad) }.isFailure
            assertTrue("'$bad' must be rejected", failed)
        }
        assertTrue("nothing must be inserted", dao.playlists.value.isEmpty())
    }

    @Test
    fun `addToPlaylist appends at the next position and caches metadata`() = runTest {
        val pid = repo.createPlaylist("Mix")
        assertTrue(repo.addToPlaylist(pid, song(id = 42L)))
        assertTrue(repo.addToPlaylist(pid, song(id = 43L)))

        val rows = dao.playlistSongs.value.sortedBy { it.position }
        assertEquals(2, rows.size)
        assertEquals(listOf(0, 1), rows.map { it.position })
        assertEquals(listOf(42L, 43L), rows.map { it.songId })
        // متادیتا کش می‌شود تا با پاک شدن فایل، پلی‌لیست از هم نپاشد
        assertEquals("Track 42", rows.first().title)
        assertEquals(180_000L, rows.first().durationMs)
        assertTrue(rows.first().uri.startsWith("https://"))
    }

    @Test
    fun `addToPlaylist refuses a duplicate and leaves positions untouched`() = runTest {
        val pid = repo.createPlaylist("Mix")
        assertTrue(repo.addToPlaylist(pid, song(id = 42L)))
        val positionAfterFirst = dao.playlistSongs.value.single().position

        assertFalse(repo.addToPlaylist(pid, song(id = 42L)))

        assertEquals(1, dao.playlistSongs.value.size)
        assertEquals(positionAfterFirst, dao.playlistSongs.value.single().position)
    }

    @Test
    fun `addToPlaylist keeps the remote flag so streams survive in a playlist`() = runTest {
        val pid = repo.createPlaylist("Radio")
        repo.addToPlaylist(pid, song(id = 9L, remote = true))
        val row = dao.playlistSongs.value.single()
        assertTrue(row.isRemote)
        assertTrue(row.uri.startsWith("https://"))
    }

    @Test
    fun `the same song can live in two different playlists`() = runTest {
        val a = repo.createPlaylist("A")
        val b = repo.createPlaylist("B")
        assertTrue(repo.addToPlaylist(a, song(id = 42L)))
        assertTrue(repo.addToPlaylist(b, song(id = 42L)))
        assertEquals(2, dao.playlistSongs.value.count { it.songId == 42L })
    }

    @Test
    fun `removing a song frees its position only for future appends`() = runTest {
        val pid = repo.createPlaylist("Mix")
        repo.addToPlaylist(pid, song(id = 1L))
        repo.addToPlaylist(pid, song(id = 2L))
        repo.removeFromPlaylist(pid, 1L)

        assertEquals(1, dao.playlistSongs.value.size)
        // nextPosition روی ردیف باقی‌مانده ادامه می‌دهد، پس ترتیب یکتا می‌ماند
        repo.addToPlaylist(pid, song(id = 3L))
        assertEquals(setOf(1, 2), dao.playlistSongs.value.map { it.position }.toSet())
    }

    @Test
    fun `deleting a playlist also drops its songs`() = runTest {
        val pid = repo.createPlaylist("Temp")
        repo.addToPlaylist(pid, song(id = 1L))
        repo.deletePlaylist(pid)

        assertTrue(dao.playlists.value.isEmpty())
        assertTrue(dao.playlistSongs.value.isEmpty())
    }

    @Test
    fun `recordPlay increments playCount on the same row`() = runTest {
        repo.recordPlay(song())
        repo.recordPlay(song())
        repo.recordPlay(song())

        val rows = dao.history.value
        assertEquals("must not duplicate rows", 1, rows.size)
        assertEquals(3, rows.single().playCount)
    }

    @Test
    fun `recordPlay keeps different songs on separate rows`() = runTest {
        repo.recordPlay(song(id = 1L))
        repo.recordPlay(song(id = 2L))
        assertEquals(2, dao.history.value.size)
        assertTrue(dao.history.value.all { it.playCount == 1 })
    }

    @Test
    fun `recordPlay prunes with a 90 day window`() = runTest {
        val before = System.currentTimeMillis()
        repo.recordPlay(song())

        assertEquals(1, dao.prunedBefore.size)
        val ninetyDays = 90L * 24 * 60 * 60 * 1000
        val cutoff = dao.prunedBefore.single()
        assertTrue(cutoff <= before - ninetyDays + 5000L)
        assertTrue(cutoff >= before - ninetyDays - 5000L)
    }

    @Test
    fun `movePlaylistSong swaps two neighbours`() = runTest {
        val pid = repo.createPlaylist("Mix")
        listOf(1L, 2L, 3L).forEach { repo.addToPlaylist(pid, song(id = it)) }

        repo.movePlaylistSong(pid, fromIndex = 0, toIndex = 1)

        assertEquals(listOf(2L, 1L, 3L), dao.order(pid))
        assertEquals(listOf(0, 1, 2), dao.positions(pid))
    }

    @Test
    fun `movePlaylistSong can push an item to the end and pull it back`() = runTest {
        val pid = repo.createPlaylist("Mix")
        listOf(1L, 2L, 3L, 4L).forEach { repo.addToPlaylist(pid, song(id = it)) }

        repo.movePlaylistSong(pid, fromIndex = 0, toIndex = 3)
        assertEquals(listOf(2L, 3L, 4L, 1L), dao.order(pid))

        repo.movePlaylistSong(pid, fromIndex = 3, toIndex = 0)
        assertEquals(listOf(1L, 2L, 3L, 4L), dao.order(pid))
    }

    @Test
    fun `movePlaylistSong keeps positions contiguous with no gaps or duplicates`() = runTest {
        val pid = repo.createPlaylist("Mix")
        listOf(1L, 2L, 3L, 4L, 5L).forEach { repo.addToPlaylist(pid, song(id = it)) }

        repo.movePlaylistSong(pid, 4, 0)
        repo.movePlaylistSong(pid, 0, 2)
        repo.movePlaylistSong(pid, 3, 4)

        val pos = dao.positions(pid)
        assertEquals("positions must be 0..n-1 with no repeats", (0..4).toList(), pos.sorted())
        assertEquals(5, pos.toSet().size)
    }

    @Test
    fun `movePlaylistSong ignores out of range indices`() = runTest {
        val pid = repo.createPlaylist("Mix")
        listOf(1L, 2L).forEach { repo.addToPlaylist(pid, song(id = it)) }
        val before = dao.order(pid)

        repo.movePlaylistSong(pid, fromIndex = 0, toIndex = 9)
        repo.movePlaylistSong(pid, fromIndex = -1, toIndex = 0)
        repo.movePlaylistSong(pid, fromIndex = 5, toIndex = 0)

        assertEquals(before, dao.order(pid))
    }

    @Test
    fun `movePlaylistSong with the same index is a no-op`() = runTest {
        val pid = repo.createPlaylist("Mix")
        listOf(1L, 2L).forEach { repo.addToPlaylist(pid, song(id = it)) }
        val before = dao.order(pid)
        repo.movePlaylistSong(pid, 1, 1)
        assertEquals(before, dao.order(pid))
    }

    @Test
    fun `reorder ignores a list that does not match the stored playlist`() = runTest {
        val pid = repo.createPlaylist("Mix")
        listOf(1L, 2L, 3L).forEach { repo.addToPlaylist(pid, song(id = it)) }
        val before = dao.order(pid)

        // طول متفاوت: مثل وقتی که همزمان آهنگی از جای دیگری حذف شده
        repo.reorderPlaylistSongs(pid, listOf(3L, 1L))
        // اعضای متفاوت: مثل وقتی که کاربر روی پلی‌لیست دیگری کار می‌کند
        repo.reorderPlaylistSongs(pid, listOf(9L, 8L, 7L))

        assertEquals(before, dao.order(pid))
    }

    @Test
    fun `a removed song does not leave the order in a broken state`() = runTest {
        val pid = repo.createPlaylist("Mix")
        listOf(1L, 2L, 3L).forEach { repo.addToPlaylist(pid, song(id = it)) }
        repo.movePlaylistSong(pid, 0, 2)
        assertEquals(listOf(2L, 3L, 1L), dao.order(pid))

        repo.removeFromPlaylist(pid, 2L)

        assertEquals(listOf(3L, 1L), dao.order(pid))
        // ردیف‌های باقی‌مانده ترتیب یکتا و بدون فاصله نگه می‌دارند
        val pos = dao.positions(pid)
        assertEquals("no duplicate positions", pos.size, pos.toSet().size)
        assertTrue("positions ascend in display order", pos == pos.sorted())
    }

    @Test
    fun `entityToSong round-trips every cached field`() {
        val e = PlaylistSongEntity(
            playlistId = 1L, songId = 42L,
            uri = "content://media/external/audio/media/42",
            title = "T", artist = "A", album = "Al",
            durationMs = 1234L, isRemote = false, position = 0
        )
        val s = repo.entityToSong(e)
        assertEquals(42L, s.id)
        assertEquals("T", s.title)
        assertEquals("A", s.artist)
        assertEquals("Al", s.album)
        assertEquals(1234L, s.durationMs)
        assertEquals("content://media/external/audio/media/42", s.uri.toString())
    }

    @Test
    fun `entityToSong survives a malformed uri instead of throwing`() {
        // نکتهٔ قرارداد: entityToSong نباید به‌خاطر دادهٔ خراب کرش کند.
        // Uri.parse روی ورودی بی‌معنا خطا نمی‌دهد و همان را برمی‌گرداند،
        // پس اینجا فقط «پرتاب نشدن» و حفظ بقیهٔ فیلدها را می‌آزماییم.
        val e = PlaylistSongEntity(
            playlistId = 1L, songId = 1L, uri = "::not a uri::",
            title = "T", artist = "A", album = "", durationMs = 0L,
            isRemote = false, position = 0
        )
        val s = repo.entityToSong(e)
        assertEquals(1L, s.id)
        assertEquals("T", s.title)
        // خطایی پرتاب نشد و Uri.parse همان رشته را برگرداند
        assertEquals("::not a uri::", s.uri.toString())
    }
}