package com.cometchat.uikit.compose.presentation.callbuttons

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.callbuttons.ui.CometChatCallButtons
import com.cometchat.uikit.compose.theme.CometChatTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * ENG-38680 — Compose CometChatCallButtons functional matrix (semantics). Voice/
 * video buttons carry "Voice Call"/"Video Call" content descriptions + Button
 * roles; visibility Ints gate them; click callbacks fire before any SDK call.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatCallButtonsComposeFunctionalTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun buttonText_showsWhenTextVisible() {
        rule.setContent {
            CometChatTheme {
                CometChatCallButtons(voiceButtonText = "PM-VOICE", videoButtonText = "PM-VIDEO", buttonTextVisibility = View.VISIBLE)
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("PM-VOICE").assertIsDisplayed()
        rule.onNodeWithText("PM-VIDEO").assertIsDisplayed()
    }

    @Test
    fun voiceCallButtonVisibility_gone_removesVoiceButton() {
        rule.setContent { CometChatTheme { CometChatCallButtons(voiceCallButtonVisibility = View.GONE) } }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Voice Call").assertDoesNotExist()
        rule.onNodeWithContentDescription("Video Call").assertExists()
    }

    @Test
    fun videoCallButtonVisibility_gone_removesVideoButton() {
        rule.setContent { CometChatTheme { CometChatCallButtons(videoCallButtonVisibility = View.GONE) } }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Video Call").assertDoesNotExist()
        rule.onNodeWithContentDescription("Voice Call").assertExists()
    }

    @Test
    fun onVoiceCallClick_firesOnVoiceButtonClick() {
        var fired = false
        rule.setContent { CometChatTheme { CometChatCallButtons(onVoiceCallClick = { _, _ -> fired = true }) } }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Voice Call").performClick()
        assertTrue("onVoiceCallClick should fire", fired)
    }

    @Test
    fun onVideoCallClick_firesOnVideoButtonClick() {
        var fired = false
        rule.setContent { CometChatTheme { CometChatCallButtons(onVideoCallClick = { _, _ -> fired = true }) } }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Video Call").performClick()
        assertTrue("onVideoCallClick should fire", fired)
    }
}
