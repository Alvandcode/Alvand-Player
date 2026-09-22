package com.alvand.player

import android.net.Uri
import com.alvand.player.data.LibraryFilter
import com.alvand.player.data.Song
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class LibraryFilterTest {

    private fun song(id: Long, title: String, artist: String, album: String = "", dur: Long = 180000L) =
        Song(id, title, artist, album, Uri.parse("content://media/external/audio/media/$id"), dur)

    @Test
    fun `search filters by title artist album`() {
        val list = listOf(
            song(1, "Yellow", "Coldplay"),
            song(2, "Photograph", "Ed Sheeran"),
            song(3, "Night", "Coldplay", album = "Dreamy")
        )
        assertEquals(2, LibraryFilter.filterAndSort(list, "coldplay", 0, emptySet(), false).size)
        assertEquals(1, LibraryFilter.filterAndSort(list, "photo", 0, emptySet(), false).size)
        assertEquals(1, LibraryFilter.filterAndSort(list, "dreamy", 0, emptySet(), false).size)
        assertEquals(3, LibraryFilter.filterAndSort(list, "", 0, emptySet(), false).size)
    }

    @Test
    fun `favoritesOnly keeps only liked`() {
        val list = listOf(song(1, "A", "X"), song(2, "B", "Y"), song(3, "C", "Z"))
        val out = LibraryFilter.filterAndSort(list, "", 0, setOf(2L), true)
        assertEquals(listOf(2L), out.map { it.id })
    }

    @Test
    fun `sort title az is case-insensitive`() {
        val list = listOf(song(1, "yellow", "b"), song(2, "Apple", "a"), song(3, "Moon", "c"))
        val out = LibraryFilter.filterAndSort(list, "", 1, emptySet(), false)
        assertEquals(listOf("Apple", "Moon", "yellow"), out.map { it.title })
    }

    @Test
    fun `sort longest first`() {
        val list = listOf(song(1, "A", "X", dur = 1000), song(2, "B", "Y", dur = 5000))
        val out = LibraryFilter.filterAndSort(list, "", 3, emptySet(), false)
        assertEquals(2L, out.first().id)
    }

    @Test
    fun `combined fav plus search plus sort`() {
        val list = listOf(
            song(1, "Yellow", "Coldplay", dur = 2000),
            song(2, "Yellow Submarine", "Beatles", dur = 5000),
            song(3, "Green", "Coldplay", dur = 9000)
        )
        val out = LibraryFilter.filterAndSort(list, "yellow", 3, setOf(1L, 2L), true)
        assertEquals(listOf(2L, 1L), out.map { it.id })
    }
}
