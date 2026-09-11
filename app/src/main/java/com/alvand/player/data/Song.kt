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

        fun fromDirectLink(url: String, titleFallback: String = "Stream"): Song? {
            val trimmed = url.trim()
            if (!(trimmed.startsWith("http://") || trimmed.startsWith("https://"))) return null
            var title = trimmed.substringAfterLast('/').substringBefore('?')
                .ifBlank { titleFallback }
            // دیکد %20 و حذف پسوند برای عنوان تمیز
            title = runCatching { java.net.URLDecoder.decode(title, "UTF-8") }.getOrNull() ?: title
            if (title.substringAfterLast('.', "").lowercase() in SUPPORTED_EXTENSIONS) {
                title = title.substringBeforeLast('.')
            }
            title = title.replace('_', ' ').replace(Regex("\\s+"), " ").trim()
                .ifBlank { titleFallback }
            return Song(
                id = trimmed.hashCode().toLong(),
                title = title,
                artist = "Direct link",
                uri = Uri.parse(trimmed),
                isRemote = true
            )
        }

        fun isSupportedPath(path: String): Boolean {
            val ext = path.substringAfterLast('.', "").substringBefore('?').lowercase()
            return ext in SUPPORTED_EXTENSIONS || path.startsWith("http")
        }
    }
}
