package com.cometchat.uikit.kotlin.presentation.shared.messagebubble

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
 * Reflective sweep over the View container's `set*` surface — the widest in the kit.
 *
 * Slot setters take a `View`, and message/style/formatter setters need real objects,
 * so those land in the functional column; the sibling functional test drives them.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatMessageBubblePropMatrixTest {

    @After fun tearDown() = NonDefaultValues.clearRegistered()

    private fun isFunctionalPending(paramType: Class<*>): Boolean =
        View::class.java.isAssignableFrom(paramType) ||
            android.graphics.drawable.Drawable::class.java.isAssignableFrom(paramType) ||
            Function::class.java.isAssignableFrom(paramType) ||
            List::class.java.isAssignableFrom(paramType) ||
            paramType.isInterface ||
            paramType.simpleName == "BaseMessage" ||
            paramType.simpleName == "SimpleDateFormat" ||
            paramType.simpleName == "SpannableString" ||
            paramType.simpleName.endsWith("Style") ||
            paramType.simpleName.endsWith("Styles") ||
            paramType.simpleName.endsWith("Factory") ||
            paramType.simpleName.endsWith("Callback")

    @Test
    fun viewContainer_mechanicalValueMatrix_coversEveryValueProp() {
        val setters = ViewPropSweep.setters(CometChatMessageBubble::class.java)
        assertTrue("expected a wide setter surface, was ${setters.size}", setters.size > 15)

        val props = mutableListOf<Prop>()
        val functionalPending = mutableListOf<String>()

        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val bubble = CometChatMessageBubble(activity)
            setters.forEach { setter ->
                when {
                    isFunctionalPending(setter.paramType) ->
                        functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                    setter.supported ->
                        props += Prop(
                            "CometChatMessageBubble",
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

        println("  [container view VALUE] ${cov.covered}/${cov.total}; functional-pending=${functionalPending.size} $functionalPending")
        if (uncovered.isNotEmpty()) println("  [container view] NOT covered: $uncovered")

        assertEquals("every swept VALUE prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }

    /** The getters must hand back what the setters stored, or a host cannot read its own config. */
    @Test
    fun configurationSettersRoundTrip() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val bubble = CometChatMessageBubble(activity)

            bubble.setTimeStampAlignment(com.cometchat.uikit.core.constants.UIKitConstants.TimeStampAlignment.TOP)
            assertEquals(
                com.cometchat.uikit.core.constants.UIKitConstants.TimeStampAlignment.TOP,
                bubble.getTimeStampAlignment(),
            )

            bubble.setTextFormatters(emptyList())
            assertEquals(emptyList<Any>(), bubble.getTextFormatters())

            bubble.setBubbleFactory(null)
            assertEquals(null, bubble.getBubbleFactory())

            bubble.setTimeFormat(null)
            assertEquals(null, bubble.getTimeFormat())

            bubble.setDateTimeFormatter(null)
            assertEquals(null, bubble.getDateTimeFormatter())
        }
        scenario.close()
    }
}
