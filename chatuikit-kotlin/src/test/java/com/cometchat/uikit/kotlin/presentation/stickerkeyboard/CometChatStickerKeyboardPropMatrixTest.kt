package com.cometchat.uikit.kotlin.presentation.stickerkeyboard

import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.stickerkeyboard.ui.CometChatStickerKeyboard
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
 * Reflective sweep over the View sticker keyboard's `set*` surface on a real themed
 * instance — the first prop matrix anywhere in the composer family, in either toolkit.
 *
 * The state-view and listener setters are functional-pending by the usual rule (a View,
 * a Drawable or an interface cannot be applied non-default mechanically); the rendering
 * and interaction layers drive those. What is left is the colour, text-appearance and
 * string surface, which is where a keyboard's styling actually lives.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatStickerKeyboardPropMatrixTest {

    @After fun tearDown() = NonDefaultValues.clearRegistered()

    private fun isFunctionalPending(paramType: Class<*>): Boolean =
        View::class.java.isAssignableFrom(paramType) ||
            android.graphics.drawable.Drawable::class.java.isAssignableFrom(paramType) ||
            Function::class.java.isAssignableFrom(paramType) ||
            List::class.java.isAssignableFrom(paramType) ||
            paramType.isInterface ||
            paramType.simpleName.endsWith("Style")

    /**
     * The three state-view setters are overloaded: a `View` overload, which is
     * functional-pending, and a `@LayoutRes Int` one, which is not — but a random int
     * is not a resolvable layout, so the sweep would inflate garbage. Register a real
     * id for each; the harness only hands an override to the overload whose type
     * accepts it, so the `View` overloads are unaffected.
     */
    private fun registerLayoutIds() {
        listOf("EmptyStateView", "ErrorStateView", "LoadingStateView").forEach {
            NonDefaultValues.registerProp("CometChatStickerKeyboard", it) {
                android.R.layout.simple_list_item_1
            }
        }
    }

    @Test
    fun mechanicalValueMatrix_coversEveryValueProp() {
        registerLayoutIds()
        val setters = ViewPropSweep.setters(CometChatStickerKeyboard::class.java)
        assertTrue("expected a setter surface", setters.isNotEmpty())

        val props = mutableListOf<Prop>()
        val functionalPending = mutableListOf<String>()

        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val keyboard = CometChatStickerKeyboard(activity)
            setters.forEach { setter ->
                when {
                    isFunctionalPending(setter.paramType) ->
                        functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                    setter.supported ->
                        props += Prop(
                            "CometChatStickerKeyboard",
                            setter.propName,
                            PropKind.VALUE,
                            covered = runCatching { setter.applyNonDefault(keyboard) }.isSuccess,
                        )
                    else -> functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                }
            }
        }
        scenario.close()

        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [stickerkeyboard view VALUE] ${cov.covered}/${cov.total}; functional-pending=${functionalPending.size} $functionalPending")
        if (uncovered.isNotEmpty()) println("  [stickerkeyboard view] NOT covered: $uncovered")

        assertEquals("every swept VALUE prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }
}
