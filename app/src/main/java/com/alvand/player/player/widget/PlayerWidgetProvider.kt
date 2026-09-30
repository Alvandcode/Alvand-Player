package com.alvand.player.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.alvand.player.MainActivity
import com.alvand.player.R
import com.alvand.player.data.Artwork
import com.alvand.player.player.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * ویجت هوم‌اسکرین: کاور + عنوان/خواننده + نوار پیشرفت + ۵ کنترل
 * (تکرار، قبلی، پخش/توقف، بعدی، علاقه‌مندی).
 *
 * - دکمه‌ها مستقیم Intent به [PlaybackService] می‌فرستند تا بدون باز شدن اپ
 *   (لازم برای Android 12+ که PendingIntent پس‌زمینه را محدود می‌کند).
 * - تپ روی کاور/پنل → باز شدن اپ (singleTop).
 * - آخرین وضعیت در SharedPreferences نگه داشته می‌شود تا بعد از ریستارت
 *   لانچر هم ویجت درست باشد؛ خود Push از [PlaybackService.updateAll] می‌آید.
 * - ساخت RemoteViews و خواندن کاور روی یک ترد پس‌زمینه انجام می‌شود تا
 *   هیچ IO یا دیکدی روی ترد broadcast انجام نشود.
 */
class PlayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        loadPersisted(context)
        push(context)
    }

    override fun onEnabled(context: Context) {
        loadPersisted(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle?
    ) {
        loadPersisted(context)
        newOptions?.let {
            val minH = it.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val maxH = it.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
            if (maxH > 0) setWidgetHeightDp(context, maxH)
            else if (minH > 0) setWidgetHeightDp(context, minH)
        }
        push(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // آهنگ در حال پخش باقی می‌ماند؛ اگر آخرین ویجت حذف شد تایمر متوقف کن
        if (appWidgetIds.isNotEmpty() && widgetIds(context).isEmpty()) stopProgressTicker()
    }

    override fun onDisabled(context: Context) {
        stopProgressTicker()
        runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        }
        resetState()
    }

    companion object {
        private const val REQ_TOGGLE = 101
        private const val REQ_NEXT = 102
        private const val REQ_PREV = 103
        private const val REQ_OPEN = 104
        private const val REQ_REPEAT = 105
        private const val REQ_FAV = 106
        private const val PREFS = "alvand_widget"
        private const val PROGRESS_MAX = 1000
        private const val ART_WIDTH_DP = 96
        private const val CORNER_DP = 18
        private const val DEFAULT_HEIGHT_DP = 96

        private val Lock = Any()
        // تک‌نویسنده: ساخت RemoteViews‌ها نباید همزمان و خارج از ترتیب انجام شود
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))

        // کاور دیکدشده برای ویجت؛ کلید = uri آهنگ (تایمر ۱ ثانیه‌ای هر بار دیکد نکند)
        private val artCache = object : android.util.LruCache<String, android.graphics.Bitmap>(2) {
            override fun sizeOf(key: String, value: android.graphics.Bitmap): Int = value.byteCount
        }

        @Volatile private var lastTitle: String = "Alvand Player"
        @Volatile private var lastArtist: String = ""
        @Volatile private var lastPlaying: Boolean = false
        @Volatile private var lastPositionMs: Long = 0L
        @Volatile private var lastDurationMs: Long = 0L
        @Volatile private var lastRepeatMode: Int = 0
        @Volatile private var lastFavourite: Boolean = false
        @Volatile private var lastArtUri: String? = null
        @Volatile private var lastHeightDp: Int = DEFAULT_HEIGHT_DP
        @Volatile private var ticker: kotlinx.coroutines.Job? = null

        /** تصویر یک‌بارهٔ وضعیت تا داده‌های ساخت RemoteViews نیمه‌کاره نباشند */
        private data class Snapshot(
            val title: String,
            val artist: String,
            val playing: Boolean,
            val positionMs: Long,
            val durationMs: Long,
            val repeatMode: Int,
            val favourite: Boolean,
            val artUri: String?,
            val heightDp: Int
        )

        private fun snapshot(): Snapshot = synchronized(Lock) {
            Snapshot(
                lastTitle, lastArtist, lastPlaying, lastPositionMs,
                lastDurationMs, lastRepeatMode, lastFavourite, lastArtUri, lastHeightDp
            )
        }

        /** ارتفاع واقعی ویجت برای برش درست کاور (کاشی‌های ۱×۲ ارتفاع بیشتری می‌دهند) */
        private fun setWidgetHeightDp(context: Context, dp: Int) {
            if (dp <= 0) return
            synchronized(Lock) {
                if (lastHeightDp == dp) return
                lastHeightDp = dp
                runCatching { artCache.evictAll() }
            }
            runCatching {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putInt("height", dp).apply()
            }
        }

        private fun resetState() = synchronized(Lock) {
            lastTitle = "Alvand Player"
            lastArtist = ""
            lastPlaying = false
            lastPositionMs = 0L
            lastDurationMs = 0L
            lastRepeatMode = 0
            lastFavourite = false
            lastArtUri = null
            artCache.evictAll()
        }

        private fun loadPersisted(context: Context) {
            runCatching {
                val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                synchronized(Lock) {
                    lastTitle = p.getString("title", null) ?: "Alvand Player"
                    lastArtist = p.getString("artist", "") ?: ""
                    lastPlaying = p.getBoolean("playing", false)
                    lastPositionMs = p.getLong("position", 0L)
                    lastDurationMs = p.getLong("duration", 0L)
                    lastRepeatMode = p.getInt("repeat", 0)
                    lastFavourite = p.getBoolean("fav", false)
                    lastArtUri = p.getString("art", null)
                    lastHeightDp = p.getInt("height", DEFAULT_HEIGHT_DP)
                }
            }
        }

        private fun widgetIds(context: Context): IntArray = runCatching {
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, PlayerWidgetProvider::class.java))
        }.getOrDefault(IntArray(0))

        private fun serviceIntent(context: Context, action: String, req: Int): PendingIntent {
            val intent = Intent(context, PlaybackService::class.java).setAction(action)
            // از API 26 به بعد سرویس باید foreground باشد؛ اگر نشد به getService برمی‌گردیم
            return if (Build.VERSION.SDK_INT >= 26) {
                runCatching {
                    PendingIntent.getForegroundService(
                        context, req, intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }.getOrNull() ?: PendingIntent.getService(
                    context, req, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            } else {
                PendingIntent.getService(
                    context, req, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
        }

        /** وضعیت کامل ویجت را از سرویس پخش می‌گیرد و prefs را هم به‌روز می‌کند */
        fun updateAll(
            context: Context,
            title: String?,
            artist: String?,
            playing: Boolean,
            artUri: String?,
            positionMs: Long,
            durationMs: Long,
            repeatMode: Int,
            favourite: Boolean
        ) {
            synchronized(Lock) {
                if (title != null) lastTitle = title.ifBlank { "Alvand Player" }
                if (artist != null) lastArtist = artist
                // آرت همیشه همراه عنوان می‌آید؛ null یعنی این آهنگ کاور محلی ندارد
                if (title != null) lastArtUri = artUri
                lastPlaying = playing
                lastPositionMs = positionMs
                lastDurationMs = durationMs
                lastRepeatMode = repeatMode
                lastFavourite = favourite
            }
            runCatching {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString("title", lastTitle)
                    .putString("artist", lastArtist)
                    .putBoolean("playing", lastPlaying)
                    .putLong("position", lastPositionMs)
                    .putLong("duration", lastDurationMs)
                    .putInt("repeat", lastRepeatMode)
                    .putBoolean("fav", lastFavourite)
                    .putString("art", lastArtUri)
                    .apply()
            }
            push(context)
        }

        /** فقط نوار پیشرفت را تازه می‌کند (بدون دست زدن به بقیهٔ state) */
        fun updateProgress(context: Context, positionMs: Long) {
            synchronized(Lock) { lastPositionMs = positionMs }
            push(context)
        }

        private fun push(context: Context) {
            val ids = widgetIds(context)
            if (ids.isEmpty()) return
            val appContext = context.applicationContext
            scope.launch {
                val views = runCatching { buildViews(appContext) }.getOrNull() ?: return@launch
                val mgr = AppWidgetManager.getInstance(appContext)
                for (id in ids) {
                    runCatching { mgr.updateAppWidget(id, views) }
                }
            }
        }

        private suspend fun buildViews(context: Context): RemoteViews {
            val s = snapshot()

            val views = RemoteViews(context.packageName, R.layout.widget_player)
            views.setTextViewText(R.id.widget_title, s.title)
            views.setTextViewText(
                R.id.widget_artist,
                s.artist.ifBlank { context.getString(R.string.unknown_artist) }
            )
            views.setTextViewText(
                R.id.widget_time,
                formatTime(s.positionMs, s.durationMs)
            )

            // نوار پیشرفت: درصد بر مبنای مدت؛ اگر مدت نامعلوم است صفر
            val dur = s.durationMs
            val pos = s.positionMs.coerceAtLeast(0L)
            val progress = if (dur > 0L) {
                ((pos * PROGRESS_MAX) / dur).toInt().coerceIn(0, PROGRESS_MAX)
            } else 0
            runCatching {
                views.setProgressBar(R.id.widget_progress, progress, PROGRESS_MAX, false)
            }
            views.setViewVisibility(
                R.id.widget_seek_dot,
                if (progress <= 1) View.VISIBLE else View.INVISIBLE
            )

            // کاور: برش‌خورده به ارتفاع واقعی ویجت با گوشهٔ گرد چپ
            val density = context.resources.displayMetrics.density
            val artW = (ART_WIDTH_DP * density).toInt().coerceAtLeast(1)
            val artH = (s.heightDp * density).toInt().coerceAtLeast(1)
            val corner = (CORNER_DP * density).toInt()
            val artUri = s.artUri
            val artKey = if (artUri.isNullOrBlank()) "" else "$artUri@$artW x$artH"
            val bitmap = if (artKey.isEmpty()) {
                null
            } else {
                artCache.get(artKey) ?: Artwork.widgetArt(
                    runCatching { Uri.parse(artUri!!) }.getOrNull(),
                    context,
                    artW,
                    artH,
                    corner
                )?.also { artCache.put(artKey, it) }
            }
            if (bitmap != null) {
                // بیت‌مپ از قبل دقیقاً هم‌اندازهٔ قاب برش خورده، پس fitXY پیکسل‌به‌پیکسل می‌نشیند
                views.setImageViewBitmap(R.id.widget_art, bitmap)
            } else {
                views.setImageViewResource(R.id.widget_art, R.drawable.widget_art_placeholder_icon)
            }

            // پخش/توقف
            views.setImageViewResource(
                R.id.widget_toggle,
                if (s.playing) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            )
            views.setContentDescription(
                R.id.widget_toggle,
                context.getString(if (s.playing) R.string.widget_pause else R.string.widget_play)
            )

            // تکرار: آیکون «یک‌بار» وقتی روی حالت تکرار یک آهنگ است
            views.setImageViewResource(
                R.id.widget_repeat,
                if (s.repeatMode == 2) R.drawable.ic_widget_repeat_one
                else R.drawable.ic_widget_repeat
            )

            // علاقه‌مندی
            views.setImageViewResource(
                R.id.widget_fav,
                if (s.favourite) R.drawable.ic_widget_fav_on else R.drawable.ic_widget_fav
            )
            views.setContentDescription(
                R.id.widget_fav,
                context.getString(
                    if (s.favourite) R.string.widget_fav_on else R.string.widget_fav
                )
            )

            views.setContentDescription(R.id.widget_prev, context.getString(R.string.widget_prev))
            views.setContentDescription(R.id.widget_next, context.getString(R.string.widget_next))
            views.setContentDescription(R.id.widget_repeat, context.getString(R.string.widget_repeat))

            views.setOnClickPendingIntent(
                R.id.widget_toggle,
                serviceIntent(context, PlaybackService.ACTION_WIDGET_TOGGLE, REQ_TOGGLE)
            )
            views.setOnClickPendingIntent(
                R.id.widget_next,
                serviceIntent(context, PlaybackService.ACTION_WIDGET_NEXT, REQ_NEXT)
            )
            views.setOnClickPendingIntent(
                R.id.widget_prev,
                serviceIntent(context, PlaybackService.ACTION_WIDGET_PREV, REQ_PREV)
            )
            views.setOnClickPendingIntent(
                R.id.widget_repeat,
                serviceIntent(context, PlaybackService.ACTION_WIDGET_REPEAT, REQ_REPEAT)
            )
            views.setOnClickPendingIntent(
                R.id.widget_fav,
                serviceIntent(context, PlaybackService.ACTION_WIDGET_FAV, REQ_FAV)
            )

            val openApp = PendingIntent.getActivity(
                context, REQ_OPEN,
                Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_MAIN
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_art, openApp)
            views.setOnClickPendingIntent(R.id.widget_panel, openApp)
            return views
        }

        private fun formatTime(positionMs: Long, durationMs: Long): String {
            val ms = positionMs.coerceIn(0L, if (durationMs > 0) durationMs else Long.MAX_VALUE)
            val totalSec = (ms / 1000).toInt()
            val h = totalSec / 3600
            val m = (totalSec % 3600) / 60
            val s = totalSec % 60
            return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
        }

        /**
         * تایمر حرکت نوار پیشرفت. سرویس پخش هر ثانیه فقط زمان را می‌فرستد و
         * خود اینجا همه‌چیز دوباره ساخته نمی‌شود مگر واقعاً ویجت نصب باشد.
         */
        fun startProgressTicker(context: Context, scope: CoroutineScope) {
            stopProgressTicker()
            val appContext = context.applicationContext
            if (widgetIds(appContext).isEmpty()) return
            ticker = scope.launch(Dispatchers.Main) {
                while (true) {
                    kotlinx.coroutines.delay(1000)
                    val s = snapshot()
                    if (s.durationMs <= 0L) continue
                    val next = if (s.playing) s.positionMs + 1000L else s.positionMs
                    if (next > s.durationMs) continue
                    updateProgress(appContext, next)
                }
            }
        }

        fun stopProgressTicker() {
            runCatching { ticker?.cancel() }
            ticker = null
        }
    }
}
