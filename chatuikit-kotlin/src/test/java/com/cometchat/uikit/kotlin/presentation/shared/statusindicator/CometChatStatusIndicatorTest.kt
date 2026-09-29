package com.cometchat.uikit.kotlin.presentation.shared.statusindicator

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * ENG-38681 — CometChatStatusIndicator (View) matrix + functional. Functional:
 * the status enum maps to a rendered drawable (online vs group states differ).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatStatusIndicatorTest {

    @After
    fun tearDown() = NonDefaultValues.clearRegistered()

    private val waivers = WaiverSet.parse(
        """
        - owner: CometChatStatusIndicator
          name: Style
          reason: whole-style object; matrixed by the CometChatStatusIndicatorStyle sweep
          by: ENG-38681
        """.trimIndent()
    )

    private fun pending(p: Class<*>): Boolean =
        View::class.java.isAssignableFrom(p) || Drawable::class.java.isAssignableFrom(p) ||
            Function::class.java.isAssignableFrom(p) || p.isInterface || p.simpleName.endsWith("Style") || p.simpleName == "StatusIndicator"

    @Test
    fun viewStatusIndicator_matrix_value_and_style() {
        NonDefaultValues.register(Drawable::class) { ColorDrawable(0xFF00C0FF.toInt()) }
        val setters = ViewPropSweep.setters(CometChatStatusIndicator::class.java)
        val props = mutableListOf<Prop>()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val s = CometChatStatusIndicator(activity)
            setters.forEach { st ->
                when {
                    waivers.isWaived("CometChatStatusIndicator", st.propName) -> props += Prop("CometChatStatusIndicator", st.propName, PropKind.VALUE, waived = true)
                    pending(st.paramType) -> {}
                    st.supported -> props += Prop("CometChatStatusIndicator", st.propName, PropKind.VALUE, covered = runCatching { st.applyNonDefault(s) }.isSuccess)
                }
            }
            val style = StylePropSweep.sweep(CometChatStatusIndicatorStyle::class)
            println("  [status style] variants=${style.variants.size} unsupported=${style.unsupported}")
            assertTrue(style.variants.size >= 2 && style.unsupported.isEmpty())
            style.variants.forEach { s.setStyle(it.instance) }
        }
        scenario.close()
        val cov = Denominator.summarize(props)
        println("  [status view VALUE] ${cov.covered}/${cov.total}")
        assertEquals(cov.total, cov.covered); assertEquals(100.0, cov.percent, 0.0)
    }

    @Test
    fun setStatusIndicator_mapsStateToDrawable() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val s = CometChatStatusIndicator(activity)
            val iv = CometChatStatusIndicator::class.java.getDeclaredField("imageView").apply { isAccessible = true }.get(s) as ImageView

            s.setStatusIndicator(StatusIndicator.ONLINE)
            val online = iv.drawable
            assertNotNull("online status should map to a drawable", online)

            s.setStatusIndicator(StatusIndicator.PRIVATE_GROUP)
            val group = iv.drawable
            assertNotNull("group status should map to a drawable", group)
            assertTrue("different statuses should map to different drawables", online !== group)
        }
        scenario.close()
    }
}
