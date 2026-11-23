package com.cometchat.uikit.compose.presentation.conversations.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.conversations.style.CometChatConversationListItemStyle
import com.cometchat.uikit.compose.presentation.shared.interfaces.DateTimeFormatterCallback
import com.cometchat.uikit.compose.theme.CometChatTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

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
 * **How these pin it.** [DefaultTrailingView] hands its chosen timestamp to `CometChatDate`, which
 * routes every formatting branch through the supplied [DateTimeFormatterCallback]. [StampCapture]
 * answers every branch with `TS:<millis>`, so the rendered text names the exact timestamp the
 * component picked — format-, locale- and timezone-independent. Reverting the fix turns each of
 * tests 1, 2 and 4 red on that text.
 *
 * Messages are **real** [TextMessage] instances, never `MockFactory.createTextMessage` — that
 * factory returns Mockito mocks with stubbed getters, so `setSentAt` would be discarded and the
 * getter would keep returning the seed, pinning nothing.
 *
 * Mirrors: chatuikit-kotlin `ui/ConversationListItemTimestampTest`.
 *
 * Run: ./gradlew :chatuikit-compose:testDebugUnitTest --tests "*ConversationListItemTimestampTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class ConversationListItemTimestampTest {

    @get:Rule
    val composeRule = createComposeRule()

    private companion object {
        /** 13 Sep 2020 — the stale server cursor. */
        const val STALE_UPDATED_AT = 1_600_000_000L

        /** 14 Nov 2023 — the last message, far newer than the cursor. */
        const val LAST_MESSAGE_SENT_AT = 1_700_000_000L

        fun stamp(seconds: Long) = "TS:${seconds * 1000L}"
    }

    /** Answers every `CometChatDate` formatting branch with the timestamp it was handed. */
    private object StampCapture : DateTimeFormatterCallback {
        private fun mark(timestampMillis: Long) = "TS:$timestampMillis"
        override fun time(timestamp: Long) = mark(timestamp)
        override fun today(timestamp: Long) = mark(timestamp)
        override fun yesterday(timestamp: Long) = mark(timestamp)
        override fun lastWeek(timestamp: Long) = mark(timestamp)
        override fun otherDays(timestamp: Long) = mark(timestamp)
        override fun minute(timestamp: Long) = mark(timestamp)
        override fun minutes(diffInMinutesFromNow: Long, timestamp: Long) = mark(timestamp)
        override fun hour(timestamp: Long) = mark(timestamp)
        override fun hours(diffInHourFromNow: Long, timestamp: Long) = mark(timestamp)
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

    private fun render(conversation: Conversation) {
        composeRule.setContent {
            CometChatTheme {
                DefaultTrailingView(
                    conversation = conversation,
                    typingIndicator = null,
                    dateTimeFormatter = StampCapture,
                    style = CometChatConversationListItemStyle.default()
                )
            }
        }
    }

    // ---- 1. the defect -------------------------------------------------------------------------

    @Test
    fun conversationRow_withLastMessage_showsLastMessageSentAt() {
        render(
            conversation(
                updatedAt = STALE_UPDATED_AT,
                lastMessage = message(LAST_MESSAGE_SENT_AT)
            )
        )

        composeRule.onNodeWithText(stamp(LAST_MESSAGE_SENT_AT)).assertIsDisplayed()
        composeRule.onNodeWithText(stamp(STALE_UPDATED_AT)).assertDoesNotExist()
    }

    // ---- 2. the online-visible case: a group row -----------------------------------------------

    @Test
    fun conversationRow_groupWithStaleUpdatedAt_showsLastMessageSentAt() {
        // BEH-CONV-05: the server never bumps a group's updatedAt on a message, so this row was
        // permanently stale even fully online.
        render(
            conversation(
                updatedAt = STALE_UPDATED_AT,
                lastMessage = message(LAST_MESSAGE_SENT_AT, CometChatConstants.RECEIVER_TYPE_GROUP),
                isGroup = true
            )
        )

        composeRule.onNodeWithText(stamp(LAST_MESSAGE_SENT_AT)).assertIsDisplayed()
        composeRule.onNodeWithText(stamp(STALE_UPDATED_AT)).assertDoesNotExist()
    }

    // ---- 3. invariant 2: no last message ⇒ updatedAt still renders ------------------------------

    @Test
    fun conversationRow_withoutLastMessage_fallsBackToUpdatedAt() {
        // The reason updatedAt was chosen originally, preserved verbatim by the fallback.
        render(conversation(updatedAt = STALE_UPDATED_AT, lastMessage = null))

        composeRule.onNodeWithText(stamp(STALE_UPDATED_AT)).assertIsDisplayed()
    }

    // ---- 4. invariant 3: a pending message renders its compose time -----------------------------

    @Test
    fun conversationRow_pendingMessage_showsComposeTime() {
        // Outbox.java:404 stamps setSentAt(composeSentAtSeconds(composeTs)) at enqueue (ENG-37513),
        // so a pre-ack message carries its compose time in epoch SECONDS while the in-memory
        // conversation built by getConversationFromMessage has updatedAt = 0. Pre-fix this row
        // rendered blank; it must now render the compose time.
        val composeSentAt = System.currentTimeMillis() / 1000L
        render(conversation(updatedAt = 0L, lastMessage = message(composeSentAt)))

        composeRule.onNodeWithText(stamp(composeSentAt)).assertIsDisplayed()
    }

    // ---- 5. invariant 4: nothing usable ⇒ the date is omitted, never epoch zero ------------------

    @Test
    fun conversationRow_noUsableTimestamp_hidesDate() {
        // No last message and no updatedAt: the composable must not be emitted at all — an
        // epoch-zero date ("01 Jan 1970") would be worse than nothing.
        render(conversation(updatedAt = 0L, lastMessage = null))

        composeRule.onNodeWithText(stamp(0L)).assertDoesNotExist()
    }
}
