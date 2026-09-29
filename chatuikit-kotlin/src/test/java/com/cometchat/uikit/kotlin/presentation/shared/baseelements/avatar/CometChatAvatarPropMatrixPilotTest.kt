package com.cometchat.uikit.kotlin.presentation.shared.baseelements.avatar

import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.propmatrix.NonDefaultValues
import com.cometchat.uikit.propmatrix.StylePropSweep
import com.cometchat.uikit.propmatrix.ViewPropSweep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * ENG-38956 — Avatar pilot for the prop-matrix harness (View flavor).
 *
 * Proves the reflection engine works end-to-end against a REAL component:
 *  - `ViewPropSweep` enumerates `CometChatAvatar`'s public single-arg `set*` props,
 *    and each supported one is invoked with a non-default value on a real
 *    Robolectric-constructed Avatar.
 *  - `StylePropSweep` sweeps `CometChatAvatarStyle` and applies each single-prop
 *    variant to the real Avatar via `setStyle`.
 *
 * Finding surfaced by the pilot: resource-id props (`@StyleRes`/`@DrawableRes`)
 * can't take a random Int (they crash resolving a garbage id), and those
 * annotations are SOURCE-retention so reflection can't skip them automatically.
 * The harness applies each prop in a try/catch and reports them as
 * "needs a registered producer" — the design refinement for v2 (register a
 * real style-res producer, or hand-handle via the DSL / waive).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatAvatarPropMatrixPilotTest {

    @Test
    fun viewSetterSweep_appliesValuePropsToRealAvatar_andSurfacesResourceIdProps() {
        val setters = ViewPropSweep.setters(CometChatAvatar::class.java)
        val names = setters.map { it.propName }
        // sanity: the sweep found Avatar's real prop surface
        assertTrue("expected AvatarBackgroundColor in $names", names.contains("AvatarBackgroundColor"))
        assertTrue("expected Name in $names", names.contains("Name"))
        // scoping check: only the component's OWN setters, not ~150 inherited framework ones
        assertTrue("sweep should be scoped to the component's own props (was ${setters.size})", setters.size < 30)
        assertTrue("inherited framework setter leaked in", !names.contains("Rotation") && !names.contains("Clickable"))

        val exercised = mutableListOf<String>()
        val needsHandling = mutableListOf<String>()

        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val avatar = CometChatAvatar(activity)
            setters.filter { it.supported }.forEach { setter ->
                try {
                    setter.applyNonDefault(avatar)   // set*(nonDefault) on the real view
                    exercised += setter.propName
                } catch (t: Throwable) {
                    needsHandling += setter.propName // e.g. @StyleRes with a garbage id
                }
            }
        }
        scenario.close()

        println("  [Avatar view sweep] exercised=$exercised")
        println("  [Avatar view sweep] needs-producer=$needsHandling")

        // the pure value/color/dimension props sweep cleanly on the real component
        assertTrue(
            "value props should apply cleanly, got $exercised",
            exercised.containsAll(
                listOf(
                    "AvatarBackgroundColor",
                    "AvatarStrokeColor",
                    "AvatarStrokeWidth",
                    "AvatarStrokeRadius",
                    "AvatarPlaceHolderTextColor",
                    "Name",
                )
            )
        )
        // any prop that threw (e.g. a resource-id setter) is surfaced for a registered producer / waiver.
        // (Robolectric is lenient with garbage resource ids, so this list may be empty — informational.)
    }

    @Test
    fun styleSweep_appliesEveryStylePropVariantToRealAvatar() {
        val result = StylePropSweep.sweep(CometChatAvatarStyle::class)
        assertEquals(
            "AvatarStyle prop surface",
            setOf(
                "backgroundColor", "strokeColor", "strokeWidth",
                "cornerRadius", "placeHolderTextAppearance", "placeHolderTextColor",
            ),
            result.variants.map { it.propName }.toSet() + result.unsupported.toSet()
        )

        val applied = mutableListOf<String>()
        val needsHandling = mutableListOf<String>()

        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val avatar = CometChatAvatar(activity)
            result.variants.forEach { variant ->
                try {
                    avatar.setStyle(variant.instance) // apply the single-prop-non-default style
                    applied += variant.propName
                } catch (t: Throwable) {
                    needsHandling += variant.propName
                }
            }
        }
        scenario.close()

        println("  [AvatarStyle sweep] applied=$applied")
        println("  [AvatarStyle sweep] needs-producer=$needsHandling")

        // colour + dimension style props apply cleanly to the real Avatar
        assertTrue(
            "colour/dimension style props should apply, got $applied",
            applied.containsAll(
                listOf("backgroundColor", "strokeColor", "strokeWidth", "cornerRadius", "placeHolderTextColor")
            )
        )
    }

    /**
     * The resource-id producer. `setStyle(@StyleRes Int)` and
     * `setAvatarPlaceHolderTextAppearance(@StyleRes Int)` take an Int that must be
     * a *resolvable* resource id — a random Int isn't one, and `@StyleRes` is
     * invisible to reflection, so the sweep can't tell it apart from a colour int.
     * The consuming test registers a real style id for those specific props via
     * `NonDefaultValues.registerProp`; the override only fits the Int overload of
     * `setStyle`, not the `CometChatAvatarStyle` one.
     */
    @Test
    fun resourceIdProducer_suppliesRealStyleRes_forStyleResSetters() {
        val realStyle = R.style.CometChatTheme_DayNight
        NonDefaultValues.registerProp("CometChatAvatar", "Style") { realStyle }
        NonDefaultValues.registerProp("CometChatAvatar", "AvatarPlaceHolderTextAppearance") { realStyle }
        try {
            // the registered id is what gets supplied — not the generic garbage int
            assertEquals(
                realStyle,
                NonDefaultValues.forProp("CometChatAvatar", "Style", Int::class.javaPrimitiveType!!)
            )

            val setters = ViewPropSweep.setters(CometChatAvatar::class.java)
            val exercised = mutableListOf<String>()
            val needsHandling = mutableListOf<String>()

            val scenario = ActivityScenario.launch(ComponentActivity::class.java)
            scenario.onActivity { activity ->
                activity.setTheme(R.style.CometChatTheme_DayNight)
                val avatar = CometChatAvatar(activity)
                setters.filter { it.supported }.forEach { setter ->
                    try {
                        setter.applyNonDefault(avatar)
                        exercised += setter.propName
                    } catch (t: Throwable) {
                        needsHandling += "${setter.propName}(${setter.paramType.simpleName})"
                    }
                }
            }
            scenario.close()

            println("  [resource-id] exercised=$exercised")
            println("  [resource-id] needs-handling=$needsHandling")

            // both @StyleRes setters resolve cleanly once given a real id
            assertTrue("Style(@StyleRes Int) should apply, needs=$needsHandling", exercised.contains("Style"))
            assertTrue(
                "AvatarPlaceHolderTextAppearance(@StyleRes) should apply, needs=$needsHandling",
                exercised.contains("AvatarPlaceHolderTextAppearance")
            )
        } finally {
            NonDefaultValues.clearPropOverrides()
        }
    }
}
