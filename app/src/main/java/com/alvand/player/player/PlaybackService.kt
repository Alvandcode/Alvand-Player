package com.alvand.player.player

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.exoplayer.ExoPlayer
import com.alvand.player.MainActivity
import com.alvand.player.R
import com.alvand.player.data.SettingsRepo
import com.alvand.player.player.widget.PlayerWidgetProvider
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * سرویس پس‌زمینه برای پخش + نوتیفیکیشن مدیا + ویجت هوم‌اسکرین + Android Auto.
 * v1.6.0: MediaSessionService → MediaLibraryService تا Auto/Assistant بتوانند صف را browse کنند.
 * کتابخانه کامل (پلی‌لیست‌ها) فاز بعد؛ فعلاً root = صف فعلی پلیر.
 */
class PlaybackService : MediaLibraryService() {
    private var session: MediaLibraryService.MediaLibrarySession? = null
    private var player: ExoPlayer? = null
    private var consecutiveErrors = 0

    /** برای تایمر نوار پیشرفت ویجت و خواندن وضعیت علاقه‌مندی */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * تنظیمات ساده و بدون وابستگی؛ سرویس با Hilt تزریق نمی‌شود ولی این پوشه
     * فقط یک wrapper روی DataStore است و ساختن دستی‌اش بی‌خطر است.
     */
    private val settings: SettingsRepo by lazy { SettingsRepo(applicationContext) }

    companion object {
        /** آی‌دی سشن صوتی پلیر — اکولایزر/تقویت صدا به آن وصل می‌شود */
        @Volatile var audioSessionId: Int = 0

        /** اکشن‌های ویجت هوم‌اسکرین */
        const val ACTION_WIDGET_TOGGLE = "com.alvand.player.WIDGET_TOGGLE"
        const val ACTION_WIDGET_NEXT = "com.alvand.player.WIDGET_NEXT"
        const val ACTION_WIDGET_PREV = "com.alvand.player.WIDGET_PREV"
        const val ACTION_WIDGET_REPEAT = "com.alvand.player.WIDGET_REPEAT"
        const val ACTION_WIDGET_FAV = "com.alvand.player.WIDGET_FAV"

        /** روت کتابخانه برای Android Auto */
        const val ROOT_ID = "alvand-root"

        /** سقف هر صفحهٔ browse تا از محدودیت Binder عبور نکنیم */
        const val MAX_BROWSE_PAGE = 100

        /** بعد از چند خطای پیاپی، دست از اسکیپ خودکار بردار */
        const val MAX_CONSECUTIVE_ERRORS = 3

        /** کانال اعلان پخش (نام نمایشی از strings) */
        const val CHANNEL_ID = "alvand_playback"
    }

    private val widgetListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) consecutiveErrors = 0
            pushWidget()
        }
        override fun onMediaItemTransition(item: MediaItem?, reason: Int) { pushWidget() }
        override fun onPlaybackStateChanged(state: Int) {
            pushWidget()
            if (state == Player.STATE_IDLE || state == Player.STATE_ENDED) {
                // اگر صف خالی و پخش تمام شده، سرویس را نگه ندار
                val p = player
                if (p != null && p.mediaItemCount == 0) {
                    runCatching { stopSelf() }
                }
            }
        }
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            PlaybackService.audioSessionId = audioSessionId
        }
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Log.w("PlaybackService", "player error: ${error.errorCodeName}", error)
            pushWidget()
            val p = player ?: return
            if (consecutiveErrors < MAX_CONSECUTIVE_ERRORS && p.hasNextMediaItem()) {
                consecutiveErrors++
                p.seekToNextMediaItem()
                p.prepare()
                p.play()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setChannelName(R.string.playback_channel)
                .build()
        )
        try {
            val exo = ExoPlayer.Builder(this)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true // handleAudioFocus = قطع تماس → داک/پاز خودکار
                )
                .setWakeMode(C.WAKE_MODE_LOCAL) // در Doze CPU نخوابد
                .setHandleAudioBecomingNoisy(true) // جدا شدن هدفون → پاز
                .build()
            player = exo
            exo.addListener(widgetListener)
            audioSessionId = exo.audioSessionId
            // تپ روی نوتیفیکیشن → باز شدن اپ (singleTop تا استک تکراری نسازد)
            val sessionActivity = PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java).apply {
                    action = Intent.ACTION_MAIN
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            session = MediaLibraryService.MediaLibrarySession.Builder(this, exo, libraryCallback)
                .setSessionActivity(sessionActivity)
                .build()
        } catch (e: Exception) {
            Log.e("PlaybackService", "ExoPlayer create failed", e)
            // بدون پلیر سرویس معنا ندارد — لوپ START_STICKY نساز
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    /** کتابخانه حداقلی برای Android Auto: root = صف فعلی (فاز بعد: پلی‌لیست‌ها/آلبوم‌ها) */
    private val libraryCallback = object : MediaLibraryService.MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibraryService.MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(getString(R.string.app_name))
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }

        override fun onGetChildren(
            session: MediaLibraryService.MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val p = player
            val all: List<MediaItem> = if (parentId == ROOT_ID && p != null && p.mediaItemCount > 0) {
                (0 until p.mediaItemCount).mapNotNull { runCatching { p.getMediaItemAt(it) }.getOrNull() }
            } else emptyList()
            val size = pageSize.coerceIn(1, MAX_BROWSE_PAGE)
            val from = (page * size).coerceAtMost(all.size)
            val to = (from + size).coerceAtMost(all.size)
            val items = if (from < to) all.subList(from, to) else emptyList()
            return Futures.immediateFuture(LibraryResult.ofItemList(items, params))
        }

        override fun onAddMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            // درخواست صوتی Auto/Assistant را همان‌طور که هست قبول کن (رزولوشن URI در MusicPlayerManager)
            return Futures.immediateFuture(mediaItems)
        }
    }

    /** اکشن‌های ویجت (بدون باز کردن اپ) */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val superResult = super.onStartCommand(intent, flags, startId)
        val p = player
        when (intent?.action) {
            ACTION_WIDGET_TOGGLE -> {
                if (p != null) {
                    runCatching {
                        if (p.mediaItemCount == 0) {
                            // تایم‌لاین خالی — بی‌صدا no-op نکن، لاگ بزن
                            Log.d("PlaybackService", "toggle ignored: empty timeline")
                        } else if (p.isPlaying) p.pause()
                        else {
                            if (p.playbackState == Player.STATE_IDLE) p.prepare()
                            p.play()
                        }
                    }
                }
            }
            ACTION_WIDGET_NEXT -> {
                if (p != null) {
                    runCatching {
                        if (p.hasNextMediaItem()) {
                            p.seekToNextMediaItem()
                            if (p.playbackState == Player.STATE_IDLE) p.prepare()
                            p.play()
                        }
                    }
                }
            }
            ACTION_WIDGET_PREV -> {
                if (p != null) {
                    runCatching {
                        if (p.currentPosition > 3000) p.seekTo(0)
                        else if (p.hasPreviousMediaItem()) {
                            p.seekToPreviousMediaItem()
                            if (p.playbackState == Player.STATE_IDLE) p.prepare()
                            p.play()
                        }
                    }
                }
            }
            ACTION_WIDGET_REPEAT -> {
                if (p != null) {
                    runCatching {
                        p.repeatMode = when (p.repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                    }
                }
            }
            ACTION_WIDGET_FAV -> {
                val id = p?.let { currentSongId(it) }
                if (id != null) {
                    scope.launch { runCatching { settings.toggleLike(id) } }
                }
            }
        }
        if (intent?.action?.startsWith("com.alvand.player.WIDGET_") == true) {
            pushWidget()
            // ویجت foreground نمی‌خواهد — سرویس مدیا خودش مدیریت می‌کند
            stopSelfResult(startId)
        }
        // برای اینتنت مدیا همان نتیجه سوپر، برای بقیه STICKY نیست تا بیهوده زنده نماند
        return if (intent?.action?.startsWith("com.alvand.player.WIDGET_") == true) START_NOT_STICKY
        else superResult
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // کاربر اپ را از recents پاک کرد ولی پخش ادامه دارد — کاری نکن.
        // اگر پخش متوقف است، سرویس را ببند.
        val p = player
        if (p == null || (!p.isPlaying && p.playbackState != Player.STATE_BUFFERING)) {
            runCatching { stopSelf() }
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun pushWidget() {
        val p = player ?: return
        val item = p.currentMediaItem
        val meta = item?.mediaMetadata
        val title = meta?.title?.toString() ?: getString(R.string.widget_name)
        val artist = meta?.artist?.toString() ?: ""
        val duration = p.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
        val artUri = item?.localConfiguration?.uri?.takeIf { isLocalAudioUri(it) }?.toString()
        val repeat = p.repeatMode
        val songId = currentSongId(p)
        scope.launch {
            // علاقه‌مندی در DataStore است؛ اگر خواندنش شکست خورد فرض می‌کنیم نیست
            val fav = songId != null && runCatching { settings.likedIds.first() }
                .getOrDefault(emptySet())
                .contains(songId)
            runCatching {
                PlayerWidgetProvider.updateAll(
                    this@PlaybackService,
                    title,
                    artist,
                    p.isPlaying,
                    artUri,
                    p.currentPosition.coerceAtLeast(0L),
                    duration,
                    repeat,
                    fav
                )
            }.onFailure { Log.w("PlaybackService", "widget push failed", it) }
            if (p.isPlaying) PlayerWidgetProvider.startProgressTicker(this@PlaybackService, scope)
            else PlayerWidgetProvider.stopProgressTicker()
        }
    }

    /** فقط طرح‌های محلی — استریم https نباید باعث باز شدن اتصال شبکه از مسیر ویجت شود */
    private fun isLocalAudioUri(uri: android.net.Uri): Boolean = when (uri.scheme?.lowercase()) {
        "content", "file", "android.resource" -> true
        else -> false
    }

    /** شناسهٔ عددی آهنگ فعلی از mediaId (که همان شناسهٔ Song است) */
    private fun currentSongId(p: ExoPlayer): Long? =
        p.currentMediaItem?.mediaId?.toLongOrNull()

    override fun onDestroy() {
        runCatching { player?.removeListener(widgetListener) }
        runCatching { PlayerWidgetProvider.stopProgressTicker() }
        val s = session
        val exo = player
        player = null
        session = null
        audioSessionId = 0
        runCatching { exo?.release() }
        runCatching { s?.release() }
        // ویجت به حالت «آهنگی در حال پخش نیست» برگردد
        runCatching {
            PlayerWidgetProvider.updateAll(
                this,
                getString(R.string.widget_name),
                "",
                false,
                null,
                0L,
                0L,
                0,
                false
            )
        }
        runCatching { scope.cancel() }
        super.onDestroy()
    }
}
