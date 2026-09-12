package com.alvand.player.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
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

    suspend fun loadLocalSongs(limit: Int = 8000): List<Song> = withContext(Dispatchers.IO) {
        val songs = ArrayList<Song>(1024)
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
        // موسیقی‌های واقعی: نه رینگتون کوتاه، نه فایل خراب بدون مدت
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 30000"
        // سورت پایدار: DATE_ADDED درشت‌دانه است، پس سورت ثانویه _ID
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC, ${MediaStore.Audio.Media._ID} DESC"
        try {
            context.contentResolver.query(
                collection, projection, selection, null,
                sortOrder
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                while (c.moveToNext()) {
                    if (songs.size >= limit) break // سقف تا OOM ندهد؛ Paging3 برای فاز بعد
                    val id = try { c.getLong(idCol) } catch (_: Exception) { continue }
                    val uri: Uri = ContentUris.withAppendedId(collection, id)
                    val dur = try {
                        if (c.isNull(durCol)) 0L else c.getLong(durCol)
                    } catch (_: Exception) { 0L }
                    if (dur <= 0) continue
                    songs += Song(
                        id = id, // MediaStore همیشه >=0 — با آیدی منفی ریموت کالیشن ندارد
                        title = c.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Unknown",
                        artist = c.getString(artistCol)?.takeIf { it.isNotBlank() } ?: "Unknown Artist",
                        album = c.getString(albumCol) ?: "",
                        uri = uri,
                        durationMs = dur
                    )
                }
            }
        } catch (e: SecurityException) {
            // پرمیشن revoke شده — caller باید پیام allow_access نشان دهد، نه لیست خالی بی‌صدا
            Log.w("Songs", "MediaStore permission denied", e)
            throw e
        } catch (e: Exception) {
            Log.w("Songs", "MediaStore query failed", e)
        }
        songs
    }

    /** آهنگ‌های نمونه برای اولین اجرا (فقط وقتی هیچ لوکال/ریموت نیست) */
    fun demoPlaylist(): List<Song> = listOf(
        Song(-1, "Night Changes", "One Direction", "Dreamy Night",
            Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"), 266000, true, "audio/mpeg"),
        Song(-2, "Yellow", "Coldplay", "Dreamy Night",
            Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"), 266000, true, "audio/mpeg"),
        Song(-3, "Photograph", "Ed Sheeran", "Dreamy Night",
            Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"), 269000, true, "audio/mpeg"),
        Song(-4, "Until I Found You", "Stephen Sanchez", "Dreamy Night",
            Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"), 178000, true, "audio/mpeg")
    )
}
