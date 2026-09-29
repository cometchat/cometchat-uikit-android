package com.cometchat.uikit.kotlin.presentation.outgoingcall

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.outgoingcall.style.CometChatOutgoingCallStyle
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
 * ENG-38680 — CometChatOutgoingCall (View) prop matrix (value + style). Infra
 * (ViewModel, whole Style) waived; nested avatarStyle waived. end-call callback +
 * the Call setter are the functional column.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatOutgoingCallPropMatrixTest {

    @After
    fun tearDown() = NonDefaultValues.clearRegistered()

    private val componentWaivers = WaiverSet.parse(
        """
        - owner: CometChatOutgoingCall
          name: ViewModel
          reason: DI/infra injection, not an integrator styling or behaviour prop
          by: ENG-38680
        - owner: CometChatOutgoingCall
          name: Style
          reason: whole-style object; matrixed field-by-field by the CometChatOutgoingCallStyle sweep
          by: ENG-38680
        """.trimIndent()
    )

    private fun isFunctionalPending(paramType: Class<*>): Boolean =
        View::class.java.isAssignableFrom(paramType) ||
            Drawable::class.java.isAssignableFrom(paramType) ||
            Function::class.java.isAssignableFrom(paramType) ||
            List::class.java.isAssignableFrom(paramType) ||
            paramType.isInterface ||
            paramType.simpleName == "Call" ||
            paramType.simpleName.endsWith("Style") ||
            paramType.simpleName.endsWith("RequestBuilder")

    @Test
    fun viewOutgoingCall_mechanicalValueMatrix_coversEveryValueProp() {
        val setters = ViewPropSweep.setters(CometChatOutgoingCall::class.java)
        assertTrue("expected a setter surface, was ${setters.size}", setters.size > 15)

        // @DrawableRes Int setters need a real resolvable id (a garbage int crashes).
        NonDefaultValues.registerProp("CometChatOutgoingCall", "EndCallIcon") { android.R.drawable.ic_menu_call }

        val props = mutableListOf<Prop>()
        val functionalPending = mutableListOf<String>()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val call = CometChatOutgoingCall(activity)
            setters.forEach { setter ->
                when {
                    componentWaivers.isWaived("CometChatOutgoingCall", setter.propName) ->
                        props += Prop("CometChatOutgoingCall", setter.propName, PropKind.VALUE, waived = true)
                    isFunctionalPending(setter.paramType) ->
                        functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                    setter.supported ->
                        props += Prop("CometChatOutgoingCall", setter.propName, PropKind.VALUE, covered = runCatching { setter.applyNonDefault(call) }.isSuccess)
                    else -> functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                }
            }
        }
        scenario.close()

        val cov = Denominator.summarize(props)
        println("  [outgoingcall view VALUE] ${cov.covered}/${cov.total} (${cov.waived} waived); functional-pending=${functionalPending.size}")
        assertTrue("expected >= 8 value props, was ${cov.total}", cov.total >= 8)
        assertEquals("every swept VALUE prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }

    @Test
    fun viewOutgoingCallStyle_matrix_coversEveryStyleProp() {
        NonDefaultValues.register(Drawable::class) { ColorDrawable(0xFF00C0FF.toInt()) }
        // Required @DrawableRes Int fields in the style resolve during applyStyle();
        // give Int a real drawable id so every variant applies cleanly.
        NonDefaultValues.register(Int::class) { android.R.drawable.ic_menu_call }
        val nestedStyleWaivers = setOf("avatarStyle")
        val result = StylePropSweep.sweep(CometChatOutgoingCallStyle::class)
        val stillUnsupported = result.unsupported.filterNot { it in nestedStyleWaivers }
        println("  [outgoingcall style] variants=${result.variants.size} stillUnsupported=$stillUnsupported")
        assertTrue("expected >= 6 style variants, was ${result.variants.size}", result.variants.size >= 6)
        assertTrue("nothing should be unsupported after producer + avatarStyle waiver: $stillUnsupported", stillUnsupported.isEmpty())

        val applied = mutableListOf<String>()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val call = CometChatOutgoingCall(activity)
            result.variants.forEach { v -> runCatching { call.setStyle(v.instance) }.onSuccess { applied += v.propName } }
        }
        scenario.close()
        assertEquals("every style variant should apply cleanly", result.variants.size, applied.size)
    }
}
