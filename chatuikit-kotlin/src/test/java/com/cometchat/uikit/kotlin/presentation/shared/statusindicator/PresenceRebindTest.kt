package com.cometchat.uikit.kotlin.presentation.shared.statusindicator

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * ENG-41551 A (Views kit): rows already on screen re-bind when the connection flips, so a row
 * whose user status did not change still re-reads `OfflinePresence.hideOnline()`. Without a flip
 * (always, offline off) nothing is re-bound.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class PresenceRebindTest {

    private class Rows(private val count: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            object : RecyclerView.ViewHolder(View(parent.context)) {}

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit
        override fun getItemCount(): Int = count
    }

    private class Changes : RecyclerView.AdapterDataObserver() {
        val ranges = mutableListOf<Pair<Int, Int>>()
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            ranges += positionStart to itemCount
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            ranges += positionStart to itemCount
        }
    }

    @Test
    fun `a connection flip re-binds every row and the current value alone re-binds nothing`() {
        RuntimeEnvironment.getApplication()
        val adapter = Rows(count = 5)
        val changes = Changes().also { adapter.registerAdapterDataObserver(it) }
        val ticks = MutableStateFlow(0)
        val scope = TestScope(UnconfinedTestDispatcher())

        val job = scope.rebindOnPresenceChange(ticks) { rebindAllRows(adapter) }
        assertEquals(emptyList<Pair<Int, Int>>(), changes.ranges)

        ticks.value = 1 // disconnected
        assertEquals(listOf(0 to 5), changes.ranges)
        ticks.value = 2 // reconnected
        assertEquals(listOf(0 to 5, 0 to 5), changes.ranges)
        job.cancel()
    }

    @Test
    fun `an empty list is not notified`() {
        val adapter = Rows(count = 0)
        val changes = Changes().also { adapter.registerAdapterDataObserver(it) }
        rebindAllRows(adapter)
        assertEquals(emptyList<Pair<Int, Int>>(), changes.ranges)
    }
}
