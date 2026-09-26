package com.alvand.player.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alvand.player.AppViewModel
import com.alvand.player.MainActivity
import com.alvand.player.ui.screens.AboutScreen
import com.alvand.player.ui.screens.PlayerScreen
import com.alvand.player.ui.screens.WelcomeScreen

/**
 * گراف ناوبری اپ.
 *
 * - [vm] با hiltViewModel گرفته می‌شود (تست‌پذیر، بدون کوپل به اکتیویتی).
 * - ایونت بیرونی (لینک shareشده) از AppViewModel.navEvents می‌آید.
 * - welcome فقط اگر onboarding دیده نشده باشد اول است.
 */
@Composable
fun AppNavHost(
    vm: AppViewModel = hiltViewModel(),
    onPickFile: () -> Unit,
    onPickBackground: () -> Unit,
    onRequestAudioPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val navEvents = vm.navEvents
    LaunchedEffect(navEvents, navController) {
        navEvents.collect { target ->
            runCatching {
                navController.navigate(target) {
                    launchSingleTop = true
                    // PLAYER و ABOUT روی هم تلنبار نشوند
                    if (target == Routes.PLAYER) popUpTo(Routes.PLAYER) { inclusive = false }
                }
            }
        }
    }
    // سازگاری با navTarget قدیمی استاتیک (اگر جایی هنوز ست می‌کند)
    @Suppress("DEPRECATION")
    val legacyTarget by MainActivity.navTarget
    LaunchedEffect(legacyTarget) {
        val t = legacyTarget
        if (t != null) {
            runCatching {
                navController.navigate(t) { launchSingleTop = true }
            }
            @Suppress("DEPRECATION")
            MainActivity.navTarget.value = null
        }
    }
    val onboardingSeen by vm.onboardingSeen.collectAsState()
    val start = if (onboardingSeen) Routes.PLAYER else Routes.WELCOME
    NavHost(navController, startDestination = start) {
        composable(Routes.WELCOME) {
            // بعد از ورود، خوشامد از بک‌استک حذف می‌شود تا با بک برنگردیم
            WelcomeScreen {
                vm.setOnboardingSeen()
                onRequestAudioPermission()
                navController.navigate(Routes.PLAYER) {
                    popUpTo(Routes.WELCOME) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        composable(Routes.PLAYER) {
            PlayerScreen(
                vm,
                onPickFile = onPickFile,
                onPickBackground = onPickBackground,
                onRequestAudioPermission = onRequestAudioPermission,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onOpenAppSettings = onOpenAppSettings,
                onOpenAbout = {
                    navController.navigate(Routes.ABOUT) { launchSingleTop = true }
                }
            )
        }
        composable(Routes.ABOUT) {
            AboutScreen(vm, onBack = { navController.popBackStack() })
        }
    }
}
