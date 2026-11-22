package com.alvand.player

import android.net.Uri
import com.alvand.player.data.Song
import com.alvand.player.player.QueueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

/** رفت‌وبرگشت صف روی دیسک — همان مسیری که بازیابی بعد از ری‌استارت می‌رود */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class QueueStoreTest {

    private fun dir(): File = File(RuntimeEnvironment.getApplication().filesDir, "test_queue")

    private fun song(id: Long, title: String = "T$id", remote: Boolean = false) = Song(
        id = id,
        title = title,
        artist = "Artist $id",
        album = "Alb",
        uri = if (remote) Uri.parse("https://example.com/$id.mp3")
        else Uri.parse("content://media/external/audio/media/$id"),
        durationMs = 123_456L,
        isRemote = remote,
        mimeHint = if (remote) "audio/mpeg" else null
    )

    @Test
    fun `save and load round trip preserves songs and index`() {
        val d = dir(); d.deleteRecursively()
        val songs = listOf(song(5), song(-2_000_000, "Remote", remote = true), song(9))
        QueueStore.save(d, songs, index = 1)

        val (loaded, idx) = QueueStore.load(d)!!
        assertEquals(songs.map { it.id }, loaded.map { it.id })
        assertEquals(songs.map { it.title }, loaded.map { it.title })
        assertEquals(songs[1].uri.toString(), loaded[1].uri.toString())
        assertEquals(true, loaded[1].isRemote)
        assertEquals("audio/mpeg", loaded[1].mimeHint)
        assertEquals(1, idx)
        d.deleteRecursively()
    }

    @Test
    fun `index is clamped into range`() {
        val d = dir(); d.deleteRecursively()
        QueueStore.save(d, listOf(song(1), song(2)), index = 99)
        assertEquals(1, QueueStore.load(d)!!.second)
        d.deleteRecursively()
    }

    @Test
    fun `missing file returns null and position returns zero`() {
        val d = dir(); d.deleteRecursively()
        assertNull(QueueStore.load(d))
        assertEquals(0L, QueueStore.loadPosition(d))
    }

    @Test
    fun `corrupt file falls back to null instead of crashing`() {
        val d = dir(); d.mkdirs()
        File(d, "last_queue.json").writeText("not json at all")
        assertNull(QueueStore.load(d))
        d.deleteRecursively()
    }

    @Test
    fun `position save and load`() {
        val d = dir(); d.deleteRecursively()
        QueueStore.savePosition(d, 42_500L)
        assertEquals(42_500L, QueueStore.loadPosition(d))
        QueueStore.clear(d)
        assertEquals(0L, QueueStore.loadPosition(d))
        assertNull(QueueStore.load(d))
    }

    @Test
    fun `empty songs list is treated as no queue`() {
        val d = dir(); d.deleteRecursively()
        QueueStore.save(d, emptyList(), 0)
        assertNull(QueueStore.load(d))
        d.deleteRecursively()
    }
}
