package com.cometchat.uikit.kotlin.presentation.shared.dialog

import android.graphics.drawable.Drawable
import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.R
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
 * ENG-38681 — CometChatConfirmDialog (View) matrix + functional. It is a Dialog,
 * so its views inflate on show(); the matrix drives the shown dialog. Functional:
 * the positive/negative click listeners fire on the respective button taps.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatConfirmDialogTest {

    @After
    fun tearDown() = NonDefaultValues.clearRegistered()

    private val waivers = WaiverSet.parse(
        """
        - owner: CometChatConfirmDialog
          name: Style
          reason: whole-style object; matrixed by the CometChatConfirmDialogStyle sweep
          by: ENG-38681
        """.trimIndent()
    )

    private fun pending(p: Class<*>): Boolean =
        View::class.java.isAssignableFrom(p) || Drawable::class.java.isAssignableFrom(p) ||
            Function::class.java.isAssignableFrom(p) || p.isInterface || p.simpleName.endsWith("Style")

    @Test
    fun viewConfirmDialog_matrix_value_and_style() {
        // @StyleRes text-appearance setters need a resolvable style, not a garbage int.
        listOf("TitleTextAppearance", "SubtitleTextAppearance", "PositiveButtonTextAppearance", "NegativeButtonTextAppearance")
            .forEach { NonDefaultValues.registerProp("CometChatConfirmDialog", it) { android.R.style.TextAppearance_Material_Body1 } }
        // Style sweep: all Int fields resolve to a real style id (safe as color/dimension too).
        NonDefaultValues.register(Int::class) { android.R.style.TextAppearance_Material_Body1 }
        NonDefaultValues.register(Drawable::class) { android.graphics.drawable.ColorDrawable(0xFF00C0FF.toInt()) }

        val setters = ViewPropSweep.setters(CometChatConfirmDialog::class.java)
        val props = mutableListOf<Prop>()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val dialog = CometChatConfirmDialog(activity)
            dialog.show() // inflates the content view (lateinit fields)
            setters.forEach { s ->
                when {
                    waivers.isWaived("CometChatConfirmDialog", s.propName) -> props += Prop("CometChatConfirmDialog", s.propName, PropKind.VALUE, waived = true)
                    pending(s.paramType) -> {}
                    s.supported -> props += Prop("CometChatConfirmDialog", s.propName, PropKind.VALUE, covered = runCatching { s.applyNonDefault(dialog) }.isSuccess)
                }
            }
            val style = StylePropSweep.sweep(CometChatConfirmDialogStyle::class)
            println("  [confirmdialog style] variants=${style.variants.size} unsupported=${style.unsupported}")
            assertTrue(style.variants.size >= 4 && style.unsupported.isEmpty())
            style.variants.forEach { dialog.setStyle(it.instance) }
            dialog.dismiss()
        }
        scenario.close()
        val cov = Denominator.summarize(props)
        println("  [confirmdialog view VALUE] ${cov.covered}/${cov.total}")
        assertEquals(cov.total, cov.covered); assertEquals(100.0, cov.percent, 0.0)
    }

    @Test
    fun positiveAndNegativeClicks_fireTheirListeners() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val dialog = CometChatConfirmDialog(activity)
            dialog.show()
            var positive = false; var negative = false
            dialog.setOnPositiveButtonClick { positive = true }
            dialog.setOnNegativeButtonClick { negative = true }

            val btnPositive = CometChatConfirmDialog::class.java.getDeclaredField("btnPositive").apply { isAccessible = true }.get(dialog) as View
            val btnNegative = CometChatConfirmDialog::class.java.getDeclaredField("btnNegative").apply { isAccessible = true }.get(dialog) as View
            btnPositive.performClick()
            btnNegative.performClick()

            assertTrue("positive listener should fire", positive)
            assertTrue("negative listener should fire", negative)
            dialog.dismiss()
        }
        scenario.close()
    }
}
