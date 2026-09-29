package com.cometchat.uikit.kotlin.presentation.shared.mediarecorder

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
import com.cometchat.uikit.propmatrix.WaiverSet
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * ENG-38681 — CometChatAudioVisualizer (View) matrix. Every own scalar setter
 * (amplitude/progress/animating + chunk geometry & colors) is a plain VALUE
 * prop. setStyle takes the shared CometChatMediaRecorderStyle, so it is waived
 * here (matrixed with the MediaRecorder component).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatAudioVisualizerTest {

    @After
    fun tearDown() = NonDefaultValues.clearRegistered()

    private val waivers = WaiverSet.parse(
        """
        - owner: CometChatAudioVisualizer
          name: Style
          reason: setStyle takes the shared CometChatMediaRecorderStyle; matrixed with the MediaRecorder component
          by: ENG-38681
        """.trimIndent()
    )

    private fun pending(p: Class<*>): Boolean =
        View::class.java.isAssignableFrom(p) || Function::class.java.isAssignableFrom(p) ||
            p.isInterface || p.simpleName.endsWith("Style")

    @Test
    fun viewAudioVisualizer_matrix_value() {
        val setters = ViewPropSweep.setters(CometChatAudioVisualizer::class.java)
        val props = mutableListOf<Prop>()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val av = CometChatAudioVisualizer(activity)
            setters.forEach { s ->
                when {
                    waivers.isWaived("CometChatAudioVisualizer", s.propName) ->
                        props += Prop("CometChatAudioVisualizer", s.propName, PropKind.VALUE, waived = true)
                    pending(s.paramType) -> {}
                    s.supported ->
                        props += Prop("CometChatAudioVisualizer", s.propName, PropKind.VALUE, covered = runCatching { s.applyNonDefault(av) }.isSuccess)
                }
            }
        }
        scenario.close()
        val cov = Denominator.summarize(props)
        println("  [audiovisualizer view VALUE] ${cov.covered}/${cov.total}")
        assertEquals(cov.total, cov.covered); assertEquals(100.0, cov.percent, 0.0)
    }
}
