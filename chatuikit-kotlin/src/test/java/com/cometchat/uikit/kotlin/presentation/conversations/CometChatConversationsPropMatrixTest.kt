package com.cometchat.uikit.kotlin.presentation.conversations

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.conversations.style.CometChatConversationsStyle
import com.cometchat.uikit.kotlin.presentation.conversations.ui.CometChatConversations
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
 * ENG-38679 — CometChatConversations (View) prop matrix — Phase 1 (value + style).
 *
 * Mechanical sweep on the real ViewModel-backed CometChatConversations under
 * Robolectric (same model as CometChatSearch): every VALUE prop set non-default
 * and applied, every CometChatConversationsStyle variant applied. SLOT/CALLBACK/
 * config props are the functional column (Phase 1b). Infra (ViewModel, whole
 * Style) is waived; the nested itemStyle is waived in the style sweep (matrixed
 * by the list-item's own style).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatConversationsPropMatrixTest {

    @After
    fun tearDown() = NonDefaultValues.clearRegistered()

    private val componentWaivers = WaiverSet.parse(
        """
        - owner: CometChatConversations
          name: ViewModel
          reason: DI/infra injection, not an integrator styling or behaviour prop
          by: ENG-38679
        - owner: CometChatConversations
          name: Style
          reason: whole-style object; matrixed field-by-field by the CometChatConversationsStyle sweep
          by: ENG-38679
        """.trimIndent()
    )

    private fun isFunctionalPending(paramType: Class<*>): Boolean =
        View::class.java.isAssignableFrom(paramType) ||
            Drawable::class.java.isAssignableFrom(paramType) ||
            Function::class.java.isAssignableFrom(paramType) ||
            List::class.java.isAssignableFrom(paramType) ||
            paramType.isInterface ||
            paramType.simpleName.endsWith("Style") ||
            paramType.simpleName.endsWith("RequestBuilder")

    @Test
    fun viewConversations_mechanicalValueMatrix_coversEveryValueProp() {
        val setters = ViewPropSweep.setters(CometChatConversations::class.java)
        assertTrue("expected a large setter surface, was ${setters.size}", setters.size > 60)

        val props = mutableListOf<Prop>()
        val functionalPending = mutableListOf<String>()

        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val conv = CometChatConversations(activity)
            setters.forEach { setter ->
                when {
                    componentWaivers.isWaived("CometChatConversations", setter.propName) ->
                        props += Prop("CometChatConversations", setter.propName, PropKind.VALUE, waived = true)
                    isFunctionalPending(setter.paramType) ->
                        functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                    setter.supported ->
                        props += Prop(
                            "CometChatConversations", setter.propName, PropKind.VALUE,
                            covered = runCatching { setter.applyNonDefault(conv) }.isSuccess,
                        )
                    else -> functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                }
            }
        }
        scenario.close()

        val cov = Denominator.summarize(props)
        println("  [conv view VALUE] ${cov.covered}/${cov.total} (${cov.waived} waived); functional-pending=${functionalPending.size}")

        assertTrue("expected >= 40 value props, was ${cov.total}", cov.total >= 40)
        assertEquals("every swept VALUE prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }

    @Test
    fun viewConversationsStyle_matrix_coversEveryStyleProp() {
        NonDefaultValues.register(Drawable::class) { ColorDrawable(0xFF00C0FF.toInt()) }
        val nestedStyleWaivers = setOf("itemStyle")

        val result = StylePropSweep.sweep(CometChatConversationsStyle::class)
        val stillUnsupported = result.unsupported.filterNot { it in nestedStyleWaivers }
        println("  [conv style] variants=${result.variants.size} waived(nested)=${nestedStyleWaivers.size} stillUnsupported=$stillUnsupported")

        assertTrue("expected >= 55 style variants, was ${result.variants.size}", result.variants.size >= 55)
        assertTrue("after Drawable producer + itemStyle waiver, nothing unsupported: $stillUnsupported", stillUnsupported.isEmpty())

        val applied = mutableListOf<String>()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val conv = CometChatConversations(activity)
            result.variants.forEach { v -> runCatching { conv.setStyle(v.instance) }.onSuccess { applied += v.propName } }
        }
        scenario.close()
        assertEquals("every style variant should apply cleanly", result.variants.size, applied.size)
    }
}
