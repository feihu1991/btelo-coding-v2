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
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dataStoreManager: com.btelo.coding.data.local.DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Auto-login: check intent extras first, then SharedPreferences
        val intentServer = intent?.getStringExtra("server")
        val intentToken = intent?.getStringExtra("ws_token")
        val intentSession = intent?.getStringExtra("session_id")

        if (intentServer != null && intentToken != null && intentSession != null) {
            // Save intent extras to SharedPreferences for persistence
            kotlinx.coroutines.runBlocking {
                dataStoreManager.saveServerAddress(intentServer)
                dataStoreManager.saveWsToken(intentToken)
                dataStoreManager.saveSessionId(intentSession)
            }
        }

        val startDest = if (!intentSession.isNullOrBlank()) {
            Screen.Chat.createRoute(intentSession)
        } else {
            Screen.Scan.route
        }

        setContent {
            BteloCodingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(startDestination = startDest)
                }
            }
        }
    }
}
