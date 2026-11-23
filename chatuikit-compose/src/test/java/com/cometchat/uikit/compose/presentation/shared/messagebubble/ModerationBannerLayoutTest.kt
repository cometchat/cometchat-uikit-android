package com.cometchat.uikit.compose.presentation.shared.messagebubble

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.width
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.enums.ModerationStatus
import com.cometchat.chat.models.Attachment
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.MediaMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.shared.messagebubble.ui.CometChatMessageBubble
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.compose.theme.lightColorScheme
import com.cometchat.uikit.core.constants.UIKitConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * ENG-39226 defect C — the Compose moderation banner must not truncate, and must not stretch a
 * media bubble.
 *
 * On device the sentence rendered as `Fuck` → "Your / messa" and `damn` → "Your / mess": the
 * narrower the message, the worse the clip. Two contributors in
 * `InternalContentRenderer.DefaultBottomView`, both addressed:
 *
 * - `maxLines = 2` hard-clipped the string. Removed.
 * - `.ignoreIntrinsicWidth()` made the banner report a 0 intrinsic width, so it could never widen
 *   the bubble's `Modifier.width(IntrinsicSize.Max)` content column
 *   (`CometChatMessageBubble.kt:681`). ENG-39226 applied it to media bubbles only.
 *
 * **ENG-39283 caps that contribution instead of zeroing or uncapping it.** Letting the banner
 * report its full single-line intrinsic width dragged a moderated `damn` out to 76 % of the screen
 * (Kotlin sits at 63 % with a 2-line banner); reporting 0 instead collapsed the bubble to its
 * content and shattered the sentence into a narrow many-line column. Text now reports **at most**
 * `MODERATION_BANNER_MAX_WIDTH_FRACTION` of the display width, so the bubble stops growing at the
 * cap and the sentence wraps. Media keeps `ignoreIntrinsicWidth`. `maxLines` stays removed on both.
 *
 * These are **layout assertions in the ordinary unit suite, not screenshots** (ticket §5, corrected
 * 2026-09-14). A golden is the wrong instrument for "is this text truncated": the Roborazzi
 * threshold on this branch is coarse enough to hide a whole text string, and the Compose screenshot
 * suite is separately known-broken here. `hasVisualOverflow` answers the question exactly, and a
 * measured bound answers the media question exactly.
 *
 * Cross-stack check (CLAUDE.md standing ruling): the View stack is the reference and is **not**
 * modified. Its live layout is `cometchat_bottom_view.xml`, whose moderation TextView is
 * `layout_width="0dp"` + `layout_weight="1"` + `android:maxLines="2"`.
 *
 * Run: ./gradlew :chatuikit-compose:testDebugUnitTest --tests "*ModerationBannerLayoutTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class ModerationBannerLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private companion object {
        const val ME = "alice"
        const val PEER = "bob"

        /** `R.string.cometchat_moderation_block_message`, verbatim. */
        const val BANNER_TEXT = "Your message was blocked due to moderation policies"

        /** The one-word message that produced the worst clip on device. */
        const val SHORT_TEXT = "damn"

        /** The image bubble puts the attachment's file name on its `contentDescription`. */
        const val IMAGE_NAME = "photo.jpg"

        /** Matches the class-level `@Config` qualifier below. */
        const val SCREEN_WIDTH_DP = 400f

        /**
         * Everything in the banner row that is not the sentence: the Box's 12dp horizontal padding
         * on each side, the 18dp warning icon and the 6dp spacer after it.
         */
        const val BANNER_CHROME_DP = 12f + 12f + 18f + 6f

        /** Long enough that the message text, not the banner, drives the bubble's width. */
        const val LONG_TEXT = "This is an ordinary long message with no moderation at all and " +
            "it should show exactly where the Compose text bubble stops growing on this screen"
    }

    private fun me(): User = User(ME, "Alice")

    private fun text(
        moderation: ModerationStatus?,
        body: String = SHORT_TEXT,
        messageId: Long = 252_010L
    ): TextMessage =
        TextMessage(PEER, body, CometChatConstants.RECEIVER_TYPE_USER).apply {
            id = messageId
            sender = me()
            sentAt = 1_729_011_360L
            moderation?.let { moderationStatus = it }
        }

    private fun image(moderation: ModerationStatus?): MediaMessage = MediaMessage(
        PEER,
        CometChatConstants.MESSAGE_TYPE_IMAGE,
        CometChatConstants.RECEIVER_TYPE_USER
    ).apply {
        id = 252_023L
        sender = me()
        sentAt = 1_729_011_360L
        moderation?.let { moderationStatus = it }
        attachment = Attachment().apply {
            fileName = IMAGE_NAME
            fileExtension = "jpg"
            fileMimeType = "image/jpeg"
            fileUrl = "https://media-us.example.com/252023.jpg"
        }
    }

    private fun setBubbles(vararg messages: BaseMessage) {
        composeRule.setContent {
            CometChatTheme(colorScheme = lightColorScheme()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    messages.forEach { message ->
                        Bubble(message)
                    }
                }
            }
        }
    }

    @Composable
    private fun Bubble(message: BaseMessage) {
        CometChatMessageBubble(
            message = message,
            alignment = UIKitConstants.MessageBubbleAlignment.RIGHT
        )
    }

    /**
     * Test 11 — the sentence is rendered in full on a short text message.
     *
     * `Text` installs a `GetTextLayoutResult` semantics action, so the real `TextLayoutResult` the
     * composable measured is readable without any production test hook. `hasVisualOverflow` is true
     * exactly when the text did not fit the space it was given — which is precisely defect C.
     */
    @Test
    fun moderationBanner_shortTextMessage_doesNotOverflow() {
        setBubbles(text(ModerationStatus.DISAPPROVED))

        val node = composeRule.onNodeWithText(BANNER_TEXT, useUnmergedTree = true)
            .fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)

        assertEquals("expected exactly one TextLayoutResult for the banner", 1, layouts.size)
        assertFalse(
            "the moderation sentence is truncated: " +
                "lineCount=${layouts.first().lineCount}, " +
                "didOverflowWidth=${layouts.first().didOverflowWidth}, " +
                "didOverflowHeight=${layouts.first().didOverflowHeight}",
            layouts.first().hasVisualOverflow
        )
    }

    /**
     * Test 12 — a disapproved media bubble is not one pixel wider than the same bubble without the
     * banner.
     *
     * Both bubbles are RIGHT-aligned in a `fillMaxWidth` row, so their right edges coincide: a
     * bubble widened by the banner shows up as its media moving **left** while the media itself
     * keeps its size. The image content node — addressed by the attachment file name, which the
     * image bubble puts on its `contentDescription` — is therefore measured in root coordinates in
     * both bubbles and compared on both left edge and width.
     *
     * The banner is `fillMaxWidth` inside the bubble's content column, so if the column had grown
     * the media would have shifted; the third assertion states the same fact from the other side —
     * the banner's own text never extends past the media.
     */
    @Test
    fun moderationBanner_mediaMessage_doesNotWidenBubble() {
        setBubbles(image(ModerationStatus.DISAPPROVED), image(ModerationStatus.UNMODERATED))

        val images = composeRule.onAllNodesWithContentDescription(IMAGE_NAME, useUnmergedTree = true)
        val moderated = images[0].getUnclippedBoundsInRoot()
        val plain = images[1].getUnclippedBoundsInRoot()

        assertEquals(
            "the moderation banner changed the media's width",
            plain.width.value, moderated.width.value, 0.5f
        )
        assertEquals(
            "the moderation banner widened the media bubble " +
                "(media left edge moved: ${plain.left} -> ${moderated.left})",
            plain.left.value, moderated.left.value, 0.5f
        )

        val banner = composeRule.onNodeWithText(BANNER_TEXT, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue(
            "the moderation banner spills past the media " +
                "(banner ${banner.left}..${banner.right} vs media ${moderated.left}..${moderated.right})",
            banner.left.value >= moderated.left.value - 0.5f &&
                banner.right.value <= moderated.right.value + 0.5f
        )
    }

    /**
     * Reads the moderated bubble's content-column width in dp.
     *
     * Taken from the width the banner `Text` was **constrained** to, not from the glyphs it laid
     * out: once the sentence wraps, its node is only as wide as its widest line, which
     * under-reports the column. `layoutInput.constraints.maxWidth` is the real available width.
     * The banner is `fillMaxWidth()` inside the content column, so adding back the chrome between
     * the sentence and the column edge — the Box's 12dp padding each side, the 18dp icon and
     * the 6dp spacer — recovers the column itself.
     */
    private fun moderatedColumnWidthDp(): Float {
        val constrainedTextPx = bannerLayout().layoutInput.constraints.maxWidth
        val constrainedTextDp = with(composeRule.density) { constrainedTextPx.toDp().value }
        return constrainedTextDp + BANNER_CHROME_DP
    }

    private fun bannerLayout(): TextLayoutResult {
        val node = composeRule.onNodeWithText(BANNER_TEXT, useUnmergedTree = true).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        assertEquals("expected exactly one TextLayoutResult for the banner", 1, layouts.size)
        return layouts.first()
    }

    /**
     * ENG-39283 test 1 (invariant 1) — a moderated short-text bubble respects the cap.
     *
     * Uncapped, the banner's single-line intrinsic width drove the bubble to 85 % of the screen in
     * this harness (76 % on the rig). Capped, it lands near
     * `InternalContentRenderer.MODERATION_BANNER_MAX_WIDTH_FRACTION` — seeded at 63 %, which is
     * where Kotlin sits.
     *
     * The bound asserted here is deliberately **looser than the constant**: the ticket (§5, §10)
     * rules that this harness guards the behaviour but never chooses the value, and the value is
     * landed by device measurement against Kotlin. 70 % sits comfortably above the seeded 63 % — so
     * the lead can retune within a sane band without touching this test — and far below the 85 %
     * the uncapped banner produced, so the test still fails if the cap stops biting.
     */
    @Test
    fun moderationBanner_shortTextMessage_bubbleRespectsCap() {
        setBubbles(text(ModerationStatus.DISAPPROVED))

        val columnWidth = moderatedColumnWidthDp()
        val fractionOfScreen = columnWidth / SCREEN_WIDTH_DP

        assertTrue(
            "the moderation banner drove the bubble to " +
                "${"%.1f".format(100f * fractionOfScreen)} % of the screen " +
                "(${columnWidth}dp of ${SCREEN_WIDTH_DP}dp) — the cap is not biting",
            fractionOfScreen <= 0.70f
        )
    }

    /**
     * ENG-39283 test 2 (invariant 3) — the banner wraps, and does **not** collapse.
     *
     * Two failure modes bracket the correct one, and both have been measured on this branch:
     * uncapped the sentence stayed on a single line and stretched the bubble; zeroed
     * (`ignoreIntrinsicWidth`, rejected at `332f8f872`) it fell to an 18.7dp column of 21 lines.
     * The cap must land between them — a sentence that wraps to a couple of lines and stays
     * readable, never clipped.
     */
    @Test
    fun moderationBanner_shortTextMessage_bannerWrapsNotCollapses() {
        setBubbles(text(ModerationStatus.DISAPPROVED))

        val layout = bannerLayout()
        val sentenceWidth = composeRule
            .onNodeWithText(BANNER_TEXT, useUnmergedTree = true)
            .getUnclippedBoundsInRoot().width.value

        assertFalse(
            "the moderation sentence is clipped: lineCount=${layout.lineCount}, " +
                "didOverflowWidth=${layout.didOverflowWidth}, " +
                "didOverflowHeight=${layout.didOverflowHeight}",
            layout.hasVisualOverflow
        )
        assertTrue(
            "expected the banner to wrap inside the capped bubble, but it rendered on " +
                "${layout.lineCount} line(s) — 1 line means the bubble was stretched to fit it, " +
                "many lines mean it collapsed into a narrow column",
            layout.lineCount in 2..5
        )
        assertTrue(
            "the moderation banner collapsed to ${sentenceWidth}dp of ${SCREEN_WIDTH_DP}dp — " +
                "a short message must not shatter the sentence into a narrow column",
            sentenceWidth >= 0.30f * SCREEN_WIDTH_DP
        )
    }

    /**
     * ENG-39283 test 3 (invariant 4) — a long moderated message still grows with its text.
     *
     * The cap constrains only the **banner's** contribution to the intrinsic-width query. The
     * message text's own contribution is untouched, so a long moderated message must still reach
     * the same width as the identical message unmoderated — measured on the body text, which is
     * what drives the column in that case.
     *
     * 🔴 This is the test that fails if the cap is applied to the whole bubble (a `widthIn(max=)`
     * on the content column, say) rather than to the banner's intrinsic contribution. That is the
     * specific mistake it exists to catch.
     */
    @Test
    fun moderationBanner_longTextMessage_bubbleStillGrowsWithText() {
        setBubbles(
            text(ModerationStatus.DISAPPROVED, body = LONG_TEXT),
            text(ModerationStatus.UNMODERATED, body = LONG_TEXT, messageId = 252_012L)
        )

        // Deliberately order-independent: onAllNodesWithText traversal order is not a contract,
        // and the assertion is symmetric anyway — both bubbles must be the same width, and
        // both must exceed the capped short-message column.
        val bodies = composeRule.onAllNodesWithText(LONG_TEXT, useUnmergedTree = true)
        val widths = listOf(
            bodies[0].getUnclippedBoundsInRoot().width.value,
            bodies[1].getUnclippedBoundsInRoot().width.value
        )

        assertEquals(
            "the cap clamped a long moderated message: the two bubbles for the same long text " +
                "measured $widths — moderated and unmoderated must be identical, because the " +
                "cap bounds only the banner's contribution, never the message text's",
            widths.max(), widths.min(), 0.5f
        )
        assertTrue(
            "a long moderated message ($widths) should be wider than the short moderated " +
                "bubble's capped column",
            widths.min() > 0.70f * SCREEN_WIDTH_DP
        )
    }
}
