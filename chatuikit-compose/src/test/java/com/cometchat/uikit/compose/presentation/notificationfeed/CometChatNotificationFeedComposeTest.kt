package com.cometchat.uikit.compose.presentation.notificationfeed

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.notificationfeed.style.CometChatNotificationFeedStyle
import com.cometchat.uikit.compose.presentation.notificationfeed.ui.CometChatNotificationFeed
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.viewmodel.CometChatNotificationFeedViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.reflect.Modifier

/**
 * ENG-38681 — Compose CometChatNotificationFeed matrix (style, all scalar) +
 * functional render with an injected listeners-off VM (no live SDK).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatNotificationFeedComposeTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun everyStyleField_isOverridable() {
        val base = CometChatNotificationFeedStyle()
        val c = Color(0xFFAB12CD); val d = 77.dp
        val custom = base.copy(
            backgroundColor = c, headerBackgroundColor = c, headerTitleColor = c, headerBorderColor = c,
            chipActiveBackgroundColor = c, chipActiveTextColor = c, chipInactiveBackgroundColor = c,
            chipInactiveTextColor = c, chipBorderColor = c,
            badgeActiveBackgroundColor = c, badgeActiveBorderColor = c, badgeActiveTextColor = c,
            badgeInactiveBackgroundColor = c, badgeInactiveTextColor = c, timestampTextColor = c,
            cardBackgroundColor = c, cardBorderColor = c, cardBorderRadius = d, cardBorderWidth = d,
            cardTitleColor = c, cardPriceColor = c, cardDescriptionColor = c,
            primaryButtonBackgroundColor = c, primaryButtonTextColor = c,
            secondaryButtonBackgroundColor = c, secondaryButtonBorderColor = c, secondaryButtonTextColor = c,
            buttonBorderRadius = d, unreadIndicatorColor = c, separatorColor = c,
            emptyTitleColor = c, emptyDescriptionColor = c,
        )
        val fields = CometChatNotificationFeedStyle::class.java.declaredFields.filter {
            !it.isSynthetic && !Modifier.isStatic(it.modifiers)
        }
        fields.forEach { it.isAccessible = true }
        val unchanged = fields.filter { it.get(base) == it.get(custom) }
        assertEquals("every style field overridden; missed=${unchanged.map { it.name }}", emptyList<String>(), unchanged.map { it.name })
        assertEquals("expected 32 style fields", 32, fields.size)
    }

    @Test
    fun rendersWithInjectedVm() {
        val vm = CometChatNotificationFeedViewModel(enableListeners = false)
        rule.setContent { CometChatTheme { CometChatNotificationFeed(viewModel = vm) } }
        rule.waitForIdle()
        rule.onRoot().assertExists()
    }
}
