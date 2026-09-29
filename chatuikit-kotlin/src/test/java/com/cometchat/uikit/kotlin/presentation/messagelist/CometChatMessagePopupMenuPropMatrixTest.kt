package com.cometchat.uikit.kotlin.presentation.messagelist

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
 * Reflective sweep over the View message popup menu's `set*` surface.
 *
 * The menu is not a View — it is a plain class over a `PopupWindow` — but the sweep
 * only needs declared setters and an instance, and this one takes a Context. The
 * list-typed and listener-typed setters are functional-pending by the usual rule;
 * [CometChatMessagePopupMenuFunctionalTest] drives every one of those against a
 * shown popup, so nothing here is left unclaimed by some layer.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatMessagePopupMenuPropMatrixTest {

    @After fun tearDown() = NonDefaultValues.clearRegistered()

    private fun isFunctionalPending(paramType: Class<*>): Boolean =
        View::class.java.isAssignableFrom(paramType) ||
            android.graphics.drawable.Drawable::class.java.isAssignableFrom(paramType) ||
            Function::class.java.isAssignableFrom(paramType) ||
            List::class.java.isAssignableFrom(paramType) ||
            paramType.isInterface ||
            paramType.simpleName.endsWith("Style")

    /** Resource-id setters need a resolvable id; the harness's default int is not one. */
    private fun registerResourceIds() {
        NonDefaultValues.registerProp("CometChatMessagePopupMenu", "Style") {
            R.style.CometChatTheme_DayNight
        }
        NonDefaultValues.registerProp("CometChatMessagePopupMenu", "AddReactionIcon") {
            android.R.drawable.ic_menu_add
        }
    }

    @Test
    fun mechanicalValueMatrix_coversEveryValueProp() {
        registerResourceIds()
        val setters = ViewPropSweep.setters(CometChatMessagePopupMenu::class.java)
        assertTrue("expected a setter surface", setters.isNotEmpty())

        val props = mutableListOf<Prop>()
        val functionalPending = mutableListOf<String>()

        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val menu = CometChatMessagePopupMenu(activity)
            setters.forEach { setter ->
                when {
                    isFunctionalPending(setter.paramType) ->
                        functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                    setter.supported ->
                        props += Prop(
                            "CometChatMessagePopupMenu",
                            setter.propName,
                            PropKind.VALUE,
                            covered = runCatching { setter.applyNonDefault(menu) }.isSuccess,
                        )
                    else -> functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                }
            }
        }
        scenario.close()

        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [messagepopupmenu view VALUE] ${cov.covered}/${cov.total}; functional-pending=${functionalPending.size} $functionalPending")
        if (uncovered.isNotEmpty()) println("  [messagepopupmenu view] NOT covered: $uncovered")

        assertEquals("every swept VALUE prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }
}
