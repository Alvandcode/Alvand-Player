package com.alvand.player.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

/** استخراج کاور امبدد آهنگ (با کش LRU بایتی + دان‌سمپل) */
object Artwork {

    // کش بایتی: ~1/8 حافظه اپ، مبنا بایت نه تعداد (۶۰ بیت‌مپ فول قبلاً OOM می‌داد)
    private val bitmapCache: LruCache<Long, Bitmap> = object : LruCache<Long, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 8).toInt().coerceAtMost(32 * 1024 * 1024)
    ) {
        override fun sizeOf(key: Long, value: Bitmap): Int = value.byteCount.coerceAtLeast(1)
    }
    private val colorCache: LruCache<Long, ArtColors> = object : LruCache<Long, ArtColors>(120) {
        override fun sizeOf(key: Long, value: ArtColors): Int = 1
    }
    // منفی‌کش با انقضا تا آهنگ بدون کاور هر بار retriever باز نکند
    private val negativeCache = LinkedHashMap<Long, Long>()
    private const val NEGATIVE_TTL_MS = 10 * 60 * 1000L
    private val mutex = Mutex()

    private fun isNegativeCached(id: Long): Boolean = synchronized(negativeCache) {
        val t = negativeCache[id] ?: return false
        if (System.currentTimeMillis() - t > NEGATIVE_TTL_MS) {
            negativeCache.remove(id)
            return false
        }
        true
    }

    suspend fun load(song: Song, ctx: Context, maxSizePx: Int = 512): Bitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            bitmapCache.get(song.id)?.let { return@withContext it }
        }
        if (isNegativeCached(song.id)) return@withContext null
        if (song.isRemote) {
            // استریم: فعلاً کاور ریموت نداریم (Coil در UI لود می‌کند) — منفی‌کش تا تکرار نشود
            synchronized(negativeCache) { negativeCache[song.id] = System.currentTimeMillis() }
            return@withContext null
        }
        val bmp: Bitmap? = try {
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(ctx, song.uri)
                val raw = mmr.embeddedPicture ?: return@withContext null.also {
                    synchronized(negativeCache) { negativeCache[song.id] = System.currentTimeMillis() }
                }
                // دان‌سمپل: اول ابعاد، بعد نمونه مناسب تا OOM ندهد
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
                var sample = 1
                val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
                while (maxDim / (sample * 2) >= maxSizePx && sample < 8) sample *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeByteArray(raw, 0, raw.size, opts)
            } finally {
                runCatching { mmr.release() }
            }
        } catch (e: OutOfMemoryError) {
            Log.w("Artwork", "OOM decoding cover", e)
            null
        } catch (e: Exception) {
            Log.d("Artwork", "load failed", e)
            null
        }
        mutex.withLock {
            if (bmp != null) bitmapCache.put(song.id, bmp)
            else synchronized(negativeCache) { negativeCache[song.id] = System.currentTimeMillis() }
        }
        bmp
    }

    /** پالت رنگی کاور (null اگر کاوری نباشد) — از همان بیت‌مپ کش استفاده می‌کند، دوباره دیکد نمی‌کند */
    suspend fun colors(song: Song, ctx: Context): ArtColors? = withContext(Dispatchers.IO) {
        mutex.withLock { colorCache.get(song.id)?.let { return@withContext it } }
        // load خودش کش دارد، پس فراخوانی دوگانه ArtImage + colors فقط یک‌بار دیکد می‌کند
        // برای پالت نسخه کوچک کافی است (سریع‌تر + کم‌حافظه)
        val bmp = load(song, ctx, maxSizePx = 256) ?: return@withContext null
        val c = try {
            // روی بیت‌مپ کوچک اسکیل‌شده پالت بگیر تا کند نشود
            val small = if (maxOf(bmp.width, bmp.height) > 256) {
                Bitmap.createScaledBitmap(bmp, 128, 128, true)
            } else bmp
            val p = androidx.palette.graphics.Palette.from(small).generate()
            if (small !== bmp) runCatching { small.recycle() }
            val black = android.graphics.Color.BLACK
            val vibrant = p.getVibrantColor(p.getDominantColor(black))
            val dark = p.getDarkVibrantColor(p.getDarkMutedColor(black))
            val dominant = p.getDominantColor(vibrant)
            val muted = p.getMutedColor(dark)
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
        } catch (e: Exception) {
            Log.d("Artwork", "palette failed", e)
            null
        } catch (e: OutOfMemoryError) {
            null
        }
        if (c != null) mutex.withLock { colorCache.put(song.id, c) }
        c
    }

    fun clear() {
        runCatching { bitmapCache.evictAll() }
        runCatching { colorCache.evictAll() }
        synchronized(negativeCache) { negativeCache.clear() }
    }
}
