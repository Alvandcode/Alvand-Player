package com.alvand.player.player

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.alvand.player.audio.EqualizerManager
import com.alvand.player.data.Song
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** وضعیت پخش برای UI */
data class PlayerUiState(
    val current: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Song> = emptyList(),
    val shuffle: Boolean = false,
    /** 0=خاموش، 1=تکرار همه، 2=تکرار تک‌آهنگ */
    val repeatMode: Int = 0,
    val error: String? = null
)

/**
 * موتور پخش بر پایه MediaController + PlaybackService.
 * پخش داخل سرویس انجام می‌شود، پس کنترل پخش/قبلی/بعدی در نوار اعلان،
 * لاک‌اسکرین و خروجی مدیا (quick settings) می‌آید و با بستن اپ قطع نمی‌شود.
 * همه فرمت‌های رایج + لینک مستقیم (progressive/HLS/DASH) پشتیبانی می‌شود.
 */
@Singleton
class MusicPlayerManager @Inject constructor(
    @ApplicationContext context: Context,
    val eqManager: EqualizerManager
) {

    private val app = context.applicationContext

    private val controllerFuture: ListenableFuture<MediaController> =
        MediaController.Builder(
            app,
            SessionToken(app, ComponentName(app, PlaybackService::class.java))
        ).buildAsync()

    private var controller: MediaController? = null
    @Volatile private var released = false

    /** دسترسی فقط‌خواندنی به پلیر (ممکن است هنوز وصل نشده باشد) */
    val player: Player? get() = controller

    private var pendingQueue: List<Song>? = null
    private var pendingIndex = 0
    private var pendingAutoplay = true
    private var eqAttachJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            // پخش موفق → خطای قبلی را پاک کن تا UI گیر نکند
            if (state == Player.STATE_READY) {
                if (_ui.value.error != null) _ui.value = _ui.value.copy(error = null)
            }
            push()
        }
        override fun onIsPlayingChanged(v: Boolean) {
            if (v && _ui.value.error != null) _ui.value = _ui.value.copy(error = null)
            push()
            // وقتی پاز شد، حلقه پیشرفت را نگه دار ولی کم‌مصرف (push بعدی خودش آپدیت می‌کند)
        }
        override fun onMediaItemTransition(item: MediaItem?, r: Int) { push() }
        override fun onPositionDiscontinuity(oldPos: Player.PositionInfo, newPos: Player.PositionInfo, reason: Int) { push() }
        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) { push() }
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            // کنترلر منبع حقیقت است (نوتیفیکیشن/Auto/هدست هم همین را عوض می‌کند)
            _ui.value = _ui.value.copy(shuffle = shuffleModeEnabled)
            push()
        }
        override fun onRepeatModeChanged(repeatMode: Int) {
            _ui.value = _ui.value.copy(
                repeatMode = when (repeatMode) {
                    Player.REPEAT_MODE_ALL -> 1
                    Player.REPEAT_MODE_ONE -> 2
                    else -> 0
                }
            )
            push()
        }
        override fun onAudioSessionIdChanged(id: Int) {
            // بدون runCatching مستقیم — attach امن است و خودش خطا را می‌بلعد
            scheduleEqAttach()
        }
        override fun onPlayerError(error: PlaybackException) {
            Log.w("Player", "play error: ${error.errorCodeName}", error)
            _ui.value = _ui.value.copy(error = "play_error")
            push()
        }
    }

    init {
        controllerFuture.addListener(
            {
                if (released) {
                    runCatching { MediaController.releaseFuture(controllerFuture) }
                    return@addListener
                }
                controller = runCatching { controllerFuture.get() }.getOrNull()
                controller?.addListener(listener)
                // حالت اولیه کنترلر را به UI بده (اگر از نوتیفیکیشن عوض شده بود)
                controller?.let { c ->
                    runCatching {
                        _ui.value = _ui.value.copy(
                            shuffle = c.shuffleModeEnabled,
                            repeatMode = when (c.repeatMode) {
                                Player.REPEAT_MODE_ALL -> 1
                                Player.REPEAT_MODE_ONE -> 2
                                else -> 0
                            }
                        )
                    }
                }
                scheduleEqAttach()
                pendingQueue?.let { q ->
                    applyQueue(q, pendingIndex, pendingAutoplay)
                    pendingQueue = null
                }
                push()
            },
            ContextCompat.getMainExecutor(app)
        )
    }

    private val _ui = MutableStateFlow(PlayerUiState())
    val ui: StateFlow<PlayerUiState> = _ui

    private var progressJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** تایمر خواب با fade-out: ولوم کم می‌شود، بعد پخش متوقف می‌گردد */
    val sleepTimer = SleepTimer(
        scope = scope,
        setVolume = { v -> runCatching { controller?.volume = v } },
        getVolume = { controller?.volume ?: 1f },
        onExpire = { controller?.pause() }
    )
    val sleepState: StateFlow<SleepTimerState> get() = sleepTimer.state

    /** شروع تایمر خواب بر حسب دقیقه (۱ تا ۷۲۰) */
    fun startSleepTimer(minutes: Int) {
        val m = minutes.coerceIn(1, 720)
        sleepTimer.start(m * 60_000L)
    }

    /** خواب در پایان آهنگ فعلی (با fade در ثانیه‌های آخر) */
    fun startSleepEndOfTrack() {
        val c = controller
        val dur = c?.duration?.takeIf { it > 0 && it != C.TIME_UNSET } ?: _ui.value.durationMs
        val pos = c?.currentPosition ?: _ui.value.positionMs
        if (dur <= 0 || dur == C.TIME_UNSET) return // بی‌صدا خارج نشو — چیزی برای fade نیست
        val remain = (dur - pos).coerceAtLeast(5_000L)
        sleepTimer.start(remain, fadeMs = minOf(15_000L, remain / 2))
    }

    fun cancelSleepTimer() = sleepTimer.cancel()

    fun setQueue(songs: List<Song>, startIndex: Int = 0, autoplay: Boolean = true) {
        val c = controller
        if (c == null) {
            // هنوز به سرویس وصل نشده — آخرین درخواست معتبر است (overwrite عمدی)
            pendingQueue = songs
            pendingIndex = startIndex
            pendingAutoplay = autoplay
            _ui.value = _ui.value.copy(queue = songs)
            // اگر خالی است، چیزی برای اعمال نیست
            return
        }
        applyQueue(songs, startIndex, autoplay)
    }

    private fun applyQueue(songs: List<Song>, startIndex: Int, autoplay: Boolean) {
        val c = controller ?: return
        if (released) return
        if (songs.isEmpty()) {
            // صف خالی: کنترلر و UI را تمیز کن، بی‌صدا رها نکن
            runCatching { c.stop(); c.clearMediaItems() }
            _ui.value = _ui.value.copy(queue = emptyList(), current = null, positionMs = 0, durationMs = 0)
            return
        }
        val items = songs.map { s ->
            MediaItem.Builder()
                .setUri(s.uri)
                .setMediaId(s.id.toString())
                .setMimeType(s.mimeHint)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(s.title).setArtist(s.artist).setAlbumTitle(s.album)
                        .build()
                ).build()
        }
        runCatching {
            c.setMediaItems(items, startIndex.coerceIn(items.indices), C.TIME_UNSET)
            c.prepare()
        }.onFailure { Log.w("Player", "setMediaItems failed", it); return }
        _ui.value = _ui.value.copy(
            queue = songs,
            shuffle = c.shuffleModeEnabled,
            repeatMode = when (c.repeatMode) {
                Player.REPEAT_MODE_ALL -> 1
                Player.REPEAT_MODE_ONE -> 2
                else -> _ui.value.repeatMode
            }
        )
        if (autoplay) runCatching { c.play() }
        startProgress()
        scheduleEqAttach(400)
    }

    /** پخش تک لینک مستقیم */
    fun playDirectLink(url: String): Song? {
        val song = Song.fromDirectLink(url) ?: return null
        setQueue(listOf(song), 0, true)
        return song
    }

    fun togglePlayPause() {
        val c = controller ?: return
        runCatching {
            if (c.mediaItemCount == 0) return
            if (c.isPlaying) c.pause()
            else {
                if (c.playbackState == Player.STATE_IDLE) c.prepare()
                c.play()
            }
        }.onFailure { Log.w("Player", "toggle failed", it) }
        push()
    }

    fun next() {
        val c = controller ?: return
        runCatching {
            if (c.mediaItemCount == 0) return
            if (c.hasNextMediaItem()) {
                c.seekToNextMediaItem()
                if (c.playbackState == Player.STATE_IDLE) c.prepare()
                c.play()
                push()
            }
            // ته صف در REPEAT_OFF: سکوت عمدی نیست — همان‌جا بمان
        }.onFailure { Log.w("Player", "next failed", it) }
    }

    fun prev() {
        val c = controller ?: return
        runCatching {
            if (c.mediaItemCount == 0) return
            if (c.currentPosition > 3000) c.seekTo(0)
            else if (c.hasPreviousMediaItem()) {
                c.seekToPreviousMediaItem()
                if (c.playbackState == Player.STATE_IDLE) c.prepare()
                c.play()
            } else {
                c.seekTo(0)
            }
        }.onFailure { Log.w("Player", "prev failed", it) }
        push()
    }

    fun seekTo(ms: Long) {
        val c = controller ?: return
        runCatching {
            val dur = c.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: Long.MAX_VALUE
            val clamped = ms.coerceIn(0L, if (dur == Long.MAX_VALUE) ms.coerceAtLeast(0) else dur)
            // استریم لایو بدون پنجره seekable → نادیده بگیر، کرش نکن
            if (c.isCurrentMediaItemSeekable || c.duration != C.TIME_UNSET) {
                c.seekTo(clamped)
            }
        }.onFailure { Log.w("Player", "seek failed", it) }
        push()
    }

    fun toggleShuffle() {
        val c = controller ?: return
        runCatching {
            // کنترلر منبع حقیقت؛ UI از کال‌بک سینک می‌شود
            c.shuffleModeEnabled = !c.shuffleModeEnabled
            _ui.value = _ui.value.copy(shuffle = c.shuffleModeEnabled)
        }.onFailure { Log.w("Player", "shuffle failed", it) }
    }

    /** چرخه تکرار: خاموش → همه → تک‌آهنگ → خاموش */
    fun cycleRepeat() {
        val c = controller ?: return
        runCatching {
            val next = when (c.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            c.repeatMode = next
            _ui.value = _ui.value.copy(
                repeatMode = when (next) {
                    Player.REPEAT_MODE_ALL -> 1
                    Player.REPEAT_MODE_ONE -> 2
                    else -> 0
                }
            )
        }.onFailure { Log.w("Player", "repeat failed", it) }
    }

    fun applyAudio(s: com.alvand.player.audio.AudioSettings) = eqManager.applyAll(s)

    fun clearError() { _ui.value = _ui.value.copy(error = null) }

    private var lastEqSession = 0

    private fun scheduleEqAttach(delayMs: Long = 0) {
        if (released) return
        eqAttachJob?.cancel()
        eqAttachJob = scope.launch {
            if (delayMs > 0) delay(delayMs)
            attachEq()
        }
    }

    /** اتصال اکولایزر به سشن صوتی سرویس (فقط وقتی عوض شده باشد) */
    private fun attachEq() {
        if (released) return
        // NOTE: MediaController (رابط Player) در Media3 1.5.1 خاصیت audioSessionId ندارد؛
        // تنها منبع معتبر PlaybackService.audioSessionId است که سرویس نگه می‌دارد.
        val id = PlaybackService.audioSessionId.takeIf { it > 0 } ?: run {
            // سشن هنوز از سرویس نرسیده — کمی بعد دوباره تلاش کن (خودترمیم)
            scheduleEqAttach(500)
            return
        }
        if (id == lastEqSession && eqManager.isAttached()) return
        // بایندر سنگین را روی Main بلاک نکن — attach خودش امن است ولی IPC دارد
        scope.launch(Dispatchers.IO) {
            runCatching {
                eqManager.attach(id)
                lastEqSession = id
            }.onFailure {
                Log.w("Player", "eq attach failed sid=$id", it)
                lastEqSession = 0
            }
        }
    }

    private fun startProgress() {
        if (released) return
        if (progressJob?.isActive == true) return
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                runCatching { push() }.onFailure { Log.w("Player", "push failed", it) }
                delay(500)
            }
        }
    }

    private fun push() {
        if (released) return
        val c = controller
        val q = _ui.value.queue
        // ضد باگ shuffle: با mediaId پیدا کن، نه با index خام
        val mediaId = c?.currentMediaItem?.mediaId
        val current: Song? = when {
            c == null -> _ui.value.current
            mediaId != null -> q.firstOrNull { it.id.toString() == mediaId }
                ?: q.getOrNull(c.currentMediaItemIndex.coerceAtLeast(0))
            else -> q.getOrNull((c.currentMediaItemIndex).coerceAtLeast(0))
        }
        val dur = c?.duration?.takeIf { it > 0 && it != C.TIME_UNSET }
            ?: (current?.durationMs?.takeIf { it > 0 } ?: _ui.value.durationMs)
        _ui.value = _ui.value.copy(
            current = current,
            isPlaying = c?.isPlaying == true,
            positionMs = (c?.currentPosition ?: 0L).coerceAtLeast(0),
            durationMs = dur
        )
    }

    fun release() {
        if (released) return
        released = true
        progressJob?.cancel()
        eqAttachJob?.cancel()
        runCatching { sleepTimer.cancel() }
        runCatching { eqManager.release() }
        runCatching { controller?.removeListener(listener) }
        controller = null
        runCatching { MediaController.releaseFuture(controllerFuture) }
        scope.cancel()
    }
}
