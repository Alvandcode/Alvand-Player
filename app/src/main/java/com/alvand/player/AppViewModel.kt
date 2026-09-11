package com.alvand.player

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvand.player.audio.AudioSettings
import com.alvand.player.data.Song
import com.alvand.player.data.SongRepository
import com.alvand.player.data.SettingsRepo
import com.alvand.player.lyrics.LyricsManager
import com.alvand.player.lyrics.LyricsResult
import com.alvand.player.player.MusicPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ویومدل اصلی اپ — وابستگی‌ها با Hilt تزریق می‌شوند */
@HiltViewModel
class AppViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    val manager: MusicPlayerManager,
    private val repo: SongRepository,
    private val settings: SettingsRepo
) : ViewModel() {

    val playerState = manager.ui
    val eqSettings = manager.eqManager.settings

    /** حالت تم (۰=سیستم، ۱=روشن، ۲=تیره) و بکگراند دلخواه */
    val themeMode: StateFlow<Int> =
        settings.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val backgroundUri: StateFlow<String?> =
        settings.backgroundUri.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setThemeMode(mode: Int) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setBackground(uriString: String?) {
        viewModelScope.launch { settings.setBackground(uriString) }
    }

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
    fun reloadLocalSongs() = scanDeviceSongs(announce = false)

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    /** نتیجه آخرین اسکن دستی: تعداد آهنگ‌های جدید (null = اسکنی انجام نشده/مصرف شده) */
    private val _lastScanAdded = MutableStateFlow<Int?>(null)
    val lastScanAdded: StateFlow<Int?> = _lastScanAdded
    fun consumeScanMessage() { _lastScanAdded.value = null }

    /**
     * اسکن آهنگ‌های گوشی از MediaStore و ادغام با پلی‌لیست.
     * آهنگ‌های قبلی (لینک/فایل دستی) حفظ می‌شوند؛ فقط لوکال‌ها به‌روز می‌شوند.
     */
    fun scanDeviceSongs(announce: Boolean = true) {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            try {
                runCatching {
                    val local = repo.loadLocalSongs()
                    val remote = _songs.value.filter { it.isRemote }
                    val knownLocalIds = _songs.value.filter { !it.isRemote }.map { it.id }.toSet()
                    val added = local.count { it.id !in knownLocalIds }
                    _songs.value = local + remote.ifEmpty { repo.demoPlaylist() }
                    if (announce) _lastScanAdded.value = added
                }
            } finally {
                _isScanning.value = false
            }
        }
    }

    // دیده‌بان MediaStore: اگر وقتی اپ باز است آهنگ جدیدی به گوشی اضافه/حذف شود،
    // بعد از ۲٫۵ ثانیه سکون، پلی‌لیست خودکار به‌روز می‌شود (بدون پیام مزاحم).
    private var autoScanJob: Job? = null
    private val mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = scheduleAutoScan()
        override fun onChange(selfChange: Boolean, uri: Uri?) = scheduleAutoScan()
    }

    private fun scheduleAutoScan() {
        autoScanJob?.cancel()
        autoScanJob = viewModelScope.launch {
            delay(2500)
            scanDeviceSongs(announce = false)
        }
    }

    init {
        scanDeviceSongs(announce = false)
        // دیده‌بان فایل‌های صوتی گوشی تا وقتی اپ زنده است
        runCatching {
            appContext.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaObserver
            )
        }
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

    override fun onCleared() {
        runCatching { appContext.contentResolver.unregisterContentObserver(mediaObserver) }
        autoScanJob?.cancel()
        manager.release()
        super.onCleared()
    }
}
