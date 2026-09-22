package com.alvand.player.data

/** حالت مرتب‌سازی کتابخانه — در SettingsRepo persist می‌شود */
object LibrarySort {
    const val DEFAULT = 0 // ترتیب اسکن (جدیدترین اول)
    const val TITLE_AZ = 1
    const val ARTIST_AZ = 2
    const val DURATION_DESC = 3
    const val DURATION_ASC = 4
}

/**
 * منطق خالص فیلتر + سورت کتابخانه — بدون وابستگی اندروید تا unit-test شود.
 * قانون: favoritesOnly اول، بعد search، بعد sort پایدار.
 */
object LibraryFilter {

    fun filterAndSort(
        songs: List<Song>,
        query: String,
        sortMode: Int,
        liked: Set<Long>,
        favoritesOnly: Boolean
    ): List<Song> {
        var out = songs
        if (favoritesOnly) {
            out = out.filter { it.id in liked }
        }
        val q = query.trim().lowercase()
        if (q.isNotEmpty()) {
            out = out.filter { s ->
                s.title.lowercase().contains(q) ||
                    s.artist.lowercase().contains(q) ||
                    s.album.lowercase().contains(q)
            }
        }
        return when (sortMode) {
            LibrarySort.TITLE_AZ -> out.sortedWith(compareBy({ it.title.lowercase() }, { it.artist.lowercase() }))
            LibrarySort.ARTIST_AZ -> out.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
            LibrarySort.DURATION_DESC -> out.sortedByDescending { it.durationMs }
            LibrarySort.DURATION_ASC -> out.sortedBy { it.durationMs }
            else -> out
        }
    }

    /** ایندکس امن برای playList روی لیست فیلترشده: id آهنگ را به ایندکس صف واقعی مپ کن */
    fun indexOf(queue: List<Song>, songId: Long): Int =
        queue.indexOfFirst { it.id == songId }.coerceAtLeast(0)

    /** گروه‌بندی آلبوم/خواننده برای تب‌های کتابخانه — بدون DB، از متادیتای MediaStore */
    fun groupByAlbum(songs: List<Song>): List<AlbumGroup> =
        songs.groupBy { it.album.trim().ifBlank { "Unknown Album" } }
            .map { (album, list) ->
                AlbumGroup(
                    name = album,
                    artist = list.groupBy { it.artist }.maxByOrNull { it.value.size }?.key ?: "",
                    songs = list,
                    artworkSong = list.firstOrNull()
                )
            }
            .sortedBy { it.name.lowercase() }

    fun groupByArtist(songs: List<Song>): List<ArtistGroup> =
        songs.groupBy { it.artist.trim().ifBlank { "Unknown Artist" } }
            .map { (artist, list) ->
                ArtistGroup(name = artist, songs = list, albums = list.map { it.album }.distinct().size)
            }
            .sortedBy { it.name.lowercase() }
}

data class AlbumGroup(
    val name: String,
    val artist: String,
    val songs: List<Song>,
    val artworkSong: Song? = null
)

data class ArtistGroup(
    val name: String,
    val songs: List<Song>,
    val albums: Int = 1
)
