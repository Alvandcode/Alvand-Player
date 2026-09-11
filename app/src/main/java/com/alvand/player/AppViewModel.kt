package com.alvand.player

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvand.player.audio.AudioSettings
import com.alvand.player.data.Song
import com.alvand.player.data.SongRepository
import com.alvand.player.lyrics.LyricsManager
import com.alvand.player.lyrics.LyricsResult
import com.alvand.player.player.MusicPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ویومدل اصلی اپ — وابستگی‌ها با Hilt تزریق می‌شوند */
@HiltViewModel
class AppViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    val manager: MusicPlayerManager,
    private val repo: SongRepository
) : ViewModel() {

    val playerState = manager.ui
    val eqSettings = manager.eqManager.settings

    private val _songs = MutableStateFlow<List<Song>>(repo.demoPlaylist())
    val songs: StateFlow<List<Song>> = _songs

    private val _lyrics = MutableStateFlow(LyricsResult(emptyList(), "", "none"))
    val lyrics: StateFlow<LyricsResult> = _lyrics
    private val _lyricsLoading = MutableStateFlow(false)
    val lyricsLoading: StateFlow<Boolean> = _lyricsLoading

    private val _liked = MutableStateFlow<Set<Long>>(emptySet())
    val liked: StateFlow<Set<Long>> = _liked

    fun toggleLike(id: Long) {
        _liked.value = if (id in _liked.value) _liked.value - id else _liked.value + id
    }

    /** بارگذاری (مجدد) آهنگ‌های دستگاه — بعد از دادن دسترسی صدا زده می‌شود */
    fun reloadLocalSongs() {
        viewModelScope.launch {
            runCatching {
                val local = repo.loadLocalSongs()
                val remote = _songs.value.filter { it.isRemote }
                _songs.value = local + remote.ifEmpty { repo.demoPlaylist() }
            }
        }
    }

    init {
        reloadLocalSongs()
        // لود لیریک هر آهنگ جدید: اول لوکال/امبدد، بعد آنلاین
        viewModelScope.launch {
            playerState.map { it.current }.distinctUntilChanged().collect { song ->
                if (song == null) return@collect
                _lyricsLoading.value = true
                var res = LyricsManager.loadLocal(song, appContext)
                if (res.lines.isEmpty()) {
                    res = LyricsManager.fetchOnline(song.artist, song.title)
                }
                _lyrics.value = res
                _lyricsLoading.value = false
            }
        }
    }

    fun playList(list: List<Song>, index: Int) = manager.setQueue(list.ifEmpty { _songs.value }, index)

    fun playDirectLink(url: String): Boolean {
        val ok = manager.playDirectLink(url)
        if (ok != null) {
            _songs.value = listOf(ok) + _songs.value
            return true
        }
        return false
    }

    fun playUri(uri: Uri, name: String) {
        val s = Song(uri.toString().hashCode().toLong(), name, "Local file", uri = uri)
        _songs.value = listOf(s) + _songs.value
        manager.setQueue(listOf(s))
    }

    fun refreshLyricsOnline() {
        val c = playerState.value.current ?: return
        viewModelScope.launch {
            _lyricsLoading.value = true
            _lyrics.value = LyricsManager.fetchOnline(c.artist, c.title)
            _lyricsLoading.value = false
        }
    }

    fun saveLyricsManual(raw: String) {
        val c = playerState.value.current ?: return
        viewModelScope.launch {
            LyricsManager.saveManual(c, appContext, raw)
            val lines = LyricsManager.parseLrc(
                if (raw.contains("[")) raw else raw.lineSequence().filter { it.isNotBlank() }
                    .mapIndexed { i, t -> "[00:${(i * 4).toString().padStart(2, '0')}.00]$t" }
                    .joinToString("\n")
            )
            _lyrics.value = LyricsResult(lines, raw, "manual")
        }
    }

    fun updateAudio(s: AudioSettings) = manager.applyAudio(s)

    override fun onCleared() { manager.release(); super.onCleared() }
}
