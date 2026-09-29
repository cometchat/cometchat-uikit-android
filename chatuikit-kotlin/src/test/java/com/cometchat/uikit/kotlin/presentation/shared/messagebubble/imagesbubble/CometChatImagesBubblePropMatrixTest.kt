package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.imagesbubble

import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.shared.messagebubble.videosbubble.CometChatVideosBubble
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
 * Reflective sweep over [CometChatImagesBubble] — the bubble an image message
 * actually reaches, since the renderer picks it whenever multiple attachments are
 * enabled and the singular bubble is the off path.
 *
 * [CometChatVideosBubble] extends it and adds no setters of its own, so the same
 * surface is swept on both to catch a future divergence.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatImagesBubblePropMatrixTest {

    @After fun tearDown() = NonDefaultValues.clearRegistered()

    private fun isFunctionalPending(paramType: Class<*>): Boolean =
        View::class.java.isAssignableFrom(paramType) ||
            android.graphics.drawable.Drawable::class.java.isAssignableFrom(paramType) ||
            Function::class.java.isAssignableFrom(paramType) ||
            List::class.java.isAssignableFrom(paramType) ||
            paramType.isInterface ||
            paramType.simpleName == "MediaMessage" ||
            paramType.simpleName.endsWith("Style") ||
            paramType.simpleName.endsWith("Alignment")

    @Test
    fun viewImagesBubble_mechanicalValueMatrix_coversEveryValueProp() {
        val setters = ViewPropSweep.setters(CometChatImagesBubble::class.java)
        assertTrue("expected a setter surface, was ${setters.size}", setters.isNotEmpty())

        val props = mutableListOf<Prop>()
        val functionalPending = mutableListOf<String>()

        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val bubble = CometChatImagesBubble(activity)
            setters.forEach { setter ->
                when {
                    isFunctionalPending(setter.paramType) ->
                        functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                    setter.supported ->
                        props += Prop(
                            "CometChatImagesBubble",
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

        println("  [imagesbubble view VALUE] ${cov.covered}/${cov.total}; functional-pending=${functionalPending.size} $functionalPending")
        if (uncovered.isNotEmpty()) println("  [imagesbubble view] NOT covered: $uncovered")

        assertEquals("every swept VALUE prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }

    /**
     * The videos bubble subclasses the images bubble and overrides only `decorateTile`,
     * so it declares no setters of its own and the sweep above covers it too.
     *
     * Worth knowing: `ViewPropSweep` reflects over *declared* methods, so an inherited
     * surface reads as empty. A subclass that gained its own setter would still show
     * up here and need a matrix entry of its own.
     */
    @Test
    fun theVideosBubbleDeclaresNoSetterSurfaceOfItsOwn() {
        assertTrue(
            "the videos bubble should subclass the images bubble",
            CometChatImagesBubble::class.java.isAssignableFrom(CometChatVideosBubble::class.java),
        )
        val declared = ViewPropSweep.setters(CometChatVideosBubble::class.java).map { it.propName }
        assertEquals(
            "the videos bubble gained setters; give it its own matrix entries. Found: $declared",
            emptyList<String>(),
            declared,
        )
    }
}
