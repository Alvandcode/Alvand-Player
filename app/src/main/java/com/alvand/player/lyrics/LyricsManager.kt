package com.alvand.player.lyrics

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
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
    val source: String // "embedded" | "lrc-file" | "online" | "manual" | "none"
)

object LyricsManager {

    private val http = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    private val LRC_TIME = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]""")

    fun parseLrc(raw: String): List<LyricLine> {
        val out = mutableListOf<LyricLine>()
        raw.lineSequence().forEach { line ->
            val matches = LRC_TIME.findAll(line).toList()
            if (matches.isEmpty()) return@forEach
            val text = line.replace(LRC_TIME, "").trim()
            if (text.isEmpty() || text.startsWith("ti:") || text.startsWith("ar:")) return@forEach
            matches.forEach { m ->
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val fracRaw = m.groupValues[3]
                val ms = when (fracRaw.length) {
                    0 -> 0L
                    2 -> fracRaw.toLong() * 10 // centiseconds
                    3 -> fracRaw.toLong()
                    else -> fracRaw.padEnd(3, '0').take(3).toLong()
                }
                out += LyricLine(min * 60_000 + sec * 1000 + ms, text)
            }
        }
        return out.sortedBy { it.timeMs }
    }

    /** ۱) لیریک امبدد داخل تگ فایل (USLT/SYLT) — همان «آهنگ‌هایی که خودشون لیریک ذخیره شده دارن» */
    suspend fun readEmbedded(song: Song, context: Context): LyricsResult? =
        withContext(Dispatchers.IO) {
            try {
                val mmr = MediaMetadataRetriever()
                if (song.isRemote) mmr.setDataSource(song.uri.toString(), HashMap())
                else mmr.setDataSource(context, song.uri)
                mmr.release()
                null
            } catch (_: Exception) { null }
        }

    /** تلاش کامل: embedded-raw (ID3 USLT) + فایل lrc هم‌نام — بدون نیاز به اینترنت */
    suspend fun loadLocal(song: Song, context: Context): LyricsResult =
        withContext(Dispatchers.IO) {
            // الف) اسکن بایت‌های ID3 برای USLT/SYLT (کار می‌کند حتی وقتی MediaMetadataRetriever نه)
            readId3Uslt(song, context)?.let { return@withContext it }

            // ب) فایل .lrc هم‌نام کنار آهنگ لوکال
            if (!song.isRemote) {
                runCatching {
                    val path = RealPathHelper.getPath(context, song.uri)
                    if (path != null) {
                        val lrc = File(path.substringBeforeLast('.') + ".lrc")
                        if (lrc.exists()) {
                            val raw = lrc.readText()
                            val lines = parseLrc(raw)
                            if (lines.isNotEmpty()) return@withContext LyricsResult(lines, raw, "lrc-file")
                        }
                    }
                }
            }
            LyricsResult(emptyList(), "", "none")
        }

    /** جستجوی آنلاین متن آهنگ (LRCLIB — رایگان، بدون کلید) */
    suspend fun fetchOnline(artist: String, title: String): LyricsResult =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://lrclib.net/api/get?artist_name=" +
                    URLEncoder.encode(artist, "UTF-8") + "&track_name=" +
                    URLEncoder.encode(title, "UTF-8")
                val res = http.newCall(Request.Builder().url(url).build()).execute()
                if (!res.isSuccessful) return@withContext LyricsResult(emptyList(), "", "none")
                val json = JSONObject(res.body!!.string())
                val synced = json.optString("syncedLyrics", "")
                val plain = json.optString("plainLyrics", "")
                if (synced.isNotBlank()) {
                    val lines = parseLrc(synced)
                    if (lines.isNotEmpty()) return@withContext LyricsResult(lines, synced, "online")
                }
                if (plain.isNotBlank()) {
                    val lines = plain.lineSequence().filter { it.isNotBlank() }
                        .mapIndexed { i, t -> LyricLine(i * 4000L, t.trim()) }.toList()
                    return@withContext LyricsResult(lines, plain, "online")
                }
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
                    .mapIndexed { i, t -> "[00:${(i * 4).toString().padStart(2, '0')}.00]$t" }
                    .joinToString("\n")
                val dir = context.getExternalFilesDir("lyrics") ?: context.cacheDir
                val safe = (song.artist + "-" + song.title).replace(Regex("[^a-zA-Z0-9-_ ]"), "").take(80)
                val file = File(dir, "$safe.lrc")
                file.writeText(lrcText)
                file
            } catch (_: Exception) { null }
        }

    // ---- ID3 USLT scanner (mp3/id3v2) ----
    private fun readId3Uslt(song: Song, context: Context): LyricsResult? {
        return try {
            val bytes: ByteArray = if (song.isRemote) {
                val req = Request.Builder().url(song.uri.toString())
                    .header("Range", "bytes=0-300000").build()
                http.newCall(req).execute().use { it.body!!.bytes() }
            } else {
                context.contentResolver.openInputStream(song.uri)?.use {
                    it.readBytes().take(400_000).toByteArray()
                } ?: return null
            }
            val text = bytes.toString(Charsets.ISO_8859_1)
            // فریم USLT
            val idx = text.indexOf("USLT")
            if (idx >= 0) {
                val chunk = text.substring(idx, (idx + 20000).coerceAtMost(text.length))
                // بعد از هدر USLT، متن واقعی می‌آید؛ کاراکترهای کنترلی را تمیز می‌کنیم
                val cleaned = chunk.replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), " ")
                    .replace(Regex("USLT.*?eng"), "").trim().take(12000)
                val lines = if (LRC_TIME.containsMatchIn(cleaned)) parseLrc(cleaned)
                else cleaned.split(Regex("\\s{2,}|\\n")).map { it.trim() }
                    .filter { it.length in 2..200 && it.any(Char::isLetter) }
                    .mapIndexed { i, t -> LyricLine(i * 4000L, t) }
                if (lines.size >= 2) return LyricsResult(lines, cleaned, "embedded")
            }
            null
        } catch (_: Exception) { null }
    }
}

/** کمکی برای گرفتن مسیر فایل از content-uri (برای پیدا کردن lrc هم‌نام) */
object RealPathHelper {
    fun getPath(context: Context, uri: Uri): String? {
        return try {
            if (uri.scheme == "file") return uri.path
            val proj = arrayOf(android.provider.MediaStore.Audio.Media.DATA)
            context.contentResolver.query(uri, proj, null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        } catch (_: Exception) { null }
    }
}
