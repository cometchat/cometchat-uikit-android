package com.cometchat.uikit.core.utils

import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.model.SessionType
import com.cometchat.chat.constants.CometChatConstants
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ringing v2 (ENG-39907): the caller's session is prepared by `CometChatCalls.initiateCall`
 * with the settings the kit passes. The Calls SDK defaults to video and does not read the
 * call type, so an audio call must carry a VOICE session or the caller's camera opens.
 */
class CallInitiationSessionTypeTest : FunSpec({

    test("audio call without an app builder prepares a voice session") {
        val settings = CallInitiationUtils.sessionSettingsFor(CometChatConstants.CALL_TYPE_AUDIO, null)
        settings.sessionType shouldBe SessionType.VOICE
    }

    test("video call without an app builder prepares a video session") {
        val settings = CallInitiationUtils.sessionSettingsFor(CometChatConstants.CALL_TYPE_VIDEO, null)
        settings.sessionType shouldBe SessionType.VIDEO
    }

    test("an app builder keeps its own options and only gets the session type") {
        val appBuilder = CometChatCalls.SessionSettingsBuilder().startVideoPaused(false)
        val audio = CallInitiationUtils.sessionSettingsFor(CometChatConstants.CALL_TYPE_AUDIO, appBuilder)
        audio.sessionType shouldBe SessionType.VOICE
        audio.isStartVideoPaused shouldBe false

        val video = CallInitiationUtils.sessionSettingsFor(CometChatConstants.CALL_TYPE_VIDEO, appBuilder)
        video.sessionType shouldBe SessionType.VIDEO
    }

    // SM-17: without the peer flag the caller stayed in the room after the callee hung up.
    test("the caller's session is a peer call, with or without an app builder") {
        CallInitiationUtils.sessionSettingsFor(CometChatConstants.CALL_TYPE_AUDIO, null).isPeerCall shouldBe true
        val appBuilder = CometChatCalls.SessionSettingsBuilder()
        CallInitiationUtils.sessionSettingsFor(CometChatConstants.CALL_TYPE_VIDEO, appBuilder).isPeerCall shouldBe true
    }
})
