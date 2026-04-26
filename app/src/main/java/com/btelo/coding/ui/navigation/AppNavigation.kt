package com.btelo.coding.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.btelo.coding.ui.chat.ChatScreen
import com.btelo.coding.ui.notification.NotificationSettingsScreen
import com.btelo.coding.ui.scan.ScanScreen
import com.btelo.coding.ui.session.SessionListScreen

sealed class Screen(val route: String) {
    object Scan : Screen("scan")
    object SessionList : Screen("session_list")
    object Chat : Screen("chat/{sessionId}") {
        fun createRoute(sessionId: String) = "chat/$sessionId"
    }
    object NotificationSettings : Screen("notification_settings")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Scan.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Scan.route) {
            ScanScreen(
                onConnected = {
                    navController.navigate(Screen.SessionList.route) {
                        popUpTo(Screen.Scan.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SessionList.route) {
            SessionListScreen(
                onSessionClick = { sessionId ->
                    navController.navigate(Screen.Chat.createRoute(sessionId))
                },
                onLogout = {
                    navController.navigate(Screen.Scan.route) {
                        popUpTo(Screen.SessionList.route) { inclusive = true }
                    }
                },
                onNotificationSettings = {
                    navController.navigate(Screen.NotificationSettings.route)
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            ChatScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NotificationSettings.route) {
            NotificationSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
