package com.cometchat.uikit.kotlin.presentation.threadheader

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.threadheader.ui.CometChatThreadHeader
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
import java.util.TimeZone

/**
 * Snapshot layer for the **View** [CometChatThreadHeader].
 *
 * The suite this replaces hand-assembled a mock layout out of TextViews and never
 * constructed the component, so its baselines were pictures of a stand-in. These
 * capture the real view.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatThreadHeaderScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/kotlin/threadheader"
        )
    )

    private companion object {
        const val CANVAS_LIGHT = 0xFFEEEEEE.toInt()
        const val CANVAS_DARK = 0xFF121212.toInt()
        const val PARENT_TEXT = "the parent message everyone replied to"
    }

    private lateinit var cometChat: MockedStatic<CometChat>
    private lateinit var originalZone: TimeZone

    @Before
    fun setUp() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
        val me = MockFactory.createUser(uid = "me", name = "Me")
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)
    }

    @After
    fun tearDown() {
        cometChat.close()
        TimeZone.setDefault(originalZone)
    }

    private fun capture(
        rtl: Boolean = false,
        configure: (ComponentActivity, CometChatThreadHeader) -> Unit,
    ) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val header = CometChatThreadHeader(activity)

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
                    header,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ),
                )
            }
            activity.setContentView(container)
            // The root has to be attached before the direction is set; the ldrtl
            // qualifier does nothing here. See RtlLayoutDirectionHarnessTest.
            if (rtl) container.layoutDirection = View.LAYOUT_DIRECTION_RTL
            ShadowLooper.idleMainLooper()

            configure(activity, header)
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

    private fun parent(text: String = PARENT_TEXT) = MockFactory.createTextMessage(sentAt = MockFactory.FIXED_SENT_AT, text = text)

    @Test fun standard() = capture { _, h -> h.setParentMessage(parent()) }

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test fun standard_dark() = capture { _, h -> h.setParentMessage(parent()) }

    @Test fun leftAligned() = capture { _, h ->
        h.setAlignment(UIKitConstants.MessageListAlignment.LEFT_ALIGNED)
        h.setParentMessage(parent())
    }

    @Test fun shortParent() = capture { _, h -> h.setParentMessage(parent("ok")) }

    @Test fun withoutTheReplyCountBar() = capture { _, h ->
        h.setParentMessage(parent())
        h.setReplyCountBarVisibility(View.GONE)
    }

    // withoutTheAvatar and withoutReactions are deliberately absent: with a plain
    // parent message there are no reactions to hide and no avatar drawn, so both
    // captured byte-identically to `standard`. Their effect is asserted in the
    // functional test instead.

    // ── right-to-left ───────────────────────────────────────────────────────

    @Test fun standard_rtl() = capture(rtl = true) { _, h -> h.setParentMessage(parent()) }
}
