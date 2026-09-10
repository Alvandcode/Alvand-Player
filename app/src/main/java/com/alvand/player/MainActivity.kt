package com.alvand.player

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.*
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
                    composable("welcome") { WelcomeScreen { nav.navigate("home") } }
                    composable("home") {
                        HomeScreen(vm,
                            onOpenPlaylist = { nav.navigate("detail") },
                            onOpenPlayer = { nav.navigate("player") },
                            onPickFile = { pickAudio.launch("audio/*") },
                            onOpenAbout = { nav.navigate("about") })
                        BottomBar(nav, "home")
                    }
                    composable("about") { AboutScreen(onBack = { nav.popBackStack() }) }
                    composable("detail") { DetailScreen(vm, onBack = { nav.popBackStack() }, onOpenPlayer = { nav.navigate("player") }) }
                    composable("player") { PlayerScreen(vm, onBack = { nav.popBackStack() }) }
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

@Composable
private fun BottomBar(nav: androidx.navigation.NavController, current: String) {
    var sel by remember { mutableIntStateOf(0) }
    val labels = listOf(
        stringResource(R.string.nav_home),
        stringResource(R.string.nav_explore),
        stringResource(R.string.nav_library),
        stringResource(R.string.nav_premium)
    )
    val icons = listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.LibraryMusic, Icons.Default.Diamond)
    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.BottomCenter) {
        NavigationBar(containerColor = Color.White.copy(0.12f)) {
            labels.forEachIndexed { i, t ->
                NavigationBarItem(selected = sel == i, onClick = {
                    sel = i
                    if (i == 0) nav.navigate("home") { launchSingleTop = true }
                    if (i == 2) nav.navigate("detail") { launchSingleTop = true }
                    if (i == 1) nav.navigate("player") { launchSingleTop = true }
                }, icon = { Icon(icons[i], null) }, label = { Text(t) })
            }
        }
    }
}
