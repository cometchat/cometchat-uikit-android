package com.cometchat.uikit.kotlin.presentation.shared.mediaviewer

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
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
import org.robolectric.shadows.ShadowLooper

/**
 * Property (prop-matrix) layer for [CometChatImagePreview].
 *
 * This is a deliberately small matrix, and worth being plain about why. The preview is a
 * gesture handler rather than a view, and its whole `set*` surface is four methods: two
 * listeners and the two `setDragToDismissDistance` overloads. The listeners are
 * functional-pending by the usual rule, so the mechanical denominator is two.
 *
 * The value here is not the two props — [CometChatImagePreviewFunctionalTest] already asserts
 * both distance overloads far more sharply, including that they disagree for the same number
 * because one is a ratio and the other is dp. The value is the sweep itself: it enumerates
 * the surface from the class rather than from a list someone remembered to update, so a
 * setter added later shows up in this file's output instead of silently having no layer.
 *
 * The instance is built the way the viewer builds it — an [ImageView] inside a container —
 * because construction reads and rewrites both, and a handler over detached views would
 * accept every setter without ever being wired to anything.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatImagePreviewPropMatrixTest {

    private companion object {
        const val OWNER = "CometChatImagePreview"
    }

    @After fun tearDown() = NonDefaultValues.clearRegistered()

    private fun isFunctionalPending(paramType: Class<*>): Boolean =
        paramType.isInterface ||
            paramType.simpleName.endsWith("Listener") ||
            paramType.simpleName.endsWith("Callback")

    @Test
    fun mechanicalValueMatrix_coversEveryValueProp() {
        val setters = ViewPropSweep.setters(CometChatImagePreview::class.java)
        assertTrue("expected the handler's setter surface, was ${setters.size}", setters.size >= 4)

        val props = mutableListOf<Prop>()
        val functionalPending = mutableListOf<String>()

        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val imageView = ImageView(activity).apply {
                setImageDrawable(ColorDrawable(Color.CYAN))
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
            val container = FrameLayout(activity).apply { addView(imageView) }
            activity.setContentView(container)
            ShadowLooper.idleMainLooper()

            val preview = CometChatImagePreview.create(imageView, container)
            setters.forEach { setter ->
                when {
                    isFunctionalPending(setter.paramType) ->
                        functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                    setter.supported ->
                        props += Prop(
                            OWNER,
                            setter.propName,
                            PropKind.VALUE,
                            covered = runCatching { setter.applyNonDefault(preview) }.isSuccess,
                        )
                    else -> functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                }
            }
        }
        scenario.close()

        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [image preview VALUE] ${cov.covered}/${cov.total}; functional-pending=${functionalPending.size} $functionalPending")
        if (uncovered.isNotEmpty()) println("  [image preview] NOT covered: $uncovered")

        assertEquals("every swept VALUE prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertTrue("the two dismiss-distance overloads should be in the denominator", cov.total >= 2)
    }
}
