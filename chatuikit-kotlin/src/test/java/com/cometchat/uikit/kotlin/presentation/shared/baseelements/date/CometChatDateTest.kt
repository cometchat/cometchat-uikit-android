package com.cometchat.uikit.kotlin.presentation.shared.baseelements.date

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
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
 * ENG-38681 — CometChatDate (View) matrix + functional. Functional: a fixed
 * timestamp formatted with an explicit pattern reaches the date TextView.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatDateTest {

    @After
    fun tearDown() = NonDefaultValues.clearRegistered()

    private val waivers = WaiverSet.parse(
        """
        - owner: CometChatDate
          name: Style
          reason: whole-style object; matrixed by the CometChatDateStyle sweep
          by: ENG-38681
        """.trimIndent()
    )

    private fun pending(p: Class<*>): Boolean =
        View::class.java.isAssignableFrom(p) || Drawable::class.java.isAssignableFrom(p) ||
            Function::class.java.isAssignableFrom(p) || p.isInterface || p.simpleName.endsWith("Style") || p.simpleName == "DatePattern" || p.simpleName == "Date"

    @Test
    fun viewDate_matrix_value_and_style() {
        NonDefaultValues.register(Drawable::class) { ColorDrawable(0xFF00C0FF.toInt()) }
        // Pattern setters need a valid SimpleDateFormat string, not the generic one.
        listOf("DatePattern", "DayPattern", "TimePattern").forEach {
            NonDefaultValues.registerProp("CometChatDate", it) { "yyyy-MM-dd" }
        }
        val setters = ViewPropSweep.setters(CometChatDate::class.java)
        val props = mutableListOf<Prop>()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val date = CometChatDate(activity)
            setters.forEach { s ->
                when {
                    waivers.isWaived("CometChatDate", s.propName) -> props += Prop("CometChatDate", s.propName, PropKind.VALUE, waived = true)
                    pending(s.paramType) -> {}
                    s.supported -> props += Prop("CometChatDate", s.propName, PropKind.VALUE, covered = runCatching { s.applyNonDefault(date) }.isSuccess)
                }
            }
            val style = StylePropSweep.sweep(CometChatDateStyle::class)
            println("  [date style] variants=${style.variants.size} unsupported=${style.unsupported}")
            assertTrue(style.variants.size >= 4 && style.unsupported.isEmpty())
            style.variants.forEach { date.setStyle(it.instance) }
        }
        scenario.close()
        val cov = Denominator.summarize(props)
        println("  [date view VALUE] ${cov.covered}/${cov.total}")
        assertEquals(cov.total, cov.covered); assertEquals(100.0, cov.percent, 0.0)
    }

    @Test
    fun setDate_withPattern_reachesTheDateText() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val date = CometChatDate(activity)
            date.setDate(1735689600L, "yyyy") // Jan 1 2025 (epoch seconds)
            val tv = date.findViewById<TextView>(R.id.tv_date)
            assertTrue("formatted date should reach tv_date, was '${tv?.text}'", tv?.text?.toString()?.contains("2025") == true)
        }
        scenario.close()
    }
}
