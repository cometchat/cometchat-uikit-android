package com.cometchat.uikit.compose.presentation.imageviewer

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.VideoView
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.imageviewer.ui.CometChatVideoViewerActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Functional layer for the Compose host [CometChatVideoViewerActivity] — the half of the
 * Compose video viewer that [CometChatVideoViewerScreenComposeFunctionalTest] cannot reach,
 * because it is the activity, not the screen: how an intent becomes the screen's arguments,
 * and what the toolbar's two actions do once they leave the composable.
 *
 * The activity sets its own content, so this uses an empty compose rule alongside a real
 * [ActivityScenario] rather than a rule that launches its own host.
 *
 * The intent parsing is the part worth pinning hardest. The activity reads list extras and
 * falls back to the single-video extras, which is what keeps intents built by older callers
 * working; nothing else in the kit exercises that fallback, and its keys are private, so the
 * legacy intent is rebuilt here by key on purpose — if those keys are renamed without a
 * migration, this fails, which is the point.
 *
 * `downloadVideo` enqueues with the platform `DownloadManager`, so the download assertions
 * read the request back off its shadow. Share is asserted only in its guard case: the happy
 * path opens a network connection on a background thread.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatVideoViewerActivityComposeFunctionalTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private companion object {
        const val BACK = "Back"
        const val DOWNLOAD = "Download"
        const val SHARE = "Share"

        val URLS = listOf(
            "https://example.invalid/first.mp4",
            "https://example.invalid/second.mp4",
        )
        val NAMES = listOf("first.mp4", "second.mp4")
        val MIMES = listOf("video/mp4", "video/mp4")

        // The activity's own extras keys, private to it. See the class doc.
        const val EXTRA_VIDEO_URL = "extra_video_url"
        const val EXTRA_FILE_NAME = "extra_file_name"
        const val EXTRA_MIME_TYPE = "extra_mime_type"
    }

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private var scenario: ActivityScenario<CometChatVideoViewerActivity>? = null

    @Before
    fun stopTheClock() {
        // Same reason as the screen's tests: a VideoView never prepares here, so the page's
        // spinner would animate forever and no wait would ever return.
        composeRule.mainClock.autoAdvance = false
    }

    @After
    fun closeScenario() {
        scenario?.close()
    }

    private fun settle() {
        repeat(3) {
            composeRule.mainClock.advanceTimeBy(300L)
            composeRule.waitForIdle()
        }
    }

    private fun launch(intent: Intent) {
        scenario = ActivityScenario.launch(intent)
        settle()
    }

    private fun tap(description: String) {
        composeRule.onNodeWithContentDescription(description).performClick()
        settle()
    }

    private fun players(): List<VideoView> {
        val found = mutableListOf<VideoView>()
        fun walk(view: View) {
            if (view is VideoView) found += view
            if (view is ViewGroup) for (i in 0 until view.childCount) walk(view.getChildAt(i))
        }
        scenario?.onActivity { walk(it.window.decorView) }
        return found
    }

    private fun downloadManager() =
        shadowOf(context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager)

    private fun downloadCount(): Int = downloadManager().requestCount

    /** Robolectric numbers downloads from 0, as the platform does, so the newest is count - 1. */
    private fun lastDownloadTitle(): String? =
        downloadManager().getRequest((downloadCount() - 1).toLong())
            ?.let { shadowOf(it).title?.toString() }

    // ==================== intent → screen ====================

    @Test
    fun aListIntentOpensOnTheRequestedVideo() {
        launch(CometChatVideoViewerActivity.createIntent(context, URLS, NAMES, MIMES, startIndex = 1))

        assertEquals("the pager should compose one player", 1, players().size)
        assertEquals(
            "the viewer should open on the requested video",
            URLS[1],
            shadowOf(players().single()).videoURIString,
        )
    }

    @Test
    fun aSingleVideoIntentOpensThatVideo() {
        launch(CometChatVideoViewerActivity.createIntent(context, URLS[0], NAMES[0], MIMES[0]))

        assertEquals(URLS[0], shadowOf(players().single()).videoURIString)
    }

    @Test
    fun anIntentCarryingOnlyTheLegacySingleExtrasStillPlays() {
        // What an intent built by an older caller looks like: no list extras at all.
        val legacy = Intent(context, CometChatVideoViewerActivity::class.java).apply {
            putExtra(EXTRA_VIDEO_URL, URLS[0])
            putExtra(EXTRA_FILE_NAME, NAMES[0])
            putExtra(EXTRA_MIME_TYPE, MIMES[0])
        }
        launch(legacy)

        assertEquals(
            "the single-extra fallback should still reach the player",
            URLS[0],
            shadowOf(players().single()).videoURIString,
        )
    }

    // ==================== toolbar actions ====================

    @Test
    fun downloadSavesTheVisibleVideoUnderItsFileName() {
        val before = downloadCount()
        launch(CometChatVideoViewerActivity.createIntent(context, URLS, NAMES, MIMES, startIndex = 1))

        tap(DOWNLOAD)

        assertEquals("one tap should enqueue one download", before + 1, downloadCount())
        assertEquals(
            "and it should be the visible video, saved under its own name",
            NAMES[1],
            lastDownloadTitle(),
        )
    }

    @Test
    fun downloadFallsBackToTheUrlsLastSegmentWhenThereIsNoFileName() {
        val before = downloadCount()
        launch(
            CometChatVideoViewerActivity.createIntent(
                context, listOf(URLS[0]), listOf(""), listOf(MIMES[0]),
            ),
        )

        tap(DOWNLOAD)

        assertEquals(before + 1, downloadCount())
        assertEquals(
            "a nameless video should be saved as the file the url points at",
            "first.mp4",
            lastDownloadTitle(),
        )
    }

    @Test
    fun downloadRefusesAVideoWithNoUrl() {
        val before = downloadCount()
        launch(
            CometChatVideoViewerActivity.createIntent(
                context, listOf(""), listOf(NAMES[0]), listOf(MIMES[0]),
            ),
        )

        tap(DOWNLOAD)

        assertEquals(
            "there is nothing to fetch, so nothing should be queued",
            before,
            downloadCount(),
        )
    }

    @Test
    fun shareRefusesAVideoWithNoMimeType() {
        launch(
            CometChatVideoViewerActivity.createIntent(
                context, listOf(URLS[0]), listOf(NAMES[0]), listOf(""),
            ),
        )

        tap(SHARE)

        var started: Intent? = null
        scenario?.onActivity { started = shadowOf(it).peekNextStartedActivity() }
        assertNull(
            "sharing without a mime type should do nothing rather than open a chooser",
            started,
        )
    }

    // ==================== navigation ====================

    @Test
    fun backClosesTheViewer() {
        launch(CometChatVideoViewerActivity.createIntent(context, URLS, NAMES, MIMES))

        tap(BACK)

        var finishing = false
        scenario?.onActivity { finishing = it.isFinishing }
        assertEquals("the back affordance should close the activity", true, finishing)
    }
}
