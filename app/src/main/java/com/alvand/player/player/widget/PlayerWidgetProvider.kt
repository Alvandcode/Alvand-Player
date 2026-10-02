package com.alvand.player.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.alvand.player.MainActivity
import com.alvand.player.R
import com.alvand.player.data.AppLocale
import com.alvand.player.data.Artwork
import com.alvand.player.player.PlaybackService
import java.util.Locale
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * ویجت هوم‌اسکرین: کاور + عنوان/خواننده + نوار پیشرفت + ۵ کنترل
 * (تکرار، قبلی، پخش/توقف، بعدی، علاقه‌مندی).
 *
 * - دکمه‌ها مستقیم Intent به [PlaybackService] می‌فرستند تا بدون باز شدن اپ
 *   (لازم برای Android 12+ که PendingIntent پس‌زمینه را محدود می‌کند).
 * - تپ روی کاور/پنل → باز شدن اپ (singleTop).
 * - آخرین وضعیت در SharedPreferences نگه داشته می‌شود تا بعد از ریستارت
 *   لانچر هم ویجت درست باشد؛ خود Push از [PlaybackService] می‌آید.
 * - ساخت RemoteViews روی یک scope تک‌نویسنده انجام می‌شود و درخواست‌های همزمان
 *   هم‌ادغام (conflate) می‌شوند تا هنگام پخش، هر ثانیه یک بازسازی کامل صف رخ ندهد.
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
        runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("enabled", true).apply()
        }
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
            setWidgetHeightDp(context, maxH.takeIf { h -> h > 0 } ?: minH)
        }
        push(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // آهنگ در حال پخش باقی می‌ماند؛ اگر آخرین ویجت حذف شد تایمر متوقف کن
        if (widgetIds(context).isEmpty()) stopProgressTicker()
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
        private const val ART_WIDTH_DP = 52
        private const val CORNER_DP = 18
        private const val DEFAULT_HEIGHT_DP = 68
        /** ۳۶۰×۳۶۰ پیکسل ≈ ۵۱۸KB — زیر سقف Binder با حاشیهٔ کافی */
        private const val MAX_ART_PIXELS = 360 * 360
        private const val ART_CACHE_BYTES = 2 * 1024 * 1024

        private val Lock = Any()
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        /** یک ساخت در جریان + یک پرچم «یکی دیگر هم آمد» = هم‌ادغام بدون صف بی‌پایان */
        @Volatile private var building = false
        @Volatile private var pushQueued = false

        // کاور دیکدشده؛ بایت‌محور است و فقط از ترد سازندهٔ ویجت لمس می‌شود
        private val artCache = object : android.util.LruCache<String, android.graphics.Bitmap>(
            ART_CACHE_BYTES
        ) {
            override fun sizeOf(key: String, value: android.graphics.Bitmap): Int =
                value.byteCount.coerceAtLeast(1)
        }

        @Volatile private var lastTitle: String = ""
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

        private fun resetState() = synchronized(Lock) {
            lastTitle = ""
            lastArtist = ""
            lastPlaying = false
            lastPositionMs = 0L
            lastDurationMs = 0L
            lastRepeatMode = 0
            lastFavourite = false
            lastArtUri = null
            lastHeightDp = DEFAULT_HEIGHT_DP
        }

        private fun widgetIds(context: Context): IntArray = runCatching {
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, PlayerWidgetProvider::class.java))
        }.getOrDefault(IntArray(0))

        /** آیا تا حالا ویجت روی صفحه بوده؟ برای اینکه کاربر بدون ویجت، هر پخش یک نوشتن روی دیسک نخورد */
        private fun everEnabled(context: Context): Boolean = runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean("enabled", false)
        }.getOrDefault(false)

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

        /** کوچک‌کردن ابعاد با حفظ نسبت تا مجموع پیکسل از سقف نگذرد */
        private fun capPixels(w: Int, h: Int, maxPixels: Int): Pair<Int, Int> {
            val total = w.toLong() * h.toLong()
            if (total <= maxPixels) return w to h
            val scale = sqrt(maxPixels.toDouble() / total.toDouble())
            return (w * scale).toInt().coerceAtLeast(1) to (h * scale).toInt().coerceAtLeast(1)
        }

        /** ارتفاع واقعی ویجت برای برش درست کاور (کاشی‌های ۱×۲ ارتفاع بیشتری می‌دهند) */
        private fun setWidgetHeightDp(context: Context, dp: Int) {
            if (dp <= 0) return
            synchronized(Lock) {
                if (lastHeightDp == dp) return
                lastHeightDp = dp
                artCache.evictAll()
            }
            runCatching {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putInt("height", dp).apply()
            }
        }

        private fun loadPersisted(context: Context) {
            runCatching {
                val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val storedPos = p.getLong("position", 0L)
                val at = p.getLong("atElapsed", 0L)
                val playing = p.getBoolean("playing", false)
                // موقعیت ذخیره‌شده مربوط به لحظهٔ قبل است؛ اگر هنوز پخش می‌شود جلو می‌بریم
                val drift = if (playing && at > 0L) SystemClock.elapsedRealtime() - at else 0L
                synchronized(Lock) {
                    lastTitle = p.getString("title", "") ?: ""
                    lastArtist = p.getString("artist", "") ?: ""
                    lastPlaying = playing
                    lastPositionMs = if (drift > 0L) storedPos + drift else storedPos
                    lastDurationMs = p.getLong("duration", 0L)
                    lastRepeatMode = p.getInt("repeat", 0)
                    lastFavourite = p.getBoolean("fav", false)
                    lastArtUri = p.getString("art", null)
                    lastHeightDp = p.getInt("height", DEFAULT_HEIGHT_DP)
                }
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
            val installed = widgetIds(context).isNotEmpty()
            synchronized(Lock) {
                if (title != null) lastTitle = title
                if (artist != null) lastArtist = artist
                // آرت همیشه همراه عنوان می‌آید؛ null یعنی این آهنگ کاور محلی ندارد
                if (title != null) lastArtUri = artUri
                lastPlaying = playing
                lastPositionMs = positionMs
                lastDurationMs = durationMs
                lastRepeatMode = repeatMode
                lastFavourite = favourite
            }
            if (installed || everEnabled(context)) {
                runCatching {
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putString("title", lastTitle)
                        .putString("artist", lastArtist)
                        .putBoolean("playing", lastPlaying)
                        .putLong("position", lastPositionMs)
                        .putLong("atElapsed", SystemClock.elapsedRealtime())
                        .putLong("duration", lastDurationMs)
                        .putInt("repeat", lastRepeatMode)
                        .putBoolean("fav", lastFavourite)
                        .putString("art", lastArtUri)
                        .apply()
                }
            }
            push(context)
        }

        /** فقط نوار پیشرفت را تازه می‌کند (بدون دست زدن به بقیهٔ state) */
        fun updateProgress(context: Context, positionMs: Long) {
            synchronized(Lock) { lastPositionMs = positionMs }
            push(context)
        }

        /**
         * ساخت و اعمال ویجت. درخواست‌های همزمان هم‌ادغام می‌شوند: تا وقتی یک
         * ساخت در جریان است پرچم می‌خورد و بعد از اتمام فقط یک‌بار دیگر اجرا می‌شود.
         * این کار از صف انباشتهٔ بازسازی در تایمر ۱ ثانیه‌ای جلوگیری می‌کند.
         */
        private fun push(context: Context) {
            val ids = widgetIds(context)
            if (ids.isEmpty()) return
            val appContext = context.applicationContext
            if (building) {
                pushQueued = true
                return
            }
            scope.launch {
                do {
                    pushQueued = false
                    building = true
                    val views = runCatching { buildViews(appContext) }.getOrNull()
                    building = false
                    if (views != null) {
                        val mgr = AppWidgetManager.getInstance(appContext)
                        for (id in ids) {
                            runCatching { mgr.updateAppWidget(id, views) }
                        }
                    }
                } while (pushQueued)
            }
        }

        private suspend fun buildViews(raw: Context): RemoteViews {
            val s = snapshot()
            // رشته‌های ویجت باید با زبان انتخابی اپ بخوانند، نه زبان دستگاه؛
            // layout را لانچر inflate می‌کند ولی متن‌هایی که خودمان می‌گذاریم اینجاست
            val context = localizedContext(raw)

            val views = RemoteViews(raw.packageName, R.layout.widget_player)
            views.setTextViewText(
                R.id.widget_title,
                s.title.ifBlank { context.getString(R.string.widget_name) }
            )
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
            // نقطه فقط وقتی معنا دارد که خطی وجود داشته باشد؛ در مدت نامعلوم
            // یک نقطهٔ تنها وسط پنل معلق می‌افتاد
            views.setViewVisibility(
                R.id.widget_seek_dot,
                if (dur > 0L && progress <= 1) View.VISIBLE else View.INVISIBLE
            )

            // کاور: برش‌خورده به ارتفاع واقعی ویجت، با گوشهٔ گرد سمتِ شروع
            val density = raw.resources.displayMetrics.density
            val rtl = raw.resources.configuration.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL
            val artW = (ART_WIDTH_DP * density).toInt().coerceAtLeast(1)
            val artH = (s.heightDp * density).toInt().coerceAtLeast(1)
            val (aw, ah) = capPixels(artW, artH, MAX_ART_PIXELS)
            val corner = (CORNER_DP * density).toInt()
            val artUri = s.artUri
            val artKey = if (artUri.isNullOrBlank()) "" else "$artUri@$aw x$ah r$rtl"
            val bitmap = if (artKey.isEmpty()) {
                null
            } else {
                artCache.get(artKey) ?: Artwork.widgetArt(
                    runCatching { Uri.parse(artUri!!) }.getOrNull(),
                    raw,
                    aw,
                    ah,
                    corner,
                    roundStart = rtl
                )?.also { artCache.put(artKey, it) }
            }
            if (bitmap != null) {
                // بیت‌مپ از قبل دقیقاً هم‌اندازهٔ قاب برش خورده، پس centerCrop بی‌اعوجاج است
                views.setImageViewBitmap(R.id.widget_art, bitmap)
            } else {
                views.setImageViewResource(R.id.widget_art, R.drawable.widget_art_note)
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
                serviceIntent(raw, PlaybackService.ACTION_WIDGET_TOGGLE, REQ_TOGGLE)
            )
            views.setOnClickPendingIntent(
                R.id.widget_next,
                serviceIntent(raw, PlaybackService.ACTION_WIDGET_NEXT, REQ_NEXT)
            )
            views.setOnClickPendingIntent(
                R.id.widget_prev,
                serviceIntent(raw, PlaybackService.ACTION_WIDGET_PREV, REQ_PREV)
            )
            views.setOnClickPendingIntent(
                R.id.widget_repeat,
                serviceIntent(raw, PlaybackService.ACTION_WIDGET_REPEAT, REQ_REPEAT)
            )
            views.setOnClickPendingIntent(
                R.id.widget_fav,
                serviceIntent(raw, PlaybackService.ACTION_WIDGET_FAV, REQ_FAV)
            )

            val openApp = PendingIntent.getActivity(
                raw, REQ_OPEN,
                Intent(raw, MainActivity::class.java).apply {
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

        /** زبان اپ را برای رشته‌هایی که خودمان ست می‌کنیم اعمال می‌کند */
        private fun localizedContext(raw: Context): Context = runCatching {
            val tag = AppLocale.currentTag()
            val locale = Locale.forLanguageTag(tag)
            val cfg = Configuration(raw.resources.configuration)
            cfg.setLocale(locale)
            cfg.setLayoutDirection(locale)
            raw.createConfigurationContext(cfg)
        }.getOrDefault(raw)

        private fun formatTime(positionMs: Long, durationMs: Long): String {
            val ms = positionMs.coerceIn(
                0L,
                if (durationMs > 0L) durationMs else Long.MAX_VALUE
            )
            val totalSec = (ms / 1000).toInt()
            val h = totalSec / 3600
            val m = (totalSec % 3600) / 60
            val s = totalSec % 60
            return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
        }

        /**
         * تایمر حرکت نوار پیشرفت. سرویس پخش روی هر رویداد وضعیت این را صدا می‌زند،
         * پس اگر همین حالا در حال اجراست دست به آن نمی‌زنیم (وگرنه هر بار Job
         * کشته و دوباره ساخته می‌شد و یک تیک از دست می‌رفت).
         */
        fun startProgressTicker(context: Context, scope: CoroutineScope) {
            val appContext = context.applicationContext
            if (widgetIds(appContext).isEmpty()) return
            if (ticker?.isActive == true) return
            ticker = scope.launch(Dispatchers.Main) {
                while (true) {
                    kotlinx.coroutines.delay(1000)
                    val s = snapshot()
                    // مدت نامعلوم (استریم زنده) یا رسیدن به انتها: دیگر چیزی برای
                    // حرکت دادن نیست، حلقه می‌ماند تا رویداد بعدی پخش دوباره بسازدش
                    if (s.durationMs <= 0L) break
                    val next = if (s.playing) s.positionMs + 1000L else s.positionMs
                    if (next > s.durationMs) break
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
