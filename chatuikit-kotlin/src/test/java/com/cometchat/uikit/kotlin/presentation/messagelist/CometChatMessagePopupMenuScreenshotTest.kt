package com.cometchat.uikit.kotlin.presentation.messagelist

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.utils.RoborazziConfig
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.util.TimeZone

/**
 * Snapshot layer for the **View** [CometChatMessagePopupMenu] — the first baselines
 * of any kind for it.
 *
 * What is captured is the popup's own content view, not the screen: the menu shows
 * through a `PopupWindow`, which never composes into the Activity's decor, so a
 * capture of the Activity would be a picture of the message list behind it. The
 * blurred backdrop the menu also installs on the WindowManager is likewise outside
 * this frame; these baselines are the card stack — preview, quick reactions, options.
 *
 * Run:
 *   ./gradlew :chatuikit-kotlin:recordRoborazziDebug  --tests "*.CometChatMessagePopupMenuScreenshotTest"
 *   ./gradlew :chatuikit-kotlin:compareRoborazziDebug --tests "*.CometChatMessagePopupMenuScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatMessagePopupMenuScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/kotlin/messagepopupmenu"
        )
    )

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

    private fun options() = listOf("Reply", "Copy", "Forward", "Edit", "Delete").map {
        MenuItem(id = it.lowercase(), name = it)
    }

    private fun View.visibleText(): String {
        val out = StringBuilder()
        fun walk(v: View) {
            if (v.visibility != View.VISIBLE) return
            if (v is android.widget.TextView) out.append(v.text).append(' ')
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(this)
        return out.toString()
    }

    private fun capture(
        rtl: Boolean = false,
        configure: (CometChatMessagePopupMenu) -> Unit,
    ) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val root = FrameLayout(activity)
            val anchor = View(activity).apply {
                layoutParams = ViewGroup.LayoutParams(200, 80)
            }
            root.addView(anchor)
            activity.setContentView(root)
            // The root has to be attached before the direction is set; the ldrtl
            // qualifier does nothing here. See RtlLayoutDirectionHarnessTest.
            if (rtl) root.layoutDirection = View.LAYOUT_DIRECTION_RTL
            root.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2160, View.MeasureSpec.EXACTLY),
            )
            root.layout(0, 0, 1080, 2160)
            ShadowLooper.idleMainLooper()

            val menu = CometChatMessagePopupMenu(activity)
            menu.setMenuItems(options())
            configure(menu)
            menu.show(anchor, root, MockFactory.createTextMessage(sentAt = MockFactory.FIXED_SENT_AT, text = "long-press me"))
            ShadowLooper.idleMainLooper()

            val popupView = Shadows.shadowOf(activity.application).latestPopupWindow.contentView

            // Two reasons this is re-hosted in the Activity rather than captured where
            // it sits. A PopupWindow's content is never measured by the host window
            // under Robolectric, and a detached, zero-sized view draws as an empty
            // bitmap; and while the popup is showing it *is* the active root, so the
            // capture cannot resolve any view in the Activity. Dismissing first hands
            // the root back, and the content view outlives the window that showed it.
            menu.dismiss()
            ShadowLooper.idleMainLooper()
            (popupView.parent as? ViewGroup)?.removeView(popupView)
            root.removeAllViews()
            root.addView(
                popupView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
            root.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2160, View.MeasureSpec.EXACTLY),
            )
            root.layout(0, 0, 1080, 2160)
            ShadowLooper.idleMainLooper()

            // A screenshot test that photographs an empty frame still passes, so make
            // that impossible: the capture must have a size and real content in it.
            assertTrue("the popup content should have been laid out", popupView.width > 0 && popupView.height > 0)
            assertTrue("the captured frame should carry the options", root.visibleText().contains("Reply"))

            root.captureRoboImage(roborazziOptions = RoborazziConfig.options())
        }
        scenario.close()
    }

    @Test fun standard() = capture { }

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test fun standard_dark() = capture { }

    @Test
    fun leftAligned() = capture {
        it.setMessageAlignment(UIKitConstants.MessageListAlignment.LEFT_ALIGNED)
    }

    @Test
    fun withoutQuickReactions() = capture {
        it.setQuickReactionsVisibility(View.GONE)
    }

    @Test
    fun withCustomQuickReactions() = capture {
        it.setQuickReactions(listOf("🎉", "🚀", "👀"))
    }

    @Test
    fun withASingleOption() = capture {
        it.setMenuItems(listOf(MenuItem(id = "reply", name = "Reply")))
    }

    // ── right-to-left ───────────────────────────────────────────────────────

    @Test fun standard_rtl() = capture(rtl = true) { }
}
