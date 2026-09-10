package com.alvand.player.player

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.exoplayer.ExoPlayer

/** سرویس پس‌زمینه برای پخش + نوتیفیکیشن مدیا */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        session = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.run { player.release(); release() }
        session = null
        super.onDestroy()
    }
}
