package com.cometchat.uikit.kotlin.presentation.reactionlist

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.ReactionsRequest
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Reaction
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.usecase.FetchReactionsUseCase
import com.cometchat.uikit.core.domain.usecase.RemoveReactionUseCase
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatReactionListViewModel
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.reactionlist.ui.CometChatReactionList
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
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

/**
 * Snapshot layer for the **View** [CometChatReactionList] — it had no baselines of any
 * kind, and was the one component in the messaging surface with a Compose baseline and
 * no View twin.
 *
 * The list is given an external ViewModel whose fetch returns rows directly, the same
 * way [com.cometchat.uikit.compose.screenshots.CometChatReactionListScreenshotTest]
 * does on the other side. Without it the SDK call throws offline and the view never
 * leaves its loading state — which is exactly what
 * [CometChatReactionListFunctionalTest] pins, and is not what a baseline should show.
 *
 * Run:
 *   ./gradlew :chatuikit-kotlin:recordRoborazziDebug  --tests "*.CometChatReactionListScreenshotTest"
 *   ./gradlew :chatuikit-kotlin:compareRoborazziDebug --tests "*.CometChatReactionListScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatReactionListScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/kotlin/reactionlist"
        )
    )

    private companion object {
        const val CANVAS_LIGHT = 0xFFEEEEEE.toInt()
        const val CANVAS_DARK = 0xFF121212.toInt()
        const val THUMBS_UP = "👍"
        const val HEART = "❤️"
    }

    private lateinit var cometChat: MockedStatic<CometChat>

    @Before
    fun stubLoggedInUser() {
        val me = MockFactory.createUser(uid = "me", name = "Me")
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)
    }

    @After fun closeStatic() = cometChat.close()

    /** Serves one page of rows, then reports the end of the list. */
    private class OnePageFetch(private val rows: List<Reaction>) : FetchReactionsUseCase(mock()) {
        private var served = false
        override suspend fun invoke(request: ReactionsRequest): Result<List<Reaction>> {
            if (served) return Result.success(emptyList())
            served = true
            return Result.success(rows)
        }
    }

    private class InertRemove : RemoveReactionUseCase(mock()) {
        override suspend fun invoke(messageId: Long, emoji: String): Result<BaseMessage> =
            Result.failure(IllegalStateException("not exercised"))
    }

    private fun reactors(vararg pairs: Pair<String, String>): List<Reaction> =
        pairs.mapIndexed { i, (name, emoji) ->
            MockFactory.createReaction(uid = "u$i", name = name, emoji = emoji, reactedAt = i.toLong())
        }

    private fun capture(rows: List<Reaction>, rtl: Boolean = false) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val list = CometChatReactionList(activity)

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
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ),
                )
            }
            activity.setContentView(container)
            // The root has to be attached before the direction is set; the ldrtl
            // qualifier does nothing here. See RtlLayoutDirectionHarnessTest.
            if (rtl) container.layoutDirection = View.LAYOUT_DIRECTION_RTL
            ShadowLooper.idleMainLooper()

            // The external ViewModel goes in before the message: binding is what starts
            // the fetch, and it must be the fake one that can actually return rows.
            list.setViewModel(
                CometChatReactionListViewModel(OnePageFetch(rows), InertRemove(), enableListeners = false)
            )
            list.setBaseMessage(MockFactory.createTextMessage(sentAt = MockFactory.FIXED_SENT_AT, text = "hello"))
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

    @Test
    fun singleReactor() = capture(reactors("Alice" to THUMBS_UP))

    @Test
    fun severalReactors() =
        capture(reactors("Alice" to THUMBS_UP, "Bob" to THUMBS_UP, "Carol" to HEART))

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test
    fun severalReactors_dark() =
        capture(reactors("Alice" to THUMBS_UP, "Bob" to THUMBS_UP, "Carol" to HEART))

    @Test
    fun mixedEmoji() = capture(reactors("Alice" to THUMBS_UP, "Bob" to HEART))

    // ── right-to-left ───────────────────────────────────────────────────────

    @Test fun severalReactors_rtl() = capture(reactors("Alice" to THUMBS_UP, "Bob" to THUMBS_UP, "Carol" to HEART), rtl = true)
}
