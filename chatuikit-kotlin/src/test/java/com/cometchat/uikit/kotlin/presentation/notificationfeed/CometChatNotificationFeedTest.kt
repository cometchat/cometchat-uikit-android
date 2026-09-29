package com.cometchat.uikit.kotlin.presentation.notificationfeed

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.core.state.NotificationFeedUIState
import com.cometchat.uikit.core.viewmodel.CometChatNotificationFeedViewModel
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.notificationfeed.style.CometChatNotificationFeedStyle
import com.cometchat.uikit.kotlin.presentation.notificationfeed.ui.CometChatNotificationFeed
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.NonDefaultValues
import com.cometchat.uikit.propmatrix.Prop
import com.cometchat.uikit.propmatrix.PropKind
import com.cometchat.uikit.propmatrix.StylePropSweep
import com.cometchat.uikit.propmatrix.ViewPropSweep
import com.cometchat.uikit.propmatrix.WaiverSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * ENG-38681 — CometChatNotificationFeed (View) matrix + functional. Functional
 * injects a listeners-off VM and drives its uiState flow to the empty state
 * (safe-VM, no live SDK).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatNotificationFeedTest {

    private val owner = "CometChatNotificationFeed"

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        NonDefaultValues.clearRegistered()
        Dispatchers.resetMain()
    }

    private val waivers = WaiverSet.parse(
        """
        - owner: CometChatNotificationFeed
          name: Style
          reason: whole-style object; matrixed by the CometChatNotificationFeedStyle sweep
          by: ENG-38681
        """.trimIndent()
    )

    private fun pending(p: Class<*>): Boolean =
        View::class.java.isAssignableFrom(p) || Drawable::class.java.isAssignableFrom(p) ||
            Function::class.java.isAssignableFrom(p) || p.isInterface ||
            p.simpleName.endsWith("Style") || p.simpleName.endsWith("RequestBuilder")

    @Test
    fun view_matrix_value_and_style() {
        NonDefaultValues.register(Int::class) { android.R.style.TextAppearance_Material_Body1 }
        NonDefaultValues.register(Drawable::class) { ColorDrawable(0xFF00C0FF.toInt()) }
        NonDefaultValues.register(android.graphics.Typeface::class) { android.graphics.Typeface.DEFAULT_BOLD }

        val setters = ViewPropSweep.setters(CometChatNotificationFeed::class.java)
        val props = mutableListOf<Prop>()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val v = CometChatNotificationFeed(activity)
            setters.forEach { s ->
                when {
                    waivers.isWaived(owner, s.propName) -> props += Prop(owner, s.propName, PropKind.VALUE, waived = true)
                    pending(s.paramType) -> {}
                    s.supported -> props += Prop(owner, s.propName, PropKind.VALUE, covered = runCatching { s.applyNonDefault(v) }.isSuccess)
                }
            }
            val style = StylePropSweep.sweep(CometChatNotificationFeedStyle::class)
            println("  [notificationfeed style] variants=${style.variants.size} unsupported=${style.unsupported}")
            assertTrue(style.variants.size >= 4 && style.unsupported.isEmpty())
            style.variants.forEach { v.setStyle(it.instance) }
        }
        scenario.close()
        val cov = Denominator.summarize(props)
        println("  [notificationfeed view VALUE] ${cov.covered}/${cov.total}")
        assertEquals(cov.total, cov.covered); assertEquals(100.0, cov.percent, 0.0)
    }

    @Test
    fun vmFlow_reachesEmptyState() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val v = CometChatNotificationFeed(activity)

            val safeVm = CometChatNotificationFeedViewModel(enableListeners = false)
            CometChatNotificationFeed::class.java.getDeclaredField("viewModel").apply { isAccessible = true }.set(v, safeVm)
            CometChatNotificationFeed::class.java
                .getDeclaredMethod("observeViewModel", LifecycleOwner::class.java).apply { isAccessible = true }
                .invoke(v, activity)

            @Suppress("UNCHECKED_CAST")
            val uiStateFlow = CometChatNotificationFeedViewModel::class.java
                .getDeclaredField("_uiState").apply { isAccessible = true }
                .get(safeVm) as MutableStateFlow<NotificationFeedUIState>
            uiStateFlow.value = NotificationFeedUIState.Empty
            shadowOf(Looper.getMainLooper()).idle()

            val emptyView = CometChatNotificationFeed::class.java
                .getDeclaredField("emptyView").apply { isAccessible = true }.get(v) as View
            assertEquals("Empty state should be visible", View.VISIBLE, emptyView.visibility)
        }
        scenario.close()
    }
}
