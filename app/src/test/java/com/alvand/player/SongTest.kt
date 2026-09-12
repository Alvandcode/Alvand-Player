package com.alvand.player

import com.alvand.player.data.Song
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class SongTest {

    @Test
    fun `fromDirectLink rejects non-http`() {
        assertNull(Song.fromDirectLink("file:///sdcard/a.mp3"))
        assertNull(Song.fromDirectLink("not a url"))
        assertNull(Song.fromDirectLink(""))
    }

    @Test
    fun `fromDirectLink rejects html url without audio ext`() {
        assertNull(Song.fromDirectLink("https://example.com/page.html"))
        assertNull(Song.fromDirectLink("https://example.com/text"))
    }

    @Test
    fun `fromDirectLink accepts mp3 and m3u8`() {
        assertNotNull(Song.fromDirectLink("https://example.com/song.mp3"))
        assertNotNull(Song.fromDirectLink("https://example.com/live.m3u8?token=1"))
        assertNotNull(Song.fromDirectLink("https://example.com/stream.mpd"))
    }

    @Test
    fun `isSupportedPath does not accept every http`() {
        assertFalse(Song.isSupportedPath("https://example.com/page.html"))
        assertFalse(Song.isSupportedPath("https://example.com/"))
        assertTrue(Song.isSupportedPath("https://example.com/a.mp3"))
        assertTrue(Song.isSupportedPath("/sdcard/a.flac"))
        assertFalse(Song.isSupportedPath("/sdcard/notes.txt"))
    }

    @Test
    fun `remote ids never collide with MediaStore ids`() {
        val a = Song.fromDirectLink("https://example.com/a.mp3")!!
        val b = Song.fromDirectLink("https://example.com/b.mp3")!!
        assertTrue(a.id < 0)
        assertTrue(b.id < 0)
        assertTrue(a.id < -999_999L)
    }

    @Test
    fun `localFileId is negative and stable`() {
        val uri = android.net.Uri.parse("content://media/external/audio/media/42")
        val id1 = Song.localFileId(uri)
        val id2 = Song.localFileId(uri)
        assertEquals(id1, id2)
        assertTrue(id1 < 0)
    }

    @Test
    fun `mime hint resolved`() {
        assertEquals("audio/mpeg", Song.mimeForUrl("https://x/a.mp3?x=1"))
        assertEquals("application/x-mpegURL", Song.mimeForUrl("https://x/live.m3u8"))
        assertNull(Song.mimeForUrl("https://x/noext"))
    }
}
