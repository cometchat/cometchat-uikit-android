package com.cometchat.uikit.kotlin.presentation.callbuttons

import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.NonDefaultValues
import com.cometchat.uikit.propmatrix.Prop
import com.cometchat.uikit.propmatrix.PropKind
import com.cometchat.uikit.propmatrix.ViewPropSweep
import com.cometchat.uikit.propmatrix.WaiverSet
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * ENG-38680 — CometChatCallButtons (View) prop matrix (value). CallButtons styles
 * via individual setters (no *Style data class), so the matrix is the setter
 * sweep. Callbacks + entity setters (User/Group) are the functional column. The
 * VM (call initiation) is waived; no live calls runtime is exercised.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatCallButtonsPropMatrixTest {

    @After
    fun tearDown() = NonDefaultValues.clearRegistered()

    private val componentWaivers = WaiverSet.parse(
        """
        - owner: CometChatCallButtons
          name: ViewModel
          reason: DI/infra injection (call initiation), not an integrator styling or behaviour prop
          by: ENG-38680
        """.trimIndent()
    )

    private fun isFunctionalPending(paramType: Class<*>): Boolean =
        View::class.java.isAssignableFrom(paramType) ||
            android.graphics.drawable.Drawable::class.java.isAssignableFrom(paramType) ||
            Function::class.java.isAssignableFrom(paramType) ||
            List::class.java.isAssignableFrom(paramType) ||
            paramType.isInterface ||
            paramType.simpleName == "User" ||
            paramType.simpleName == "Group" ||
            paramType.simpleName.endsWith("Style") ||
            paramType.simpleName.endsWith("RequestBuilder")

    @Test
    fun viewCallButtons_mechanicalValueMatrix_coversEveryValueProp() {
        val setters = ViewPropSweep.setters(com.cometchat.uikit.kotlin.presentation.callbuttons.CometChatCallButtons::class.java)
        assertTrue("expected a setter surface, was ${setters.size}", setters.size > 20)

        val props = mutableListOf<Prop>()
        val functionalPending = mutableListOf<String>()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val buttons = com.cometchat.uikit.kotlin.presentation.callbuttons.CometChatCallButtons(activity)
            setters.forEach { setter ->
                when {
                    componentWaivers.isWaived("CometChatCallButtons", setter.propName) ->
                        props += Prop("CometChatCallButtons", setter.propName, PropKind.VALUE, waived = true)
                    isFunctionalPending(setter.paramType) ->
                        functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                    setter.supported ->
                        props += Prop("CometChatCallButtons", setter.propName, PropKind.VALUE, covered = runCatching { setter.applyNonDefault(buttons) }.isSuccess)
                    else -> functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                }
            }
        }
        scenario.close()

        val cov = Denominator.summarize(props)
        println("  [callbuttons view VALUE] ${cov.covered}/${cov.total} (${cov.waived} waived); functional-pending=${functionalPending.size}")
        assertTrue("expected >= 15 value props, was ${cov.total}", cov.total >= 15)
        assertEquals("every swept VALUE prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }
}
