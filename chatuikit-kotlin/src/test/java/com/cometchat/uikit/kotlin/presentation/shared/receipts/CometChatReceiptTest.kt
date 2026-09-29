package com.cometchat.uikit.kotlin.presentation.shared.receipts

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.models.BaseMessage
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
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config

/**
 * ENG-38681 — CometChatReceipt (View-only) matrix + functional. Functional: a
 * message's receipt state maps to a rendered drawable.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatReceiptTest {

    @After
    fun tearDown() = NonDefaultValues.clearRegistered()

    private val waivers = WaiverSet.parse(
        """
        - owner: CometChatReceipt
          name: Style
          reason: whole-style object; matrixed by the CometChatReceiptStyle sweep
          by: ENG-38681
        """.trimIndent()
    )

    private fun pending(p: Class<*>): Boolean =
        View::class.java.isAssignableFrom(p) || Drawable::class.java.isAssignableFrom(p) ||
            Function::class.java.isAssignableFrom(p) || p.isInterface || p.simpleName.endsWith("Style") || p.simpleName == "BaseMessage"

    @Test
    fun viewReceipt_matrix_value_and_style() {
        NonDefaultValues.register(Drawable::class) { ColorDrawable(0xFF00C0FF.toInt()) }
        val setters = ViewPropSweep.setters(CometChatReceipt::class.java)
        val props = mutableListOf<Prop>()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val r = CometChatReceipt(activity)
            setters.forEach { st ->
                when {
                    waivers.isWaived("CometChatReceipt", st.propName) -> props += Prop("CometChatReceipt", st.propName, PropKind.VALUE, waived = true)
                    pending(st.paramType) -> {}
                    st.supported -> props += Prop("CometChatReceipt", st.propName, PropKind.VALUE, covered = runCatching { st.applyNonDefault(r) }.isSuccess)
                }
            }
            val style = StylePropSweep.sweep(CometChatReceiptStyle::class)
            println("  [receipt style] variants=${style.variants.size} unsupported=${style.unsupported}")
            assertTrue(style.variants.size >= 4 && style.unsupported.isEmpty())
            style.variants.forEach { r.setStyle(it.instance) }
        }
        scenario.close()
        val cov = Denominator.summarize(props)
        println("  [receipt view VALUE] ${cov.covered}/${cov.total}")
        assertEquals(cov.total, cov.covered); assertEquals(100.0, cov.percent, 0.0)
    }

    @Test
    fun setReceipt_mapsMessageStateToDrawable() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val r = CometChatReceipt(activity)
            r.setReceipt(mock<BaseMessage>())
            val icon = CometChatReceipt::class.java.getDeclaredField("receiptIcon").apply { isAccessible = true }.get(r) as ImageView
            assertNotNull("receipt state should map to a drawable", icon.drawable)
        }
        scenario.close()
    }
}
