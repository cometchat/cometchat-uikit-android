package com.cometchat.uikit.compose.presentation.imageviewer

import android.view.View
import android.view.ViewGroup
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.imageviewer.ui.CometChatVideoViewerScreen
import com.cometchat.uikit.compose.theme.CometChatTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Functional layer for [CometChatVideoViewerScreen] — the behaviour the prop matrix
 * ([CometChatVideoViewerScreenComposePropMatrixTest]) does not reach, because it is about what
 * the screen *does* with its props rather than whether each one arrives.
 *
 * Four things are pinned here:
 *
 *  - the single-video overload really delegates to the pager rather than keeping its own
 *    parallel implementation;
 *  - a starting page outside the list is clamped instead of throwing, which is the guard that
 *    stands between a stale index in an old intent and a crash on open;
 *  - the mime routing, in both directions: a non-video page gets the no-preview screen with a
 *    download of its own, while an *empty* mime (a message sent before mimes were recorded)
 *    still goes down the playback path; and
 *  - playback follows the host's lifecycle — the player pauses when the activity does, so
 *    audio does not keep running off-screen, and resumes on return.
 *
 * As in the matrix, the clock is off auto-advance: a `VideoView` never prepares under
 * Robolectric, so the page's spinner would otherwise animate forever and no wait would return.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatVideoViewerScreenComposeFunctionalTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val DOWNLOAD = "Download"
        const val NO_PREVIEW = "No preview available"

        val URLS = listOf(
            "https://example.invalid/first.mp4",
            "https://example.invalid/second.mp4",
            "https://example.invalid/third.pdf",
        )
        val NAMES = listOf("first.mp4", "second.mp4", "third.pdf")
        val MIMES = listOf("video/mp4", "video/mp4", "application/pdf")
    }

    @Before
    fun stopTheClock() {
        composeRule.mainClock.autoAdvance = false
    }

    /** See [CometChatVideoViewerScreenComposePropMatrixTest.settle] — same reasoning. */
    private fun settle() {
        repeat(3) {
            composeRule.mainClock.advanceTimeBy(300L)
            composeRule.waitForIdle()
        }
    }

    private fun textCount(text: String) =
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().size

    /** The pager's pages are interop views, so playback state is read off the Android tree. */
    private fun videoViews(): List<VideoView> {
        val found = mutableListOf<VideoView>()
        fun walk(view: View) {
            if (view is VideoView) found += view
            if (view is ViewGroup) for (i in 0 until view.childCount) walk(view.getChildAt(i))
        }
        walk(composeRule.activity.window.decorView)
        return found
    }

    private class TestLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle get() = registry
    }

    private fun showViewer(
        urls: List<String> = URLS,
        names: List<String> = NAMES,
        mimes: List<String> = MIMES,
        initialPage: Int = 0,
        lifecycleOwner: LifecycleOwner? = null,
        onDownload: (String, String, String) -> Unit = { _, _, _ -> },
        onShare: (String, String, String) -> Unit = { _, _, _ -> },
    ) {
        composeRule.setContent {
            val content = @androidx.compose.runtime.Composable {
                CometChatTheme {
                    CometChatVideoViewerScreen(
                        videoUrls = urls,
                        fileNames = names,
                        mimeTypes = mimes,
                        initialPage = initialPage,
                        onBack = {},
                        onDownload = onDownload,
                        onShare = onShare,
                    )
                }
            }
            if (lifecycleOwner != null) {
                CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) { content() }
            } else {
                content()
            }
        }
        settle()
    }

    @Test
    fun singleVideoOverload_delegatesToThePager() {
        var shared: Triple<String, String, String>? = null
        composeRule.setContent {
            CometChatTheme {
                CometChatVideoViewerScreen(
                    videoUrl = URLS[1],
                    fileName = NAMES[1],
                    mimeType = MIMES[1],
                    onBack = {},
                    onShare = { u, n, m -> shared = Triple(u, n, m) },
                )
            }
        }
        settle()

        // One page, one player, and the toolbar acts on the single video it was given —
        // i.e. the overload built a one-element list rather than a viewer of its own.
        assertEquals("the overload should compose exactly one player", 1, videoViews().size)
        composeRule.onNodeWithContentDescription("Share").performClick()
        settle()
        assertEquals(Triple(URLS[1], NAMES[1], MIMES[1]), shared)
    }

    @Test
    fun startingPageBeyondTheListIsClamped() {
        var shared: Triple<String, String, String>? = null
        showViewer(initialPage = 99, onShare = { u, n, m -> shared = Triple(u, n, m) })

        // Nothing threw, and the viewer opened on the last page rather than an index that
        // does not exist. The last page is the PDF, so it is also visibly the no-preview one.
        composeRule.onNodeWithText(NO_PREVIEW).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Share").performClick()
        settle()
        assertEquals("an out-of-range start should land on the last video", URLS[2], shared?.first)
    }

    @Test
    fun negativeStartingPageIsClamped() {
        var shared: Triple<String, String, String>? = null
        showViewer(initialPage = -5, onShare = { u, n, m -> shared = Triple(u, n, m) })

        composeRule.onNodeWithContentDescription("Share").performClick()
        settle()
        assertEquals("a negative start should land on the first video", URLS[0], shared?.first)
    }

    @Test
    fun nonVideoPageOffersItsOwnDownload() {
        var downloaded: Triple<String, String, String>? = null
        showViewer(initialPage = 2, onDownload = { u, n, m -> downloaded = Triple(u, n, m) })

        composeRule.onNodeWithText(NO_PREVIEW).assertIsDisplayed()
        assertEquals("a page with no preview composes no player", 0, videoViews().size)

        // The no-preview screen carries a Download button of its own, below the message —
        // distinct from the toolbar's, and it must target the same page.
        composeRule.onNodeWithText(DOWNLOAD).performClick()
        settle()
        assertEquals(
            "the no-preview page's own download should target that page",
            Triple(URLS[2], NAMES[2], MIMES[2]),
            downloaded,
        )
    }

    @Test
    fun anEmptyMimeStillPlaysAsAVideo() {
        // Messages sent before mime types were recorded arrive with an empty mime. Those must
        // keep playing rather than being routed to "no preview available".
        showViewer(
            urls = listOf(URLS[0]),
            names = listOf(NAMES[0]),
            mimes = listOf(""),
        )

        assertEquals("an empty mime should still compose a player", 1, videoViews().size)
        assertEquals("and should not be treated as unpreviewable", 0, textCount(NO_PREVIEW))
    }

    @Test
    fun theVisiblePageStartsPlaying() {
        showViewer(urls = listOf(URLS[0]), names = listOf(NAMES[0]), mimes = listOf(MIMES[0]))

        val player = videoViews().single()
        assertEquals(URLS[0], shadowOf(player).videoURIString)
        assertTrue("the visible page should be playing", player.isPlaying)
    }

    @Test
    fun backgroundingPausesPlaybackAndReturningResumesIt() {
        val owner = TestLifecycleOwner()
        owner.registry.currentState = Lifecycle.State.RESUMED
        showViewer(
            urls = listOf(URLS[0]),
            names = listOf(NAMES[0]),
            mimes = listOf(MIMES[0]),
            lifecycleOwner = owner,
        )

        val player = videoViews().single()
        assertTrue("the visible page should start playing", player.isPlaying)

        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        settle()
        assertFalse(
            "playback should stop when the host is backgrounded, or audio keeps running off-screen",
            player.isPlaying,
        )

        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        settle()
        assertTrue("and resume on return", player.isPlaying)
    }
}
