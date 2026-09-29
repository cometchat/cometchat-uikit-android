package com.cometchat.uikit.kotlin.presentation.shared.aiconversationsummary

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.NonDefaultValues
import com.cometchat.uikit.propmatrix.Prop
import com.cometchat.uikit.propmatrix.PropKind
import com.cometchat.uikit.propmatrix.StylePropSweep
import com.cometchat.uikit.propmatrix.ViewPropSweep
import com.cometchat.uikit.propmatrix.WaiverSet
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * ENG-38681 — CometChatAIConversationSummaryView (View) matrix + functional.
 * Functional: a summary string reaches the RecyclerView adapter.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatAIConversationSummaryViewTest {

    private val owner = "CometChatAIConversationSummaryView"

    @After
    fun tearDown() = NonDefaultValues.clearRegistered()

    private val waivers = WaiverSet.parse(
        """
        - owner: CometChatAIConversationSummaryView
          name: Style
          reason: whole-style object; matrixed by the CometChatAIConversationSummaryStyle sweep
          by: ENG-38681
        """.trimIndent()
    )

    private fun pending(p: Class<*>): Boolean =
        View::class.java.isAssignableFrom(p) || Drawable::class.java.isAssignableFrom(p) ||
            Function::class.java.isAssignableFrom(p) || p.isInterface || p.simpleName.endsWith("Style")

    @Test
    fun view_matrix_value_and_style() {
        NonDefaultValues.register(Int::class) { android.R.style.TextAppearance_Material_Body1 }
        listOf("ErrorViewLayout", "LoadingViewLayout").forEach {
            NonDefaultValues.registerProp(owner, it) { android.R.layout.simple_list_item_1 }
        }
        NonDefaultValues.register(Drawable::class) { ColorDrawable(0xFF00C0FF.toInt()) }

        val setters = ViewPropSweep.setters(CometChatAIConversationSummaryView::class.java)
        val props = mutableListOf<Prop>()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val v = CometChatAIConversationSummaryView(activity)
            setters.forEach { s ->
                when {
                    waivers.isWaived(owner, s.propName) -> props += Prop(owner, s.propName, PropKind.VALUE, waived = true)
                    pending(s.paramType) -> {}
                    s.supported -> props += Prop(owner, s.propName, PropKind.VALUE, covered = runCatching { s.applyNonDefault(v) }.isSuccess)
                }
            }
            val style = StylePropSweep.sweep(CometChatAIConversationSummaryStyle::class)
            println("  [summary style] variants=${style.variants.size} unsupported=${style.unsupported}")
            assertTrue(style.variants.size >= 4 && style.unsupported.isEmpty())
            style.variants.forEach { v.setStyle(it.instance) }
        }
        scenario.close()
        val cov = Denominator.summarize(props)
        println("  [summary view VALUE] ${cov.covered}/${cov.total}")
        assertEquals(cov.total, cov.covered); assertEquals(100.0, cov.percent, 0.0)
    }

    @Test
    fun setSummary_reachesAdapter() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val v = CometChatAIConversationSummaryView(activity)
            v.setSummary("This is the AI-generated summary of the conversation.")
            val adapter = CometChatAIConversationSummaryView::class.java
                .getDeclaredField("aiConversationSummaryAdapter").apply { isAccessible = true }
                .get(v) as RecyclerView.Adapter<*>
            assertEquals("summary should reach the adapter", 1, adapter.itemCount)
        }
        scenario.close()
    }
}
