package com.cometchat.uikit.kotlin.presentation.stickerkeyboard

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.stickerkeyboard.ui.CometChatStickerKeyboard
import com.cometchat.uikit.kotlin.presentation.utils.RoborazziConfig
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

/**
 * Snapshot layer for the **View** [CometChatStickerKeyboard].
 *
 * The suite this replaces hand-assembled the whole keyboard — shimmer grid, tab bar,
 * empty and error layouts — out of raw LinearLayouts, and never constructed
 * [CometChatStickerKeyboard] at all. Its ten baselines were pictures of a stand-in and
 * could not have caught a regression in the component. It said so in its own header,
 * and gave the reason: the keyboard builds its own ViewModel from a hardcoded factory,
 * so there is no seam to inject sticker data through.
 *
 * There is a seam, just one layer down. The factory's repository ends at
 * `StickerDataSourceImpl`, which reaches the SDK through two *static* calls —
 * `CometChat.isExtensionEnabled` and `CometChat.callExtension`. Stubbing those and
 * driving the extension's own callback puts the real component into each of its real
 * states: resolve with stickers for content, resolve with none for empty, fail for
 * error, and never resolve for loading.
 *
 * The content state is the one thing not photographed here. Every tile is a remote
 * sticker loaded through Glide, which never resolves under Robolectric, so a capture of
 * it is a grid of blank tiles — exactly the kind of baseline this file exists to remove.
 * It is asserted structurally instead, in a test that writes no image.
 *
 * Run:
 *   ./gradlew :chatuikit-kotlin:recordRoborazziDebug  --tests "*.CometChatStickerKeyboardScreenshotTest"
 *   ./gradlew :chatuikit-kotlin:compareRoborazziDebug --tests "*.CometChatStickerKeyboardScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatStickerKeyboardScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/kotlin/stickerkeyboard"
        )
    )

    private companion object {
        const val CANVAS_LIGHT = 0xFFEEEEEE.toInt()
        const val CANVAS_DARK = 0xFF121212.toInt()
    }

    private lateinit var cometChat: MockedStatic<CometChat>

    @Before
    fun stubTheExtension() {
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<Boolean> { CometChat.isExtensionEnabled(anyString()) }.thenReturn(true)
    }

    @After fun closeStatic() = cometChat.close()

    /** The sticker payload shape `StickerDataSourceImpl.parseStickerSets` reads. */
    private fun payload(vararg sets: Pair<String, Int>): JSONObject {
        val stickers = JSONArray()
        sets.forEach { (setName, count) ->
            (1..count).forEach { i ->
                stickers.put(
                    JSONObject()
                        .put("stickerName", "$setName $i")
                        .put("stickerUrl", "https://example.invalid/stickers/$setName/$i.png")
                        .put("stickerSetName", setName)
                )
            }
        }
        return JSONObject().put("data", JSONObject().put("defaultStickers", stickers))
    }

    /**
     * Decides what the extension call does. `null` for both leaves the callback
     * un-resolved, which is how the loading state is reached.
     */
    private fun answerWith(success: JSONObject? = null, failure: CometChatException? = null) {
        cometChat.`when`<Unit> {
            CometChat.callExtension(anyString(), anyString(), anyString(), any(), any())
        }.thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val listener = invocation.arguments[4] as CometChat.CallbackListener<JSONObject>
            when {
                success != null -> listener.onSuccess(success)
                failure != null -> listener.onError(failure)
                else -> Unit // still in flight — the shimmer stays up
            }
            Unit
        }
    }

    /** Which state layout the keyboard is currently showing, by visible id. */
    private fun CometChatStickerKeyboard.visibleState(): String = when {
        findViewById<View>(R.id.stickers_view)?.visibility == View.VISIBLE -> "content"
        findViewById<View>(R.id.empty_sticker_layout)?.visibility == View.VISIBLE -> "empty"
        findViewById<View>(R.id.error_sticker_layout)?.visibility == View.VISIBLE -> "error"
        findViewById<View>(R.id.shimmer_effect_frame)?.visibility == View.VISIBLE -> "loading"
        else -> "none"
    }

    private fun capture(
        expect: String,
        rtl: Boolean = false,
        configure: (CometChatStickerKeyboard) -> Unit = {},
    ) = withKeyboard(expect, configure, capture = true, rtl = rtl)

    private fun withKeyboard(
        expect: String,
        configure: (CometChatStickerKeyboard) -> Unit = {},
        capture: Boolean = false,
        rtl: Boolean = false,
        assert: (CometChatStickerKeyboard) -> Unit = {},
    ) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val keyboard = CometChatStickerKeyboard(activity)

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
                    keyboard,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
            // The keyboard resolves its ViewModel from the view tree's lifecycle owner,
            // so it has to be attached to the Activity before it will fetch anything.
            activity.setContentView(container)
            // The root has to be attached before the direction is set; the ldrtl
            // qualifier does nothing here. See RtlLayoutDirectionHarnessTest.
            if (rtl) container.layoutDirection = View.LAYOUT_DIRECTION_RTL
            ShadowLooper.idleMainLooper()

            configure(keyboard)
            ShadowLooper.idleMainLooper()

            val widthSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(2160, View.MeasureSpec.EXACTLY)
            // ViewPager2 populates its pages from a post-layout pass, and the tab bar's
            // RecyclerView from another, so one measure/layout leaves the content state
            // drawn empty. Three settle it.
            repeat(3) {
                container.measure(widthSpec, heightSpec)
                container.layout(0, 0, 1080, 2160)
                ShadowLooper.idleMainLooper()
            }

            // A screenshot test that photographs an empty frame still passes, and this
            // component keeps its own size even when its grid does not render — so the
            // guard has to be about the state, and about the state having children.
            assertTrue("the keyboard should have been laid out", keyboard.width > 0 && keyboard.height > 0)
            assertEquals("wrong state on screen", expect, keyboard.visibleState())
            if (expect == "content") {
                val tabs = keyboard.findViewById<ViewGroup>(R.id.rv_tab_bar)
                assertTrue("the tab bar should carry a tab per sticker set", tabs.childCount > 0)
            }

            assert(keyboard)
            if (capture) container.captureRoboImage(roborazziOptions = RoborazziConfig.options())
        }
        scenario.close()
    }

    // ── empty ───────────────────────────────────────────────────────────────

    @Test
    fun stateEmpty() {
        answerWith(success = payload())
        capture("empty")
    }

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test
    fun stateEmpty_dark() {
        answerWith(success = payload())
        capture("empty")
    }

    // ── error ───────────────────────────────────────────────────────────────

    @Test
    fun stateError() {
        answerWith(failure = CometChatException("ERR_FETCH", "Could not load stickers", "stickers"))
        capture("error")
    }

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test
    fun stateError_dark() {
        answerWith(failure = CometChatException("ERR_FETCH", "Could not load stickers", "stickers"))
        capture("error")
    }

    // ── loading ─────────────────────────────────────────────────────────────

    /** The extension call never resolves, so the shimmer grid stays up. */
    @Test
    fun stateLoading() {
        answerWith()
        capture("loading")
    }

    /**
     * The content state is reached and structurally correct, but it is deliberately not
     * photographed: every tile is a remote sticker loaded through Glide, which never
     * resolves under Robolectric, so a capture is a grid of blank tiles that would pin
     * nothing and pass through any regression. What can be asserted is that the right
     * state is on screen with a tab per sticker set — so that is asserted instead.
     */
    @Test
    fun theContentStateIsReachedThoughItsTilesAreRemoteImages() {
        answerWith(success = payload("Gery" to 8, "Bunny" to 6))
        withKeyboard("content", assert = { keyboard ->
            val tabs = keyboard.findViewById<ViewGroup>(R.id.rv_tab_bar)
            assertEquals("one tab per sticker set", 2, tabs.childCount)
        })
    }

    // ── styling ─────────────────────────────────────────────────────────────

    @Test
    fun styledEmptyState() {
        answerWith(success = payload())
        capture("empty") { keyboard ->
            keyboard.setEmptyStateTitleText("No stickers yet")
            keyboard.setEmptyStateSubtitleText("Ask an admin to enable a sticker pack")
        }}

    @Test
    fun styledErrorState() {
        answerWith(failure = CometChatException("ERR_FETCH", "Could not load stickers", "stickers"))
        capture("error") { keyboard ->
            keyboard.setErrorStateText("Stickers are having a moment")
        }}

    // ── right-to-left ───────────────────────────────────────────────────────

    @Test
    fun stateEmpty_rtl() {
        answerWith(success = payload())
        capture("empty", rtl = true)
    }
}
