package com.cometchat.sampleapp.compose.e2e

import android.view.KeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.sampleapp.compose.e2e.helpers.E2ETestConfig
import com.cometchat.sampleapp.compose.e2e.helpers.E2ETestHelper
import com.cometchat.sampleapp.compose.e2e.helpers.E2ETestHelper.SHORT_TIMEOUT
import com.cometchat.sampleapp.compose.e2e.helpers.E2ETestHelper.TIMEOUT
import com.cometchat.sampleapp.compose.e2e.helpers.RestApiHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Rich-text composer E2E for sample-app-compose — parity with iOS RichTextComposerTests
 * (TC-001…027) and a mirror of the kotlin suite.
 *
 * The Compose composer serialises rich-text spans to markdown on send
 * (BOLD=`**` · ITALIC=`_` · STRIKETHROUGH=`~~` · INLINE_CODE=`` ` `` · CODE_BLOCK=fenced ```` ``` ```` ·
 * BULLET=`- ` · ORDERED=`1. `). The sent message's raw text IS that markdown; the bubble renders it
 * (strips the markers), so each test reads the sent payload back over the SDK ([MessagesRequest])
 * for the deterministic 1:1 partner and asserts the exact markdown.
 *
 * Compose has no resource IDs: the composer input is the sole EditText, and format buttons are
 * matched by their accessibility label (Bold / Italic / Strikethrough / Inline Code / Code Block /
 * Bullet List / Numbered List). Whole-text cases select-all (Ctrl+A); sub-range cases (TC-001/002)
 * long-press a word — iOS's double-tap word selection. The soft keyboard is kept clear of the
 * toolbar so the format button is tappable.
 *
 * Covers only the iOS cases that are **passing** (not the blocked receive-render case).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class RichTextComposerE2ETest {

    private lateinit var device: UiDevice
    private val partnerUid get() = E2ETestConfig.ONE_TO_ONE_UID

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // Pixel/API-34 stylus handwriting popup hijacks composer taps — disable it.
        device.executeShellCommand("settings put secure stylus_handwriting_enabled 0")
        E2ETestHelper.fullSetupAndLogin(device)
    }

    // ─── iOS-passing cases ─────────────────────────────────────────────────────────

    /** TC-006/001 — Bold via the toolbar produces **…** markdown. */
    @Test
    fun test01_boldSendsMarkdown() {
        val token = token()
        openPartnerChat()
        typeFormatAndSend("Bold$token", "Bold")
        assertSentMarkdown(token, "**Bold$token**")
    }

    /**
     * TC-002 — Bold toggle-off closes the range: text after the bold segment is unformatted.
     * Selection equivalent: bold only the leading word "AAA", leaving " rest" plain → `**AAA** rest`.
     */
    @Test
    fun test02_boldToggleOffClosesRange() {
        val token = token()
        openPartnerChat()
        typeFormatWordAndSend("AAA rest$token", 0.06, "Bold")
        assertSentMarkdown(token, "**AAA** rest$token")
    }

    /** TC-005 — Italic produces _…_ markdown. */
    @Test
    fun test03_italicSendsMarkdown() {
        val token = token()
        openPartnerChat()
        typeFormatAndSend("Italic$token", "Italic")
        assertSentMarkdown(token, "_Italic${token}_")
    }

    /** TC-010 — Strikethrough produces ~~…~~ markdown. */
    @Test
    fun test04_strikethroughSendsMarkdown() {
        val token = token()
        openPartnerChat()
        typeFormatAndSend("Strike$token", "Strikethrough")
        assertSentMarkdown(token, "~~Strike$token~~")
    }

    /** TC-024 — Inline code produces `…` markdown. */
    @Test
    fun test05_inlineCodeSendsMarkdown() {
        val token = token()
        openPartnerChat()
        typeFormatAndSend("Code$token", "Inline Code")
        assertSentMarkdown(token, "`Code$token`")
    }

    /** TC-026/027 — Code block wraps the text in a fenced block. */
    @Test
    fun test06_codeBlockSendsFenced() {
        val token = token()
        openPartnerChat()
        typeFormatAndSend("Block$token", "Code Block")
        assertSentMarkdown(token, "```\nBlock$token\n```")
    }

    /** TC-018 — Bullet list prefixes the line with "- ". */
    @Test
    fun test07_bulletListPrefix() {
        val token = token()
        openPartnerChat()
        typeFormatAndSend("Item$token", "Bullet List")
        assertSentMarkdown(token, "- Item$token")
    }

    /** TC-015 — Numbered list prefixes the line with "1. ". */
    @Test
    fun test08_numberedListPrefix() {
        val token = token()
        openPartnerChat()
        typeFormatAndSend("Item$token", "Numbered List")
        assertSentMarkdown(token, "1. Item$token")
    }

    /** TC-001 — selection-based flow: select one word, Bold wraps just that word. */
    @Test
    fun test09_boldOnSelectedWord() {
        val token = token()
        openPartnerChat()
        // Long-press selects the whole first word "WORD"; the rest stays plain.
        typeFormatWordAndSend("WORD tail$token", 0.06, "Bold")
        assertSentMarkdown(token, "**WORD** tail$token")
    }

    // ─── Interaction helpers ─────────────────────────────────────────────────────────

    private fun token(): String = "_" + (System.currentTimeMillis() % 1_000_000L).toString()

    private fun composerInput(): UiObject2? =
        device.wait(Until.findObject(By.clazz("android.widget.EditText")), TIMEOUT)

    /**
     * Whole-text case: set [text], select all (Ctrl+A) while focused, dismiss the IME so the
     * rich-text toolbar (which the IME covers) is on-screen, then tap the format button and send.
     */
    private fun typeFormatAndSend(text: String, formatDesc: String) {
        val editText = composerInput()
        assertNotNull("Composer input (EditText) not found", editText)

        editText!!.click()          // focus + open IME → the toolbar renders above the keyboard
        Thread.sleep(600)
        editText.text = text        // ACTION_SET_TEXT on the composer field
        Thread.sleep(800)

        selectAll()                 // Ctrl+A while focused
        tapFormat(formatDesc)       // toolbar is above the IME while focused — keep the keyboard open
        sendComposer(text)
    }

    /**
     * Sub-range case: focus the field (so the toolbar is shown above the keyboard), set [text], then
     * long-press over the word at [wordXFraction] to select just that word — iOS's double-tap word
     * selection — tap the format button, clear the selection action mode, and send.
     */
    private fun typeFormatWordAndSend(text: String, wordXFraction: Double, formatDesc: String) {
        val editText = composerInput()
        assertNotNull("Composer input (EditText) not found", editText)

        editText!!.click()          // focus + open IME → toolbar visible above the keyboard
        Thread.sleep(600)
        editText.text = text
        Thread.sleep(800)
        longPressWord(editText, wordXFraction)
        tapFormat(formatDesc)
        device.pressBack()          // clear the text-selection action mode (does not leave the chat)
        Thread.sleep(500)
        sendComposer(text)
    }

    private fun sendComposer(text: String) {
        val send = device.wait(Until.findObject(By.desc("Send message")), SHORT_TIMEOUT)
            ?: device.findObject(By.descContains("Send message"))
            ?: device.findObject(By.desc("Send"))
        assertNotNull("Send button not found", send)
        send!!.click()
        device.wait(Until.gone(By.text(text)), SHORT_TIMEOUT) // composer clears on success
        Thread.sleep(1500)
    }

    private fun tapFormat(desc: String) {
        // Right-side buttons can sit clipped at the edge; scroll the toolbar fully right first so
        // Inline Code / Code Block are wholly on-screen and not tapped at a clipped edge.
        if (desc == "Inline Code" || desc == "Code Block") {
            device.findObject(By.desc("Rich Text Toolbar"))?.let { bar ->
                repeat(3) { try { bar.scroll(Direction.RIGHT, 1.0f) } catch (_: Exception) {} ; Thread.sleep(300) }
            }
        }
        var btn = device.findObject(By.desc(desc)) ?: device.findObject(By.descContains(desc))
        // The toolbar scrolls horizontally; right-side buttons (e.g. Code Block) are off-screen and
        // absent from the semantics tree until scrolled in. Swipe left along the toolbar row —
        // anchored on a currently-visible button's Y — to reveal them.
        if (btn == null) {
            // Right-side buttons (Code Block) are off-screen in the horizontally-scrollable toolbar.
            // Scroll the toolbar container (it carries both the "Rich Text Toolbar" description and
            // the horizontal-scroll semantics) to bring them into view.
            val bar = device.findObject(By.desc("Rich Text Toolbar"))
            if (bar != null) {
                repeat(5) {
                    try { bar.scroll(Direction.RIGHT, 1.0f) } catch (_: Exception) {}
                    Thread.sleep(500)
                    btn = device.findObject(By.desc(desc)) ?: device.findObject(By.descContains(desc))
                    if (btn != null) return@repeat
                }
            }
            // Fallback: drag along the toolbar row anchored on a visible mid button.
            if (btn == null) {
                val anchor = device.findObject(By.desc("Bullet List"))
                    ?: device.findObject(By.desc("Numbered List"))
                val y = anchor?.visibleBounds?.centerY() ?: (device.displayHeight * 0.86).toInt()
                val w = device.displayWidth
                repeat(5) {
                    device.swipe(w * 85 / 100, y, w * 15 / 100, y, 60)
                    Thread.sleep(600)
                    btn = device.findObject(By.desc(desc)) ?: device.findObject(By.descContains(desc))
                    if (btn != null) return@repeat
                }
            }
        }
        assertNotNull("Format toolbar button '$desc' not found on the composer toolbar", btn)
        btn!!.click()
        Thread.sleep(400)
    }

    private fun selectAll() {
        device.pressKeyCode(KeyEvent.KEYCODE_A, KeyEvent.META_CTRL_ON)
        Thread.sleep(300)
    }

    /** Long-press over the word at [xFraction] of the input's width to select that word. */
    private fun longPressWord(editText: UiObject2, xFraction: Double) {
        val b = editText.visibleBounds
        val x = b.left + (b.width() * xFraction).toInt()
        val y = b.centerY()
        device.swipe(x, y, x, y, 140) // same start/end, many steps ≈ a long press
        Thread.sleep(700)
    }

    // ─── Navigation + read-back ──────────────────────────────────────────────────────

    /**
     * Open the deterministic 1:1 partner's chat. Seeds one peer→me message over REST so the
     * conversation row is present near the top, then opens it from the Chats tab (compose rows
     * expose the uid in their contentDescription).
     */
    private fun openPartnerChat() {
        try {
            RestApiHelper.sendMessage(
                sender = partnerUid,
                receiver = E2ETestConfig.LOGGED_IN_UID,
                text = "rt-seed" + System.currentTimeMillis()
            )
        } catch (_: Exception) { /* a conversation may already exist */ }

        E2ETestHelper.navigateToTab(device, "Chats")
        val row = E2ETestHelper.waitFor(device, By.descContains(partnerUid), TIMEOUT)
            ?: E2ETestHelper.waitFor(device, By.textContains(partnerUid), SHORT_TIMEOUT)
        assertNotNull("Conversation row for partner '$partnerUid' not found in Chats list", row)
        row!!.click()
        assertNotNull("Composer did not open for partner chat", composerInput())
    }

    /** Fetch the most recent TextMessage to the partner whose text contains [token]; assert equals [expected]. */
    private fun assertSentMarkdown(token: String, expected: String) {
        var actual: String? = null
        val deadline = System.currentTimeMillis() + 15_000L
        while (System.currentTimeMillis() < deadline && actual == null) {
            actual = fetchLastTextContaining(token)
            if (actual == null) Thread.sleep(1500)
        }
        assertNotNull("No sent message containing token '$token' found over the SDK", actual)
        assertEquals("Sent markdown payload mismatch", expected, actual)
    }

    private fun fetchLastTextContaining(token: String): String? {
        val request = MessagesRequest.MessagesRequestBuilder()
            .setUID(partnerUid)
            .setLimit(40)
            .build()
        val latch = CountDownLatch(1)
        var found: String? = null
        request.fetchPrevious(object : CometChat.CallbackListener<List<BaseMessage>>() {
            override fun onSuccess(messages: List<BaseMessage>) {
                found = messages.filterIsInstance<TextMessage>()
                    .lastOrNull { it.text?.contains(token) == true }
                    ?.text
                latch.countDown()
            }
            override fun onError(e: CometChatException?) { latch.countDown() }
        })
        latch.await(12, TimeUnit.SECONDS)
        return found
    }
}
