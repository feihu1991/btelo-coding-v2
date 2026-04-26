package com.btelo.coding

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.btelo.coding.ui.navigation.AppNavigation
import com.btelo.coding.ui.navigation.Screen
import com.btelo.coding.ui.theme.BteloCodingTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if we have saved connection info for auto-connect
        val prefs = getSharedPreferences("btelo_settings", MODE_PRIVATE)
        val hasWsToken = prefs.getString("ws_token", null) != null
        val startDestination = if (hasWsToken) Screen.Agents.route else Screen.Scan.route

        setContent {
            BteloCodingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(startDestination = startDestination)
                }
            }
        }
    }
}
