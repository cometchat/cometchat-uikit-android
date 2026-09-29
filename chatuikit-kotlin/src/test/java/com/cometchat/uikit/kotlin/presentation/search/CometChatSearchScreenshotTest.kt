package com.cometchat.uikit.kotlin.presentation.search

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.search.ui.CometChatSearch
import com.cometchat.uikit.kotlin.presentation.utils.RoborazziConfig
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

/**
 * ENG-38678 — View CometChatSearch themed golden, Phase 4 (snapshot layer).
 *
 * The View-toolkit visual counterpart to the compose Search golden: captures the
 * real CometChatSearch View (search bar + filter chips + initial state) themed
 * light and dark. Goldens: screenshot-gallery/kotlin/search/.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatSearchScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/kotlin/search"
        )
    )

    private fun launchAndCapture(rtl: Boolean = false) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val view = CometChatSearch(activity)

            val container = FrameLayout(activity).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
            container.addView(
                view,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            activity.setContentView(container)
            // The root has to be attached before the direction is set; the ldrtl
            // qualifier does nothing here. See RtlLayoutDirectionHarnessTest.
            if (rtl) container.layoutDirection = View.LAYOUT_DIRECTION_RTL
            ShadowLooper.idleMainLooper()

            // Idle first, then lay out: the activity runs its own traversal off the
            // looper and sizes the container to the window. Idling after the manual
            // layout is a race, and the captured canvas flips size between runs.
            ShadowLooper.idleMainLooper()
            container.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2160, View.MeasureSpec.EXACTLY),
            )
            container.layout(0, 0, 1080, 2160)

            view.captureRoboImage(roborazziOptions = RoborazziConfig.options())
        }
        scenario.close()
    }

    @Test
    fun search_light() {
        launchAndCapture()
    }

    @Test
    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    fun search_dark() {
        launchAndCapture()
    }

    // ── right-to-left ───────────────────────────────────────────────────────

    @Test
    fun search_rtl() {
        launchAndCapture(rtl = true)
    }
}
