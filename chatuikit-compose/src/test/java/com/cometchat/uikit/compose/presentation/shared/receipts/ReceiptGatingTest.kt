package com.cometchat.uikit.compose.presentation.shared.receipts

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.MessageStatus
import com.cometchat.chat.models.TextMessage
import com.cometchat.uikit.compose.presentation.messagelist.utils.isSwipeToReplyEligible
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Receipt gating on the offline path (W4, ENG-41089; G5, BEH-SEND-30/31/32), Compose stack: a
 * failed send shows the failed icon and is checked first; an unsent row (no server id) shows the
 * clock, never a receipt; and an unsent row cannot be swiped to reply.
 */
class ReceiptGatingTest : StringSpec({

    fun text(id: Long = 0L) = TextMessage("peer", "hi", CometChatConstants.RECEIVER_TYPE_USER).apply {
        this.id = id
        sentAt = 100L
    }

    "a FAILED send shows the failed icon, checked before any receipt" {
        val failed = text().apply {
            status = MessageStatus.FAILED
            readAt = 300L
        }
        MessageReceiptUtils.getMessageReceipt(failed) shouldBe Receipt.ERROR
    }

    "an unsent row shows the clock, never a receipt" {
        val pending = text().apply {
            status = MessageStatus.PENDING
            deliveredAt = 200L
            readAt = 300L
        }
        MessageReceiptUtils.getMessageReceipt(pending) shouldBe Receipt.IN_PROGRESS
    }


    "a sent row keeps today's receipts" {
        MessageReceiptUtils.getMessageReceipt(text(5L).apply { readAt = 300L }) shouldBe Receipt.READ
        MessageReceiptUtils.getMessageReceipt(text(5L).apply { deliveredAt = 200L }) shouldBe Receipt.DELIVERED
        MessageReceiptUtils.getMessageReceipt(text(5L)) shouldBe Receipt.SENT
    }

    "swipe-to-reply stays blocked on an unsent row" {
        isSwipeToReplyEligible(text().apply { status = MessageStatus.PENDING }) shouldBe false
        isSwipeToReplyEligible(text(5L)) shouldBe true
    }
})
