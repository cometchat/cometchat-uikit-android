package com.cometchat.sampleapp.kotlin.e2e

import android.view.KeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.sampleapp.kotlin.e2e.helpers.E2ETestConfig
import com.cometchat.sampleapp.kotlin.e2e.helpers.E2ETestHelper
import com.cometchat.sampleapp.kotlin.e2e.helpers.E2ETestHelper.PACKAGE
import com.cometchat.sampleapp.kotlin.e2e.helpers.E2ETestHelper.SHORT_TIMEOUT
import com.cometchat.sampleapp.kotlin.e2e.helpers.E2ETestHelper.TIMEOUT
import com.cometchat.sampleapp.kotlin.e2e.helpers.RestApiHelper
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
 * Rich-text composer E2E — Android parity with iOS RichTextComposerTests (TC-001…027).
 *
 * iOS asserts that each toolbar format produces the correct **markdown on send**. The Android
 * composer works the same way: applying a format adds a [RichTextFormatSpan] and, on send,
 * [MarkdownConverter.toMarkdown] serialises those spans to markdown delimiters
 * (BOLD=`**` · ITALIC=`_` · STRIKETHROUGH=`~~` · INLINE_CODE=`` ` `` · CODE_BLOCK=fenced ```` ``` ```` ·
 * BULLET=`- ` · ORDERED=`1. `). The sent message's raw text IS that markdown string.
 *
 * The displayed bubble *renders* markdown (strips the markers), so the delimiters can't be read off
 * the screen — instead each test reads the sent payload back over the SDK ([MessagesRequest]) for the
 * deterministic 1:1 partner and asserts the exact markdown.
 *
 * Interaction model: the composer applies a format to the EditText's **current selection**
 * (see CometChatMessageComposer.toggleFormat, the `selStart != selEnd` branch). Whole-text cases
 * select all (Ctrl+A); sub-range cases (TC-001/002) long-press a word to select just it — mirroring
 * iOS's double-tap word selection. The soft keyboard is kept clear of the toolbar so the format
 * button is tappable. The result is the identical sent markdown the manual toolbar flow produces.
 *
 * Covers only the iOS cases that are **passing** (not the blocked receive-render case).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class RichTextComposerE2ETest {

    private lateinit var device: UiDevice
    private val uikitPkg = "com.cometchat.uikit.kotlin"
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
        typeFormatAndSend("Bold$token", "ivFormatBold", "Bold")
        assertSentMarkdown(token, "**Bold$token**")
    }

    /**
     * TC-002 — Bold toggle-off closes the range: text after the bold segment is unformatted.
     * Selection equivalent: bold only the leading "AAA", leaving " rest" plain → `**AAA** rest`,
     * the identical sent markdown the manual bold-on / type / bold-off / type flow produces.
     */
    @Test
    fun test02_boldToggleOffClosesRange() {
        val token = token()
        openPartnerChat()
        typeFormatWordAndSend("AAA rest$token", 0.06, "ivFormatBold", "Bold")
        assertSentMarkdown(token, "**AAA** rest$token")
    }

    /** TC-005 — Italic produces _…_ markdown. */
    @Test
    fun test03_italicSendsMarkdown() {
        val token = token()
        openPartnerChat()
        typeFormatAndSend("Italic$token", "ivFormatItalic", "Italic")
        assertSentMarkdown(token, "_Italic${token}_")
    }

    /** TC-010 — Strikethrough produces ~~…~~ markdown. */
    @Test
    fun test04_strikethroughSendsMarkdown() {
        val token = token()
        openPartnerChat()
        typeFormatAndSend("Strike$token", "ivFormatStrikethrough", "Strikethrough")
        assertSentMarkdown(token, "~~Strike$token~~")
    }

    /** TC-024 — Inline code produces `…` markdown. */
    @Test
    fun test05_inlineCodeSendsMarkdown() {
        val token = token()
        openPartnerChat()
        typeFormatAndSend("Code$token", "ivFormatCode", "Code")
        assertSentMarkdown(token, "`Code$token`")
    }

    /** TC-026/027 — Code block wraps the text in a fenced block. */
    @Test
    fun test06_codeBlockSendsFenced() {
        val token = token()
        openPartnerChat()
        typeFormatAndSend("Block$token", "ivFormatCodeBlock", "Code Block")
        assertSentMarkdown(token, "```\nBlock$token\n```")
    }

    /** TC-018 — Bullet list prefixes the line with "- ". */
    @Test
    fun test07_bulletListPrefix() {
        val token = token()
        openPartnerChat()
        typeFormatAndSend("Item$token", "ivFormatBulletList", "Bullet List")
        assertSentMarkdown(token, "- Item$token")
    }

    /** TC-015 — Numbered list prefixes the line with "1. ". */
    @Test
    fun test08_numberedListPrefix() {
        val token = token()
        openPartnerChat()
        typeFormatAndSend("Item$token", "ivFormatOrderedList", "Ordered List")
        assertSentMarkdown(token, "1. Item$token")
    }

    /** TC-001 — selection-based flow: select one word, Bold wraps just that word. */
    @Test
    fun test09_boldOnSelectedWord() {
        val token = token()
        openPartnerChat()
        // Long-press selects the whole first word "WORD"; the rest stays plain.
        typeFormatWordAndSend("WORD tail$token", 0.06, "ivFormatBold", "Bold")
        assertSentMarkdown(token, "**WORD** tail$token")
    }

    // ─── Interaction helpers ─────────────────────────────────────────────────────────

    private fun token(): String = "_" + (System.currentTimeMillis() % 1_000_000L).toString()

    /**
     * Whole-text case: set [text], select all (Ctrl+A) while focused, dismiss the IME so the
     * rich-text toolbar (which the IME covers) is on-screen, then tap the format button and send.
     * The select-all selection survives the IME closing.
     */
    private fun typeFormatAndSend(text: String, formatResId: String, formatDesc: String) {
        val editText = device.wait(Until.findObject(By.res(PACKAGE, "etMessageInput")), TIMEOUT)
        assertNotNull("Composer input (etMessageInput) not found", editText)

        editText!!.click()          // focus + open IME
        Thread.sleep(600)
        editText.text = text        // ACTION_SET_TEXT on the real composer field
        Thread.sleep(600)

        selectAll()                 // Ctrl+A while focused
        closeKeyboard()             // reveal the toolbar; select-all survives the IME closing
        tapFormat(formatResId, formatDesc)
        sendComposer(text)
    }

    /**
     * Sub-range case: set [text] without focusing (so the IME never opens and the toolbar stays
     * visible), then long-press over the word at [wordXFraction] to select just that word —
     * mirroring iOS's double-tap word selection — then tap the format button and send.
     */
    private fun typeFormatWordAndSend(text: String, wordXFraction: Double, formatResId: String, formatDesc: String) {
        val editText = device.wait(Until.findObject(By.res(PACKAGE, "etMessageInput")), TIMEOUT)
        assertNotNull("Composer input (etMessageInput) not found", editText)

        editText!!.text = text      // set text without a click → IME stays closed, toolbar visible
        Thread.sleep(600)
        longPressWord(editText, wordXFraction)
        tapFormat(formatResId, formatDesc)
        // The word long-press leaves a text-selection action mode active, which hides the send
        // button. BACK clears that action mode (it does not leave the chat) — the applied span stays.
        device.pressBack()
        Thread.sleep(500)
        sendComposer(text)
    }

    private fun sendComposer(text: String) {
        val send = device.wait(Until.findObject(By.res(PACKAGE, "ivSend")), SHORT_TIMEOUT)
            ?: device.findObject(By.desc("Send"))
        assertNotNull("Send button not found", send)
        send!!.click()
        device.wait(Until.gone(By.text(text)), SHORT_TIMEOUT) // composer clears on success
        Thread.sleep(1500)
    }

    /** Long-press over the word at [xFraction] of the input's width to select that word. */
    private fun longPressWord(editText: UiObject2, xFraction: Double) {
        val b = editText.visibleBounds
        val x = b.left + (b.width() * xFraction).toInt()
        val y = b.centerY()
        device.swipe(x, y, x, y, 140) // same start/end, many steps ≈ a long press
        Thread.sleep(700)
    }

    private fun tapFormat(resId: String, desc: String) {
        val btn = device.findObject(By.res(PACKAGE, resId))
            ?: device.findObject(By.res(uikitPkg, resId))
            ?: device.findObject(By.desc(desc))
        assertNotNull("Format toolbar button '$desc' ($resId) not found on the composer toolbar", btn)
        btn!!.click()
        Thread.sleep(400)
    }

    /** Dismiss the soft keyboard (first BACK only closes the IME, keeping the screen). */
    private fun closeKeyboard() {
        device.pressBack()
        Thread.sleep(500)
    }

    private fun selectAll() {
        device.pressKeyCode(KeyEvent.KEYCODE_A, KeyEvent.META_CTRL_ON)
        Thread.sleep(250)
    }


    // ─── Navigation + read-back ──────────────────────────────────────────────────────

    /**
     * Open the deterministic 1:1 partner's chat. Seeds one peer→me message over REST so the
     * conversation row is guaranteed present near the top, then opens it from the Chats tab via the
     * proven `conversations_item_container` + uid selector (mirrors ConversationsE2ETest CONV-04).
     */
    private fun openPartnerChat() {
        try {
            RestApiHelper.sendMessage(
                sender = partnerUid,
                receiver = E2ETestConfig.LOGGED_IN_UID,
                text = "rt-seed" + System.currentTimeMillis()
            )
        } catch (_: Exception) { /* a conversation may already exist — fall through to open it */ }

        E2ETestHelper.navigateToTab(device, "Chats")
        val byRes = By.res(PACKAGE, "conversations_item_container")
        var row: UiObject2? = E2ETestHelper.waitForObject(
            device, byRes.hasDescendant(By.text(partnerUid)), TIMEOUT
        ) ?: device.findObject(byRes.hasDescendant(By.textContains(partnerUid)))
        assertNotNull("Conversation row for partner '$partnerUid' not found in Chats list", row)
        row!!.click()
        assertNotNull("Composer did not open for partner chat", E2ETestHelper.waitForComposer(device))
    }

    /** Fetch the most recent TextMessage to the partner whose text contains [token]; assert it equals [expected]. */
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
