package com.alvand.player

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.alvand.player.data.Song
import com.alvand.player.data.ThemeMode
import com.alvand.player.ui.navigation.AppNavHost
import com.alvand.player.ui.navigation.Routes
import com.alvand.player.ui.theme.AlvandTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val vm: AppViewModel by viewModels()

    private val pickAudio = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "Local audio"
            vm.playUri(uri, name)
            vm.navigateTo(Routes.PLAYER)
            @Suppress("DEPRECATION")
            navTarget.value = Routes.PLAYER
        }
    }
    // بعد از جواب کاربر به دسترسی فایل صوتی، فقط اگر grant شد اسکن کن
    private val permReq = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val granted = grants.values.any { it }
        if (granted) {
            vm.clearPermissionError()
            vm.reloadLocalSongs()
        } else {
            // deny — اسکن بی‌فایده نزن؛ پیام rationale در UI نشان داده می‌شود
        }
    }

    // انتخاب عکس بکگراند (OpenDocument تا دسترسی ماندگار بگیریم و بعد از ری‌استارت هم بماند)
    private val pickBackground =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                runCatching {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                vm.setBackground(uri.toString())
            }
        }

    companion object {
        /** @deprecated به‌جای آن از AppViewModel.navEvents استفاده کن؛ برای سازگاری نگه داشته شده */
        @Deprecated("Use AppViewModel.navEvents")
        var navTarget = mutableStateOf<String?>(null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // فقط اولین ساخت (نه چرخش) پرمیشن بخواه و intent را هندل کن
        if (savedInstanceState == null) {
            requestPermsIfNeeded()
            handleIntent(intent)
        }
        setContent {
            val mode by vm.themeMode.collectAsState()
            val dark = when (mode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }
            AlvandTheme(darkTheme = dark) {
                AppNavHost(
                    vm = vm,
                    onPickFile = { pickAudio.launch("audio/*") },
                    onPickBackground = { pickBackground.launch(arrayOf("image/*")) },
                )
            }
        }
        // پل سازگاری: ایونت‌های جدید را به navTarget قدیمی هم بده تا کد قدیمی نشکند
        lifecycleScope.launch {
            vm.navEvents.collect { navTarget.value = it }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** باز کردن لینک مستقیم shareشده از اپ‌های دیگر */
    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: (intent?.getStringExtra(Intent.EXTRA_TEXT)?.let {
            Regex("https?://\\S+").find(it)?.value
        }?.let { android.net.Uri.parse(it) })
        if (uri != null && intent?.action in listOf(Intent.ACTION_VIEW, Intent.ACTION_SEND)) {
            val url = uri.toString()
            if (Song.isSupportedPath(url)) {
                if (vm.playDirectLink(url)) {
                    vm.navigateTo(Routes.PLAYER)
                    @Suppress("DEPRECATION")
                    navTarget.value = Routes.PLAYER
                }
            }
        }
    }

    private fun requestPermsIfNeeded() {
        val perms = buildList {
            if (Build.VERSION.SDK_INT >= 33) {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    add(Manifest.permission.READ_MEDIA_AUDIO)
                }
            } else {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (perms.isNotEmpty()) permReq.launch(perms.toTypedArray())
        else vm.reloadLocalSongs()
    }
}
