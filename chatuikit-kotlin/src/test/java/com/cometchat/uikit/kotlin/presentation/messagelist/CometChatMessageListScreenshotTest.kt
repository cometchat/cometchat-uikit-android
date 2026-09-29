package com.cometchat.uikit.kotlin.presentation.messagelist

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.messagelist.adapter.MessageAdapter
import com.cometchat.uikit.kotlin.presentation.messagelist.ui.CometChatMessageList
import com.cometchat.uikit.kotlin.presentation.utils.RoborazziConfig
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

/**
 * Snapshot layer for the **View** [CometChatMessageList].
 *
 * Replaces a suite that never constructed the component: it hand-assembled a
 * LinearLayout of date separators and stand-in rows, so its baselines could not catch a
 * regression in the list.
 *
 * Rows come from the component's own adapter rather than the SDK: the fetch is what
 * needs a backend, the rendering is not.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatMessageListScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/kotlin/messagelist"
        )
    )

    private companion object {
        const val CANVAS_LIGHT = 0xFFEEEEEE.toInt()
        const val CANVAS_DARK = 0xFF121212.toInt()
    }

    private lateinit var cometChat: MockedStatic<CometChat>

    @Before
    fun stubLoggedInUser() {
        val me = MockFactory.createUser(uid = "me", name = "Me")
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)
    }

    @After fun closeStatic() = cometChat.close()

    private fun conversation(): List<BaseMessage> = listOf(
        MockFactory.createTextMessage(sentAt = MockFactory.FIXED_SENT_AT, id = 1L, text = "Morning! Are we still on for the review?", senderUid = "u1", receiverId = "me"),
        MockFactory.createTextMessage(sentAt = MockFactory.FIXED_SENT_AT, id = 2L, text = "Yes, 3pm works for me.", senderUid = "me", receiverId = "u1"),
        MockFactory.createTextMessage(sentAt = MockFactory.FIXED_SENT_AT, id = 3L, text = "I pushed the latest deck to the shared drive.", senderUid = "me", receiverId = "u1"),
        MockFactory.createTextMessage(sentAt = MockFactory.FIXED_SENT_AT, id = 4L, text = "Got it, taking a look now.", senderUid = "u1", receiverId = "me"),
    )

    private fun capture(
        rtl: Boolean = false,
        configure: (CometChatMessageList) -> Unit,
    ) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val list = CometChatMessageList(activity)

            val isNight = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            val container = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(if (isNight) CANVAS_DARK else CANVAS_LIGHT)
                addView(
                    list,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
            activity.setContentView(container)
            // The root has to be attached before the direction is set; the ldrtl
            // qualifier does nothing here. See RtlLayoutDirectionHarnessTest.
            if (rtl) container.layoutDirection = View.LAYOUT_DIRECTION_RTL
            ShadowLooper.idleMainLooper()

            configure(list)
            ShadowLooper.idleMainLooper()

            // Drive the component's own adapter: the fetch needs a backend, the
            // rendering does not.
            list.findViewById<View>(R.id.loading_state_view).visibility = View.GONE
            list.findViewById<View>(R.id.message_list_layout).visibility = View.VISIBLE
            val recycler = list.findViewById<RecyclerView>(R.id.recyclerview_message_list)
            (recycler.adapter as MessageAdapter).setMessages(conversation())
            ShadowLooper.idleMainLooper()

            // Idle first, then lay out: the activity runs its own traversal off the
            // looper and sizes the container to the window (1200x2400 at these
            // qualifiers). Idling after the manual layout is a race, and the captured
            // canvas flips size between runs. Our layout has to be the last thing
            // before the capture.
            ShadowLooper.idleMainLooper()
            val widthSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(2160, View.MeasureSpec.EXACTLY)
            container.measure(widthSpec, heightSpec)
            container.layout(0, 0, 1080, 2160)

            container.captureRoboImage(roborazziOptions = RoborazziConfig.options())
        }
        scenario.close()
    }

    @Test fun userConversation() = capture { it.setUser(MockFactory.createUser(uid = "u1", name = "Alice")) }

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test fun userConversation_dark() =
        capture { it.setUser(MockFactory.createUser(uid = "u1", name = "Alice")) }

    // ── right-to-left ───────────────────────────────────────────────────────

    @Test fun userConversation_rtl() = capture(rtl = true) { it.setUser(MockFactory.createUser(uid = "u1", name = "Alice")) }
}
