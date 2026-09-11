package com.alvand.player.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alvand.player.AppViewModel
import com.alvand.player.ui.screens.AboutScreen
import com.alvand.player.ui.screens.PlayerScreen
import com.alvand.player.ui.screens.WelcomeScreen

/**
 * گراف ناوبری اپ.
 *
 * - [vm] از اکتیویتی (Hilt) می‌آید تا همه مقصدها یک نمونه مشترک داشته باشند؛
 *   صفحه‌های آینده می‌توانند با `hiltViewModel()` ویومدل‌های فیچر خودشان را بگیرند
 *   (وابستگی `hilt-navigation-compose` برای همین آماده است).
 * - [pendingTarget] روت درخواستی از بیرون (مثلاً باز کردن لینک shareشده) است؛
 *   بعد از ناوبری مصرف (null) می‌شود.
 */
@Composable
fun AppNavHost(
    vm: AppViewModel,
    onPickFile: () -> Unit,
    pendingTarget: String?,
    onConsumeTarget: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val target = pendingTarget
    LaunchedEffect(target) {
        if (target != null) {
            runCatching {
                navController.navigate(target) { launchSingleTop = true }
            }
            onConsumeTarget()
        }
    }
    NavHost(navController, startDestination = Routes.WELCOME) {
        composable(Routes.WELCOME) {
            // بعد از ورود، خوشامد از بک‌استک حذف می‌شود تا با بک برنگردیم
            WelcomeScreen {
                navController.navigate(Routes.PLAYER) {
                    popUpTo(Routes.WELCOME) { inclusive = true }
                }
            }
        }
        composable(Routes.PLAYER) {
            PlayerScreen(
                vm,
                onPickFile = onPickFile,
                onOpenAbout = { navController.navigate(Routes.ABOUT) }
            )
        }
        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
