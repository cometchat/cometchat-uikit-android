package com.cometchat.uikit.core.offline

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Row/render identity and the G6 lookup. Uses real SDK model instances (setters are allowed in
 * tests; the invariant that production never writes these fields is checked elsewhere).
 */
class MessageKeyTest : FunSpec({

    fun msg(
        id: Long = 0L,
        localId: String? = null,
        muid: String? = null,
        uid: String = "me",
        conv: String? = "c1"
    ): BaseMessage = TextMessage("r1", "hi", CometChatConstants.RECEIVER_TYPE_USER).apply {
        this.id = id
        this.localMessageId = localId
        if (muid != null) this.muid = muid
        this.sender = User().apply { this.uid = uid }
        if (conv != null) this.conversationId = conv
    }

    test("rowKey: acked → server id; un-acked → local id; else muid") {
        MessageKey.rowKey(msg(id = 5L)) shouldBe "s:5"
        MessageKey.rowKey(msg(id = 0L, localId = "l1")) shouldBe "l:l1"
        MessageKey.rowKey(msg(id = 0L, localId = null, muid = "u9")) shouldBe "l:m:u9"
    }

    test("renderKey: own message stays on local id across its ack; others use server id") {
        // Own, un-acked and acked both render on the local id (stable across the swap).
        MessageKey.renderKey(msg(id = 0L, localId = "l1", uid = "me"), "me") shouldBe "l1"
        MessageKey.renderKey(msg(id = 7L, localId = "l1", uid = "me"), "me") shouldBe "l1"
        // Someone else's message renders on the server id.
        MessageKey.renderKey(msg(id = 7L, localId = "l1", uid = "other"), "me") shouldBe "s:7"
        // Own with no local id/muid falls back to the server id.
        MessageKey.renderKey(msg(id = 7L, uid = "me"), "me") shouldBe "s:7"
    }

    test("findMatch: server id wins") {
        val rows = listOf(msg(id = 3L), msg(id = 4L))
        MessageKey.findMatch(rows, msg(id = 4L)) shouldBe rows[1]
    }

    test("findMatch: localMessageId matches only an un-acked row, same sender and conversation") {
        val unacked = msg(id = 0L, localId = "l1", uid = "me", conv = "c1")
        val rows = listOf(unacked)
        // incoming carries the same local id + server id now assigned
        MessageKey.findMatch(rows, msg(id = 9L, localId = "l1", uid = "me", conv = "c1")) shouldBe unacked
    }

    test("findMatch: no match across a different sender") {
        val rows = listOf(msg(id = 0L, localId = "l1", uid = "me", conv = "c1"))
        MessageKey.findMatch(rows, msg(id = 9L, localId = "l1", uid = "other", conv = "c1")) shouldBe null
    }

    test("findMatch: no match across a different conversation") {
        val rows = listOf(msg(id = 0L, localId = "l1", uid = "me", conv = "c1"))
        MessageKey.findMatch(rows, msg(id = 9L, localId = "l1", uid = "me", conv = "c2")) shouldBe null
    }

    test("findMatch: never matches localMessageId against a row that already has a server id") {
        val rows = listOf(msg(id = 5L, localId = "l1", uid = "me", conv = "c1"))
        MessageKey.findMatch(rows, msg(id = 0L, localId = "l1", uid = "me", conv = "c1")) shouldBe null
    }

    test("findMatch: muid fallback on an un-acked row") {
        val unacked = msg(id = 0L, localId = null, muid = "u9", uid = "me")
        MessageKey.findMatch(listOf(unacked), msg(id = 0L, muid = "u9", uid = "me")) shouldBe unacked
    }

    test("findMatch: two rows with nothing in common never collide (null safety)") {
        val a = msg(id = 0L, localId = null, muid = null, uid = "me")
        val b = msg(id = 0L, localId = null, muid = null, uid = "me")
        MessageKey.findMatch(listOf(a), b) shouldBe null
    }

    test("alias map records acks, ignores blanks, and clears") {
        val map = AliasMap()
        map.onAck("l1", 42L)
        map.serverIdFor("l1") shouldBe 42L
        map.onAck("", 7L)
        map.serverIdFor("") shouldBe null
        map.onAck("l2", 0L)
        map.serverIdFor("l2") shouldBe null
        map.serverIdFor(null) shouldBe null
        map.clear()
        map.serverIdFor("l1") shouldBe null
    }
})
