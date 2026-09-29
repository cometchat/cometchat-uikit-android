package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.textbubble

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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Property (prop-matrix) layer for the **View** [CometChatTextBubble].
 *
 * The View flavour of the harness works by reflection: [ViewPropSweep] enumerates
 * the `set*` surface, produces a non-default value per parameter type and applies
 * it to a real, themed instance. That is the opposite of the Compose flavour, whose
 * value-class styles cannot be constructed reflectively and so need the hand-written
 * DSL — see `CometChatTextBubbleComposePropMatrixTest`.
 *
 * Everything the sweep cannot produce a value for lands in the functional column and
 * is exercised by the sibling functional test instead: `setMessage` needs a real
 * `TextMessage` plus an alignment, `setLinkPreview` a populated preview, and the
 * `*Style` / `View` / `Drawable` parameters are structural rather than value props.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatTextBubblePropMatrixTest {

    @After
    fun tearDown() = NonDefaultValues.clearRegistered()

    /**
     * Params the mechanical sweep cannot meaningfully drive — they need a real
     * message, a real view, or a style object. Reported separately rather than
     * counted as covered.
     */
    private fun isFunctionalPending(paramType: Class<*>): Boolean =
        View::class.java.isAssignableFrom(paramType) ||
            android.graphics.drawable.Drawable::class.java.isAssignableFrom(paramType) ||
            Function::class.java.isAssignableFrom(paramType) ||
            List::class.java.isAssignableFrom(paramType) ||
            paramType.isInterface ||
            paramType.simpleName == "TextMessage" ||
            paramType.simpleName == "SpannableString" ||
            paramType.simpleName.endsWith("Style") ||
            paramType.simpleName.endsWith("Data")

    @Test
    fun viewTextBubble_mechanicalValueMatrix_coversEveryValueProp() {
        val setters = ViewPropSweep.setters(CometChatTextBubble::class.java)
        assertTrue("expected a setter surface, was ${setters.size}", setters.size > 10)

        val props = mutableListOf<Prop>()
        val functionalPending = mutableListOf<String>()

        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val bubble = CometChatTextBubble(activity)
            setters.forEach { setter ->
                when {
                    isFunctionalPending(setter.paramType) ->
                        functionalPending += "${setter.propName}(${setter.paramType.simpleName})"

                    setter.supported ->
                        props += Prop(
                            "CometChatTextBubble",
                            setter.propName,
                            PropKind.VALUE,
                            covered = runCatching { setter.applyNonDefault(bubble) }.isSuccess,
                        )

                    else -> functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                }
            }
        }
        scenario.close()

        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [textbubble view VALUE] ${cov.covered}/${cov.total}; functional-pending=${functionalPending.size} $functionalPending")
        if (uncovered.isNotEmpty()) println("  [textbubble view] NOT covered: $uncovered")

        assertTrue("expected a real value surface, was ${cov.total}", cov.total >= 8)
        assertEquals("every swept VALUE prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }
}
