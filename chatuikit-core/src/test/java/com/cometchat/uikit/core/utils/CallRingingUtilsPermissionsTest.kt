package com.cometchat.uikit.core.utils

import android.Manifest
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.uikit.core.testutils.MockFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain

/**
 * ENG-40552: a voice call asks for the microphone only; the camera is added for video calls.
 * Covers [CallRingingUtils.permissionsForCallType] and [CallRingingUtils.permissionsToAnswer]
 * directly, since the ring screens and the ongoing screen all derive their prompts from them.
 */
class CallRingingUtilsPermissionsTest : FunSpec({

    test("an audio call needs the microphone only") {
        CallRingingUtils.permissionsForCallType(CometChatConstants.CALL_TYPE_AUDIO).toList()
            .shouldContainExactly(Manifest.permission.RECORD_AUDIO)
    }

    test("a video call needs the microphone and the camera") {
        CallRingingUtils.permissionsForCallType(CometChatConstants.CALL_TYPE_VIDEO).toList()
            .shouldContainExactly(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
    }

    test("the call type is matched ignoring case") {
        CallRingingUtils.permissionsForCallType("VIDEO").toList()
            .shouldContainExactly(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
    }

    test("an unknown or missing call type never asks for the camera") {
        CallRingingUtils.permissionsForCallType(null).toList()
            .shouldContainExactly(Manifest.permission.RECORD_AUDIO)
        CallRingingUtils.permissionsForCallType("screen").toList()
            .shouldNotContain(Manifest.permission.CAMERA)
    }

    test("permissionsToAnswer follows the call's type and tolerates a null call") {
        val audio = MockFactory.createCall(sessionId = "s-audio", type = CometChatConstants.CALL_TYPE_AUDIO)
        val video = MockFactory.createCall(sessionId = "s-video", type = CometChatConstants.CALL_TYPE_VIDEO)

        CallRingingUtils.permissionsToAnswer(audio).toList()
            .shouldContainExactly(Manifest.permission.RECORD_AUDIO)
        CallRingingUtils.permissionsToAnswer(video).toList()
            .shouldContainExactly(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
        CallRingingUtils.permissionsToAnswer(null).toList()
            .shouldContainExactly(Manifest.permission.RECORD_AUDIO)
    }
})
