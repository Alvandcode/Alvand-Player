package com.alvand.player.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.exoplayer.ExoPlayer
import com.alvand.player.MainActivity
import com.alvand.player.player.widget.PlayerWidgetProvider

/** سرویس پس‌زمینه برای پخش + نوتیفیکیشن مدیا + ویجت هوم‌اسکرین */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private var player: ExoPlayer? = null

    companion object {
        /** آی‌دی سشن صوتی پلیر — اکولایزر/تقویت صدا به آن وصل می‌شود */
        @Volatile var audioSessionId: Int = 0

        /** اکشن‌های ویجت هوم‌اسکرین */
        const val ACTION_WIDGET_TOGGLE = "com.alvand.player.WIDGET_TOGGLE"
        const val ACTION_WIDGET_NEXT = "com.alvand.player.WIDGET_NEXT"
        const val ACTION_WIDGET_PREV = "com.alvand.player.WIDGET_PREV"
    }

    private val widgetListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) { pushWidget() }
        override fun onMediaItemTransition(item: MediaItem?, reason: Int) { pushWidget() }
        override fun onPlaybackStateChanged(state: Int) { pushWidget() }
    }

    override fun onCreate() {
        super.onCreate()
        val exo = ExoPlayer.Builder(this).build()
        player = exo
        exo.addListener(widgetListener)
        audioSessionId = exo.audioSessionId
        // تپ روی نوتیفیکیشن → باز شدن اپ
        val sessionActivity = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        session = MediaSession.Builder(this, exo)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /** اکشن‌های ویجت (بدون باز کردن اپ) */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val p = player
        when (intent?.action) {
            ACTION_WIDGET_TOGGLE -> {
                if (p != null) {
                    if (p.isPlaying) p.pause()
                    else {
                        if (p.playbackState == Player.STATE_IDLE) p.prepare()
                        p.play()
                    }
                }
            }
            ACTION_WIDGET_NEXT -> {
                if (p != null && p.hasNextMediaItem()) {
                    p.seekToNextMediaItem()
                    if (p.playbackState == Player.STATE_IDLE) p.prepare()
                    p.play()
                }
            }
            ACTION_WIDGET_PREV -> {
                if (p != null) {
                    if (p.currentPosition > 3000) p.seekTo(0)
                    else if (p.hasPreviousMediaItem()) {
                        p.seekToPreviousMediaItem()
                        if (p.playbackState == Player.STATE_IDLE) p.prepare()
                        p.play()
                    }
                }
            }
        }
        if (intent?.action in setOf(ACTION_WIDGET_TOGGLE, ACTION_WIDGET_NEXT, ACTION_WIDGET_PREV)) {
            pushWidget()
        }
        return START_STICKY
    }

    private fun pushWidget() {
        val p = player ?: return
        val meta = p.currentMediaItem?.mediaMetadata
        runCatching {
            PlayerWidgetProvider.updateAll(
                this,
                meta?.title?.toString() ?: getString(com.alvand.player.R.string.widget_name),
                meta?.artist?.toString() ?: "",
                p.isPlaying
            )
        }
    }

    override fun onDestroy() {
        runCatching { player?.removeListener(widgetListener) }
        player = null
        session?.run { player.release(); release() }
        session = null
        super.onDestroy()
    }
}
