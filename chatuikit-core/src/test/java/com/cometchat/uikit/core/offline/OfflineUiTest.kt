package com.cometchat.uikit.core.offline

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.model.StreamMessage
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.mockito.MockedStatic
import org.mockito.Mockito

/**
 * [OfflineUi] per-stack identity. With offline off it must equal today's Compose key and today's
 * Views `areItemsTheSame` exactly (copied below as the oracle); with offline on an own optimistic
 * row keeps its identity across its ack.
 */
class OfflineUiTest : FunSpec({

    lateinit var cometChatMock: MockedStatic<CometChat>

    beforeTest {
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        cometChatMock.`when`<User> { CometChat.getLoggedInUser() }.thenReturn(User().apply { uid = "me" })
    }
    afterTest { cometChatMock.close() }

    fun msg(id: Long = 0L, muid: String? = null, localId: String? = null, uid: String = "me"): BaseMessage =
        TextMessage("r1", "hi", CometChatConstants.RECEIVER_TYPE_USER).apply {
            this.id = id
            if (muid != null) this.muid = muid
            this.localMessageId = localId
            this.sender = User().apply { this.uid = uid }
        }

    fun stream(muid: String?): BaseMessage =
        StreamMessage("r1", CometChatConstants.RECEIVER_TYPE_USER, "").apply { if (muid != null) this.muid = muid }

    // Oracles: today's code, verbatim.
    fun composeOracle(m: BaseMessage): Any = if (m.id == 0L) m.muid ?: m.hashCode().toLong() else m.id
    fun viewsOracle(a: BaseMessage, b: BaseMessage): Boolean {
        val sa = a is StreamMessage
        val sb = b is StreamMessage
        if (sa && sb) {
            return if (!a.muid.isNullOrEmpty() && !b.muid.isNullOrEmpty()) a.muid == b.muid else a === b
        }
        if (sa || sb) return false
        return a.id == b.id
    }

    test("offline off: Compose key equals today's key for acked, pending-with-muid, pending-without-muid, stream") {
        val rows = listOf(msg(id = 5L), msg(id = 0L, muid = "m1"), msg(id = 0L), stream("stream_1"), stream(null))
        rows.forEach { OfflineUi.renderKey(it, offlineOn = false) shouldBe composeOracle(it) }
    }

    test("offline off: a pending row is never keyed 0 and two pending rows get distinct keys") {
        val a = msg(id = 0L, muid = "m1")
        val b = msg(id = 0L, muid = "m2")
        OfflineUi.renderKey(a, false) shouldNotBe 0L
        OfflineUi.renderKey(a, false) shouldNotBe OfflineUi.renderKey(b, false)
    }

    test("offline off: Views areItemsTheSame equals today's rule on every pairing") {
        val rows = listOf(msg(id = 5L), msg(id = 5L), msg(id = 6L), msg(id = 0L, muid = "m1"),
            stream("s1"), stream("s1"), stream("s2"), stream(null))
        for (a in rows) for (b in rows) {
            OfflineUi.isSameItem(a, b, offlineOn = false) shouldBe viewsOracle(a, b)
        }
    }

    test("offline on: an own optimistic row and its acked copy share one identity on both stacks") {
        val pending = msg(id = 0L, localId = "l1")
        val acked = msg(id = 9L, localId = "l1")
        OfflineUi.renderKey(pending, true) shouldBe OfflineUi.renderKey(acked, true)
        OfflineUi.isSameItem(pending, acked, true) shouldBe true
        // Today's identity would treat them as different rows.
        OfflineUi.isSameItem(pending, acked, false) shouldBe false
    }

    test("offline on: streams and key-less rows keep today's identity (no shared s:0 key)") {
        OfflineUi.renderKey(stream("stream_1"), true) shouldBe "stream_1"
        val bare = msg(id = 0L, uid = "other")
        OfflineUi.renderKey(bare, true) shouldBe composeOracle(bare)
        OfflineUi.isSameItem(msg(id = 0L, uid = "other"), msg(id = 0L, uid = "other"), true) shouldBe true // today: id == id
    }
})
