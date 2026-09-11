package com.alvand.player.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.alvand.player.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** رنگ‌های استخراج‌شده از کاور (برای تم داینامیک) */
data class ArtColors(
    val vibrant: Int,
    val dark: Int,
    /** رنگ غالب تصویر — بهترین گزینه برای accent پس‌زمینه */
    val dominant: Int = vibrant,
    /** رنگ ملایم برای گرادیان */
    val muted: Int = dark,
    /** آیا کاور روشن است؟ (برای انتخاب رنگ متن روی آرت) */
    val isLight: Boolean = false
)

/** استخراج کاور امبدد آهنگ (با کش حافظه) */
object Artwork {

    private val cache = LinkedHashMap<Long, Bitmap?>()
    private val colorCache = LinkedHashMap<Long, ArtColors?>()

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

    /** پالت رنگی کاور (null اگر کاوری نباشد) */
    suspend fun colors(song: Song, ctx: Context): ArtColors? = withContext(Dispatchers.IO) {
        synchronized(colorCache) {
            if (colorCache.containsKey(song.id)) return@withContext colorCache[song.id]
        }
        val bmp = load(song, ctx)
        val c = bmp?.let {
            runCatching {
                val p = androidx.palette.graphics.Palette.from(it).generate()
                val black = android.graphics.Color.BLACK
                val vibrant = p.getVibrantColor(p.getDominantColor(black))
                val dark = p.getDarkVibrantColor(p.getDarkMutedColor(black))
                val dominant = p.getDominantColor(vibrant)
                val muted = p.getMutedColor(dark)
                // روشنایی غالب برای تصمیم متن تیره/روشن روی آرت
                val r = android.graphics.Color.red(dominant)
                val g = android.graphics.Color.green(dominant)
                val b = android.graphics.Color.blue(dominant)
                val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
                ArtColors(
                    vibrant = vibrant,
                    dark = dark,
                    dominant = dominant,
                    muted = muted,
                    isLight = luminance > 0.6
                )
            }.getOrNull()
        }
        synchronized(colorCache) {
            if (colorCache.size > 60) colorCache.clear()
            colorCache[song.id] = c
        }
        c
    }
}
