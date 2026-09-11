package com.alvand.player.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.alvand.player.audio.AudioSettings
import com.alvand.player.audio.EqualizerManager
import com.alvand.player.data.Song
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** وضعیت پخش برای UI */
data class PlayerUiState(
    val current: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Song> = emptyList(),
    val shuffle: Boolean = false,
    val repeatOne: Boolean = false,
    val error: String? = null
)

/**
 * موتور پخش بر پایه MediaController + PlaybackService.
 * پخش داخل سرویس انجام می‌شود، پس کنترل پخش/قبلی/بعدی در نوار اعلان،
 * لاک‌اسکرین و خروجی مدیا (quick settings) می‌آید و با بستن اپ قطع نمی‌شود.
 * همه فرمت‌های رایج + لینک مستقیم (progressive/HLS/DASH) پشتیبانی می‌شود.
 */
class MusicPlayerManager(context: Context) {

    private val app = context.applicationContext

    val eqManager = EqualizerManager()

    private val controllerFuture: ListenableFuture<MediaController> =
        MediaController.Builder(
            app,
            SessionToken(app, ComponentName(app, PlaybackService::class.java))
        ).buildAsync()

    private var controller: MediaController? = null

    /** دسترسی فقط‌خواندنی به پلیر (ممکن است هنوز وصل نشده باشد) */
    val player: Player? get() = controller

    private var pendingQueue: List<Song>? = null
    private var pendingIndex = 0
    private var pendingAutoplay = true

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) { push() }
        override fun onIsPlayingChanged(v: Boolean) { push() }
        override fun onMediaItemTransition(item: MediaItem?, r: Int) { push() }
        override fun onAudioSessionIdChanged(id: Int) { eqManager.attach(id) }
        override fun onPlayerError(error: PlaybackException) {
            _ui.value = _ui.value.copy(error = "play_error")
            push()
        }
    }

    init {
        controllerFuture.addListener(
            {
                controller = runCatching { controllerFuture.get() }.getOrNull()
                controller?.addListener(listener)
                attachEq()
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
    private val scope = CoroutineScope(Dispatchers.Main)

    fun setQueue(songs: List<Song>, startIndex: Int = 0, autoplay: Boolean = true) {
        val c = controller
        if (c == null) {
            // هنوز به سرویس وصل نشده — در صف انتظار نگه دار
            pendingQueue = songs
            pendingIndex = startIndex
            pendingAutoplay = autoplay
            _ui.value = _ui.value.copy(queue = songs)
            return
        }
        applyQueue(songs, startIndex, autoplay)
    }

    private fun applyQueue(songs: List<Song>, startIndex: Int, autoplay: Boolean) {
        val c = controller ?: return
        val items = songs.map { s ->
            MediaItem.Builder()
                .setUri(s.uri)
                .setMediaId(s.id.toString())
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(s.title).setArtist(s.artist).setAlbumTitle(s.album)
                        .build()
                ).build()
        }
        if (items.isEmpty()) return
        c.setMediaItems(items, startIndex.coerceIn(items.indices), 0)
        c.prepare()
        _ui.value = _ui.value.copy(queue = songs)
        if (autoplay) c.play()
        startProgress()
        // اکولایزر را به سشن جدید وصل کن
        scope.launch {
            delay(400)
            attachEq()
        }
    }

    /** پخش تک لینک مستقیم */
    fun playDirectLink(url: String): Song? {
        val song = Song.fromDirectLink(url) ?: return null
        setQueue(listOf(song), 0, true)
        return song
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause()
        else {
            // بعد از خطا، پلیر به prepare مجدد نیاز دارد
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
        push()
    }

    fun next() {
        val c = controller ?: return
        if (c.hasNextMediaItem()) {
            c.seekToNextMediaItem()
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            push()
        }
    }

    fun prev() {
        val c = controller ?: return
        if (c.currentPosition > 3000) c.seekTo(0)
        else if (c.hasPreviousMediaItem()) {
            c.seekToPreviousMediaItem()
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
        }
        push()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        push()
    }

    fun toggleShuffle() {
        val c = controller ?: return
        _ui.value = _ui.value.copy(shuffle = !_ui.value.shuffle)
        c.shuffleModeEnabled = _ui.value.shuffle
    }

    fun toggleRepeatOne() {
        val c = controller ?: return
        _ui.value = _ui.value.copy(repeatOne = !_ui.value.repeatOne)
        c.repeatMode = if (_ui.value.repeatOne) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_ALL
    }

    fun applyAudio(s: AudioSettings) = eqManager.applyAll(s)

    fun clearError() { _ui.value = _ui.value.copy(error = null) }

    private var lastEqSession = 0

    /** اتصال اکولایزر به سشن صوتی سرویس (فقط وقتی عوض شده باشد) */
    private fun attachEq() {
        val id = PlaybackService.audioSessionId
        if (id > 0 && id != lastEqSession) {
            lastEqSession = id
            eqManager.attach(id)
        }
    }

    private fun startProgress() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                push()
                delay(500)
            }
        }
    }

    private fun push() {
        attachEq()
        val c = controller
        val q = _ui.value.queue
        val idx = c?.currentMediaItemIndex ?: -1
        _ui.value = _ui.value.copy(
            current = q.getOrNull(idx),
            isPlaying = c?.isPlaying == true,
            positionMs = (c?.currentPosition ?: 0L).coerceAtLeast(0),
            durationMs = c?.duration?.takeIf { it > 0 }
                ?: (_ui.value.current?.durationMs ?: 0L)
        )
    }

    fun release() {
        progressJob?.cancel()
        eqManager.release()
        runCatching { controller?.removeListener(listener) }
        controller = null
        runCatching { MediaController.releaseFuture(controllerFuture) }
    }
}
