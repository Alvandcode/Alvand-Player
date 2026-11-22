package com.alvand.player.player

import android.net.Uri
import com.alvand.player.data.Song
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/**
 * پایدارسازی صف پخش بین اجراها — جدا از manager تا با Robolectric تست شود.
 * دو فایل کوچک در filesDir: last_queue.json (صف + ایندکس) و last_pos.json (موقعیت).
 * خرابی/ناهنجاری فایل همیشه به «صف خالی» برمی‌گردد، هرگز کرش نمی‌دهد.
 */
object QueueStore {

    private const val QUEUE_FILE = "last_queue.json"
    private const val POS_FILE = "last_pos.json"

    fun save(dir: File, songs: List<Song>, index: Int) {
        runCatching {
            val arr = JSONArray()
            songs.forEach { s ->
                arr.put(
                    JSONObject()
                        .put("id", s.id)
                        .put("title", s.title)
                        .put("artist", s.artist)
                        .put("album", s.album)
                        .put("uri", s.uri.toString())
                        .put("durationMs", s.durationMs)
                        .put("isRemote", s.isRemote)
                        .putOpt("mimeHint", s.mimeHint)
                )
            }
            dir.mkdirs()
            File(dir, QUEUE_FILE).writeText(
                JSONObject().put("index", index).put("songs", arr).toString()
            )
        }
    }

    fun savePosition(dir: File, positionMs: Long) {
        runCatching {
            dir.mkdirs()
            File(dir, POS_FILE).writeText(positionMs.toString())
        }
    }

    /** (صف، ایندکس امن) یا null اگر فایل نبود/ناقص بود/خالی بود */
    fun load(dir: File): Pair<List<Song>, Int>? {
        val f = File(dir, QUEUE_FILE)
        if (!f.isFile) return null
        return runCatching {
            val obj = JSONObject(f.readText())
            val arr = obj.getJSONArray("songs")
            val songs = ArrayList<Song>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                songs += Song(
                    id = o.getLong("id"),
                    title = o.optString("title"),
                    artist = o.optString("artist"),
                    album = o.optString("album"),
                    uri = Uri.parse(o.getString("uri")),
                    durationMs = o.optLong("durationMs"),
                    isRemote = o.optBoolean("isRemote"),
                    mimeHint = o.optString("mimeHint").takeIf { it.isNotEmpty() }
                )
            }
            if (songs.isEmpty()) null
            else songs to obj.optInt("index", 0).coerceIn(0, songs.size - 1)
        }.getOrNull()
    }

    fun loadPosition(dir: File): Long =
        runCatching { File(dir, POS_FILE).readText().trim().toLongOrNull() }.getOrNull() ?: 0L

    fun clear(dir: File) {
        runCatching { File(dir, QUEUE_FILE).delete(); File(dir, POS_FILE).delete() }
    }
}
