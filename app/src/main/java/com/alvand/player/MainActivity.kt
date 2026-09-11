package com.alvand.player

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import com.alvand.player.data.Song
import com.alvand.player.data.ThemeMode
import com.alvand.player.ui.navigation.AppNavHost
import com.alvand.player.ui.navigation.Routes
import com.alvand.player.ui.theme.AlvandTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val vm: AppViewModel by viewModels()

    private val pickAudio = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "Local audio"
            vm.playUri(uri, name)
            navTarget.value = Routes.PLAYER
        }
    }
    // بعد از جواب کاربر به دسترسی فایل صوتی، کتابخانه دوباره اسکن می‌شود
    // (اسکن اولِ startup معمولاً قبل از grant اجرا شده و خالی برگشته است)
    private val permReq = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.reloadLocalSongs()
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
        var navTarget = mutableStateOf<String?>(null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPerms()
        handleIntent(intent)
        setContent {
            val mode by vm.themeMode.collectAsState()
            val dark = when (mode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }
            AlvandTheme(darkTheme = dark) {
                val target by navTarget
                AppNavHost(
                    vm = vm,
                    onPickFile = { pickAudio.launch("audio/*") },
                    onPickBackground = { pickBackground.launch(arrayOf("image/*")) },
                    pendingTarget = target,
                    onConsumeTarget = { navTarget.value = null }
                )
            }
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
                vm.playDirectLink(url)
                navTarget.value = Routes.PLAYER
            }
        }
    }

    private fun requestPerms() {
        val perms = buildList {
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.READ_MEDIA_AUDIO)
            else add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permReq.launch(perms.toTypedArray())
    }
}
