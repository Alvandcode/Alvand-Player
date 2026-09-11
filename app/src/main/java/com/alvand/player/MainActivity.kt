package com.alvand.player

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alvand.player.data.Song
import com.alvand.player.ui.screens.*
import com.alvand.player.ui.theme.AlvandTheme

class MainActivity : AppCompatActivity() {

    private val vm: AppViewModel by viewModels()

    private val pickAudio = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "Local audio"
            vm.playUri(uri, name)
            navTarget.value = "player"
        }
    }
    private val permReq = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    companion object {
        var navTarget = mutableStateOf<String?>(null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPerms()
        handleIntent(intent)
        setContent {
            AlvandTheme {
                val nav = rememberNavController()
                val target by navTarget
                LaunchedEffect(target) {
                    target?.let { nav.navigate(it) { launchSingleTop = true }; navTarget.value = null }
                }
                NavHost(nav, startDestination = "welcome") {
                    composable("welcome") {
                        // بعد از ورود، خوشامد از بک‌استک حذف می‌شود تا با بک برنگردیم
                        WelcomeScreen {
                            nav.navigate("player") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        }
                    }
                    composable("player") {
                        PlayerScreen(
                            vm,
                            onPickFile = { pickAudio.launch("audio/*") },
                            onOpenAbout = { nav.navigate("about") }
                        )
                    }
                    composable("about") {
                        AboutScreen(onBack = { nav.popBackStack() })
                    }
                }
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
                navTarget.value = "player"
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
