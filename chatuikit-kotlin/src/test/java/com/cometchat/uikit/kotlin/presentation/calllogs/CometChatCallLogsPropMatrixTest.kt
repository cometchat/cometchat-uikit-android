package com.cometchat.uikit.kotlin.presentation.calllogs

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.calllogs.style.CometChatCallLogsStyle
import com.cometchat.uikit.kotlin.presentation.calllogs.ui.CometChatCallLogs
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
 * ENG-38680 — CometChatCallLogs (View) prop matrix (value + style). CallLogs is a
 * VM-backed list; same model as the ENG-38679 lists. Infra (ViewModel, whole
 * Style) waived; nested itemStyle waived in the style sweep. The Calls SDK is only
 * used by the VM's fetch (not exercised here) — the matrix asserts the UI Kit's
 * own prop surface without a live calls runtime.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatCallLogsPropMatrixTest {

    @After
    fun tearDown() = NonDefaultValues.clearRegistered()

    private val componentWaivers = WaiverSet.parse(
        """
        - owner: CometChatCallLogs
          name: ViewModel
          reason: DI/infra injection, not an integrator styling or behaviour prop
          by: ENG-38680
        - owner: CometChatCallLogs
          name: Style
          reason: whole-style object; matrixed field-by-field by the CometChatCallLogsStyle sweep
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
            paramType.simpleName.endsWith("RequestBuilder")

    @Test
    fun viewCallLogs_mechanicalValueMatrix_coversEveryValueProp() {
        val setters = ViewPropSweep.setters(CometChatCallLogs::class.java)
        assertTrue("expected a setter surface, was ${setters.size}", setters.size > 25)

        val props = mutableListOf<Prop>()
        val functionalPending = mutableListOf<String>()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val logs = CometChatCallLogs(activity)
            setters.forEach { setter ->
                when {
                    componentWaivers.isWaived("CometChatCallLogs", setter.propName) ->
                        props += Prop("CometChatCallLogs", setter.propName, PropKind.VALUE, waived = true)
                    isFunctionalPending(setter.paramType) ->
                        functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                    setter.supported ->
                        props += Prop("CometChatCallLogs", setter.propName, PropKind.VALUE, covered = runCatching { setter.applyNonDefault(logs) }.isSuccess)
                    else -> functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                }
            }
        }
        scenario.close()

        val cov = Denominator.summarize(props)
        println("  [calllogs view VALUE] ${cov.covered}/${cov.total} (${cov.waived} waived); functional-pending=${functionalPending.size}")
        assertTrue("expected >= 12 value props, was ${cov.total}", cov.total >= 12)
        assertEquals("every swept VALUE prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }

    @Test
    fun viewCallLogsStyle_matrix_coversEveryStyleProp() {
        NonDefaultValues.register(Drawable::class) { ColorDrawable(0xFF00C0FF.toInt()) }
        val nestedStyleWaivers = setOf("itemStyle")
        val result = StylePropSweep.sweep(CometChatCallLogsStyle::class)
        val stillUnsupported = result.unsupported.filterNot { it in nestedStyleWaivers }
        println("  [calllogs style] variants=${result.variants.size} stillUnsupported=$stillUnsupported")
        assertTrue("expected >= 10 style variants, was ${result.variants.size}", result.variants.size >= 10)
        assertTrue("nothing should be unsupported after producer + itemStyle waiver: $stillUnsupported", stillUnsupported.isEmpty())

        val applied = mutableListOf<String>()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val logs = CometChatCallLogs(activity)
            result.variants.forEach { v -> runCatching { logs.setStyle(v.instance) }.onSuccess { applied += v.propName } }
        }
        scenario.close()
        assertEquals("every style variant should apply cleanly", result.variants.size, applied.size)
    }
}
