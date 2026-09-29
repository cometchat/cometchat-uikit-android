package com.cometchat.uikit.kotlin.presentation.messageinformation

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.messageinformation.ui.CometChatMessageInformation
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/** Instrumented layer for the **View** [CometChatMessageInformation]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatMessageInformationFunctionalTest {

    private companion object {
        const val DEFAULT_TITLE = "Message Info"
        const val CUSTOM_TITLE = "Delivery detail"
    }

    private lateinit var cometChat: MockedStatic<CometChat>

    @Before
    fun stubLoggedInUser() {
        val me = MockFactory.createUser(uid = "me", name = "Me")
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)
    }

    @After fun closeStatic() = cometChat.close()

    private fun withComponent(block: (CometChatMessageInformation) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val c = CometChatMessageInformation(activity)
            activity.setContentView(c)
            ShadowLooper.idleMainLooper()
            block(c)
            ShadowLooper.idleMainLooper()
        }
        scenario.close()
    }

    private fun View.visibleText(): String {
        ShadowLooper.idleMainLooper()
        val out = StringBuilder()
        fun walk(v: View) {
            if (v.visibility != View.VISIBLE) return
            if (v is TextView) out.append(v.text).append(' ')
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(this)
        return out.toString()
    }

    private fun message(): BaseMessage = MockFactory.createTextMessage(text = "hello")

    @Test
    fun itShowsItsToolbarTitle() = withComponent { c ->
        assertTrue(
            "expected the default title, got '${c.visibleText()}'",
            c.visibleText().contains(DEFAULT_TITLE),
        )
    }

    @Test
    fun setMessage_isHeldAndHandedBack() = withComponent { c ->
        val m = message()
        c.setMessage(m)
        assertEquals(m, c.getMessage())
    }

    @Test
    fun aCustomTitleReplacesTheDefault() = withComponent { c ->
        c.setToolBarTitleText(CUSTOM_TITLE)
        assertTrue(c.visibleText().contains(CUSTOM_TITLE))
        assertFalse(c.visibleText().contains(DEFAULT_TITLE))
    }

    @Test
    fun theToolbarCanBeHidden() = withComponent { c ->
        c.hideToolBar(true)
        assertFalse("the title should be gone", c.visibleText().contains(DEFAULT_TITLE))
    }

    @Test
    fun textFormattersRoundTrip() = withComponent { c ->
        c.setTextFormatters(emptyList())
        assertEquals(emptyList<Any>(), c.getTextFormatters())
    }

    @Test
    fun bubbleFactoriesRoundTrip() = withComponent { c ->
        c.setBubbleFactories(emptyMap())
        assertEquals(emptyMap<String, Any>(), c.getBubbleFactories())
    }

    @Test
    fun theErrorCallbackIsAccepted() = withComponent { c ->
        c.setOnError { }
        c.setMessage(message())
    }
}
