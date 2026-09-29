package com.cometchat.uikit.compose.presentation.shared.baseelements.avatar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.NonDefaultValues
import com.cometchat.uikit.propmatrix.PropKind
import com.cometchat.uikit.propmatrix.StylePropSweep
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * ENG-38956 — Avatar pilot for the prop-matrix harness (Compose flavor).
 *
 * The View pilot ([com.cometchat.uikit.kotlin...CometChatAvatarPropMatrixPilotTest])
 * proved the *reflection* sweeps (`ViewPropSweep` + `StylePropSweep`) against the
 * View `CometChatAvatar`. Compose is different, and this pilot shows exactly why:
 *
 *  1. **Reflection can't construct Compose style objects.** `CometChatAvatarStyle`'s
 *     fields are Compose inline value-classes (`Color?`, `Dp`, `TextStyle?`).
 *     kotlin-reflect can *enumerate* those props and even *produce* non-default
 *     values for them, but `KFunction.callBy` (constructor, `createInstance`, and
 *     `copy`) all throw `IllegalArgumentException: argument type mismatch` on
 *     value-class parameters — only the compiler-generated `.copy(...)` works.
 *     So `StylePropSweep` returns every prop as `unsupported` for this class.
 *     This is the concrete reason the Compose flavor uses a hand-written DSL
 *     (compile-time construction) instead of reflection.
 *
 *  2. **The DSL path works end-to-end.** `composePropMatrix` declares the
 *     composable's param surface; each entry's `exercise` renders the REAL
 *     component (styles built at compile time) and asserts the effect via Compose
 *     semantics. A prop counts as covered only when its assertion passes.
 *
 * Note on style-prop *effects*: Compose exposes no semantics for colour / corner /
 * border, and plain `captureToImage()` doesn't settle under Robolectric — so those
 * effects are asserted by the Roborazzi screenshot harness (V, ENG-38672), whose
 * capture pipeline is walled off into the `*ScreenshotTest` task. The DSL supplies
 * those exercises with pixel assertions once V is wired. Here we assert the one
 * semantics-observable prop, `name` (initials text).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatAvatarComposePropMatrixPilotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun reflectionCannotConstructComposeValueClassStyle_soComposeUsesTheDsl() {
        // Register producers so the failure is isolated to *construction*, not to
        // "no non-default value available" — supports() is true for all six.
        NonDefaultValues.register(Color::class) { Color(0xFF00C0FF) }
        NonDefaultValues.register(Dp::class) { 99.dp }
        NonDefaultValues.register(TextStyle::class) { TextStyle(fontSize = 30.sp) }

        val result = StylePropSweep.sweep(CometChatAvatarStyle::class)

        // The whole prop surface is still enumerated…
        assertEquals(
            "CometChatAvatarStyle prop surface",
            setOf("backgroundColor", "borderColor", "borderWidth", "cornerRadius", "textColor", "textStyle"),
            result.variants.map { it.propName }.toSet() + result.unsupported.toSet()
        )
        // …but none can be reflectively constructed (value-class ctor args), so the
        // sweep yields no variants and hands every prop back for the DSL to handle.
        assertTrue("reflection should not construct any value-class style variant", result.variants.isEmpty())
        assertEquals(
            "every prop comes back for hand-handling via the DSL",
            setOf("backgroundColor", "borderColor", "borderWidth", "cornerRadius", "textColor", "textStyle"),
            result.unsupported.toSet()
        )

        // The compiler-generated copy() DOES construct it — which is what the DSL uses.
        val styled = CometChatAvatarStyle().copy(backgroundColor = Color(0xFF00C0FF))
        assertTrue("compile-time copy() constructs the non-default style", styled.backgroundColor != null)
    }

    @Test
    fun dslPilot_rendersRealAvatarComposable_andAssertsNamePropEffect() {
        // "Prop Matrix" -> two words -> initials "PM", rendered as an uppercase Text node.
        // Style is built at compile time (the only way to construct a value-class style).
        composeRule.setContent {
            CometChatTheme {
                CometChatAvatar(
                    name = "Prop Matrix",
                    style = CometChatAvatarStyle().copy(backgroundColor = Color(0xFF00C0FF), cornerRadius = 8.dp),
                )
            }
        }

        val matrix = composePropMatrix("CometChatAvatar") {
            // VALUE prop `name`: set non-default, assert its rendered effect (initials).
            value("name") {
                composeRule.onNodeWithText("PM").assertIsDisplayed()
            }
        }

        val props = matrix.evaluate()
        val coverage = Denominator.summarize(props)

        val nameProp = props.first { it.name == "name" }
        assertEquals(PropKind.VALUE, nameProp.kind)
        assertTrue("name prop effect should be asserted on the real composable", nameProp.covered)
        assertEquals(1, coverage.total)
        assertEquals(1, coverage.covered)
        assertEquals(100.0, coverage.percent, 0.0)
    }
}
