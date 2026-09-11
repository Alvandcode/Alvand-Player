package com.alvand.player.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** خواندن آهنگ‌های دستگاه از MediaStore — همه فرمت‌های صوتی */
@Singleton
class SongRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun loadLocalSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
            // NOTE: ستون DATA عمداً نیست — از اندروید ۱۰ به بعد deprecated است و روی بعضی گوشی‌ها کوئری را می‌شکند
        )
        // موسیقی‌های واقعی (نه نوتیفیکیشن/رینگتون خیلی کوتاه)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        context.contentResolver.query(
            collection, projection, selection, null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val uri: Uri = ContentUris.withAppendedId(collection, id)
                songs += Song(
                    id = id,
                    title = c.getString(titleCol) ?: "Unknown",
                    artist = c.getString(artistCol) ?: "Unknown Artist",
                    album = c.getString(albumCol) ?: "",
                    uri = uri,
                    durationMs = c.getLong(durCol)
                )
            }
        }
        songs
    }

    /** آهنگ‌های نمونه برای اولین اجرا */
    fun demoPlaylist(): List<Song> = listOf(
        Song(-1, "Night Changes", "One Direction", "Dreamy Night",
            Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"), 266000, true),
        Song(-2, "Yellow", "Coldplay", "Dreamy Night",
            Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"), 266000, true),
        Song(-3, "Photograph", "Ed Sheeran", "Dreamy Night",
            Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"), 269000, true),
        Song(-4, "Until I Found You", "Stephen Sanchez", "Dreamy Night",
            Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"), 178000, true)
    )
}
