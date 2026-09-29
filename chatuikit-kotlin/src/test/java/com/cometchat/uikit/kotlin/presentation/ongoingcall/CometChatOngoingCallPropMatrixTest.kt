package com.cometchat.uikit.kotlin.presentation.ongoingcall

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.ongoingcall.style.CometChatOngoingCallStyle
import com.cometchat.uikit.kotlin.presentation.ongoingcall.ui.CometChatOngoingCall
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.NonDefaultValues
import com.cometchat.uikit.propmatrix.Prop
import com.cometchat.uikit.propmatrix.PropKind
import com.cometchat.uikit.propmatrix.StylePropSweep
import com.cometchat.uikit.propmatrix.ViewPropSweep
import com.cometchat.uikit.propmatrix.WaiverSet
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * ENG-38680 — CometChatOngoingCall (View), Phase 4 (Calls-SDK boundary). Thin prop
 * matrix (value + style) + a construction regression net: the component builds and
 * accepts session config without a live calls runtime (the ClassNotFoundException
 * contract). Goldens deferred (statusBarColor bug, Track 1/T5).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatOngoingCallPropMatrixTest {

    @After
    fun tearDown() = NonDefaultValues.clearRegistered()

    private val componentWaivers = WaiverSet.parse(
        """
        - owner: CometChatOngoingCall
          name: Style
          reason: whole-style object; matrixed field-by-field by the CometChatOngoingCallStyle sweep
          by: ENG-38680
        """.trimIndent()
    )

    private fun isFunctionalPending(paramType: Class<*>): Boolean =
        View::class.java.isAssignableFrom(paramType) ||
            Drawable::class.java.isAssignableFrom(paramType) ||
            Function::class.java.isAssignableFrom(paramType) ||
            List::class.java.isAssignableFrom(paramType) ||
            paramType.isInterface ||
            paramType.simpleName.endsWith("Style") ||
            paramType.simpleName.endsWith("Builder")

    @Test
    fun viewOngoingCall_mechanicalValueMatrix_coversEveryValueProp() {
        val setters = ViewPropSweep.setters(CometChatOngoingCall::class.java)
        val props = mutableListOf<Prop>()
        val functionalPending = mutableListOf<String>()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val call = CometChatOngoingCall(activity)
            setters.forEach { setter ->
                when {
                    componentWaivers.isWaived("CometChatOngoingCall", setter.propName) ->
                        props += Prop("CometChatOngoingCall", setter.propName, PropKind.VALUE, waived = true)
                    isFunctionalPending(setter.paramType) ->
                        functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                    setter.supported ->
                        props += Prop("CometChatOngoingCall", setter.propName, PropKind.VALUE, covered = runCatching { setter.applyNonDefault(call) }.isSuccess)
                    else -> functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                }
            }
        }
        scenario.close()

        val cov = Denominator.summarize(props)
        println("  [ongoingcall view VALUE] ${cov.covered}/${cov.total} (${cov.waived} waived); functional-pending=$functionalPending")
        assertTrue("expected >= 2 value props, was ${cov.total}", cov.total >= 2)
        assertEquals("every swept VALUE prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }

    @Test
    fun viewOngoingCallStyle_matrix_coversEveryStyleProp() {
        NonDefaultValues.register(Drawable::class) { ColorDrawable(0xFF00C0FF.toInt()) }
        val result = StylePropSweep.sweep(CometChatOngoingCallStyle::class)
        println("  [ongoingcall style] variants=${result.variants.size} unsupported=${result.unsupported}")
        assertTrue("expected >= 4 style variants, was ${result.variants.size}", result.variants.size >= 4)
        assertTrue("nothing should be unsupported: ${result.unsupported}", result.unsupported.isEmpty())

        val applied = mutableListOf<String>()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val call = CometChatOngoingCall(activity)
            result.variants.forEach { v -> runCatching { call.setStyle(v.instance) }.onSuccess { applied += v.propName } }
        }
        scenario.close()
        assertEquals("every style variant should apply cleanly", result.variants.size, applied.size)
    }

    @Test
    fun constructsAndAcceptsSessionConfig_withoutCallsRuntime() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            // Constructs (creates a SessionSettingsBuilder internally) and accepts
            // session config without crashing at the Calls-SDK boundary.
            val call = CometChatOngoingCall(activity)
            call.setSessionId("session-1")
            call.setCallType("audio")
            assertNotNull("OngoingCall should construct without a live calls runtime", call)
        }
        scenario.close()
    }
}
