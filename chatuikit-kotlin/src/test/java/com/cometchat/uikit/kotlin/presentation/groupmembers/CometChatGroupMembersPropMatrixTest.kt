package com.cometchat.uikit.kotlin.presentation.groupmembers

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.groupmembers.style.CometChatGroupMembersStyle
import com.cometchat.uikit.kotlin.presentation.groupmembers.ui.CometChatGroupMembers
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
 * ENG-38679 — CometChatGroupMembers (View) prop matrix (value + style). Same model
 * as the other lists; infra (ViewModel, whole Style) waived; nested avatarStyle +
 * statusIndicatorStyle waived in the style sweep (matrixed by their own components).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatGroupMembersPropMatrixTest {

    @After
    fun tearDown() = NonDefaultValues.clearRegistered()

    private val componentWaivers = WaiverSet.parse(
        """
        - owner: CometChatGroupMembers
          name: ViewModel
          reason: DI/infra injection, not an integrator styling or behaviour prop
          by: ENG-38679
        - owner: CometChatGroupMembers
          name: Style
          reason: whole-style object; matrixed field-by-field by the CometChatGroupMembersStyle sweep
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
            paramType.simpleName.endsWith("RequestBuilder") ||
            paramType.simpleName == "Group"

    @Test
    fun viewGroupMembers_mechanicalValueMatrix_coversEveryValueProp() {
        val setters = ViewPropSweep.setters(CometChatGroupMembers::class.java)
        assertTrue("expected a large setter surface, was ${setters.size}", setters.size > 40)

        val props = mutableListOf<Prop>()
        val functionalPending = mutableListOf<String>()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val gm = CometChatGroupMembers(activity)
            setters.forEach { setter ->
                when {
                    componentWaivers.isWaived("CometChatGroupMembers", setter.propName) ->
                        props += Prop("CometChatGroupMembers", setter.propName, PropKind.VALUE, waived = true)
                    isFunctionalPending(setter.paramType) ->
                        functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                    setter.supported ->
                        props += Prop("CometChatGroupMembers", setter.propName, PropKind.VALUE, covered = runCatching { setter.applyNonDefault(gm) }.isSuccess)
                    else -> functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                }
            }
        }
        scenario.close()

        val cov = Denominator.summarize(props)
        println("  [gm view VALUE] ${cov.covered}/${cov.total} (${cov.waived} waived); functional-pending=${functionalPending.size}")
        assertTrue("expected >= 25 value props, was ${cov.total}", cov.total >= 25)
        assertEquals("every swept VALUE prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }

    @Test
    fun viewGroupMembersStyle_matrix_coversEveryStyleProp() {
        NonDefaultValues.register(Drawable::class) { ColorDrawable(0xFF00C0FF.toInt()) }
        val nestedStyleWaivers = setOf("avatarStyle", "statusIndicatorStyle")
        val result = StylePropSweep.sweep(CometChatGroupMembersStyle::class)
        val stillUnsupported = result.unsupported.filterNot { it in nestedStyleWaivers }
        println("  [gm style] variants=${result.variants.size} stillUnsupported=$stillUnsupported")
        assertTrue("expected >= 20 style variants, was ${result.variants.size}", result.variants.size >= 20)
        assertTrue("nothing should be unsupported after producer + nested waivers: $stillUnsupported", stillUnsupported.isEmpty())

        val applied = mutableListOf<String>()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val gm = CometChatGroupMembers(activity)
            result.variants.forEach { v -> runCatching { gm.setStyle(v.instance) }.onSuccess { applied += v.propName } }
        }
        scenario.close()
        assertEquals("every style variant should apply cleanly", result.variants.size, applied.size)
    }
}
