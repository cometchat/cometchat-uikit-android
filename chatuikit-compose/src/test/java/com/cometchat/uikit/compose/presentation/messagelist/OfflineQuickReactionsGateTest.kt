package com.cometchat.uikit.compose.presentation.messagelist

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.enums.ModerationStatus
import com.cometchat.chat.models.TextMessage
import com.cometchat.uikit.compose.presentation.messagelist.ui.showQuickReactions
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The long-press sheet's quick reactions (Compose stack). Offline off: today's rules only, an
 * unsent row included. Offline on: an unsent row (no server id) gets none (G5). Mirrors the View test.
 */
class OfflineQuickReactionsGateTest : FunSpec({

    fun text(id: Long) = TextMessage("peer", "hi", CometChatConstants.RECEIVER_TYPE_USER).apply { this.id = id }

    test("offline off: quick reactions follow today's rules, whatever the server id") {
        showQuickReactions(text(0L), hideMessageReactionOption = false, offlineSendUiOn = false) shouldBe true
        showQuickReactions(text(5L), hideMessageReactionOption = false, offlineSendUiOn = false) shouldBe true
        showQuickReactions(text(0L), hideMessageReactionOption = true, offlineSendUiOn = false) shouldBe false
        val disapproved = text(5L).apply { moderationStatus = ModerationStatus.DISAPPROVED }
        showQuickReactions(disapproved, hideMessageReactionOption = false, offlineSendUiOn = false) shouldBe false
    }

    test("offline on: hidden for an unsent row, shown for a sent one") {
        showQuickReactions(text(0L), hideMessageReactionOption = false, offlineSendUiOn = true) shouldBe false
        showQuickReactions(text(5L), hideMessageReactionOption = false, offlineSendUiOn = true) shouldBe true
        showQuickReactions(text(5L), hideMessageReactionOption = true, offlineSendUiOn = true) shouldBe false
    }
})
