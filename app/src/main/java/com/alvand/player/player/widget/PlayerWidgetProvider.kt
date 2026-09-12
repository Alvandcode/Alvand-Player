package com.alvand.player.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.alvand.player.MainActivity
import com.alvand.player.R
import com.alvand.player.player.PlaybackService

/**
 * ویجت هوم‌اسکرین: نمایش آهنگ فعلی + قبلی/پخش-توقف/بعدی.
 *
 * - دکمه‌ها مستقیم به [PlaybackService] می‌روند (بدون باز کردن اپ).
 * - تپ روی متن → باز شدن اپ (singleTop).
 * - به‌روزرسانی از سمت [PlaybackService] با [updateAll] پوش می‌شود
 *   (بدون updatePeriodMillis دوره‌ای — مصرف باتری صفر در سکون).
 */
class PlayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // استیت واقعی را از کش persist شده بخوان (نه فقط RAM که بعد از ریبوت می‌پرد)
        loadPersisted(context)
        for (id in appWidgetIds) {
            runCatching { appWidgetManager.updateAppWidget(id, buildViews(context)) }
        }
    }

    override fun onEnabled(context: Context) {
        loadPersisted(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // چیزی برای پاک‌سازی خاص نیست
    }

    override fun onDisabled(context: Context) {
        // آخرین ویجت پاک شد — کش را ریست کن
        runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        }
        synchronized(Lock) {
            lastTitle = "Alvand Player"
            lastArtist = ""
            lastPlaying = false
        }
    }

    private fun buildViews(context: Context): RemoteViews {
        val (title, artist, playing) = synchronized(Lock) {
            Triple(lastTitle, lastArtist, lastPlaying)
        }
        val views = RemoteViews(context.packageName, R.layout.widget_player)
        views.setTextViewText(R.id.widget_title, title)
        views.setTextViewText(R.id.widget_artist, artist)
        views.setImageViewResource(
            R.id.widget_toggle,
            if (playing) R.drawable.ic_widget_pause
            else R.drawable.ic_widget_play
        )
        // دکمه‌های قبلی/بعدی هم آیکون اپ (برای سازگاری OEM)
        runCatching { views.setImageViewResource(R.id.widget_prev, R.drawable.ic_widget_prev) }
        runCatching { views.setImageViewResource(R.id.widget_next, R.drawable.ic_widget_next) }
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
        // تپ روی متن → باز شدن اپ (singleTop تا استک تکراری نسازد)
        val openApp = PendingIntent.getActivity(
            context, REQ_OPEN,
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_text_zone, openApp)
        return views
    }

    private fun serviceIntent(context: Context, action: String, req: Int): PendingIntent {
        val intent = Intent(context, PlaybackService::class.java).setAction(action)
        // اندروید ۱۲+ استارت سرویس از بک‌گراند محدود است؛ getForegroundService امن‌تر است
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

    companion object {
        private const val REQ_TOGGLE = 101
        private const val REQ_NEXT = 102
        private const val REQ_PREV = 103
        private const val REQ_OPEN = 104
        private const val PREFS = "alvand_widget"
        private val Lock = Any()

        @Volatile private var lastTitle: String = "Alvand Player"
        @Volatile private var lastArtist: String = ""
        @Volatile private var lastPlaying: Boolean = false

        private fun loadPersisted(context: Context) {
            runCatching {
                val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                synchronized(Lock) {
                    lastTitle = p.getString("title", "Alvand Player") ?: "Alvand Player"
                    lastArtist = p.getString("artist", "") ?: ""
                    lastPlaying = p.getBoolean("playing", false)
                }
            }
        }

        /** پوش وضعیت جدید به همه ویجت‌ها (از سرویس پخش صدا زده می‌شود) */
        fun updateAll(context: Context, title: String?, artist: String?, playing: Boolean) {
            synchronized(Lock) {
                if (title != null) lastTitle = title.ifBlank { "Alvand Player" }
                if (artist != null) lastArtist = artist
                lastPlaying = playing
            }
            // persist تا بعد از ریبوت/مرگ پروسس onUpdate درست رندر کند
            runCatching {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString("title", lastTitle)
                    .putString("artist", lastArtist)
                    .putBoolean("playing", lastPlaying)
                    .apply()
            }
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, PlayerWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val provider = PlayerWidgetProvider()
            for (id in ids) {
                runCatching { mgr.updateAppWidget(id, provider.buildViews(context)) }
            }
            // notifyAppWidgetViewDataChanged فقط برای AdapterView است — برای TextView بی‌اثر بود، حذف شد
        }
    }
}
