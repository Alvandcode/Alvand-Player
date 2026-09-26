package com.alvand.player

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
            requestNotificationPermission()
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "Local audio"
            vm.playUri(uri, name)
            vm.navigateTo(Routes.PLAYER)
            @Suppress("DEPRECATION")
            navTarget.value = Routes.PLAYER
        }
    }
    private val audioPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            vm.onAudioPermissionResult(granted)
        }

    private val notificationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

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
        if (savedInstanceState == null) {
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
                    onRequestAudioPermission = ::requestAudioPermission,
                    onRequestNotificationPermission = ::requestNotificationPermission,
                    onOpenAppSettings = ::openAppSettings,
                )
            }
        }
        // پل سازگاری: ایونت‌های جدید را به navTarget قدیمی هم بده تا کد قدیمی نشکند
        lifecycleScope.launch {
            vm.navEvents.collect { navTarget.value = it }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.syncAudioPermission()
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
                requestNotificationPermission()
                if (vm.playDirectLink(url)) {
                    vm.navigateTo(Routes.PLAYER)
                    @Suppress("DEPRECATION")
                    navTarget.value = Routes.PLAYER
                }
            }
        }
    }

    private fun requestAudioPermission() {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            vm.onAudioPermissionResult(true)
        } else {
            audioPermissionRequest.launch(permission)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return
        notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            )
        )
    }
}
