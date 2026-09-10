package com.alvand.player.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.alvand.player.audio.AudioSettings
import com.alvand.player.audio.EqualizerManager
import com.alvand.player.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/** وضعیت پخش برای UI */
data class PlayerUiState(
    val current: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Song> = emptyList(),
    val shuffle: Boolean = false,
    val repeatOne: Boolean = false
)

/**
 * موتور پخش بر پایه Media3 ExoPlayer.
 * همه فرمت‌های رایج + لینک مستقیم (progressive/HLS/DASH) را پوشش می‌دهد.
 */
class MusicPlayerManager(context: Context) {

    private val app = context.applicationContext
    private val okhttp = OkHttpClient.Builder().build()

    val eqManager = EqualizerManager()

    val player: ExoPlayer by lazy {
        val dataSourceFactory = OkHttpDataSource.Factory(okhttp)
            .setUserAgent("AlvandPlayer/1.0")
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        ExoPlayer.Builder(app)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().also { exo ->
                exo.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) { push() }
                    override fun onIsPlayingChanged(v: Boolean) { push() }
                    override fun onMediaItemTransition(item: MediaItem?, r: Int) { push() }
                    override fun onAudioSessionIdChanged(id: Int) {
                        eqManager.attach(id)
                    }
                })
            }
    }

    private val _ui = MutableStateFlow(PlayerUiState())
    val ui: StateFlow<PlayerUiState> = _ui

    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun setQueue(songs: List<Song>, startIndex: Int = 0, autoplay: Boolean = true) {
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
        player.setMediaItems(items, startIndex.coerceIn(items.indices), 0)
        player.prepare()
        _ui.value = _ui.value.copy(queue = songs)
        if (autoplay) player.play()
        startProgress()
        // اکولایزر را به سشن جدید وصل کن
        scope.launch {
            delay(400)
            runCatching { eqManager.attach(player.audioSessionId) }
        }
    }

    /** پخش تک لینک مستقیم */
    fun playDirectLink(url: String): Song? {
        val song = Song.fromDirectLink(url) ?: return null
        setQueue(listOf(song), 0, true)
        return song
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
        push()
    }
    fun next() { if (player.hasNextMediaItem()) player.seekToNextMediaItem() }
    fun prev() {
        if (player.currentPosition > 3000) player.seekTo(0) else player.seekToPreviousMediaItem()
    }
    fun seekTo(ms: Long) { player.seekTo(ms); push() }
    fun toggleShuffle() {
        _ui.value = _ui.value.copy(shuffle = !_ui.value.shuffle)
        player.shuffleModeEnabled = _ui.value.shuffle
    }
    fun toggleRepeatOne() {
        _ui.value = _ui.value.copy(repeatOne = !_ui.value.repeatOne)
        player.repeatMode = if (_ui.value.repeatOne) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_ALL
    }

    fun applyAudio(s: AudioSettings) = eqManager.applyAll(s)

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
        val q = _ui.value.queue
        val idx = player.currentMediaItemIndex
        _ui.value = _ui.value.copy(
            current = q.getOrNull(idx),
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.takeIf { it > 0 } ?: (_ui.value.current?.durationMs ?: 0L)
        )
    }

    fun release() {
        progressJob?.cancel()
        eqManager.release()
        runCatching { player.release() }
    }
}
