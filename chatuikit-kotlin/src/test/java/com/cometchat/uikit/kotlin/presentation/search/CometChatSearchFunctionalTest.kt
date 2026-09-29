package com.cometchat.uikit.kotlin.presentation.search

import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.uikit.core.state.SearchUIState
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.search.ui.CometChatSearch
import com.cometchat.uikit.core.constants.SearchFilter
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * ENG-38678 — CometChatSearch (View) functional surface, Phase 1b-i.
 *
 * Real effect / interaction assertions for the props the mechanical matrix
 * deliberately left as "functional-pending" and that are observable at the View
 * level without live search data:
 *  - visibility flags reach the actual binding views (search bar, filter chips)
 *  - the back-press CALLBACK fires on the real back-icon click
 *  - a custom back-icon Drawable reaches the icon ImageView
 *
 * The remaining functional-pending props (result-click callbacks, empty/error
 * state custom views, query flow) need the ViewModel driven with search results
 * and are covered in Phase 1b-ii.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatSearchFunctionalTest {

    private fun withSearch(block: (CometChatSearch) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            block(CometChatSearch(activity))
        }
        scenario.close()
    }

    // --- Phase 1b-ii: reflection-drive the internal ViewModel's state flows ---

    /** The internal CometChatSearchViewModel created during construction. */
    private fun viewModelOf(search: CometChatSearch): Any {
        val f = CometChatSearch::class.java.getDeclaredField("viewModel")
        f.isAccessible = true
        return requireNotNull(f.get(search)) { "internal ViewModel was not created" }
    }

    /** Emits [value] into the VM's private MutableStateFlow named [field]. */
    private fun emit(vm: Any, field: String, value: Any?) {
        val f = vm.javaClass.getDeclaredField(field)
        f.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (f.get(vm) as MutableStateFlow<Any?>).value = value
    }

    private fun idleMain() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun hideSearchBar_togglesSearchBarVisibility() {
        withSearch { search ->
            val bar = search.findViewById<View>(R.id.search_bar_layout)
            assertNotNull("search_bar_layout should exist", bar)

            search.setHideSearchBar(true)
            assertEquals(View.GONE, bar.visibility)

            search.setHideSearchBar(false)
            assertEquals(View.VISIBLE, bar.visibility)
        }
    }

    @Test
    fun hideFilterChips_togglesChipGroupVisibility() {
        withSearch { search ->
            val chips = search.findViewById<View>(R.id.chip_group)
            assertNotNull("chip_group should exist", chips)

            search.setHideFilterChips(true)
            assertEquals(View.GONE, chips.visibility)

            search.setHideFilterChips(false)
            assertEquals(View.VISIBLE, chips.visibility)
        }
    }

    @Test
    fun onBackPress_callbackFiresOnBackIconClick() {
        withSearch { search ->
            var fired = false
            search.setOnBackPress { fired = true }

            val back = search.findViewById<View>(R.id.iv_back)
            assertNotNull("iv_back should exist", back)
            back.performClick()

            assertTrue("onBackPress callback should fire on back-icon click", fired)
        }
    }

    @Test
    fun setBackIcon_customDrawableReachesTheBackIconView() {
        withSearch { search ->
            val custom = ColorDrawable(0xFF00C0FF.toInt())
            search.setBackIcon(custom)

            val back = search.findViewById<ImageView>(R.id.iv_back)
            assertNotNull("iv_back should be an ImageView", back)
            assertEquals("custom back-icon drawable should reach the ImageView", custom, back.drawable)
        }
    }

    @Test
    fun setSearchFilters_rendersAChipPerFilter() {
        withSearch { search ->
            search.setSearchFilters(listOf(SearchFilter.PHOTOS, SearchFilter.VIDEOS, SearchFilter.DOCUMENTS))
            val chipGroup = search.findViewById<ViewGroup>(R.id.chip_group)
            assertNotNull("chip_group should exist", chipGroup)
            assertEquals("one chip per filter", 3, chipGroup.childCount)
        }
    }

    @Test
    fun initialState_attachesCustomInitialView() {
        withSearch { search ->
            val custom = TextView(search.context).apply { text = "INITIAL-PM" }
            search.setInitialView(custom)
            emit(viewModelOf(search), "_uiState", SearchUIState.Initial)
            idleMain()
            assertTrue("custom initial view should be attached", custom.parent != null)
        }
    }

    @Test
    fun loadingState_attachesCustomLoadingView() {
        withSearch { search ->
            val custom = TextView(search.context).apply { text = "LOADING-PM" }
            search.setLoadingView(custom)
            emit(viewModelOf(search), "_uiState", SearchUIState.Loading)
            idleMain()
            assertTrue("custom loading view should be attached", custom.parent != null)
        }
    }

    @Test
    fun emptyState_attachesCustomEmptyView_andFiresOnEmpty() {
        withSearch { search ->
            val custom = TextView(search.context).apply { text = "EMPTY-PM" }
            search.setEmptyView(custom)
            var onEmptyFired = false
            search.setOnEmpty { onEmptyFired = true }

            emit(viewModelOf(search), "_uiState", SearchUIState.Empty)
            idleMain()

            assertTrue("custom empty view should be attached", custom.parent != null)
            assertTrue("onEmpty should fire on empty state", onEmptyFired)
        }
    }

    @Test
    fun errorState_attachesCustomErrorView_andFiresOnErrorWithException() {
        withSearch { search ->
            val custom = TextView(search.context).apply { text = "ERROR-PM" }
            search.setErrorView(custom)
            var captured: CometChatException? = null
            search.setOnError { captured = it }

            val ex = CometChatException("PM_ERR", "boom")
            emit(viewModelOf(search), "_uiState", SearchUIState.Error(ex))
            idleMain()

            assertTrue("custom error view should be attached", custom.parent != null)
            assertEquals("onError should fire with the exact exception", ex, captured)
        }
    }
}
