package com.cometchat.uikit.kotlin.presentation.callbuttons

import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * ENG-38680 — CometChatCallButtons (View) functional surface. Visibility toggles
 * reach the button containers; voice/video click callbacks fire on the real
 * button clicks. No live calls runtime is used (the callbacks are the UI-Kit's
 * own lambdas, invoked before any SDK call).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatCallButtonsFunctionalTest {

    private fun withButtons(block: (com.cometchat.uikit.kotlin.presentation.callbuttons.CometChatCallButtons) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            block(com.cometchat.uikit.kotlin.presentation.callbuttons.CometChatCallButtons(activity))
        }
        scenario.close()
    }

    @Test
    fun voiceCallButtonVisibility_togglesTheContainer() {
        withButtons { b ->
            val container = b.findViewById<View>(R.id.voice_call_container)
            assertNotNull("voice_call_button_container should exist", container)
            b.setVoiceCallButtonVisibility(View.GONE)
            assertEquals(View.GONE, container.visibility)
            b.setVoiceCallButtonVisibility(View.VISIBLE)
            assertEquals(View.VISIBLE, container.visibility)
        }
    }

    @Test
    fun videoCallButtonVisibility_togglesTheContainer() {
        withButtons { b ->
            val container = b.findViewById<View>(R.id.video_call_container)
            assertNotNull("video_call_button_container should exist", container)
            b.setVideoCallButtonVisibility(View.GONE)
            assertEquals(View.GONE, container.visibility)
            b.setVideoCallButtonVisibility(View.VISIBLE)
            assertEquals(View.VISIBLE, container.visibility)
        }
    }

    @Test
    fun onVoiceCallClick_firesOnVoiceButtonClick() {
        withButtons { b ->
            var fired = false
            b.setOnVoiceCallClick { _, _ -> fired = true }
            b.findViewById<View>(R.id.voice_call_container).performClick()
            assertTrue("onVoiceCallClick should fire on voice button click", fired)
        }
    }

    @Test
    fun onVideoCallClick_firesOnVideoButtonClick() {
        withButtons { b ->
            var fired = false
            b.setOnVideoCallClick { _, _ -> fired = true }
            b.findViewById<View>(R.id.video_call_container).performClick()
            assertTrue("onVideoCallClick should fire on video button click", fired)
        }
    }
}
