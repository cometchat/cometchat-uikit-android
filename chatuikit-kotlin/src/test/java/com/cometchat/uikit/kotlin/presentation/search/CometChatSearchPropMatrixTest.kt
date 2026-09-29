package com.cometchat.uikit.kotlin.presentation.search

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.search.style.CometChatSearchStyle
import com.cometchat.uikit.kotlin.presentation.search.ui.CometChatSearch
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
 * ENG-38678 — CometChatSearch (View) prop matrix, Phase 1 (hybrid model).
 *
 * Mechanical sweep gives the bulk coverage: every VALUE prop (colour / dimension /
 * text-appearance / flag) set to a non-default and applied to a real
 * Robolectric-rendered CometChatSearch, and every StylePropSweep variant applied.
 * SLOT props (custom item/empty/error views), CALLBACK props (click/error
 * listeners) and functional config (scopes / filters / request builders) are
 * covered by the functional-surface tests (Phase 1b) with real effect assertions,
 * not by the mechanical sweep — so here they are tracked as "functional-pending".
 * Genuine infra (`ViewModel`) and the whole-`Style` object (matrixed field-by-field
 * by the style sweep) are waived. Nested style objects (`avatarStyle`, …) are
 * waived in the style sweep because they're matrixed by their own component.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatSearchPropMatrixTest {

    @After
    fun tearDown() {
        NonDefaultValues.clearRegistered()
    }

    private val componentWaivers = WaiverSet.parse(
        """
        - owner: CometChatSearch
          name: ViewModel
          reason: DI/infra injection, not an integrator styling or behaviour prop
          by: ENG-38678
        - owner: CometChatSearch
          name: Style
          reason: whole-style object; matrixed field-by-field by the CometChatSearchStyle sweep
          by: ENG-38678
        """.trimIndent()
    )

    /** SLOT / CALLBACK / functional-config prop types deferred to the functional tests. */
    private fun isFunctionalPending(paramType: Class<*>): Boolean =
        View::class.java.isAssignableFrom(paramType) ||
            Drawable::class.java.isAssignableFrom(paramType) ||
            Function::class.java.isAssignableFrom(paramType) ||
            List::class.java.isAssignableFrom(paramType) ||
            paramType.isInterface ||
            paramType.simpleName.endsWith("RequestBuilder")

    @Test
    fun viewSearch_mechanicalValueMatrix_coversEveryValueProp() {
        val setters = ViewPropSweep.setters(CometChatSearch::class.java)
        assertTrue("expected a large setter surface, was ${setters.size}", setters.size > 100)

        val props = mutableListOf<Prop>()
        val functionalPending = mutableListOf<String>()

        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val search = CometChatSearch(activity)
            setters.forEach { setter ->
                when {
                    componentWaivers.isWaived("CometChatSearch", setter.propName) ->
                        props += Prop("CometChatSearch", setter.propName, PropKind.VALUE, waived = true)
                    isFunctionalPending(setter.paramType) ->
                        functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                    setter.supported -> {
                        val ok = runCatching { setter.applyNonDefault(search) }.isSuccess
                        props += Prop("CometChatSearch", setter.propName, PropKind.VALUE, covered = ok)
                    }
                    else ->
                        functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                }
            }
        }
        scenario.close()

        val cov = Denominator.summarize(props)
        println("  [search view VALUE] ${cov.covered}/${cov.total} (${cov.waived} waived); functional-pending=${functionalPending.size}")
        println("  [search view] functional-pending props = $functionalPending")

        // every mechanically-swept VALUE prop applies cleanly to the real component
        assertTrue("expected >= 60 value props, was ${cov.total}", cov.total >= 60)
        assertEquals("every swept VALUE prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }

    @Test
    fun viewSearchStyle_matrix_coversEveryStyleProp() {
        // Drawable icon fields need a producer (a random Int isn't a Drawable);
        // nested style objects are matrixed by their own components -> waived here.
        NonDefaultValues.register(Drawable::class) { ColorDrawable(0xFF00C0FF.toInt()) }
        val nestedStyleWaivers = setOf("avatarStyle", "badgeStyle", "dateSeparatorStyle", "statusIndicatorStyle")

        val result = StylePropSweep.sweep(CometChatSearchStyle::class)
        val stillUnsupported = result.unsupported.filterNot { it in nestedStyleWaivers }

        println("  [search style] variants=${result.variants.size} waived(nested)=${nestedStyleWaivers.size} stillUnsupported=$stillUnsupported")

        assertTrue("expected >= 64 style variants, was ${result.variants.size}", result.variants.size >= 64)
        assertTrue(
            "after the Drawable producer + nested-style waivers, nothing should be unsupported: $stillUnsupported",
            stillUnsupported.isEmpty()
        )

        // apply every variant to a real Search instance without throwing
        val applied = mutableListOf<String>()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val search = CometChatSearch(activity)
            result.variants.forEach { variant ->
                runCatching { search.setStyle(variant.instance) }.onSuccess { applied += variant.propName }
            }
        }
        scenario.close()
        println("  [search style] applied ${applied.size}/${result.variants.size} variants to the real component")
        assertEquals("every style variant should apply cleanly", result.variants.size, applied.size)
    }

    @Test
    fun viewSearch_hintTextEffect_reachesTheSearchInput() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val search = CometChatSearch(activity)
            search.setHintText("PROP-MATRIX-HINT")

            val edit = firstEditText(search)
            assertTrue("expected an EditText in the search view tree", edit != null)
            assertEquals("PROP-MATRIX-HINT", edit!!.hint?.toString())
        }
        scenario.close()
    }

    private fun firstEditText(root: View): EditText? {
        if (root is EditText) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                firstEditText(root.getChildAt(i))?.let { return it }
            }
        }
        return null
    }
}
