package com.cometchat.uikit.kotlin.presentation.aiassistantchathistory

import android.graphics.drawable.Drawable
import android.view.View
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.state.ChatHistoryUIState
import com.cometchat.uikit.core.viewmodel.CometChatAIAssistantChatHistoryViewModel
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.aiassistantchathistory.ui.CometChatAIAssistantChatHistory
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.NonDefaultValues
import com.cometchat.uikit.propmatrix.Prop
import com.cometchat.uikit.propmatrix.PropKind
import com.cometchat.uikit.propmatrix.ViewPropSweep
import com.cometchat.uikit.propmatrix.WaiverSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.os.Looper

/**
 * ENG-38681 — CometChatAIAssistantChatHistory (View) matrix + functional. It is a
 * shared-VM screen; the VM does not fetch until setUser, so construction is
 * SDK-safe. Functional: driving the VM's message/uiState flows reaches the
 * adapter and the empty state (safe-VM, no live SDK).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatAIAssistantChatHistoryTest {

    private val owner = "CometChatAIAssistantChatHistory"

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        NonDefaultValues.clearRegistered()
        Dispatchers.resetMain()
    }

    private val waivers = WaiverSet.parse(
        """
        - owner: CometChatAIAssistantChatHistory
          name: Style
          reason: setStyle takes a @StyleRes theme; whole-style application
          by: ENG-38681
        """.trimIndent()
    )

    private fun pending(p: Class<*>): Boolean =
        View::class.java.isAssignableFrom(p) || Drawable::class.java.isAssignableFrom(p) ||
            Function::class.java.isAssignableFrom(p) || p.isInterface ||
            p.simpleName.endsWith("Style") || p.simpleName == "User" || p.simpleName == "Group"

    @Test
    fun view_matrix_value() {
        val setters = ViewPropSweep.setters(CometChatAIAssistantChatHistory::class.java)
        val props = mutableListOf<Prop>()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val v = CometChatAIAssistantChatHistory(activity)
            setters.forEach { s ->
                when {
                    waivers.isWaived(owner, s.propName) -> props += Prop(owner, s.propName, PropKind.VALUE, waived = true)
                    pending(s.paramType) -> {}
                    s.supported -> props += Prop(owner, s.propName, PropKind.VALUE, covered = runCatching { s.applyNonDefault(v) }.isSuccess)
                }
            }
        }
        scenario.close()
        val cov = Denominator.summarize(props)
        println("  [aiassist view VALUE] ${cov.covered}/${cov.total}")
        assertEquals(cov.total, cov.covered); assertEquals(100.0, cov.percent, 0.0)
    }

    @Test
    fun vmFlows_reachAdapterAndEmptyState() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val v = CometChatAIAssistantChatHistory(activity)
            val vm = CometChatAIAssistantChatHistory::class.java
                .getDeclaredField("viewModel").apply { isAccessible = true }
                .get(v) as CometChatAIAssistantChatHistoryViewModel

            // messages flow -> adapter
            @Suppress("UNCHECKED_CAST")
            val messagesFlow = CometChatAIAssistantChatHistoryViewModel::class.java
                .getDeclaredField("_messages").apply { isAccessible = true }
                .get(vm) as MutableStateFlow<List<BaseMessage>>
            messagesFlow.value = listOf(mock(), mock(), mock())
            shadowOf(Looper.getMainLooper()).idle()

            val adapter = CometChatAIAssistantChatHistory::class.java
                .getDeclaredField("adapter").apply { isAccessible = true }
                .get(v) as RecyclerView.Adapter<*>
            assertEquals("messages should reach the adapter", 3, adapter.itemCount)

            // uiState flow -> empty state view visible
            @Suppress("UNCHECKED_CAST")
            val uiStateFlow = CometChatAIAssistantChatHistoryViewModel::class.java
                .getDeclaredField("_uiState").apply { isAccessible = true }
                .get(vm) as MutableStateFlow<ChatHistoryUIState>
            uiStateFlow.value = ChatHistoryUIState.Loading
            shadowOf(Looper.getMainLooper()).idle()
            uiStateFlow.value = ChatHistoryUIState.Empty
            shadowOf(Looper.getMainLooper()).idle()

            val binding = CometChatAIAssistantChatHistory::class.java
                .getDeclaredField("binding").apply { isAccessible = true }.get(v)
            val emptyStateView = binding.javaClass.getField("emptyStateView").get(binding) as View
            assertEquals("Empty state should be visible", View.VISIBLE, emptyStateView.visibility)
        }
        scenario.close()
    }
}
