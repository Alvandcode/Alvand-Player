package com.alvand.player.lyrics

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.alvand.player.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** یک خط لیریک همگام‌شده */
data class LyricLine(val timeMs: Long, val text: String)

/** خروجی لود لیریک */
data class LyricsResult(
    val lines: List<LyricLine>,
    val plainText: String,
    val source: String // "embedded" | "lrc-file" | "lyrics-cache" | "online" | "manual" | "none"
)

object LyricsManager {

    private val http = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private const val MAX_READ_BYTES = 400_000L
    private const val MAX_LRC_FILE_BYTES = 300_000L
    private const val MAX_ID3_SCAN_BYTES = 300_000L

    private val LRC_TIME = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]""")

    fun parseLrc(raw: String): List<LyricLine> {
        val out = mutableListOf<LyricLine>()
        raw.lineSequence().forEach { line ->
            val matches = LRC_TIME.findAll(line).toList()
            if (matches.isEmpty()) return@forEach
            val text = line.replace(LRC_TIME, "").trim()
            if (text.isEmpty() || text.startsWith("ti:") || text.startsWith("ar:")) return@forEach
            matches.forEach { m ->
                val min = m.groupValues[1].toLongOrNull() ?: return@forEach
                val sec = m.groupValues[2].toLongOrNull()?.coerceIn(0, 59) ?: return@forEach
                val fracRaw = m.groupValues[3]
                val ms = when (fracRaw.length) {
                    0 -> 0L
                    2 -> (fracRaw.toLongOrNull() ?: 0L) * 10 // centiseconds
                    3 -> fracRaw.toLongOrNull() ?: 0L
                    else -> fracRaw.padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                }
                out += LyricLine(min * 60_000 + sec * 1000 + ms, text)
            }
        }
        return out.sortedBy { it.timeMs }
    }

    /** ۱) لیریک امبدد داخل تگ فایل — با بستن امن retriever */
    suspend fun readEmbedded(song: Song, context: Context): LyricsResult? =
        withContext(Dispatchers.IO) {
            val mmr = MediaMetadataRetriever()
            try {
                if (song.isRemote) mmr.setDataSource(song.uri.toString(), HashMap())
                else mmr.setDataSource(context, song.uri)
                // MediaMetadataRetriever متن USLT را نمی‌دهد؛ فقط برای سازگاری آینده نگه داشته شده
                null
            } catch (e: Exception) {
                Log.d("Lyrics", "embedded read failed", e)
                null
            } catch (e: OutOfMemoryError) {
                Log.w("Lyrics", "embedded OOM", e)
                null
            } finally {
                runCatching { mmr.release() }
            }
        }

    /** تلاش کامل: کش اپ + ID3 USLT + فایل lrc هم‌نام — بدون نیاز به اینترنت */
    suspend fun loadLocal(song: Song, context: Context): LyricsResult =
        withContext(Dispatchers.IO) {
            try {
                // ۰) کش اپ (saveManual همین‌جا می‌نویسد — قبلاً خوانده نمی‌شد)
                loadFromCache(song, context)?.let { return@withContext it }

                // الف) اسکن بایت‌های ID3 برای USLT/SYLT
                readId3Uslt(song, context)?.let { return@withContext it }

                // ب) فایل .lrc هم‌نام کنار آهنگ لوکال
                if (!song.isRemote) {
                    runCatching {
                        val lrc = findSidecarLrc(context, song.uri)
                        if (lrc != null && lrc.exists() && lrc.length() <= MAX_LRC_FILE_BYTES * 2) {
                            val raw = runCatching {
                                lrc.inputStream().buffered().use { ins ->
                                    val buf = ByteArray(MAX_LRC_FILE_BYTES.toInt() + 1)
                                    val n = ins.read(buf)
                                    if (n <= 0) "" else buf.copyOf(n).toString(detectCharset(buf.copyOf(n)))
                                }
                            }.getOrNull() ?: ""
                            if (raw.isNotBlank()) {
                                val lines = parseLrc(raw)
                                if (lines.isNotEmpty()) return@withContext LyricsResult(lines, raw, "lrc-file")
                            }
                        }
                    }
                }
                LyricsResult(emptyList(), "", "none")
            } catch (e: OutOfMemoryError) {
                Log.w("Lyrics", "loadLocal OOM", e)
                LyricsResult(emptyList(), "", "none")
            } catch (e: Exception) {
                Log.w("Lyrics", "loadLocal failed", e)
                LyricsResult(emptyList(), "", "none")
            }
        }

    /** جستجوی آنلاین متن آهنگ (LRCLIB — رایگان، بدون کلید) */
    suspend fun fetchOnline(artist: String, title: String, durationSec: Long = 0): LyricsResult =
        withContext(Dispatchers.IO) {
            try {
                // ورودی خالی/پیش‌فرض را به API نزن
                if (title.isBlank() || title == "Stream" || title == "Direct link") {
                    return@withContext LyricsResult(emptyList(), "", "none")
                }
                var url = "https://lrclib.net/api/get?artist_name=" +
                    URLEncoder.encode(artist, "UTF-8") + "&track_name=" +
                    URLEncoder.encode(title, "UTF-8")
                if (durationSec > 0) url += "&duration=$durationSec"
                http.newCall(Request.Builder().url(url).build()).execute().use { res ->
                    if (!res.isSuccessful) return@withContext LyricsResult(emptyList(), "", "none")
                    val bodyStr = res.body?.string() ?: return@withContext LyricsResult(emptyList(), "", "none")
                    val json = JSONObject(bodyStr)
                    val synced = json.optString("syncedLyrics", "")
                    val plain = json.optString("plainLyrics", "")
                    if (synced.isNotBlank()) {
                        val lines = parseLrc(synced)
                        if (lines.isNotEmpty()) return@withContext LyricsResult(lines, synced, "online")
                    }
                    if (plain.isNotBlank()) {
                        val step = if (durationSec > 0) {
                            val count = plain.lineSequence().count { it.isNotBlank() }.coerceAtLeast(1)
                            (durationSec * 1000 / count).coerceIn(2000L, 8000L)
                        } else 4000L
                        val lines = plain.lineSequence().filter { it.isNotBlank() }
                            .mapIndexed { i, t -> LyricLine(i * step, t.trim()) }.toList()
                        return@withContext LyricsResult(lines, plain, "online")
                    }
                    LyricsResult(emptyList(), "", "none")
                }
            } catch (e: OutOfMemoryError) {
                LyricsResult(emptyList(), "", "none")
            } catch (_: Exception) {
                LyricsResult(emptyList(), "", "none")
            }
        }

    /** ذخیره متن دستی کاربر به‌صورت .lrc (کش اپ برای استریم‌ها) */
    suspend fun saveManual(song: Song, context: Context, rawText: String): File? =
        withContext(Dispatchers.IO) {
            try {
                val lrcText = if (LRC_TIME.containsMatchIn(rawText)) rawText
                else rawText.lineSequence().filter { it.isNotBlank() }
                    .mapIndexed { i, t ->
                        val totalSec = i * 4L
                        val mm = (totalSec / 60).coerceAtMost(999)
                        val ss = (totalSec % 60)
                        "[%02d:%02d.00]$t".format(mm, ss)
                    }
                    .joinToString("\n")
                writeLrcCache(song, context, lrcText)
            } catch (_: Exception) { null } catch (_: OutOfMemoryError) { null }
        }

    /**
     * کش نتیجه آنلاین تا دفعه بعد بدون اینترنت از loadLocal بیاید.
     * دستی کاربر اولویت دارد: اگر فایل manual/cache از قبل هست و source دستی است، بازنویسی نکن.
     * اینجا ساده: فقط اگر فایلی نیست بنویس؛ اگر هست و تازه‌تر از ۳۰ روز نیست، نگه دار.
     */
    suspend fun cacheOnline(song: Song, context: Context, rawText: String): File? =
        withContext(Dispatchers.IO) {
            try {
                if (rawText.isBlank()) return@withContext null
                if (hasCache(song, context)) return@withContext null
                if (rawText.toByteArray().size > MAX_LRC_FILE_BYTES) return@withContext null
                writeLrcCache(song, context, rawText)
            } catch (_: Exception) { null } catch (_: OutOfMemoryError) { null }
        }

    private fun writeLrcCache(song: Song, context: Context, lrcText: String): File? {
        return try {
            val dir = File(context.filesDir, "lyrics").apply { mkdirs() }
                .takeIf { it.exists() }
                ?: (context.getExternalFilesDir("lyrics") ?: context.cacheDir)
            val safe = sanitizeFileName(song.artist + "-" + song.title).take(80).ifBlank { "lyrics" }
            // کلید پایدار بر اساس id تا save/load همیشه هم‌خوان باشند
            val file = File(dir, "${song.id}_$safe.lrc")
            // محدودیت حجم تا دیسک پر نشود
            if (lrcText.toByteArray().size > MAX_LRC_FILE_BYTES) return null
            file.writeText(lrcText)
            file
        } catch (_: Exception) { null }
    }

    private fun hasCache(song: Song, context: Context): Boolean {
        return try {
            val dirs = listOfNotNull(
                runCatching { File(context.filesDir, "lyrics") }.getOrNull(),
                runCatching { context.getExternalFilesDir("lyrics") }.getOrNull()
            )
            dirs.any { dir ->
                dir.listFiles { f -> f.name.startsWith("${song.id}_") && f.name.endsWith(".lrc") }
                    ?.isNotEmpty() == true
            }
        } catch (_: Exception) { false }
    }

    private fun loadFromCache(song: Song, context: Context): LyricsResult? {
        return try {
            val dirs = listOfNotNull(
                runCatching { File(context.filesDir, "lyrics") }.getOrNull(),
                runCatching { context.getExternalFilesDir("lyrics") }.getOrNull()
            )
            for (dir in dirs) {
                val matches = dir.listFiles { f -> f.name.startsWith("${song.id}_") && f.name.endsWith(".lrc") }
                    ?: continue
                val f = matches.firstOrNull() ?: continue
                if (f.length() > MAX_LRC_FILE_BYTES * 2) continue
                val raw = runCatching { f.readText() }.getOrNull() ?: continue
                val lines = parseLrc(raw)
                if (lines.isNotEmpty()) return LyricsResult(lines, raw, "lyrics-cache")
            }
            null
        } catch (_: Exception) { null }
    }

    private fun sanitizeFileName(input: String): String {
        // حروف فارسی/عربی و همه زبان‌ها حفظ می‌شود؛ فقط کاراکترهای خطرناک فایل‌سیستم حذف
        return input.replace(Regex("[\\\\/:*?\"<>|\\x00-\\x1F]"), "").trim().take(80)
    }

    private fun detectCharset(bytes: ByteArray): java.nio.charset.Charset {
        // BOMチェック ساده؛ وگرنه UTF-8 (فارسی معمولاً UTF-8 است)
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return Charsets.UTF_8
        }
        return Charsets.UTF_8
    }

    private fun findSidecarLrc(context: Context, uri: Uri): File? {
        // file:// مستقیم
        if (uri.scheme == "file") {
            val p = uri.path ?: return null
            if (!p.contains('.')) return null
            return File(p.substringBeforeLast('.') + ".lrc")
        }
        // content:// — از DISPLAY_NAME استفاده کن (DATA دیپرکیت و در A10+ null است)
        return try {
            var displayName: String? = null
            context.contentResolver.query(uri, arrayOf(android.provider.MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) displayName = it.getString(0)
            }
            if (displayName != null && displayName.contains('.')) {
                // هم‌نام در همان پوشه قابل دسترس نیست بدون مسیر؛ پس فقط کش را برمی‌گردانیم (null)
                null
            } else null
        } catch (_: Exception) { null }
    }

    // ---- ID3 USLT scanner (mp3/id3v2) ----
    private fun readId3Uslt(song: Song, context: Context): LyricsResult? {
        return try {
            val bytes: ByteArray = if (song.isRemote) {
                val req = Request.Builder().url(song.uri.toString())
                    .header("Range", "bytes=0-${MAX_ID3_SCAN_BYTES.toInt()}").build()
                http.newCall(req).execute().use { resp ->
                    val body = resp.body ?: return null
                    // اگر سرور Range را نادیده گرفت، استریم محدود بخوان نه کل فایل
                    body.byteStream().use { ins ->
                        readBounded(ins, MAX_ID3_SCAN_BYTES)
                    }
                }
            } else {
                context.contentResolver.openInputStream(song.uri)?.use {
                    readBounded(it, MAX_READ_BYTES)
                } ?: return null
            }
            if (bytes.size < 10) return null
            // اعتبارسنجی هدر ID3 (نه indexOf کور روی دیتای صوتی)
            if (!(bytes.size > 3 && bytes[0] == 'I'.code.toByte() && bytes[1] == 'D'.code.toByte() && bytes[2] == '3'.code.toByte())) {
                return null
            }
            val text = bytes.toString(Charsets.ISO_8859_1)
            val idx = text.indexOf("USLT")
            if (idx < 0) return null
            // زبان را هم پوشش بده (eng/fas/per/ara/...) نه فقط eng
            val chunk = text.substring(idx, (idx + 20000).coerceAtMost(text.length))
            val cleaned = chunk.replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), " ")
                .replace(Regex("USLT.{0,16}?(eng|fas|per|ara|fre|ger|spa|tur|und)"), "").trim().take(12000)
            val lines = if (LRC_TIME.containsMatchIn(cleaned)) parseLrc(cleaned)
            else cleaned.split(Regex("\\s{2,}|\\n")).map { it.trim() }
                .filter { it.length in 2..200 && it.any(Char::isLetter) }
                .mapIndexed { i, t -> LyricLine(i * 4000L, t) }
            // تک‌لاین معتبر را دور نریز
            if (lines.isNotEmpty()) return LyricsResult(lines, cleaned, "embedded")
            null
        } catch (_: OutOfMemoryError) { null } catch (_: Exception) { null }
    }

    private fun readBounded(ins: java.io.InputStream, max: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0L
        while (true) {
            val n = ins.read(buf)
            if (n <= 0) break
            val remain = max - total
            if (remain <= 0) break
            val toWrite = minOf(n.toLong(), remain).toInt()
            out.write(buf, 0, toWrite)
            total += toWrite
            if (total >= max) break
        }
        return out.toByteArray()
    }
}

/** کمکی برای گرفتن مسیر فایل از content-uri (فقط file:// معتبر است؛ DATA دیپرکیت است) */
object RealPathHelper {
    fun getPath(context: Context, uri: Uri): String? {
        return try {
            if (uri.scheme == "file") return uri.path
            // Scoped Storage: ستون DATA در اندروید ۱۰+ غالباً null است — عمداً null برمی‌گردانیم
            null
        } catch (_: Exception) { null }
    }
}
