package com.cometchat.uikit.kotlin.presentation.messagelist

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.TextMessage
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.kotlin.presentation.messagelist.ui.quickReactionsVisible
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The long-press sheet's quick reactions (View stack). Offline off: today's rules only, an unsent
 * row included. Offline on: an unsent row (no server id) gets none (G5). Mirrors the Compose test.
 */
class OfflineQuickReactionsGateTest : FunSpec({

    fun text(id: Long) = TextMessage("peer", "hi", CometChatConstants.RECEIVER_TYPE_USER).apply { this.id = id }

    test("offline off: quick reactions follow today's rules, whatever the server id") {
        quickReactionsVisible(text(0L), true, null, offlineSendUiOn = false) shouldBe true
        quickReactionsVisible(text(5L), true, null, offlineSendUiOn = false) shouldBe true
        quickReactionsVisible(text(0L), false, null, offlineSendUiOn = false) shouldBe false
        quickReactionsVisible(
            text(5L), true, UIKitConstants.ModerationConstants.DISAPPROVED, offlineSendUiOn = false
        ) shouldBe false
    }

    test("offline on: hidden for an unsent row, shown for a sent one") {
        quickReactionsVisible(text(0L), true, null, offlineSendUiOn = true) shouldBe false
        quickReactionsVisible(text(5L), true, null, offlineSendUiOn = true) shouldBe true
        quickReactionsVisible(text(5L), false, null, offlineSendUiOn = true) shouldBe false
    }
})
