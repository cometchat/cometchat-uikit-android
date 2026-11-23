package com.cometchat.uikit.core.offline

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/**
 * [OfflineNoticeRelay] backs both list ViewModels' `offlineNotices` (§2.8): no notice is lost while
 * the list's UI is not collecting, and a list-less hub notice goes to a list that is being shown.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineNoticeRelayTest : FunSpec({

    beforeTest { OfflineHub.resetForTest() }
    afterTest { OfflineHub.resetForTest() }

    test("a list's own notice raised while nothing collects is delivered once a subscriber arrives") {
        runTest(UnconfinedTestDispatcher()) {
            val relay = OfflineNoticeRelay()
            val job = relay.start(this)
            relay.raise(OfflineNotice.QueueFull)
            relay.raise(OfflineNotice.UnsentRemoved("hi"))
            advanceUntilIdle()

            val got = mutableListOf<OfflineNotice>()
            val collector = launch { relay.notices.collect { got += it } }
            advanceUntilIdle()

            got shouldBe listOf(OfflineNotice.QueueFull, OfflineNotice.UnsentRemoved("hi"))
            collector.cancel(); job.cancel()
        }
    }

    test("a hub notice posted while no list collects waits and goes to the first list that subscribes") {
        runTest(UnconfinedTestDispatcher()) {
            val relay = OfflineNoticeRelay()
            val job = relay.start(this)
            OfflineHub.postNotice(OfflineNotice.UnsentLost(2))
            advanceUntilIdle()

            val got = mutableListOf<OfflineNotice>()
            val collector = launch { relay.notices.collect { got += it } }
            advanceUntilIdle()

            got shouldBe listOf(OfflineNotice.UnsentLost(2))
            collector.cancel(); job.cancel()
        }
    }

    test("a hub notice is delivered once, to the shown list, never swallowed by a hidden one") {
        runTest(UnconfinedTestDispatcher()) {
            val hidden = OfflineNoticeRelay()
            val shown = OfflineNoticeRelay()
            val jobs = listOf(hidden.start(this), shown.start(this))
            val gotShown = mutableListOf<OfflineNotice>()
            val c1 = launch { shown.notices.collect { gotShown += it } }
            advanceUntilIdle()

            OfflineHub.postNotice(OfflineNotice.MessagesNotSent(3))
            advanceUntilIdle()
            gotShown shouldBe listOf(OfflineNotice.MessagesNotSent(3))

            val gotHidden = mutableListOf<OfflineNotice>()
            val c2 = launch { hidden.notices.collect { gotHidden += it } }
            advanceUntilIdle()
            gotHidden.shouldBeEmpty()

            c1.cancel(); c2.cancel(); jobs.forEach { it.cancel() }
        }
    }

    test("a list whose UI stopped collecting while waiting hands the notice to the live list, exactly once") {
        runTest(UnconfinedTestDispatcher()) {
            // The conversations list was visible: its relay passed the subscriber check and is now
            // waiting on the hub. Then a chat opened over it — its UI stopped collecting.
            val covered = OfflineNoticeRelay()
            val coveredJob = covered.start(this)
            val gotCovered = mutableListOf<OfflineNotice>()
            val coveredUi = launch { covered.notices.collect { gotCovered += it } }
            advanceUntilIdle()
            coveredUi.cancel()
            advanceUntilIdle()

            // The message list is on screen and collecting.
            val onScreen = OfflineNoticeRelay()
            val onScreenJob = onScreen.start(this)
            val gotOnScreen = mutableListOf<OfflineNotice>()
            val onScreenUi = launch { onScreen.notices.collect { gotOnScreen += it } }
            advanceUntilIdle()

            OfflineHub.postNotice(OfflineNotice.MessagesNotSent(2))
            advanceUntilIdle()
            gotOnScreen shouldBe listOf(OfflineNotice.MessagesNotSent(2))

            // When the covered list's UI comes back it does not show the notice a second time.
            val backUi = launch { covered.notices.collect { gotCovered += it } }
            advanceUntilIdle()
            gotCovered.shouldBeEmpty()

            backUi.cancel(); onScreenUi.cancel(); coveredJob.cancel(); onScreenJob.cancel()
        }
    }

    test("a cleared list never swallows a hub notice") {
        runTest(UnconfinedTestDispatcher()) {
            val cleared = OfflineNoticeRelay()
            val clearedJob = cleared.start(this)
            val clearedUi = launch { cleared.notices.collect { } }
            advanceUntilIdle()
            // Its ViewModel is cleared while its relay waits on the hub.
            clearedUi.cancel(); clearedJob.cancel()
            advanceUntilIdle()

            OfflineHub.postNotice(OfflineNotice.UnsentMaybeLost)
            val live = OfflineNoticeRelay()
            val liveJob = live.start(this)
            val got = mutableListOf<OfflineNotice>()
            val liveUi = launch { live.notices.collect { got += it } }
            advanceUntilIdle()
            got shouldBe listOf(OfflineNotice.UnsentMaybeLost)

            liveUi.cancel(); liveJob.cancel()
        }
    }
})
