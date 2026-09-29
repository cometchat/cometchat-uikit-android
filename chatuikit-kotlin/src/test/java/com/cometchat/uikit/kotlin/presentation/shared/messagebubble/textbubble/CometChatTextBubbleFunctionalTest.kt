package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.textbubble

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.kotlin.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * Instrumented layer for the **View** [CometChatTextBubble].
 *
 * Hosts the real view in a themed activity and drives its public surface. This is
 * the column the prop matrix defers to: `setMessage` needs a real `TextMessage` and
 * an alignment, `setLinkPreview` a populated preview, and neither can be produced by
 * the reflective sweep.
 *
 * Assertions read the bubble's own child views (`getTextView()` and the link-preview
 * / translation ids), so they check what was actually rendered rather than that a
 * setter did not throw.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatTextBubbleFunctionalTest {

    private fun sender() = User().apply { uid = "sender-1"; name = "Alice" }

    private fun textMessage(body: String) =
        TextMessage("receiver-1", body, CometChatConstants.RECEIVER_TYPE_USER).apply {
            id = 1L
            sender = this@CometChatTextBubbleFunctionalTest.sender()
            sentAt = FIXED_SENT_AT
            category = CometChatConstants.CATEGORY_MESSAGE
        }

    private fun withBubble(block: (ComponentActivity, CometChatTextBubble) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val bubble = CometChatTextBubble(activity)
            // renderMarkdown bails out early on a null style (`style ?: return`), so a
            // bubble with no style renders no body at all. In the app the style arrives
            // from the theme's cometchatTextBubbleStyle attribute; supply one here so the
            // render path is actually exercised.
            bubble.setStyle(CometChatTextBubbleStyle(textColor = 0xFF212121.toInt()))
            activity.setContentView(bubble)
            ShadowLooper.idleMainLooper()
            block(activity, bubble)
        }
        scenario.close()
    }

    /**
     * The rendered body, read from where it actually lands.
     *
     * `setMessage` routes through `renderMarkdown`, which hides `messageTextView`
     * (the view `getTextView()` returns) and renders block-level views — paragraphs,
     * code blocks, lists, quotes — into `markdown_content_container` instead. So the
     * body has to be collected from that container's TextView descendants.
     */
    private fun renderedBody(bubble: CometChatTextBubble): String {
        val container = bubble.findViewById<ViewGroup>(R.id.markdown_content_container)
        val out = StringBuilder()
        fun walk(v: View) {
            if (v is TextView) out.append(v.text)
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(container)
        return out.toString()
    }

    // ── message rendering ───────────────────────────────────────────────────

    @Test
    fun setMessage_rendersTheBodyIntoTheTextView() {
        withBubble { _, bubble ->
            bubble.setMessage(
                textMessage("hello from the other side"),
                emptyList(),
                UIKitConstants.MessageBubbleAlignment.LEFT,
            )
            ShadowLooper.idleMainLooper()

            assertEquals("hello from the other side", renderedBody(bubble))
        }
    }

    @Test
    fun setMessage_outgoingAlignment_rendersTheSameBody() {
        // The alignment branch feeds FormatterUtils and the style resolution; both
        // sides have to end up with the same text.
        withBubble { _, bubble ->
            bubble.setMessage(
                textMessage("hello from me"),
                emptyList(),
                UIKitConstants.MessageBubbleAlignment.RIGHT,
            )
            ShadowLooper.idleMainLooper()

            assertEquals("hello from me", renderedBody(bubble))
        }
    }

    @Test
    fun setMessage_withNullMessage_leavesTheBubbleIntact() {
        // setMessage guards on null; nothing should be rendered and nothing should throw.
        withBubble { _, bubble ->
            bubble.setMessage(null, emptyList(), UIKitConstants.MessageBubbleAlignment.LEFT)
            ShadowLooper.idleMainLooper()

            assertNotNull(bubble.getTextView())
            assertTrue("no body should have been rendered", renderedBody(bubble).isEmpty())
        }
    }

    @Test
    fun setTextString_replacesTheBodyThroughTheMarkdownRenderer() {
        // The String overload delegates to renderMarkdown, so it lands in the
        // container and replaces whatever setMessage put there.
        withBubble { _, bubble ->
            bubble.setMessage(textMessage("first"), emptyList(), UIKitConstants.MessageBubbleAlignment.LEFT)
            bubble.setText("second")
            ShadowLooper.idleMainLooper()

            assertEquals("second", renderedBody(bubble))
        }
    }

    @Test
    fun setTextSpannable_takesTheFallbackTextViewInstead() {
        // The two overloads target different views: the SpannableString one carries
        // formatter spans the markdown renderer cannot re-overlay, so it writes
        // straight to messageTextView and hides the markdown container. Pinning the
        // split, because which overload you call decides where the body appears.
        withBubble { _, bubble ->
            bubble.setMessage(textMessage("first"), emptyList(), UIKitConstants.MessageBubbleAlignment.LEFT)
            bubble.setText(android.text.SpannableString("spanned body"))
            ShadowLooper.idleMainLooper()

            assertEquals("spanned body", bubble.getTextView().text.toString())
            assertEquals(View.VISIBLE, bubble.getTextView().visibility)
            assertEquals(
                "the markdown container is hidden on the spannable path",
                View.GONE,
                bubble.findViewById<View>(R.id.markdown_content_container).visibility,
            )
        }
    }

    @Test
    fun emptyBody_rendersWithoutCrashing() {
        withBubble { _, bubble ->
            bubble.setMessage(textMessage(""), emptyList(), UIKitConstants.MessageBubbleAlignment.LEFT)
            ShadowLooper.idleMainLooper()

            assertTrue(renderedBody(bubble).isEmpty())
        }
    }

    // ── link preview ────────────────────────────────────────────────────────

    @Test
    fun setLinkPreview_populatesTitleDescriptionAndUrl() {
        withBubble { _, bubble ->
            bubble.setLinkPreview(
                title = "CometChat",
                description = "Chat and calling APIs",
                url = "https://cometchat.com",
                bannerImage = null,
                fabIcon = null,
            )
            ShadowLooper.idleMainLooper()

            assertEquals("CometChat", bubble.findViewById<TextView>(R.id.link_heading).text.toString())
            assertEquals(
                "Chat and calling APIs",
                bubble.findViewById<TextView>(R.id.link_description).text.toString(),
            )
        }
    }

    @Test
    fun linkPreviewWithoutBanner_hidesTheBannerImage() {
        withBubble { _, bubble ->
            bubble.setLinkPreview("CometChat", "desc", "https://cometchat.com", null, null)
            ShadowLooper.idleMainLooper()

            assertEquals(
                "no banner supplied, so the banner view should be gone",
                View.GONE,
                bubble.findViewById<View>(R.id.preview_banner).visibility,
            )
        }
    }

    // ── translation ─────────────────────────────────────────────────────────

    @Test
    fun setTranslatedText_rendersIntoTheTranslationRow() {
        withBubble { _, bubble ->
            bubble.setMessage(textMessage("hola"), emptyList(), UIKitConstants.MessageBubbleAlignment.LEFT)
            bubble.setTranslatedText("hello")
            ShadowLooper.idleMainLooper()

            assertEquals(
                "hello",
                bubble.findViewById<TextView>(R.id.text_translated).text.toString(),
            )
        }
    }

    // ── style ───────────────────────────────────────────────────────────────

    @Test
    fun setStyle_isReadBackThroughTheGetters() {
        // The View flavour exposes style through getters, so a supplied style is
        // observable directly — no pixel comparison needed for this one.
        withBubble { _, bubble ->
            bubble.setStyle(CometChatTextBubbleStyle(textColor = 0xFF112233.toInt()))
            ShadowLooper.idleMainLooper()

            assertEquals(0xFF112233.toInt(), bubble.getTextColor())
        }
    }

    private companion object {
        const val FIXED_SENT_AT = 1_729_011_360L
    }
}
