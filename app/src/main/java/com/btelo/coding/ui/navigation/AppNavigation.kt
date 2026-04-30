package com.btelo.coding.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.btelo.coding.ui.chat.ChatScreen
import com.btelo.coding.ui.scan.ScanScreen

sealed class Screen(val route: String) {
    object Scan : Screen("scan")
    object Chat : Screen("chat/{sessionId}") {
        fun createRoute(sessionId: String) = "chat/$sessionId"
    }
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
        // Scan/Connect page
        composable(Screen.Scan.route) {
            ScanScreen(
                onConnected = { sessionId ->
                    navController.navigate(Screen.Chat.createRoute(sessionId)) {
                        popUpTo(Screen.Scan.route) { inclusive = true }
                    }
                }
            )
        }

        // Chat page
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            ChatScreen(
                sessionId = sessionId,
                onDisconnect = {
                    navController.navigate(Screen.Scan.route) {
                        popUpTo(Screen.Chat.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
