package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.audiobubble

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
 * Property (prop-matrix) layer for the **View** [CometChatAudioBubble].
 *
 * Reflective sweep over the `set*` surface applied to a real themed instance. This
 * bubble has the widest colour surface of any audio component — play and pause tints,
 * button fill, and three separate wave colours (`audioWave`, `playedWave`,
 * `unplayedWave`) — so the sweep is worth more here than the assertion count suggests:
 * each of those setters also writes through to a child view, not just to the style
 * copy, and a setter that only updated the data class would still pass a naive test.
 *
 * `setMessage` / `setAudioUrl` need real media and belong to the functional column;
 * the sibling functional test drives them.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatAudioBubblePropMatrixTest {

    @After
    fun tearDown() = NonDefaultValues.clearRegistered()

    /** Params the mechanical sweep cannot meaningfully drive — reported, not counted. */
    private fun isFunctionalPending(paramType: Class<*>): Boolean =
        View::class.java.isAssignableFrom(paramType) ||
            android.graphics.drawable.Drawable::class.java.isAssignableFrom(paramType) ||
            Function::class.java.isAssignableFrom(paramType) ||
            List::class.java.isAssignableFrom(paramType) ||
            paramType.isInterface ||
            paramType.simpleName == "MediaMessage" ||
            paramType.simpleName == "File" ||
            paramType.simpleName == "SpannableString" ||
            paramType.simpleName.endsWith("Style") ||
            paramType.simpleName.endsWith("Data")

    @Test
    fun viewAudioBubble_mechanicalValueMatrix_coversEveryValueProp() {
        val setters = ViewPropSweep.setters(CometChatAudioBubble::class.java)
        assertTrue("expected a setter surface, was ${setters.size}", setters.size > 8)

        val props = mutableListOf<Prop>()
        val functionalPending = mutableListOf<String>()

        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val bubble = CometChatAudioBubble(activity)
            setters.forEach { setter ->
                when {
                    isFunctionalPending(setter.paramType) ->
                        functionalPending += "${setter.propName}(${setter.paramType.simpleName})"

                    setter.supported ->
                        props += Prop(
                            "CometChatAudioBubble",
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

        println("  [audiobubble view VALUE] ${cov.covered}/${cov.total}; functional-pending=${functionalPending.size} $functionalPending")
        if (uncovered.isNotEmpty()) println("  [audiobubble view] NOT covered: $uncovered")

        assertTrue("expected a real value surface, was ${cov.total}", cov.total >= 8)
        assertEquals("every swept VALUE prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }

    /**
     * The colour setters must reach the views, not only the style copy — otherwise a
     * bubble restyled after construction keeps painting its old colours.
     */
    @Test
    fun colourSetters_writeThroughToTheViewsAndBack() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val bubble = CometChatAudioBubble(activity)

            bubble.setPlayIconTint(0xFF102030.toInt())
            bubble.setPauseIconTint(0xFF203040.toInt())
            bubble.setButtonTint(0xFF304050.toInt())
            bubble.setAudioWaveColor(0xFF405060.toInt())
            bubble.setPlayedWaveColor(0xFF506070.toInt())
            bubble.setUnplayedWaveColor(0xFF607080.toInt())
            bubble.setSubtitleTextColor(0xFF708090.toInt())

            assertEquals(0xFF102030.toInt(), bubble.getPlayIconTint())
            assertEquals(0xFF203040.toInt(), bubble.getPauseIconTint())
            assertEquals(0xFF304050.toInt(), bubble.getButtonTint())
            assertEquals(0xFF405060.toInt(), bubble.getAudioWaveColor())
            assertEquals(0xFF506070.toInt(), bubble.getPlayedWaveColor())
            assertEquals(0xFF607080.toInt(), bubble.getUnplayedWaveColor())
            assertEquals(0xFF708090.toInt(), bubble.getSubtitleTextColor())
            assertEquals(
                "the subtitle view should carry the colour, not just the style",
                0xFF708090.toInt(),
                bubble.getSubtitle().currentTextColor,
            )
        }
        scenario.close()
    }

    /**
     * `setSubtitleTextColor` and `setSubtitleTextAppearance` both guard on 0 — the
     * "unset" value an unresolved theme attribute produces. Passing 0 must leave the
     * existing colour alone rather than painting the subtitle transparent-black.
     */
    @Test
    fun zeroIsTreatedAsUnsetRatherThanAsBlack() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val bubble = CometChatAudioBubble(activity)

            bubble.setSubtitleTextColor(0xFF708090.toInt())
            bubble.setSubtitleTextColor(0)
            assertEquals(
                "0 should be ignored, not applied",
                0xFF708090.toInt(),
                bubble.getSubtitleTextColor(),
            )

            val appearance = bubble.getSubtitleTextAppearance()
            bubble.setSubtitleTextAppearance(0)
            assertEquals(appearance, bubble.getSubtitleTextAppearance())
        }
        scenario.close()
    }
}
