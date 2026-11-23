package com.cometchat.uikit.kotlin.presentation.conversations.ui

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.shared.baseelements.date.CometChatDate
import com.cometchat.uikit.kotlin.presentation.shared.baseelements.date.DatePattern
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * ENG-39134 — the conversation row must print the **last message's** `sentAt`, not the
 * conversation's `updatedAt`.
 *
 * `updatedAt` is the server's LWW sync cursor, not a message clock. The local send/receive bump
 * deliberately never writes it (`OfflineStore.java:222-244`), and per BEH-CONV-05 the server never
 * bumps a **group's** `updatedAt` on a message at all — so the old render showed a permanently
 * stale time on every group row even fully online, a stale time on a cold read after an offline
 * send, and nothing at all on a just-sent row (`getConversationFromMessage` yields `updatedAt = 0`).
 * `updatedAt` survives only as the fallback for a conversation that has no last message.
 *
 * **How these pin it.** The item is built for real and bound through the public `setConversation`,
 * then the tail's `CometChatDate` text is compared against a reference `CometChatDate` formatted
 * with the timestamp we expect — so the assertion is on *which timestamp was chosen*, independent
 * of pattern, locale and timezone. The two candidate stamps sit in different years, so their
 * rendered strings are always distinct. Reverting the fix turns tests 1, 2 and 4 red.
 *
 * Messages are **real** [TextMessage] instances, never `MockFactory.createTextMessage` — that
 * factory returns Mockito mocks with stubbed getters, so `setSentAt` would be discarded and the
 * getter would keep returning the seed, pinning nothing.
 *
 * Mirrors: chatuikit-compose `ui/ConversationListItemTimestampTest`.
 *
 * Run: ./gradlew :chatuikit-kotlin:testDebugUnitTest --tests "*ConversationListItemTimestampTest"
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConversationListItemTimestampTest {

    private companion object {
        /** 13 Sep 2020 — the stale server cursor. */
        const val STALE_UPDATED_AT = 1_600_000_000L

        /** 14 Nov 2023 — the last message, far newer than the cursor. */
        const val LAST_MESSAGE_SENT_AT = 1_700_000_000L
    }

    // Binding the row resolves receipts/alignment through CometChat.getLoggedInUser(); mocked so
    // the test never needs CometChat.init.
    private lateinit var cometChatMock: MockedStatic<CometChat>

    @Before
    fun setUp() {
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
    }

    @After
    fun tearDown() {
        cometChatMock.close()
    }

    private fun user(uid: String = "cometchat-uid-1") = User().apply {
        this.uid = uid
        name = "Peer"
    }

    private fun group(guid: String = "rba-test") = Group().apply {
        this.guid = guid
        name = "rba test"
        groupType = CometChatConstants.GROUP_TYPE_PUBLIC
    }

    /** A real message — never a Mockito mock, whose stubbed getters would ignore [sentAt]. */
    private fun message(sentAt: Long, receiverType: String = CometChatConstants.RECEIVER_TYPE_USER) =
        TextMessage("cometchat-uid-1", "hello", receiverType).apply {
            id = 42
            sender = user("cometchat-uid-2")
            setSentAt(sentAt)
        }

    private fun conversation(
        updatedAt: Long,
        lastMessage: TextMessage?,
        isGroup: Boolean = false
    ) = Conversation(
        if (isGroup) "rba-test" else "cometchat-uid-1_user_cometchat-uid-2",
        if (isGroup) CometChatConstants.CONVERSATION_TYPE_GROUP
        else CometChatConstants.CONVERSATION_TYPE_USER
    ).apply {
        conversationWith = if (isGroup) group() else user()
        setUpdatedAt(updatedAt)
        setLastMessage(lastMessage)
    }

    /**
     * Binds the row inside a themed Activity — the Material views the item inflates need one — and
     * hands the block a [Row] view over the bound item.
     */
    private fun withRow(conversation: Conversation, block: (Row) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val item = CometChatConversationListItem(activity)
            activity.setContentView(item)
            item.setConversation(conversation)
            block(Row(activity, item))
        }
        scenario.close()
    }

    /** The bound row, plus the oracle for "which stamp won". */
    private class Row(private val context: Context, item: CometChatConversationListItem) {
        val dateView: CometChatDate = item.findViewById(R.id.date_view)
        val renderedText: String get() = dateView.findViewById<TextView>(R.id.tv_date).text.toString()

        /** What the shared date component prints for [timestamp], formatted the same way. */
        fun expectedTextFor(timestamp: Long): String =
            CometChatDate(context).apply { setDate(timestamp, DatePattern.DAY_DATE_TIME) }
                .findViewById<TextView>(R.id.tv_date).text.toString()
    }

    // ---- 1. the defect -------------------------------------------------------------------------

    @Test
    fun conversationRow_withLastMessage_showsLastMessageSentAt() {
        withRow(
            conversation(
                updatedAt = STALE_UPDATED_AT,
                lastMessage = message(LAST_MESSAGE_SENT_AT)
            )
        ) { row ->
            assertNotEquals(
                "guard: the two candidate stamps must render differently",
                row.expectedTextFor(STALE_UPDATED_AT),
                row.expectedTextFor(LAST_MESSAGE_SENT_AT)
            )
            assertEquals(row.expectedTextFor(LAST_MESSAGE_SENT_AT), row.renderedText)
            assertEquals(View.VISIBLE, row.dateView.visibility)
        }
    }

    // ---- 2. the online-visible case: a group row -----------------------------------------------

    @Test
    fun conversationRow_groupWithStaleUpdatedAt_showsLastMessageSentAt() {
        // BEH-CONV-05: the server never bumps a group's updatedAt on a message, so this row was
        // permanently stale even fully online.
        withRow(
            conversation(
                updatedAt = STALE_UPDATED_AT,
                lastMessage = message(LAST_MESSAGE_SENT_AT, CometChatConstants.RECEIVER_TYPE_GROUP),
                isGroup = true
            )
        ) { row ->
            assertEquals(row.expectedTextFor(LAST_MESSAGE_SENT_AT), row.renderedText)
            assertNotEquals(row.expectedTextFor(STALE_UPDATED_AT), row.renderedText)
        }
    }

    // ---- 3. invariant 2: no last message ⇒ updatedAt still renders ------------------------------

    @Test
    fun conversationRow_withoutLastMessage_fallsBackToUpdatedAt() {
        // The reason updatedAt was chosen originally, preserved verbatim by the fallback.
        withRow(conversation(updatedAt = STALE_UPDATED_AT, lastMessage = null)) { row ->
            assertEquals(row.expectedTextFor(STALE_UPDATED_AT), row.renderedText)
            assertEquals(View.VISIBLE, row.dateView.visibility)
        }
    }

    // ---- 4. invariant 3: a pending message renders its compose time -----------------------------

    @Test
    fun conversationRow_pendingMessage_showsComposeTime() {
        // Outbox.java:404 stamps setSentAt(composeSentAtSeconds(composeTs)) at enqueue (ENG-37513),
        // so a pre-ack message carries its compose time in epoch SECONDS while the in-memory
        // conversation built by getConversationFromMessage has updatedAt = 0. Pre-fix this row
        // rendered nothing at all; it must now render the compose time.
        val composeSentAt = System.currentTimeMillis() / 1000L
        withRow(conversation(updatedAt = 0L, lastMessage = message(composeSentAt))) { row ->
            assertEquals(View.VISIBLE, row.dateView.visibility)
            assertEquals(row.expectedTextFor(composeSentAt), row.renderedText)
            assertTrue("a pending row must not render blank", row.renderedText.isNotEmpty())
        }
    }

    // ---- 5. invariant 4: nothing usable ⇒ the date is hidden, never epoch zero -------------------

    @Test
    fun conversationRow_noUsableTimestamp_hidesDate() {
        // No last message and no updatedAt: the date view must be GONE — an epoch-zero date
        // ("01 Jan 1970") would be worse than nothing.
        withRow(conversation(updatedAt = 0L, lastMessage = null)) { row ->
            assertEquals(View.GONE, row.dateView.visibility)
        }
    }
}
