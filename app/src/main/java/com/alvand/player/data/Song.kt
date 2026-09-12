package com.alvand.player.data

import android.net.Uri

/** مدل آهنگ — هم فایل لوکال، هم لینک مستقیم */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String = "",
    val uri: Uri,
    val durationMs: Long = 0L,
    val isRemote: Boolean = false,
    val mimeHint: String? = null
) {
    companion object {
        /** پسوندهای صوتی پشتیبانی‌شده (برای فایل‌پیکر و اعتبارسنجی لینک) */
        val SUPPORTED_EXTENSIONS = setOf(
            "mp3", "wav", "flac", "ogg", "oga", "opus", "m4a",
            "aac", "wma", "alac", "aiff", "aif", "amr", "mid",
            "midi", "mp4", "mka", "mkv", "ts", "m3u8", "mpd"
        )

        private val AUDIO_MIME_BY_EXT = mapOf(
            "mp3" to "audio/mpeg", "wav" to "audio/wav", "flac" to "audio/flac",
            "ogg" to "audio/ogg", "oga" to "audio/ogg", "opus" to "audio/opus",
            "m4a" to "audio/mp4", "aac" to "audio/aac", "mid" to "audio/midi",
            "midi" to "audio/midi", "m3u8" to "application/x-mpegURL", "mpd" to "application/dash+xml",
            "mp4" to "audio/mp4", "mka" to "audio/x-matroska", "ts" to "video/mp2t"
        )

        fun mimeForUrl(url: String): String? {
            val ext = url.substringBefore('?').substringAfterLast('.', "").lowercase()
            return AUDIO_MIME_BY_EXT[ext]
        }

        fun fromDirectLink(url: String, titleFallback: String = "Stream"): Song? {
            val trimmed = url.trim()
            if (!(trimmed.startsWith("http://") || trimmed.startsWith("https://"))) return null
            // فقط http(s) کافی نیست — باید پسوند صوتی یا هینت استریم داشته باشد
            if (!isSupportedPath(trimmed)) return null
            var title = trimmed.substringAfterLast('/').substringBefore('?')
                .ifBlank { titleFallback }
            // دیکد %20 و حذف پسوند برای عنوان تمیز
            title = runCatching { java.net.URLDecoder.decode(title, "UTF-8") }.getOrNull() ?: title
            if (title.substringAfterLast('.', "").lowercase() in SUPPORTED_EXTENSIONS) {
                title = title.substringBeforeLast('.')
            }
            title = title.replace('_', ' ').replace(Regex("\\s+"), " ").trim()
                .ifBlank { titleFallback }
            // آیدی منفی پایدار در فضای جدا از MediaStore (که همیشه >=0 است) تا کالیشن نخورد
            val stableId = -(trimmed.hashCode().toLong() and 0x7FFFFFFFL) - 1_000_000L
            return Song(
                id = stableId,
                title = title,
                artist = "Direct link",
                uri = Uri.parse(trimmed),
                isRemote = true,
                mimeHint = mimeForUrl(trimmed)
            )
        }

        fun isSupportedPath(path: String): Boolean {
            val p = path.trim()
            val lower = p.lowercase()
            val isHttp = lower.startsWith("http://") || lower.startsWith("https://")
            val ext = p.substringBefore('?').substringAfterLast('.', "").lowercase()
                .substringAfterLast('/', "")
            if (ext in SUPPORTED_EXTENSIONS) return true
            // لینک http بدون پسوند فقط اگر هینت HLS/DASH یا کوئری صوتی داشته باشد
            if (isHttp) {
                if (lower.contains(".m3u8") || lower.contains(".mpd")) return true
                // بقیه URLهای متنی/html قبول نیست (قبلاً با || http همه قبول می‌شد)
                return false
            }
            return false
        }

        /** آیدی فایل لوکال picked (فضای منفی جدا، بدون کالیشن با MediaStore) */
        fun localFileId(uri: Uri): Long {
            val h = uri.toString().hashCode().toLong() and 0x7FFFFFFFL
            return -(h + 2_000_000L)
        }
    }
}
