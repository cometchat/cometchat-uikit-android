package com.cometchat.uikit.kotlin.presentation.shared.baseelements.badgecount

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
 * ENG-38681 — CometChatBadgeCount (View) matrix + functional. A pure value/style
 * base component. Functional: the count cap ("999+") and small-count rendering.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatBadgeCountTest {

    @After
    fun tearDown() = NonDefaultValues.clearRegistered()

    private val componentWaivers = WaiverSet.parse(
        """
        - owner: CometChatBadgeCount
          name: Style
          reason: whole-style object; matrixed field-by-field by the CometChatBadgeCountStyle sweep
          by: ENG-38681
        """.trimIndent()
    )

    private fun isFunctionalPending(paramType: Class<*>): Boolean =
        View::class.java.isAssignableFrom(paramType) || Drawable::class.java.isAssignableFrom(paramType) ||
            Function::class.java.isAssignableFrom(paramType) || paramType.isInterface || paramType.simpleName.endsWith("Style")

    @Test
    fun viewBadgeCount_matrix_value_and_style() {
        val setters = ViewPropSweep.setters(CometChatBadgeCount::class.java)
        val props = mutableListOf<Prop>()
        NonDefaultValues.register(Drawable::class) { android.graphics.drawable.ColorDrawable(0xFF00C0FF.toInt()) }
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val badge = CometChatBadgeCount(activity)
            setters.forEach { s ->
                when {
                    componentWaivers.isWaived("CometChatBadgeCount", s.propName) -> props += Prop("CometChatBadgeCount", s.propName, PropKind.VALUE, waived = true)
                    isFunctionalPending(s.paramType) -> {}
                    s.supported -> props += Prop("CometChatBadgeCount", s.propName, PropKind.VALUE, covered = runCatching { s.applyNonDefault(badge) }.isSuccess)
                }
            }
            val style = StylePropSweep.sweep(CometChatBadgeCountStyle::class)
            println("  [badge style] variants=${style.variants.size} unsupported=${style.unsupported}")
            assertTrue("style variants", style.variants.size >= 4 && style.unsupported.isEmpty())
            style.variants.forEach { badge.setStyle(it.instance) }
        }
        scenario.close()
        val cov = Denominator.summarize(props)
        println("  [badge view VALUE] ${cov.covered}/${cov.total}")
        assertEquals(cov.total, cov.covered); assertEquals(100.0, cov.percent, 0.0)
    }

    @Test
    fun setCount_rendersNumber_andCapsAt999() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val badge = CometChatBadgeCount(activity)
            val tv = CometChatBadgeCount::class.java.getDeclaredField("tvMessageCount").apply { isAccessible = true }.get(badge) as TextView

            badge.setCount(5)
            assertTrue("small count shows the number, was '${tv.text}'", tv.text.toString().contains("5"))

            badge.setCount(1500)
            val cap = activity.getString(R.string.cometchat_unread_message_count_max)
            assertEquals("count >= 999 caps to the max string", cap, tv.text.toString())
        }
        scenario.close()
    }
}
