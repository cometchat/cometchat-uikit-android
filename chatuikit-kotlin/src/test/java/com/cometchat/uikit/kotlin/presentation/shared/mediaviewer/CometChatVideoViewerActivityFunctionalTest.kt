package com.cometchat.uikit.kotlin.presentation.shared.mediaviewer

import android.app.DownloadManager
import android.content.Context
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.VideoView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.viewpager.widget.ViewPager
import com.cometchat.uikit.kotlin.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * Functional layer for the View [CometChatVideoViewerActivity], which had no test of any kind.
 *
 * The activity is driven for real rather than modelled: every test launches it through its own
 * `createIntent`, so the intent contract and the screen are asserted together — an extras key
 * that stopped matching would fail here the same way a broken pager would.
 *
 * What is pinned:
 *
 *  - the two `createIntent` factories, including the single-video one delegating to the list;
 *  - the empty-url guard, which closes the screen instead of showing an empty player;
 *  - the start index, including a stale out-of-range one being clamped rather than thrown;
 *  - one player per video, only the visible one playing, and playback following the
 *    activity's own lifecycle — the reason `onPause`/`onResume`/`onDestroy` are overridden;
 *  - the toolbar's download targeting the *visible* page, and its guards refusing to act on
 *    incomplete data.
 *
 * `MediaUtils.downloadFile` enqueues with the platform `DownloadManager`, so the download
 * assertions read the enqueued request back off its shadow rather than mocking the util.
 * Share is only asserted in its guard case: the happy path opens a network connection on a
 * background thread, which is the util's business and covered where that util is tested.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatVideoViewerActivityFunctionalTest {

    private companion object {
        val URLS = listOf(
            "https://example.invalid/first.mp4",
            "https://example.invalid/second.mp4",
            "https://example.invalid/third.mp4",
        )
        val NAMES = listOf("first.mp4", "second.mp4", "third.mp4")
        val MIMES = listOf("video/mp4", "video/mp4", "video/mp4")
    }

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun launch(
        urls: List<String> = URLS,
        names: List<String> = NAMES,
        mimes: List<String> = MIMES,
        startIndex: Int = 0,
    ): ActivityController<CometChatVideoViewerActivity> {
        val intent = CometChatVideoViewerActivity.createIntent(context, urls, names, mimes, startIndex)
        val controller = Robolectric.buildActivity(CometChatVideoViewerActivity::class.java, intent).setup()
        layOut(controller)
        return controller
    }

    /** ViewPager only instantiates pages once it has been measured. */
    private fun layOut(controller: ActivityController<CometChatVideoViewerActivity>) {
        val root = controller.get().findViewById<View>(android.R.id.content)
        root.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, 1080, 1920)
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun pager(activity: CometChatVideoViewerActivity): ViewPager =
        activity.findViewById(R.id.video_pager)

    private fun players(activity: CometChatVideoViewerActivity): List<VideoView> {
        val found = mutableListOf<VideoView>()
        fun walk(view: View) {
            if (view is VideoView) found += view
            if (view is ViewGroup) for (i in 0 until view.childCount) walk(view.getChildAt(i))
        }
        walk(pager(activity))
        return found
    }

    private fun playerFor(activity: CometChatVideoViewerActivity, url: String): VideoView =
        players(activity).first { shadowOf(it).videoURIString == url }

    private fun downloadManager() =
        shadowOf(context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager)

    private fun downloadCount(): Int = downloadManager().requestCount

    /** Robolectric numbers downloads from 0, as the platform does, so the newest is count - 1. */
    private fun lastDownloadTitle(): String? =
        downloadManager().getRequest((downloadCount() - 1).toLong())
            ?.let { shadowOf(it).title?.toString() }

    private fun tapDownload(activity: CometChatVideoViewerActivity) {
        activity.findViewById<ImageView>(R.id.button_download).performClick()
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun tapShare(activity: CometChatVideoViewerActivity) {
        activity.findViewById<ImageView>(R.id.button_share).performClick()
        shadowOf(Looper.getMainLooper()).idle()
    }

    // ==================== intent contract ====================

    @Test
    fun singleVideoIntentDelegatesToTheListFactory() {
        val intent = CometChatVideoViewerActivity.createIntent(context, URLS[0], NAMES[0], MIMES[0])
        val controller = Robolectric.buildActivity(CometChatVideoViewerActivity::class.java, intent).setup()
        layOut(controller)
        val activity = controller.get()

        assertFalse("a single video is enough to open the viewer", activity.isFinishing)
        assertEquals("one video means one page", 1, pager(activity).adapter?.count)
        assertEquals(URLS[0], shadowOf(players(activity).single()).videoURIString)
    }

    @Test
    fun everyVideoInTheListGetsItsOwnPage() {
        val activity = launch().get()
        assertEquals(URLS.size, pager(activity).adapter?.count)
    }

    @Test
    fun noUrlsClosesTheScreenInsteadOfShowingAnEmptyPlayer() {
        val intent = CometChatVideoViewerActivity.createIntent(
            context, emptyList(), emptyList(), emptyList(),
        )
        val activity = Robolectric.buildActivity(CometChatVideoViewerActivity::class.java, intent)
            .setup().get()

        assertTrue("a viewer with nothing to play should close itself", activity.isFinishing)
    }

    // ==================== start index ====================

    @Test
    fun theViewerOpensOnTheRequestedPage() {
        val activity = launch(startIndex = 1).get()
        assertEquals(1, pager(activity).currentItem)
    }

    @Test
    fun aStaleStartIndexIsClampedRatherThanThrown() {
        val activity = launch(startIndex = 99).get()
        assertEquals("an index past the end should land on the last video", 2, pager(activity).currentItem)
    }

    // ==================== playback ====================

    @Test
    fun onlyTheVisiblePageIsPlaying() {
        val activity = launch(startIndex = 0).get()

        assertTrue("the opening page should play", playerFor(activity, URLS[0]).isPlaying)
        players(activity)
            .filterNot { shadowOf(it).videoURIString == URLS[0] }
            .forEach { assertFalse("only the visible page should play", it.isPlaying) }
    }

    @Test
    fun swipingToAnotherPageMovesPlaybackWithIt() {
        val controller = launch(startIndex = 0)
        val activity = controller.get()

        pager(activity).currentItem = 1
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue("the page swiped to should play", playerFor(activity, URLS[1]).isPlaying)
        assertFalse("the page left behind should not", playerFor(activity, URLS[0]).isPlaying)
    }

    @Test
    fun backgroundingPausesEveryPlayerAndReturningResumesTheVisibleOne() {
        val controller = launch(startIndex = 0)
        val activity = controller.get()
        assertTrue(playerFor(activity, URLS[0]).isPlaying)

        controller.pause()
        assertTrue(
            "nothing should keep playing off-screen",
            players(activity).none { it.isPlaying },
        )

        controller.resume()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue("the visible page should resume", playerFor(activity, URLS[0]).isPlaying)
    }

    @Test
    fun destroyingTheScreenReleasesEveryPlayer() {
        val controller = launch(startIndex = 0)
        val activity = controller.get()
        val opened = players(activity)
        assertTrue("the fixture should have opened at least one player", opened.isNotEmpty())

        controller.pause().stop().destroy()

        assertTrue(
            "every player should be stopped when the screen goes away",
            opened.none { it.isPlaying },
        )
    }

    // ==================== toolbar actions ====================

    @Test
    fun downloadEnqueuesTheVisiblePagesFile() {
        val before = downloadCount()
        val activity = launch(startIndex = 1).get()

        tapDownload(activity)

        assertEquals("one tap should enqueue one download", before + 1, downloadCount())
        assertEquals("the download should target the visible page", NAMES[1], lastDownloadTitle())
    }

    @Test
    fun downloadFollowsThePageTheUserSwipedTo() {
        val before = downloadCount()
        val activity = launch(startIndex = 0).get()

        pager(activity).currentItem = 2
        shadowOf(Looper.getMainLooper()).idle()
        tapDownload(activity)

        assertEquals(before + 1, downloadCount())
        assertEquals(NAMES[2], lastDownloadTitle())
    }

    @Test
    fun downloadRefusesAVideoWithNoFileName() {
        val before = downloadCount()
        val activity = launch(names = listOf("", "", "")).get()

        tapDownload(activity)

        assertEquals(
            "a nameless file should be refused rather than saved as something arbitrary",
            before,
            downloadCount(),
        )
    }

    @Test
    fun shareRefusesAVideoWithNoMimeType() {
        val activity = launch(mimes = listOf("", "", "")).get()

        tapShare(activity)

        assertNull(
            "sharing without a mime type should do nothing rather than open a chooser",
            shadowOf(activity).peekNextStartedActivity(),
        )
    }

    // ==================== navigation ====================

    @Test
    fun navigateUpClosesTheViewer() {
        val activity = launch().get()

        assertTrue(activity.onSupportNavigateUp())
        assertTrue("the back arrow should close the viewer", activity.isFinishing)
    }
}
