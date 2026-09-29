package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.cardbubble

import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.propmatrix.NonDefaultValues
import com.cometchat.uikit.propmatrix.ViewPropSweep
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The View card bubble has no mechanical prop surface: its only public method is
 * `setMessage(message, alignment)`. Every other View bubble exposes colour, icon and
 * appearance setters; this one exposes none, so it cannot be restyled after
 * construction.
 *
 * That asymmetry is asserted rather than worked around. If setters are added later
 * this test fails, which is the prompt to give the bubble a real matrix.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatCardBubblePropMatrixTest {

    @After fun tearDown() = NonDefaultValues.clearRegistered()

    @Test
    fun theViewCardBubbleExposesNoSweepableSetters() {
        val setters = ViewPropSweep.setters(CometChatCardBubble::class.java)
        assertEquals(
            "the card bubble gained setters; give it a real prop matrix. Found: ${setters.map { it.propName }}",
            0,
            setters.size,
        )
    }

    @Test
    fun itsOneEntryPointBinds() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            CometChatCardBubble(activity).setMessage(
                MockFactory.createCardMessage(),
                UIKitConstants.MessageBubbleAlignment.LEFT,
            )
        }
        scenario.close()
    }
}
