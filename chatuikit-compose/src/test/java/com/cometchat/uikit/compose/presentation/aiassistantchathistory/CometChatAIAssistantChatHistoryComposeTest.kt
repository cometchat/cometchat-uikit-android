package com.cometchat.uikit.compose.presentation.aiassistantchathistory

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.aiassistantchathistory.style.CometChatAIAssistantChatHistoryStyle
import com.cometchat.uikit.compose.presentation.aiassistantchathistory.ui.CometChatAIAssistantChatHistory
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.viewmodel.CometChatAIAssistantChatHistoryViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.reflect.Modifier

/**
 * ENG-38681 — Compose CometChatAIAssistantChatHistory matrix (style, scalar +
 * icon) + functional render with an injected listeners-off VM (no live SDK).
 * The 5 TextStyle fields are waived (composite typography).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatAIAssistantChatHistoryComposeTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val textStyleFields = setOf(
        "chatHistoryHeaderTextStyle", "newChatTextStyle", "dateSeparatorTextStyle",
        "itemTextStyle", "deleteOptionTextStyle",
    )

    @Test
    fun everyScalarStyleField_isOverridable() {
        val vm = CometChatAIAssistantChatHistoryViewModel(enableListeners = false)
        lateinit var base: CometChatAIAssistantChatHistoryStyle
        lateinit var custom: CometChatAIAssistantChatHistoryStyle
        rule.setContent {
            CometChatTheme {
                base = CometChatAIAssistantChatHistoryStyle.default()
                val c = Color(0xFFAB12CD); val p = ColorPainter(Color(0xFF00C0FF))
                custom = base.copy(
                    chatHistoryBackgroundColor = c, chatHistoryHeaderBackgroundColor = c, chatHistoryHeaderTextColor = c,
                    chatHistoryHeaderCloseIcon = p, chatHistoryHeaderCloseIconTint = c,
                    newChatBackgroundColor = c, newChatTextColor = c, newChatIcon = p, newChatIconTint = c,
                    dateSeparatorBackgroundColor = c, dateSeparatorTextColor = c,
                    itemBackgroundColor = c, itemTextColor = c,
                    deleteOptionIcon = p, deleteOptionIconTint = c, deleteOptionTextColor = c,
                )
                CometChatAIAssistantChatHistory(viewModel = vm, style = custom)
            }
        }
        rule.waitForIdle()
        val scalarFields = CometChatAIAssistantChatHistoryStyle::class.java.declaredFields.filter {
            !it.isSynthetic && !Modifier.isStatic(it.modifiers) && it.name !in textStyleFields
        }
        scalarFields.forEach { it.isAccessible = true }
        val unchanged = scalarFields.filter { it.get(base) == it.get(custom) }
        assertEquals("every scalar style field overridden; missed=${unchanged.map { it.name }}", emptyList<String>(), unchanged.map { it.name })
        assertEquals("expected 16 scalar style fields", 16, scalarFields.size)
    }

    @Test
    fun rendersWithInjectedVm() {
        val vm = CometChatAIAssistantChatHistoryViewModel(enableListeners = false)
        rule.setContent { CometChatTheme { CometChatAIAssistantChatHistory(viewModel = vm) } }
        rule.waitForIdle()
        rule.onRoot().assertExists()
    }
}
