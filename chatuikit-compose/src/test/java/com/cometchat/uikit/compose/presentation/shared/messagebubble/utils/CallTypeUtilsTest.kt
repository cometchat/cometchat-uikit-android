package com.cometchat.uikit.compose.presentation.shared.messagebubble.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Unit layer for the call-action bubble's classification.
 *
 * The four-argument [getCallType] exists so this can be tested without the SDK, and
 * the rule it encodes is easy to get backwards: a call counts as *missed* only when
 * it went unanswered **and** the viewer was not the one who placed it. An unanswered
 * call the viewer placed is outgoing, not missed.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CallTypeUtilsTest {

    private companion object {
        const val ME = "me"
        const val THEM = "them"
        val AUDIO: String = CometChatConstants.CALL_TYPE_AUDIO
        val VIDEO: String = CometChatConstants.CALL_TYPE_VIDEO
        val UNANSWERED: String = CometChatConstants.CALL_STATUS_UNANSWERED
        val INITIATED: String = CometChatConstants.CALL_STATUS_INITIATED
        val ENDED: String = CometChatConstants.CALL_STATUS_ENDED
    }

    // ── missed, and the asymmetry that defines it ───────────────────────────

    @Test
    fun unansweredFromSomeoneElseIsMissed() =
        assertEquals(CallType.AUDIO_MISSED, getCallType(AUDIO, UNANSWERED, THEM, ME))

    @Test
    fun unansweredVideoFromSomeoneElseIsMissed() =
        assertEquals(CallType.VIDEO_MISSED, getCallType(VIDEO, UNANSWERED, THEM, ME))

    @Test
    fun aCallIPlacedThatWentUnansweredIsOutgoingNotMissed() {
        // The half of the rule that is easy to invert.
        assertEquals(CallType.AUDIO_OUTGOING, getCallType(AUDIO, UNANSWERED, ME, ME))
        assertEquals(CallType.VIDEO_OUTGOING, getCallType(VIDEO, UNANSWERED, ME, ME))
    }

    // ── direction ───────────────────────────────────────────────────────────

    @Test
    fun initiatedByMeIsOutgoing() {
        assertEquals(CallType.AUDIO_OUTGOING, getCallType(AUDIO, INITIATED, ME, ME))
        assertEquals(CallType.VIDEO_OUTGOING, getCallType(VIDEO, INITIATED, ME, ME))
    }

    @Test
    fun initiatedByThemIsIncoming() {
        assertEquals(CallType.AUDIO_INCOMING, getCallType(AUDIO, INITIATED, THEM, ME))
        assertEquals(CallType.VIDEO_INCOMING, getCallType(VIDEO, INITIATED, THEM, ME))
    }

    @Test
    fun anEndedCallFallsBackToDirection() {
        assertEquals(CallType.AUDIO_OUTGOING, getCallType(AUDIO, ENDED, ME, ME))
        assertEquals(CallType.AUDIO_INCOMING, getCallType(AUDIO, ENDED, THEM, ME))
    }

    @Test
    fun nullsDoNotThrowAndFallBackToIncoming() {
        // Both uids null compare equal, which reads as "initiator", so this pins the
        // actual behaviour rather than an assumption about it.
        assertEquals(CallType.AUDIO_OUTGOING, getCallType(null, null, null, null))
    }

    @Test
    fun anUnknownLoggedInUserTreatsTheCallAsIncoming() =
        assertEquals(CallType.AUDIO_INCOMING, getCallType(AUDIO, ENDED, THEM, null))

    // ── the predicates the bubble draws from ────────────────────────────────

    @Test
    fun isMissedCallOnlyForTheTwoMissedVariants() {
        assertTrue(isMissedCall(CallType.AUDIO_MISSED))
        assertTrue(isMissedCall(CallType.VIDEO_MISSED))
        listOf(
            CallType.AUDIO_INCOMING, CallType.AUDIO_OUTGOING,
            CallType.VIDEO_INCOMING, CallType.VIDEO_OUTGOING,
        ).forEach { assertFalse("$it is not missed", isMissedCall(it)) }
    }

    @Test
    fun isVideoCallSplitsTheEnumInHalf() {
        listOf(CallType.VIDEO_INCOMING, CallType.VIDEO_OUTGOING, CallType.VIDEO_MISSED)
            .forEach { assertTrue("$it is video", isVideoCall(it)) }
        listOf(CallType.AUDIO_INCOMING, CallType.AUDIO_OUTGOING, CallType.AUDIO_MISSED)
            .forEach { assertFalse("$it is audio", isVideoCall(it)) }
    }

    @Test
    fun isIncomingCallCoversMissedCallsToo() {
        // A missed call was, by definition, one that came in.
        assertTrue(isIncomingCall(CallType.AUDIO_INCOMING))
        assertTrue(isIncomingCall(CallType.VIDEO_INCOMING))
        assertFalse(isIncomingCall(CallType.AUDIO_OUTGOING))
        assertFalse(isIncomingCall(CallType.VIDEO_OUTGOING))
    }

    @Test
    fun everyCallTypeResolvesItsOwnIcon() {
        val icons = CallType.entries.associateWith { getCallTypeIcon(it) }
        icons.forEach { (type, id) -> assertTrue("$type should resolve an icon", id != 0) }
        assertEquals("icons must not be shared", CallType.entries.size, icons.values.toSet().size)
    }

    @Test
    fun everyCallTypeHasNonEmptyText() =
        CallType.entries.forEach { assertTrue("$it should have text", getCallTypeText(it).isNotEmpty()) }
}
