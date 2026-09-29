package com.cometchat.sampleapp.compose.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.cometchat.sampleapp.compose.app.SampleApplication
import com.cometchat.sampleapp.compose.navigation.AppNavGraph
import com.cometchat.sampleapp.compose.ui.theme.SampleAppTheme
import com.cometchat.uikit.compose.presentation.incomingcall.ui.CometChatIncomingCall

/**
 * Main activity for the CometChat Sample App (Jetpack Compose).
 *
 * This is the single activity that hosts all Compose screens.
 * It sets up the app theme and navigation graph.
 *
 * ## Architecture:
 * - Single Activity architecture with Compose Navigation
 * - All screens are composables managed by NavHost
 * - Theme is applied at the root level
 *
 * ## Features:
 * - Edge-to-edge display support
 * - Light and dark theme support
 * - CometChat UIKit theme integration
 * - Incoming-call overlay: observes [SampleApplication.incomingCall] and shows
 *   [CometChatIncomingCall] on top of the app when a call arrives.
 *
 * @see AppNavGraph for navigation setup
 * @see SampleAppTheme for theme configuration
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Ensure the window handles insets properly for keyboard
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val sampleApplication = application as? SampleApplication

        setContent {
            SampleAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppNavGraph()

                        // Incoming-call overlay, driven by the Application's call listener.
                        val incomingCall by (sampleApplication?.incomingCall?.collectAsState()
                            ?: remember { mutableStateOf(null) })
                        incomingCall?.let { call ->
                            CometChatIncomingCall(
                                call = call,
                                // Sound is managed by SampleApplication.
                                disableSoundForCalls = true,
                                // Let the component handle accept/reject via its ViewModel; the
                                // resulting CallAccepted/CallRejected events drive dismissal +
                                // the ongoing-call screen from SampleApplication.
                                onError = { exception ->
                                    Log.e(
                                        "MainActivity",
                                        "IncomingCall error: code=${exception.code}, message=${exception.message}",
                                        exception
                                    )
                                    sampleApplication?.dismissIncomingCall()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
