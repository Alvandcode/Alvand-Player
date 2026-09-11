package com.alvand.player.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.alvand.player.MainActivity
import com.alvand.player.R
import com.alvand.player.player.PlaybackService

/**
 * ویجت هوم‌اسکرین: نمایش آهنگ فعلی + قبلی/پخش-توقف/بعدی.
 *
 * - دکمه‌ها مستقیم به [PlaybackService] می‌روند (بدون باز کردن اپ).
 * - تپ روی متن → باز شدن اپ.
 * - به‌روزرسانی از سمت [PlaybackService] با [updateAll] پوش می‌شود
 *   (بدون updatePeriodMillis دوره‌ای — مصرف باتری صفر در سکون).
 */
class PlayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
    }

    private fun buildViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_player)
        views.setTextViewText(R.id.widget_title, lastTitle)
        views.setTextViewText(R.id.widget_artist, lastArtist)
        views.setImageViewResource(
            R.id.widget_toggle,
            if (lastPlaying) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play
        )
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
        // تپ روی متن → باز شدن اپ
        val openApp = PendingIntent.getActivity(
            context, REQ_OPEN,
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_text_zone, openApp)
        return views
    }

    private fun serviceIntent(context: Context, action: String, req: Int): PendingIntent {
        val intent = Intent(context, PlaybackService::class.java).setAction(action)
        return PendingIntent.getService(
            context, req, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        private const val REQ_TOGGLE = 101
        private const val REQ_NEXT = 102
        private const val REQ_PREV = 103
        private const val REQ_OPEN = 104

        @Volatile private var lastTitle: String = "Alvand Player"
        @Volatile private var lastArtist: String = ""
        @Volatile private var lastPlaying: Boolean = false

        /** پوش وضعیت جدید به همه ویجت‌ها (از سرویس پخش صدا زده می‌شود) */
        fun updateAll(context: Context, title: String?, artist: String?, playing: Boolean) {
            if (title != null) lastTitle = title.ifBlank { "Alvand Player" }
            if (artist != null) lastArtist = artist
            lastPlaying = playing
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, PlayerWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val provider = PlayerWidgetProvider()
            for (id in ids) {
                runCatching { mgr.updateAppWidget(id, provider.buildViews(context)) }
            }
            runCatching { mgr.notifyAppWidgetViewDataChanged(ids, R.id.widget_title) }
        }
    }
}
