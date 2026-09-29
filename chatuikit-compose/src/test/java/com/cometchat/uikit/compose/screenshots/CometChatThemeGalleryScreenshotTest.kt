package com.cometchat.uikit.compose.screenshots

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.utils.RoborazziConfig
import com.cometchat.uikit.compose.theme.CometChatColorScheme
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.compose.theme.CometChatTypography
import com.cometchat.uikit.compose.theme.darkColorScheme
import com.cometchat.uikit.compose.theme.lightColorScheme
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * ENG-38678 — CometChatTheme golden gallery, Phase 3 Slice 3b (snapshot layer).
 *
 * LAYER: snapshot / Roborazzi. Renders a swatch for EVERY color token and a
 * sample line for every typography token, reflectively — so each token is
 * visibly on screen and the golden gives per-token pixel coverage. Recorded in
 * light and dark; a change to any theme token flips its swatch and fails
 * verifyRoborazzi (the ticket's "deliberate theme-token break fails a test").
 * Goldens: screenshot-gallery/compose/theme/.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h1600dp-xxhdpi")
class CometChatThemeGalleryScreenshotTest {

    @get:Rule(order = 0)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule(order = 1)
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/compose/theme"
        )
    )

    private val colorTokens: List<KProperty1<CometChatColorScheme, *>> =
        CometChatColorScheme::class.memberProperties.filter { it.returnType.classifier == Color::class }

    private val typeTokens: List<KProperty1<CometChatTypography, *>> =
        CometChatTypography::class.memberProperties.filter { it.returnType.classifier == TextStyle::class }

    @Composable
    private fun ThemeGallery() {
        val scheme = CometChatTheme.colorScheme
        val typography = CometChatTheme.typography
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF808080)) // neutral ground so both light + dark swatches read
                .verticalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            colorTokens.chunked(8).forEach { rowTokens ->
                Row {
                    rowTokens.forEach { prop ->
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .size(36.dp)
                                .background(prop.get(scheme) as Color)
                        )
                    }
                }
            }
            typeTokens.forEach { prop ->
                Text(text = "Aa ${prop.name}", style = prop.get(typography) as TextStyle)
            }
        }
    }

    private fun capture() {
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = RoborazziConfig.options())
    }

    @Test
    fun themeGallery_light() {
        composeTestRule.setContent { CometChatTheme(colorScheme = lightColorScheme()) { ThemeGallery() } }
        capture()
    }

    @Test
    @Config(qualifiers = "w400dp-h1600dp-night-xxhdpi")
    fun themeGallery_dark() {
        composeTestRule.setContent { CometChatTheme(colorScheme = darkColorScheme()) { ThemeGallery() } }
        capture()
    }
}
