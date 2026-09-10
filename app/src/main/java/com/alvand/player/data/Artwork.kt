package com.alvand.player.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.alvand.player.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** استخراج کاور امبدد آهنگ (با کش حافظه) */
object Artwork {

    private val cache = LinkedHashMap<Long, Bitmap?>()

    suspend fun load(song: Song, ctx: Context): Bitmap? = withContext(Dispatchers.IO) {
        synchronized(cache) {
            if (cache.containsKey(song.id)) return@withContext cache[song.id]
        }
        val bmp: Bitmap? = runCatching {
            if (song.isRemote) return@runCatching null
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(ctx, song.uri)
                mmr.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            } finally {
                runCatching { mmr.release() }
            }
        }.getOrNull()
        synchronized(cache) {
            if (cache.size > 60) cache.clear()
            cache[song.id] = bmp
        }
        bmp
    }
}
