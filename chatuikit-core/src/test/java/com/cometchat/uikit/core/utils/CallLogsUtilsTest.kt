package com.cometchat.uikit.core.utils

import com.cometchat.calls.constants.CometChatCallsConstants
import com.cometchat.calls.model.CallGroup
import com.cometchat.calls.model.CallLog
import com.cometchat.calls.model.CallUser
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.User
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Tests for [CallLogsUtils], which classifies call logs for the call-history UI.
 *
 * Every classification is relative to the logged-in user, read from [CometChat.getLoggedInUser],
 * so each test fixes that user and then varies the log around them. The rules under test:
 *
 * - A call is outgoing when its initiator is the logged-in user.
 * - Only an *incoming* call can be missed; an outgoing unanswered call is not "missed" from
 *   this user's point of view.
 * - Incoming means neither outgoing nor missed — i.e. received and answered.
 * - A video call covers both "video" and "audio/video"; audio covers only "audio".
 * - Display name, avatar and uid all resolve to *the other party*, and for group calls to the
 *   group itself regardless of who initiated.
 *
 * `CallLog`, `CallUser` and `CallGroup` are mocked rather than constructed: the real types pull
 * in Android static initialisers that a JVM unit test cannot satisfy.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*CallLogsUtilsTest"
 */
class CallLogsUtilsTest : FunSpec({

    isolationMode = IsolationMode.SingleInstance

    val me = "logged-in-uid"
    val them = "other-uid"

    lateinit var cometChatMock: MockedStatic<CometChat>

    beforeTest {
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        val loggedInUser = mock<User>()
        whenever(loggedInUser.uid).thenReturn(me)
        cometChatMock.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(loggedInUser)
    }

    afterTest { cometChatMock.close() }

    fun callUser(uid: String, name: String = "Name-$uid", avatar: String? = "avatar-$uid"): CallUser {
        val u = mock<CallUser>()
        whenever(u.uid).thenReturn(uid)
        whenever(u.name).thenReturn(name)
        whenever(u.avatar).thenReturn(avatar)
        return u
    }

    fun userCall(
        initiatorUid: String,
        receiverUid: String,
        status: String = CometChatCallsConstants.CALL_STATUS_ONGOING,
        type: String = CometChatCallsConstants.CALL_TYPE_AUDIO,
        initiatorName: String = "Initiator",
        receiverName: String = "Receiver",
        initiatorAvatar: String? = "initiator-avatar",
        receiverAvatar: String? = "receiver-avatar"
    ): CallLog {
        // Build the participant mocks first: stubbing one mock inside an open whenever() on
        // another is what Mockito reports as UnfinishedStubbingException.
        val initiator = callUser(initiatorUid, initiatorName, initiatorAvatar)
        val receiver = callUser(receiverUid, receiverName, receiverAvatar)
        val log = mock<CallLog>()
        whenever(log.receiverType).thenReturn("user")
        whenever(log.status).thenReturn(status)
        whenever(log.type).thenReturn(type)
        whenever(log.initiator).thenReturn(initiator)
        whenever(log.receiver).thenReturn(receiver)
        return log
    }

    fun groupCall(name: String? = "Design Team", icon: String? = "group-icon", guid: String? = "g-1"): CallLog {
        val group = mock<CallGroup>()
        whenever(group.name).thenReturn(name)
        whenever(group.icon).thenReturn(icon)
        whenever(group.guid).thenReturn(guid)
        val initiator = callUser(them)
        val log = mock<CallLog>()
        whenever(log.receiverType).thenReturn(CometChatCallsConstants.RECEIVER_TYPE_GROUP)
        whenever(log.receiver).thenReturn(group)
        whenever(log.initiator).thenReturn(initiator)
        return log
    }

    // ==================== direction ====================

    test("a call I started is outgoing") {
        CallLogsUtils.isOutgoingCall(userCall(initiatorUid = me, receiverUid = them)) shouldBe true
    }

    test("a call someone else started is not outgoing") {
        CallLogsUtils.isOutgoingCall(userCall(initiatorUid = them, receiverUid = me)) shouldBe false
    }

    test("with no logged-in user, a call with an initiator is not outgoing") {
        cometChatMock.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(null)
        CallLogsUtils.isOutgoingCall(userCall(initiatorUid = them, receiverUid = me)) shouldBe false
    }

    test("a log whose initiator is a group, not a user, is not outgoing") {
        // initiator casts to CallUser? and yields null, so it cannot match the logged-in uid
        val groupAsInitiator = mock<CallGroup>()
        val log = mock<CallLog>()
        whenever(log.initiator).thenReturn(groupAsInitiator)
        CallLogsUtils.isOutgoingCall(log) shouldBe false
    }

    // ==================== missed ====================

    test("an incoming unanswered, missed or cancelled call counts as missed") {
        listOf(
            CometChatCallsConstants.CALL_STATUS_UNANSWERED,
            CometChatCallsConstants.CALL_STATUS_MISSED,
            CometChatCallsConstants.CALL_STATUS_CANCELLED
        ).forEach { status ->
            CallLogsUtils.isMissedCall(
                userCall(initiatorUid = them, receiverUid = me, status = status)
            ) shouldBe true
        }
    }

    test("an incoming call that was answered is not missed") {
        CallLogsUtils.isMissedCall(
            userCall(initiatorUid = them, receiverUid = me, status = CometChatCallsConstants.CALL_STATUS_ENDED)
        ) shouldBe false
    }

    test("an outgoing unanswered call is never missed — missed is from the receiver's side") {
        CallLogsUtils.isMissedCall(
            userCall(
                initiatorUid = me,
                receiverUid = them,
                status = CometChatCallsConstants.CALL_STATUS_UNANSWERED
            )
        ) shouldBe false
    }

    // ==================== incoming ====================

    test("incoming means received and answered") {
        CallLogsUtils.isIncomingCall(
            userCall(initiatorUid = them, receiverUid = me, status = CometChatCallsConstants.CALL_STATUS_ENDED)
        ) shouldBe true
    }

    test("a missed call is not incoming, and neither is an outgoing one") {
        CallLogsUtils.isIncomingCall(
            userCall(them, me, status = CometChatCallsConstants.CALL_STATUS_MISSED)
        ) shouldBe false
        CallLogsUtils.isIncomingCall(
            userCall(me, them, status = CometChatCallsConstants.CALL_STATUS_ENDED)
        ) shouldBe false
    }

    // ==================== media type ====================

    test("audio calls are audio and nothing else") {
        val log = userCall(me, them, type = CometChatCallsConstants.CALL_TYPE_AUDIO)
        CallLogsUtils.isAudioCall(log) shouldBe true
        CallLogsUtils.isVideoCall(log) shouldBe false
    }

    test("both video and audio/video count as a video call") {
        listOf(
            CometChatCallsConstants.CALL_TYPE_VIDEO,
            CometChatCallsConstants.CALL_TYPE_AUDIO_VIDEO
        ).forEach { type ->
            val log = userCall(me, them, type = type)
            CallLogsUtils.isVideoCall(log) shouldBe true
            CallLogsUtils.isAudioCall(log) shouldBe false
        }
    }

    // ==================== the other party ====================

    test("for a call I started, the other party is the receiver") {
        val log = userCall(me, them, receiverName = "Ada", receiverAvatar = "ada.png")
        CallLogsUtils.getDisplayName(log) shouldBe "Ada"
        CallLogsUtils.getAvatarUrl(log) shouldBe "ada.png"
        CallLogsUtils.getOtherParticipantUid(log) shouldBe them
    }

    test("for a call I received, the other party is the initiator") {
        val log = userCall(them, me, initiatorName = "Grace", initiatorAvatar = "grace.png")
        CallLogsUtils.getDisplayName(log) shouldBe "Grace"
        CallLogsUtils.getAvatarUrl(log) shouldBe "grace.png"
        CallLogsUtils.getOtherParticipantUid(log) shouldBe them
    }

    test("a group call resolves to the group, whoever started it") {
        val log = groupCall(name = "Design Team", icon = "team.png", guid = "guid-7")
        CallLogsUtils.getDisplayName(log) shouldBe "Design Team"
        CallLogsUtils.getAvatarUrl(log) shouldBe "team.png"
        CallLogsUtils.getOtherParticipantUid(log) shouldBe "guid-7"
    }

    test("a group call with missing fields degrades to empty strings and a null avatar") {
        val log = groupCall(name = null, icon = null, guid = null)
        CallLogsUtils.getDisplayName(log) shouldBe ""
        CallLogsUtils.getAvatarUrl(log) shouldBe null
        CallLogsUtils.getOtherParticipantUid(log) shouldBe ""
    }

    test("a group call whose receiver is not a CallGroup degrades rather than throwing") {
        val wrongType = callUser(them) // a CallUser where a CallGroup is expected
        val log = mock<CallLog>()
        whenever(log.receiverType).thenReturn(CometChatCallsConstants.RECEIVER_TYPE_GROUP)
        whenever(log.receiver).thenReturn(wrongType)
        CallLogsUtils.getDisplayName(log) shouldBe ""
        CallLogsUtils.getAvatarUrl(log) shouldBe null
        CallLogsUtils.getOtherParticipantUid(log) shouldBe ""
    }

    test("a user call with a missing counterparty degrades to empty strings and a null avatar") {
        val initiator = callUser(me)
        val log = mock<CallLog>()
        whenever(log.receiverType).thenReturn("user")
        whenever(log.initiator).thenReturn(initiator)
        whenever(log.receiver).thenReturn(null)
        CallLogsUtils.getDisplayName(log) shouldBe ""
        CallLogsUtils.getAvatarUrl(log) shouldBe null
        CallLogsUtils.getOtherParticipantUid(log) shouldBe ""
    }

    test("with no logged-in user, the other party resolves to the initiator") {
        cometChatMock.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(null)
        val log = userCall(them, me, initiatorName = "Grace", initiatorAvatar = "grace.png")
        // initiator?.uid is "other-uid" and loggedInUserId is null, so the else branch runs
        CallLogsUtils.getDisplayName(log) shouldBe "Grace"
        CallLogsUtils.getAvatarUrl(log) shouldBe "grace.png"
        CallLogsUtils.getOtherParticipantUid(log) shouldBe them
    }

    test("a user call whose initiator is not a CallUser falls to the initiator branch and degrades") {
        // the cast yields null, so initiator?.uid (null) != loggedInUserId and the else branch runs
        val groupAsInitiator = mock<CallGroup>()
        val receiver = callUser(me)
        val log = mock<CallLog>()
        whenever(log.receiverType).thenReturn("user")
        whenever(log.initiator).thenReturn(groupAsInitiator)
        whenever(log.receiver).thenReturn(receiver)
        CallLogsUtils.getDisplayName(log) shouldBe ""
        CallLogsUtils.getAvatarUrl(log) shouldBe null
        CallLogsUtils.getOtherParticipantUid(log) shouldBe ""
    }

    test("a call I started whose receiver has no name or uid degrades to empty strings") {
        val initiator = callUser(me)
        val receiver = callUser(them, avatar = null)
        whenever(receiver.name).thenReturn(null)
        whenever(receiver.uid).thenReturn(null)
        val log = mock<CallLog>()
        whenever(log.receiverType).thenReturn("user")
        whenever(log.initiator).thenReturn(initiator)
        whenever(log.receiver).thenReturn(receiver)
        CallLogsUtils.getDisplayName(log) shouldBe ""
        CallLogsUtils.getAvatarUrl(log) shouldBe null
        CallLogsUtils.getOtherParticipantUid(log) shouldBe ""
    }

    test("a received call with a null-named initiator degrades to an empty display name") {
        val namelessInitiator = callUser(them, name = "ignored", avatar = null)
        whenever(namelessInitiator.name).thenReturn(null)
        val receiver = callUser(me)
        val log = mock<CallLog>()
        whenever(log.receiverType).thenReturn("user")
        whenever(log.initiator).thenReturn(namelessInitiator)
        whenever(log.receiver).thenReturn(receiver)
        CallLogsUtils.getDisplayName(log) shouldBe ""
        CallLogsUtils.getAvatarUrl(log) shouldBe null
    }
})
