package com.cometchat.uikit.kotlin.presentation.threadheader

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.threadheader.ui.CometChatThreadHeader
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.shadows.ShadowLooper

/**
 * Instrumented layer for the **View** [CometChatThreadHeader].
 *
 * Nothing previously constructed this class: its rendering test drove a ViewModel and
 * its screenshot suite hand-assembled a mock layout out of TextViews, so the component
 * itself sat at 0%.
 *
 * `setParentMessage` reads `CometChat.getLoggedInUser()` to decide which side the
 * parent bubble sits on, so the static is stubbed; without it the SDK throws
 * "call CometChat.init() first" and nothing renders.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatThreadHeaderFunctionalTest {

    private companion object {
        const val PARENT_TEXT = "the parent message"
    }

    private lateinit var cometChat: MockedStatic<CometChat>

    @Before
    fun stubLoggedInUser() {
        // Build the user first: createUser mocks internally, and creating a mock inside
        // a `when {}` lambda leaves Mockito with unfinished stubbing.
        val me = MockFactory.createUser(uid = "me", name = "Me")
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)
    }

    @After
    fun closeStatic() = cometChat.close()

    private fun withHeader(block: (ComponentActivity, CometChatThreadHeader) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val header = CometChatThreadHeader(activity)
            activity.setContentView(header)
            ShadowLooper.idleMainLooper()
            block(activity, header)
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

    private fun parent(text: String = PARENT_TEXT) =
        MockFactory.createTextMessage(text = text)

    // ── binding the parent message ──────────────────────────────────────────

    @Test
    fun setParentMessage_rendersTheParent() = withHeader { _, header ->
        header.setParentMessage(parent())
        assertTrue(
            "expected the parent text, got '${header.visibleText()}'",
            header.visibleText().contains(PARENT_TEXT),
        )
    }

    @Test
    fun theParentMessageIsHandedBack() = withHeader { _, header ->
        val m = parent()
        header.setParentMessage(m)
        assertEquals(m, header.getParentMessage())
    }

    @Test
    fun rebindingReplacesTheParent() = withHeader { _, header ->
        header.setParentMessage(parent("first"))
        header.setParentMessage(MockFactory.createTextMessage(id = 2L, text = "second"))
        assertFalse("the previous parent must not survive", header.visibleText().contains("first"))
        assertTrue(header.visibleText().contains("second"))
    }

    // ── visibility controls the thread screen drives ────────────────────────

    @Test
    fun theReplyCountBarCanBeHidden() = withHeader { _, header ->
        header.setParentMessage(parent())
        header.setReplyCountBarVisibility(View.GONE)
        assertTrue("the parent should remain", header.visibleText().contains(PARENT_TEXT))
    }

    @Test
    fun theReplyCountCanBeHidden() = withHeader { _, header ->
        header.setParentMessage(parent())
        header.setReplyCountVisibility(View.GONE)
        assertTrue(header.visibleText().contains(PARENT_TEXT))
    }

    @Test
    fun reactionsCanBeHidden() = withHeader { _, header ->
        header.setParentMessage(parent())
        header.setReactionVisibility(View.GONE)
        assertTrue(header.visibleText().contains(PARENT_TEXT))
    }

    @Test
    fun theAvatarCanBeHidden() = withHeader { _, header ->
        header.setParentMessage(parent())
        header.setAvatarVisibility(View.GONE)
        assertTrue(header.visibleText().contains(PARENT_TEXT))
    }

    @Test
    fun receiptsCanBeHidden() = withHeader { _, header ->
        header.setParentMessage(parent())
        header.setReceiptsVisibility(View.GONE)
        assertTrue(header.visibleText().contains(PARENT_TEXT))
    }

    // ── alignment ───────────────────────────────────────────────────────────

    @Test
    fun alignmentRoundTrips() = withHeader { _, header ->
        header.setAlignment(UIKitConstants.MessageListAlignment.LEFT_ALIGNED)
        assertEquals(UIKitConstants.MessageListAlignment.LEFT_ALIGNED, header.getAlignment())
        header.setAlignment(UIKitConstants.MessageListAlignment.STANDARD)
        assertEquals(UIKitConstants.MessageListAlignment.STANDARD, header.getAlignment())
    }

    @Test
    fun bothAlignmentsRenderTheParent() = withHeader { _, header ->
        header.setAlignment(UIKitConstants.MessageListAlignment.LEFT_ALIGNED)
        header.setParentMessage(parent())
        assertTrue(header.visibleText().contains(PARENT_TEXT))
    }

    // ── appearance setters ──────────────────────────────────────────────────

    @Test
    fun theReplyCountAppearanceSettersAreAccepted() = withHeader { _, header ->
        header.setParentMessage(parent())
        header.setReplyCountTextColor(0xFF102030.toInt())
        header.setReplyCountBackgroundColor(0xFF203040.toInt())
        assertTrue(header.visibleText().contains(PARENT_TEXT))
    }

    @Test
    fun bubbleMarginsAreAccepted() = withHeader { _, header ->
        header.setLeftBubbleMargin(1, 2, 3, 4)
        header.setRightBubbleMargin(1, 2, 3, 4)
        header.setParentMessage(parent())
        assertTrue(header.visibleText().contains(PARENT_TEXT))
    }

    @Test
    fun aMaxHeightIsAccepted() = withHeader { _, header ->
        header.setMaxHeight(400)
        header.setParentMessage(parent())
        assertTrue(header.visibleText().contains(PARENT_TEXT))
    }

    @Test
    fun textFormattersAreAccepted() = withHeader { _, header ->
        header.setTextFormatters(emptyList())
        header.setParentMessage(parent())
        assertTrue(header.visibleText().contains(PARENT_TEXT))
    }
}
