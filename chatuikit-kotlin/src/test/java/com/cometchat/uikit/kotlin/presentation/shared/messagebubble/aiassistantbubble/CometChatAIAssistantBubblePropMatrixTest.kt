package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.aiassistantbubble

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
 * Property (prop-matrix) layer for the **View** [CometChatAIAssistantBubble].
 *
 * Reflective sweep over the `set*` surface applied to a real themed instance.
 * `setMessage`, `setStreamMessage` and `setAIStreamService` need real domain objects
 * or a live stream and fall to the functional column.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatAIAssistantBubblePropMatrixTest {

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
            paramType.simpleName == "AIAssistantMessage" ||
            paramType.simpleName == "StreamMessage" ||
            paramType.simpleName == "CometChatAIStreamService" ||
            paramType.simpleName == "SpannableString" ||
            paramType.simpleName.endsWith("Style") ||
            paramType.simpleName.endsWith("Data")

    @Test
    fun viewTextBubble_mechanicalValueMatrix_coversEveryValueProp() {
        val setters = ViewPropSweep.setters(CometChatAIAssistantBubble::class.java)
        // A smaller surface than the media bubbles: 9 setters, most of them styling.
        assertTrue("expected a setter surface, was ${setters.size}", setters.size >= 8)

        val props = mutableListOf<Prop>()
        val functionalPending = mutableListOf<String>()

        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val bubble = CometChatAIAssistantBubble(activity)
            setters.forEach { setter ->
                when {
                    isFunctionalPending(setter.paramType) ->
                        functionalPending += "${setter.propName}(${setter.paramType.simpleName})"

                    setter.supported ->
                        props += Prop(
                            "CometChatAIAssistantBubble",
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

        println("  [aiassistantbubble view VALUE] ${cov.covered}/${cov.total}; functional-pending=${functionalPending.size} $functionalPending")
        if (uncovered.isNotEmpty()) println("  [aiassistantbubble view] NOT covered: $uncovered")

        assertTrue("expected a real value surface, was ${cov.total}", cov.total >= 3)
        assertEquals("every swept VALUE prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }
}
