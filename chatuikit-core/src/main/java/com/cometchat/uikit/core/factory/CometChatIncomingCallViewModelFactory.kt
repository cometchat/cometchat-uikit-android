package com.cometchat.uikit.core.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cometchat.uikit.core.viewmodel.CometChatIncomingCallViewModel

/**
 * Factory for creating [CometChatIncomingCallViewModel] instances.
 * 
 * This factory is used by both chatuikit-jetpack (Compose) and chatuikit-kotlin (Views)
 * to create ViewModel instances with the correct configuration.
 * 
 * @param enableListeners Whether to enable CometChat call listeners. Set to false for testing
 *                        to avoid side effects from real-time call events.
 * 
 * Validates: Requirements 13.1, 13.2, 13.3
 */
@Deprecated("Ringing v2 runs the Push SDK in AUTO mode: push-notifications-calls-android rings and draws the incoming call itself. The app's answer activity opens CometChatOngoingCallActivity with the answered call; drop this component unless the app draws its own ring screen with setIncomingCallActivity and wires accept and decline to CometChatCallNotifications itself.")
public class CometChatIncomingCallViewModelFactory(
    private val enableListeners: Boolean = true
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CometChatIncomingCallViewModel::class.java)) {
            return CometChatIncomingCallViewModel(
                enableListeners = enableListeners
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
