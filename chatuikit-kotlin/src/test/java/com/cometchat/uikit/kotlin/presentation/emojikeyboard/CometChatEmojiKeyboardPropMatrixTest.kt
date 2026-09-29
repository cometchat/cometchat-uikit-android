package com.cometchat.uikit.kotlin.presentation.emojikeyboard

import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.emojikeyboard.ui.CometChatEmojiKeyboard
import com.cometchat.uikit.kotlin.presentation.emojikeyboard.ui.EmojiKeyBoardView
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
 * Prop matrix for the emoji keyboard.
 *
 * The sweep targets [EmojiKeyBoardView], not [CometChatEmojiKeyboard]: the public
 * entry point is a `BottomSheetDialogFragment` whose two setters are null-safe
 * forwards to the view it hosts, so on a bare fragment instance they are no-ops and a
 * sweep over them would be an assertion that cannot fail. The view is where the nine
 * real props live, and it is what the fragment is a lifecycle wrapper around.
 *
 * The forwarding contract itself is pinned separately below, since "inert until the
 * view exists" is a real property of the public API and not an accident.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatEmojiKeyboardPropMatrixTest {

    @After fun tearDown() = NonDefaultValues.clearRegistered()

    private fun isFunctionalPending(paramType: Class<*>): Boolean =
        View::class.java.isAssignableFrom(paramType) ||
            android.graphics.drawable.Drawable::class.java.isAssignableFrom(paramType) ||
            Function::class.java.isAssignableFrom(paramType) ||
            List::class.java.isAssignableFrom(paramType) ||
            paramType.isInterface ||
            paramType.simpleName.endsWith("Style")

    /** Resource-id setters need a resolvable id; a random int is not one. */
    private fun registerResourceIds() {
        NonDefaultValues.registerProp("EmojiKeyBoardView", "Style") {
            R.style.CometChatTheme_DayNight
        }
        NonDefaultValues.registerProp("EmojiKeyBoardView", "CategoryTextAppearance") {
            android.R.style.TextAppearance_Material_Body1
        }
    }

    @Test
    fun mechanicalValueMatrix_coversEveryValueProp() {
        registerResourceIds()
        val setters = ViewPropSweep.setters(EmojiKeyBoardView::class.java)
        assertTrue("expected a setter surface", setters.isNotEmpty())

        val props = mutableListOf<Prop>()
        val functionalPending = mutableListOf<String>()

        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val view = EmojiKeyBoardView(activity)
            setters.forEach { setter ->
                when {
                    isFunctionalPending(setter.paramType) ->
                        functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                    setter.supported ->
                        props += Prop(
                            "EmojiKeyBoardView",
                            setter.propName,
                            PropKind.VALUE,
                            covered = runCatching { setter.applyNonDefault(view) }.isSuccess,
                        )
                    else -> functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                }
            }
        }
        scenario.close()

        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [emojikeyboard view VALUE] ${cov.covered}/${cov.total}; functional-pending=${functionalPending.size} $functionalPending")
        if (uncovered.isNotEmpty()) println("  [emojikeyboard view] NOT covered: $uncovered")

        assertEquals("every swept VALUE prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }

    /**
     * The fragment's setters forward to a view that does not exist until
     * `onCreateView`. Calling them early is deliberately harmless rather than a crash
     * — hosts configure the sheet before showing it — but it also means the call is
     * dropped, which is the part worth having on record.
     */
    @Test
    fun theFragmentSettersAreInertBeforeTheViewExists() {
        val keyboard = CometChatEmojiKeyboard()
        keyboard.setStyle(R.style.CometChatTheme_DayNight)
        keyboard.setOnClick(null)
        // No view yet, so nothing to assert beyond the absence of a crash: the point is
        // that a host may call these before show() without guarding.
    }
}
