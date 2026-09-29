package com.cometchat.uikit.compose.screenshots

import android.graphics.Bitmap
import android.graphics.Canvas

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.messagelist.ui.CometChatMessagePopupMenu
import com.cometchat.uikit.compose.presentation.utils.RoborazziConfig
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.compose.theme.darkColorScheme
import com.cometchat.uikit.compose.theme.lightColorScheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.domain.model.CometChatMessageOption
import com.cometchat.uikit.core.testutils.MockFactory
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.util.TimeZone

/**
 * Snapshot layer for the Compose [CometChatMessagePopupMenu] — the first baselines of
 * any kind for it, and the pair to the View twin in
 * `chatuikit-kotlin/.../CometChatMessagePopupMenuScreenshotTest`.
 *
 * The two toolkits reach the same picture through different plumbing, and it shows in
 * how they are captured: the View menu is a `PopupWindow`, this one is a `Dialog`. A
 * Dialog composes into its own window, so what is captured here is that window's decor
 * view, fetched through [ShadowDialog] — the Activity behind it holds nothing.
 *
 * That window is also why this file writes its own output paths instead of using
 * `RoborazziRule`. The rule's view capture goes through Espresso, which resolves views
 * against the *active* root; under Robolectric that stays the Activity, so the dialog's
 * decor is never matched. Drawing the decor into a bitmap and handing that to Roborazzi
 * sidesteps the lookup, and the path below reproduces the rule's own naming so these
 * baselines sit in the gallery beside every other component's.
 *
 * Run:
 *   ./gradlew :chatuikit-compose:recordRoborazziDebug  --tests "*.CometChatMessagePopupMenuScreenshotTest"
 *   ./gradlew :chatuikit-compose:compareRoborazziDebug --tests "*.CometChatMessagePopupMenuScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatMessagePopupMenuScreenshotTest {

    @get:Rule(order = 0)
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule(order = 1)
    val testName = TestName()

    private lateinit var cometChat: MockedStatic<CometChat>
    private lateinit var originalZone: TimeZone

    @Before
    fun setUp() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
        val me = MockFactory.createUser(uid = "me", name = "Me")
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)
    }

    @After
    fun tearDown() {
        cometChat.close()
        TimeZone.setDefault(originalZone)
    }

    private companion object {
        const val BODY = "long-press me"
        val DEFAULT_REACTIONS = listOf("😍", "👍🏻", "🔥", "😊", "❤️")
    }

    private fun options(vararg names: String) =
        names.map { CometChatMessageOption(id = it.lowercase(), title = it) }

    private fun capture(
        dark: Boolean = false,
        menuItems: List<CometChatMessageOption> = options("Reply", "Copy", "Forward", "Edit", "Delete"),
        quickReactions: List<String> = DEFAULT_REACTIONS,
        showQuickReactions: Boolean = true,
        messageAlignment: UIKitConstants.MessageListAlignment = UIKitConstants.MessageListAlignment.STANDARD,
    ) {
        composeRule.setContent {
            CometChatTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                CometChatMessagePopupMenu(
                    message = MockFactory.createTextMessage(sentAt = MockFactory.FIXED_SENT_AT, text = BODY),
                    menuItems = menuItems,
                    quickReactions = quickReactions,
                    showQuickReactions = showQuickReactions,
                    messageAlignment = messageAlignment,
                    onOptionClick = {},
                    onReactionClick = {},
                    onEmojiPickerClick = {},
                    onDismiss = {},
                    // Sized explicitly: the slot inherits no typography of its own, and
                    // an unstyled Text renders at Material's display size, which is not
                    // what the list hands the menu.
                    messageBubbleContent = { Text(BODY, fontSize = 14.sp) },
                )
            }
        }
        composeRule.waitForIdle()
        // The entrance is a staggered set of tweens; capture the settled state, not a
        // frame partway through it, or the baseline encodes whatever the clock happened
        // to be on.
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()

        val decor = ShadowDialog.getLatestDialog()?.window?.decorView
        assertNotNull("the menu should have opened a Dialog", decor)

        // A screenshot test that photographs an empty frame still passes, so make that
        // impossible: the capture must have a size, and the menu's own parts must be in
        // the tree. Compose draws its text rather than holding it in TextViews, so the
        // content check goes through semantics, not the view hierarchy.
        assertTrue("the dialog should have been laid out", decor!!.width > 0 && decor.height > 0)
        assertTrue(
            "the overlay and preview should be in the tree",
            composeRule.onNodeWithTag("cometchat_message_popup_menu_overlay")
                .fetchSemanticsNode().layoutInfo.isPlaced &&
                composeRule.onNodeWithTag("cometchat_message_popup_menu_preview")
                    .fetchSemanticsNode().layoutInfo.isPlaced,
        )

        val bitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
        decor.draw(Canvas(bitmap))
        bitmap.captureRoboImage(
            filePath = "../screenshot-gallery/compose/messagepopupmenu/" +
                "${javaClass.name}.${testName.methodName}.png",
            roborazziOptions = RoborazziConfig.options(),
        )
    }

    @Test fun standard() = capture()

    @Test fun standard_dark() = capture(dark = true)

    @Test
    fun leftAligned() = capture(messageAlignment = UIKitConstants.MessageListAlignment.LEFT_ALIGNED)

    @Test fun withoutQuickReactions() = capture(showQuickReactions = false)

    @Test fun withCustomQuickReactions() = capture(quickReactions = listOf("🎉", "🚀", "👀"))

    @Test fun withASingleOption() = capture(menuItems = options("Reply"))
}
