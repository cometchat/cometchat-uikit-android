package com.cometchat.uikit.kotlin.presentation.shared.receipts

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.MessageStatus
import com.cometchat.chat.models.TextMessage
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Receipt gating on the offline path (W4, ENG-41089; G5, BEH-SEND-30/32), View stack: a failed
 * send shows the failed icon and is checked first; an unsent row (no server id) shows the clock,
 * never a receipt, even if a receipt timestamp is present.
 */
class ReceiptGatingTest {

    private fun text(id: Long = 0L) = TextMessage("peer", "hi", CometChatConstants.RECEIVER_TYPE_USER).apply {
        this.id = id
        sentAt = 100L
    }

    @Test
    fun failedStatusShowsTheFailedIconFirst() {
        val failed = text().apply {
            status = MessageStatus.FAILED
            readAt = 300L
        }
        assertEquals(ReceiptStatus.ERROR, MessageReceiptUtils.getMessageReceipt(failed))
    }

    @Test
    fun anUnsentRowShowsTheClockNeverAReceipt() {
        val pending = text().apply {
            status = MessageStatus.PENDING
            deliveredAt = 200L
            readAt = 300L
        }
        assertEquals(ReceiptStatus.IN_PROGRESS, MessageReceiptUtils.getMessageReceipt(pending))
    }


    @Test
    fun aSentRowKeepsTodaysReceipts() {
        assertEquals(ReceiptStatus.READ, MessageReceiptUtils.getMessageReceipt(text(5L).apply { readAt = 300L }))
        assertEquals(ReceiptStatus.DELIVERED, MessageReceiptUtils.getMessageReceipt(text(5L).apply { deliveredAt = 200L }))
        assertEquals(ReceiptStatus.SENT, MessageReceiptUtils.getMessageReceipt(text(5L)))
    }
}
