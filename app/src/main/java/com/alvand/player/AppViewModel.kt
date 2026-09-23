package com.alvand.player

import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvand.player.audio.AudioSettings
import com.alvand.player.data.AlbumGroup
import com.alvand.player.data.ArtistGroup
import com.alvand.player.data.CrashInfo
import com.alvand.player.data.CrashLog
import com.alvand.player.data.LibraryFilter
import com.alvand.player.data.LibrarySort
import com.alvand.player.data.PlaylistRepository
import com.alvand.player.data.Song
import com.alvand.player.data.SongRepository
import com.alvand.player.data.SettingsRepo
import com.alvand.player.data.local.PlayHistoryEntity
import com.alvand.player.data.local.PlaylistEntity
import com.alvand.player.data.local.PlaylistSongEntity
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
import java.util.concurrent.atomic.AtomicLong

/** ویومدل اصلی اپ — وابستگی‌ها با Hilt تزریق می‌شوند */
@HiltViewModel
class AppViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    val manager: MusicPlayerManager,
    private val repo: SongRepository,
    private val settings: SettingsRepo,
    private val playlists: PlaylistRepository
) : ViewModel() {

    val playerState = manager.ui
    val eqSettings = manager.eqManager.settings

    /** حالت تم (۰=سیستم، ۱=روشن، ۲=تیره) و بکگراند دلخواه */
    val themeMode: StateFlow<Int> =
        settings.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val backgroundUri: StateFlow<String?> =
        settings.backgroundUri.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val onboardingSeen: StateFlow<Boolean> =
        settings.onboardingSeen.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setThemeMode(mode: Int) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setOnboardingSeen() {
        viewModelScope.launch { settings.setOnboardingSeen(true) }
    }

    fun setBackground(uriString: String?) {
        viewModelScope.launch { settings.setBackground(uriString) }
    }

    // ایونت ناوبری lifecycle-aware (جایگزین navTarget استاتیک MainActivity که لیک می‌داد)
    private val _navEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navEvents: SharedFlow<String> = _navEvents.asSharedFlow()
    fun navigateTo(route: String) { _navEvents.tryEmit(route) }

    // دمو فقط وقتی نشان داده می‌شود که واقعاً هیچ آهنگی نیست (نه قاطی لوکال)
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs
    private var hasScannedOnce = false

    private val _lyrics = MutableStateFlow(LyricsResult(emptyList(), "", "none"))
    val lyrics: StateFlow<LyricsResult> = _lyrics
    private val _lyricsLoading = MutableStateFlow(false)
    val lyricsLoading: StateFlow<Boolean> = _lyricsLoading
    private val lyricsGen = AtomicLong(0)

    /** علاقه‌مندی پایدار (DataStore) — بین اجراها می‌ماند */
    val liked: StateFlow<Set<Long>> =
        settings.likedIds.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun toggleLike(id: Long) {
        viewModelScope.launch { settings.toggleLike(id) }
    }

    fun isLiked(id: Long): Boolean = id in liked.value

    // ---- جستجو / سورت / فیلتر علاقه‌مندی‌ها ----
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val sortMode: StateFlow<Int> =
        settings.librarySort.stateIn(viewModelScope, SharingStarted.Eagerly, LibrarySort.DEFAULT)

    private val _favoritesOnly = MutableStateFlow(false)
    val favoritesOnly: StateFlow<Boolean> = _favoritesOnly

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun clearSearch() { _searchQuery.value = "" }
    fun setSortMode(mode: Int) {
        viewModelScope.launch { settings.setLibrarySort(mode) }
    }
    fun toggleFavoritesOnly() { _favoritesOnly.value = !_favoritesOnly.value }

    /** لیست نهایی برای UI: فیلتر + سرچ + سورت (خالص و تست‌پذیر) */
    val filteredSongs: StateFlow<List<Song>> = combine(
        _songs, _searchQuery, sortMode, liked, _favoritesOnly
    ) { songs, query, sort, likeSet, favOnly ->
        LibraryFilter.filterAndSort(songs, query, sort, likeSet, favOnly)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---- v1.5.0: پلی‌لیست‌ها + تاریخچه + گروه‌بندی ----
    val playlistList: StateFlow<List<PlaylistEntity>> =
        playlists.observePlaylists().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentHistory: StateFlow<List<PlayHistoryEntity>> =
        playlists.observeRecent(50).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistId: StateFlow<Long?> = _selectedPlaylistId

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedPlaylistSongs: StateFlow<List<PlaylistSongEntity>> =
        _selectedPlaylistId.flatMapLatest { pid ->
            if (pid == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else playlists.observePlaylistSongs(pid)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun selectPlaylist(id: Long?) { _selectedPlaylistId.value = id }

    suspend fun createPlaylist(name: String): Long = playlists.createPlaylist(name)
    fun createPlaylistAsync(name: String, onDone: (Long?) -> Unit = {}) {
        viewModelScope.launch {
            onDone(runCatching { playlists.createPlaylist(name) }.getOrNull())
        }
    }
    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            runCatching { playlists.deletePlaylist(id) }
            if (_selectedPlaylistId.value == id) _selectedPlaylistId.value = null
        }
    }
    fun addToPlaylist(pid: Long, song: Song, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch { onDone(runCatching { playlists.addToPlaylist(pid, song) }.getOrDefault(false)) }
    }
    fun removeFromPlaylist(pid: Long, songId: Long) {
        viewModelScope.launch { runCatching { playlists.removeFromPlaylist(pid, songId) } }
    }
    fun playPlaylistSongs(pid: Long) {
        viewModelScope.launch {
            val entities = runCatching {
                playlists.observePlaylistSongs(pid).first()
            }.getOrNull() ?: emptyList()
            val songsToPlay = entities.map { playlists.entityToSong(it) }
            if (songsToPlay.isNotEmpty()) manager.setQueue(songsToPlay, 0, true)
        }
    }
    fun clearHistory() {
        viewModelScope.launch { runCatching { playlists.clearHistory() } }
    }

    /** گروه‌بندی زنده برای تب‌های آلبوم/خواننده */
    val albumGroups: StateFlow<List<AlbumGroup>> = _songs.map { LibraryFilter.groupByAlbum(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val artistGroups: StateFlow<List<ArtistGroup>> = _songs.map { LibraryFilter.groupByArtist(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** بارگذاری (مجدد) آهنگ‌های دستگاه — بعد از دادن دسترسی صدا زده می‌شود */
    fun reloadLocalSongs() = scanDeviceSongs(announce = false)

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    /** نتیجه آخرین اسکن دستی: تعداد آهنگ‌های جدید (null = اسکنی انجام نشده/مصرف شده) */
    private val _lastScanAdded = MutableStateFlow<Int?>(null)
    val lastScanAdded: StateFlow<Int?> = _lastScanAdded
    fun consumeScanMessage() { _lastScanAdded.value = null }

    private val _permissionError = MutableStateFlow(false)
    val permissionError: StateFlow<Boolean> = _permissionError

    fun hasAudioPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.READ_MEDIA_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * اسکن آهنگ‌های گوشی از MediaStore و ادغام با پلی‌لیست.
     * آهنگ‌های قبلی (لینک/فایل دستی) حفظ می‌شوند؛ فقط لوکال‌ها به‌روز می‌شوند.
     */
    fun scanDeviceSongs(announce: Boolean = true) {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val local = try {
                    repo.loadLocalSongs()
                } catch (e: SecurityException) {
                    _permissionError.value = true
                    Log.w("VM", "scan: permission denied", e)
                    return@launch
                }
                _permissionError.value = false
                val prev = _songs.value
                val remote = prev.filter { it.isRemote }
                val manualLocal = prev.filter { !it.isRemote && it.id < 0 }
                val knownLocalIds = prev.filter { !it.isRemote && it.id >= 0 }.map { it.id }.toSet()
                val added = local.count { it.id !in knownLocalIds }
                val merged = local + manualLocal + remote
                _songs.value = merged.ifEmpty {
                    // فقط وقتی واقعاً خالی است دمو بده (نه قاطی لوکال)
                    if (!hasScannedOnce) repo.demoPlaylist() else emptyList()
                }
                hasScannedOnce = true
                if (announce) _lastScanAdded.value = added
            } catch (e: Exception) {
                Log.w("VM", "scan failed", e)
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

    // URI درست برای observer: همان کالکشنی که کوئری می‌زنیم (VOLUME_EXTERNAL در A29+)
    private fun observedUri(): Uri {
        return if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
    }

    init {
        scanDeviceSongs(announce = false)
        // دیده‌بان فایل‌های صوتی گوشی تا وقتی اپ زنده است
        runCatching {
            appContext.contentResolver.registerContentObserver(
                observedUri(),
                true,
                mediaObserver
            )
        }
        // لود لیریک هر آهنگ جدید: اول لوکال/امبدد، بعد آنلاین — با نسل تا پاسخ قدیمی روی آهنگ جدید ننشیند
        viewModelScope.launch {
            playerState.map { it.current }.distinctUntilChanged().collect { song ->
                if (song == null) return@collect
                // تاریخچه بیرون از مسیر بحرانی: خطای DB هرگز نباید پخش یا لیریک را خراب کند
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching { playlists.recordPlay(song) }
                }
                val gen = lyricsGen.incrementAndGet()
                _lyricsLoading.value = true
                var res = LyricsManager.loadLocal(song, appContext)
                if (res.lines.isEmpty()) {
                    val durSec = (song.durationMs / 1000).takeIf { it > 0 } ?: 0L
                    res = LyricsManager.fetchOnline(song.artist, song.title, durSec)
                    // کش آنلاین تا دفعه بعد آفلاین بیاید (بدون بلاک UI)
                    if (res.lines.isNotEmpty() && res.plainText.isNotBlank()) {
                        runCatching { LyricsManager.cacheOnline(song, appContext, res.plainText) }
                    }
                }
                // فقط اگر هنوز همین آهنگ است اعمال کن (ضد race اسکیپ سریع)
                if (lyricsGen.get() == gen) {
                    _lyrics.value = res
                    _lyricsLoading.value = false
                }
            }
        }
    }

    fun playList(list: List<Song>, index: Int) = manager.setQueue(list.ifEmpty { _songs.value }, index)

    fun playDirectLink(url: String): Boolean {
        val ok = manager.playDirectLink(url)
        if (ok != null) {
            // تکراری اضافه نکن
            if (_songs.value.none { it.id == ok.id }) {
                _songs.value = listOf(ok) + _songs.value
            }
            return true
        }
        return false
    }

    fun playUri(uri: Uri, name: String) {
        val s = Song(Song.localFileId(uri), name, "Local file", uri = uri)
        if (_songs.value.none { it.id == s.id }) {
            _songs.value = listOf(s) + _songs.value
        }
        manager.setQueue(listOf(s))
    }

    fun refreshLyricsOnline() {
        val c = playerState.value.current ?: return
        viewModelScope.launch {
            val gen = lyricsGen.incrementAndGet()
            _lyricsLoading.value = true
            val durSec = (c.durationMs / 1000).takeIf { it > 0 } ?: 0L
            val res = LyricsManager.fetchOnline(c.artist, c.title, durSec)
            if (res.lines.isNotEmpty() && res.plainText.isNotBlank()) {
                runCatching { LyricsManager.cacheOnline(c, appContext, res.plainText) }
            }
            if (lyricsGen.get() == gen) {
                _lyrics.value = res
                _lyricsLoading.value = false
            }
        }
    }

    fun saveLyricsManual(raw: String) {
        val c = playerState.value.current ?: return
        viewModelScope.launch {
            LyricsManager.saveManual(c, appContext, raw)
            val lines = LyricsManager.parseLrc(
                if (raw.contains("[")) raw else raw.lineSequence().filter { it.isNotBlank() }
                    .mapIndexed { i, t ->
                        val totalSec = i * 4L
                        "[%02d:%02d.00]$t".format(totalSec / 60, totalSec % 60)
                    }
                    .joinToString("\n")
            )
            _lyrics.value = LyricsResult(lines, raw, "manual")
        }
    }

    fun updateAudio(s: AudioSettings) = manager.applyAudio(s)

    fun clearPermissionError() { _permissionError.value = false }

    // ---- v1.6.3: گزارش کرش داخل اپ ----
    private val _crash = MutableStateFlow<CrashInfo?>(null)
    val crashReport: StateFlow<CrashInfo?> = _crash

    /** فقط کرشِ دیده‌نشده (برای دیالوگ شروع) */
    fun loadUnseenCrash() {
        _crash.value = runCatching { CrashLog.unseenCrash(appContext) }.getOrNull()
    }

    fun loadLastCrash() {
        _crash.value = runCatching { CrashLog.lastCrash(appContext) }.getOrNull()
    }

    fun markCrashSeen() {
        runCatching { CrashLog.markSeen(appContext) }
        _crash.value = null
    }

    fun clearCrashReport() {
        runCatching { CrashLog.clear(appContext) }
        _crash.value = null
    }

    override fun onCleared() {
        runCatching { appContext.contentResolver.unregisterContentObserver(mediaObserver) }
        autoScanJob?.cancel()
        // NOTE: manager.release() اینجا صدا زده نمی‌شود چون @Singleton تا آخر عمر اپ زنده است؛
        // بستن آن در onCleared ویومدل (که با چرخش هم می‌میرد) صف/سرویس را می‌پراند.
        super.onCleared()
    }
}
