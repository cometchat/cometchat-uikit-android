package com.cometchat.uikit.kotlin.presentation.messageheader

import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.messageheader.ui.CometChatMessageHeader
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
import org.robolectric.shadows.ShadowLooper

/**
 * Reflective sweep over the View header's `set*` surface, applied to an attached
 * instance -- the header binds its ViewModel on attach, so a detached one would
 * accept every setter without rendering anything.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatMessageHeaderPropMatrixTest {

    @After fun tearDown() = NonDefaultValues.clearRegistered()

    private fun isFunctionalPending(paramType: Class<*>): Boolean =
        View::class.java.isAssignableFrom(paramType) ||
            android.graphics.drawable.Drawable::class.java.isAssignableFrom(paramType) ||
            Function::class.java.isAssignableFrom(paramType) ||
            List::class.java.isAssignableFrom(paramType) ||
            paramType.isInterface ||
            paramType.simpleName == "User" ||
            paramType.simpleName == "Group" ||
            paramType.simpleName == "SimpleDateFormat" ||
            paramType.simpleName.endsWith("Style") ||
            paramType.simpleName.endsWith("Listener") ||
            paramType.simpleName.endsWith("Callback")

    @Test
    fun viewMessageHeader_mechanicalValueMatrix_coversEveryValueProp() {
        val setters = ViewPropSweep.setters(CometChatMessageHeader::class.java)
        assertTrue("expected a wide setter surface, was ${setters.size}", setters.size > 8)

        val props = mutableListOf<Prop>()
        val functionalPending = mutableListOf<String>()

        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val header = CometChatMessageHeader(activity)
            activity.setContentView(header)
            ShadowLooper.idleMainLooper()
            setters.forEach { setter ->
                when {
                    isFunctionalPending(setter.paramType) ->
                        functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                    setter.supported ->
                        props += Prop(
                            "CometChatMessageHeader",
                            setter.propName,
                            PropKind.VALUE,
                            covered = runCatching { setter.applyNonDefault(header) }.isSuccess,
                        )
                    else -> functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                }
            }
        }
        scenario.close()

        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [message header view VALUE] ${cov.covered}/${cov.total}; functional-pending=${functionalPending.size} $functionalPending")
        if (uncovered.isNotEmpty()) println("  [message header view] NOT covered: $uncovered")

        assertEquals("every swept VALUE prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }
}
