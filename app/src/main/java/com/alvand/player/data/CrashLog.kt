package com.alvand.player.data

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** یک گزارش کرش ذخیره‌شده */
data class CrashInfo(
    val fileName: String,
    val timeMs: Long,
    val headline: String,
    val fullText: String
) {
    fun timeLabel(): String = runCatching {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(timeMs))
    }.getOrNull() ?: timeMs.toString()
}

/**
 * شکارچی کرش: اولین خطای uncaught را در فایل نگه می‌دارد تا کاربر بتواند
 * از داخل اپ (دیالوگ شروع / صفحه درباره ما) متن آن را کپی یا ارسال کند.
 * حداکثر ۵ فایل آخر نگه داشته می‌شود؛ هیچ‌چیز به اینترنت فرستاده نمی‌شود.
 */
object CrashLog {
    private const val DIR = "crashes"
    private const val PREFS = "crash_prefs"
    private const val KEY_SEEN = "seen_ts"
    private const val MAX_FILES = 5
    private const val MAX_CHARS = 60_000

    @Volatile private var installed = false

    fun install(appContext: Context) {
        if (installed) return
        installed = true
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching { writeCrash(appContext.applicationContext, t, e) }
            // رفتار عادی سیستم (دیالوگ کرش) حفظ شود
            if (prev != null) runCatching { prev.uncaughtException(t, e) }
            else runCatching { android.os.Process.killProcess(android.os.Process.myPid()) }
        }
    }

    fun lastCrash(context: Context): CrashInfo? {
        val files = crashDir(context).listFiles { f -> f.name.endsWith(".txt") }
            ?.sortedByDescending { it.lastModified() } ?: return null
        val f = files.firstOrNull() ?: return null
        val text = runCatching { f.readText().take(MAX_CHARS) }.getOrNull() ?: return null
        if (text.isBlank()) return null
        val headline = text.lineSequence().firstOrNull { it.isNotBlank() }?.take(220) ?: "Crash"
        return CrashInfo(f.name, f.lastModified(), headline, text)
    }

    /** کرشِ دیده‌نشده: فایلی که جدیدتر از آخرین «بستن» کاربر است */
    fun unseenCrash(context: Context): CrashInfo? {
        val c = lastCrash(context) ?: return null
        val seen = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_SEEN, 0L)
        return if (c.timeMs > seen) c else null
    }

    fun markSeen(context: Context) {
        val last = lastCrash(context) ?: return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_SEEN, last.timeMs).apply()
    }

    fun clear(context: Context) {
        runCatching {
            crashDir(context).listFiles()?.forEach { runCatching { it.delete() } }
        }
    }

    private fun crashDir(context: Context): File =
        File(context.filesDir, DIR).apply { runCatching { mkdirs() } }

    private fun writeCrash(context: Context, thread: Thread, e: Throwable) {
        val dir = crashDir(context)
        // هرس قدیمی‌ها
        dir.listFiles { f -> f.name.endsWith(".txt") }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_FILES - 1)
            ?.forEach { runCatching { it.delete() } }
        val ts = System.currentTimeMillis()
        val appVer = runCatching {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pi.versionName} (${pi.versionCode})"
        }.getOrNull() ?: "?"
        val header = buildString {
            appendLine("Alvand Player crash — $ts")
            appendLine("app=$appVer pkg=${context.packageName}")
            appendLine("android=${Build.VERSION.RELEASE} (sdk=${Build.VERSION.SDK_INT}) device=${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("thread=${thread.name}")
            appendLine("---")
        }
        val sw = java.io.StringWriter()
        e.printStackTrace(java.io.PrintWriter(sw))
        var cause = e.cause
        var depth = 0
        while (cause != null && depth < 3) {
            sw.append("\nCaused by: ")
            cause.printStackTrace(java.io.PrintWriter(sw))
            cause = cause.cause
            depth++
        }
        val file = File(dir, "crash-$ts.txt")
        runCatching { file.writeText((header + sw.toString()).take(MAX_CHARS)) }
    }
}
