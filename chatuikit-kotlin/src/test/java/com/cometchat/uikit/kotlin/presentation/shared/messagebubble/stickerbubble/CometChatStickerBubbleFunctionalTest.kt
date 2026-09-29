package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.stickerbubble

import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** No network under Robolectric, so these pin that each entry point binds without throwing. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatStickerBubbleFunctionalTest {

    private fun withBubble(block: (CometChatStickerBubble) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            block(CometChatStickerBubble(activity))
        }
        scenario.close()
    }

    @Test fun setMessage_binds() = withBubble { it.setMessage(MockFactory.createStickerMessage()) }

    @Test fun setMessage_withTheLegacyUrlKey_binds() =
        withBubble { it.setMessage(MockFactory.createStickerMessage(useLegacyUrlKey = true)) }

    @Test fun setMessage_withNoName_binds() =
        withBubble { it.setMessage(MockFactory.createStickerMessage(name = null)) }

    @Test fun setMessage_withNull_isHarmless() = withBubble { it.setMessage(null) }

    @Test fun setStickerUrl_binds() =
        withBubble { it.setStickerUrl("https://cdn.example.com/stickers/party.png") }

    @Test fun setStickerUrl_withAnEmptyString_isHarmless() = withBubble { it.setStickerUrl("") }

    @Test fun setImageUrl_binds() =
        withBubble { it.setImageUrl("https://cdn.example.com/stickers/party.png") }

    @Test fun bothAlignmentsBind() = withBubble { bubble ->
        bubble.setAlignment(UIKitConstants.MessageBubbleAlignment.LEFT)
        bubble.setMessage(MockFactory.createStickerMessage())
        bubble.setAlignment(UIKitConstants.MessageBubbleAlignment.RIGHT)
        bubble.setMessage(MockFactory.createStickerMessage())
    }

    @Test fun theClickListenerFiresOnTap() = withBubble { bubble ->
        var fired = false
        bubble.setOnStickerClickListener { fired = true }
        bubble.setMessage(MockFactory.createStickerMessage())
        bubble.performClick()
        assertTrue("a tap should reach the integrator", fired)
    }

    @Test fun rebindingReplacesTheSticker() = withBubble { bubble ->
        bubble.setMessage(MockFactory.createStickerMessage(name = "First"))
        bubble.setMessage(MockFactory.createStickerMessage(name = "Second"))
    }
}
