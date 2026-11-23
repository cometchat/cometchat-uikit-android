package com.cometchat.uikit.core.offline

import android.content.Context
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.MediaMessage
import com.cometchat.chat.models.MessageStatus
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.utils.MessageOptionsUtils
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Options on an unsent row (G4, G5; BEH-SEND-31): Retry, Delete and Copy (text) only — nothing
 * that needs a server id — and only on an own row with no server id, with offline on.
 */
class UnsentOptionsTest : FunSpec({

    lateinit var cometChat: MockedStatic<CometChat>
    val context = mock<Context>().also { whenever(it.getString(anyInt())).thenReturn("label") }
    val me = User("me", "Me")

    beforeTest {
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User> { CometChat.getLoggedInUser() }.thenReturn(me)
    }
    afterTest { cometChat.close() }

    fun unsentText() = textTo().apply { sender = me; localMessageId = "l1"; status = MessageStatus.PENDING }

    test("a text row offers exactly Retry, Delete, Copy") {
        MessageOptionsUtils.getUnsentMessageOptions(context, unsentText()).map { it.id } shouldBe listOf(
            UIKitConstants.MessageOption.RETRY,
            UIKitConstants.MessageOption.DELETE,
            UIKitConstants.MessageOption.COPY
        )
    }

    test("a media row offers Retry and Delete (nothing to copy)") {
        val media = MediaMessage("peer", "image", CometChatConstants.RECEIVER_TYPE_USER).apply { sender = me }
        MessageOptionsUtils.getUnsentMessageOptions(context, media).map { it.id } shouldBe listOf(
            UIKitConstants.MessageOption.RETRY,
            UIKitConstants.MessageOption.DELETE
        )
    }

    test("the integrator's visibility map still hides an option") {
        MessageOptionsUtils.getUnsentMessageOptions(
            context, unsentText(), mapOf(UIKitConstants.MessageOption.COPY to false)
        ).map { it.id } shouldBe listOf(UIKitConstants.MessageOption.RETRY, UIKitConstants.MessageOption.DELETE)
    }

    test("no option that needs a server id is ever offered") {
        val serverIdOptions = setOf(
            UIKitConstants.MessageOption.REPLY, UIKitConstants.MessageOption.REPLY_IN_THREAD,
            UIKitConstants.MessageOption.REACT, UIKitConstants.MessageOption.EDIT,
            UIKitConstants.MessageOption.PIN, UIKitConstants.MessageOption.SAVE,
            UIKitConstants.MessageOption.MARK_AS_UNREAD, UIKitConstants.MessageOption.MESSAGE_INFORMATION,
            UIKitConstants.MessageOption.TRANSLATE, UIKitConstants.MessageOption.REPORT,
            UIKitConstants.MessageOption.FORWARD, UIKitConstants.MessageOption.SHARE,
            UIKitConstants.MessageOption.THREAD_SUBSCRIPTION
        )
        MessageOptionsUtils.getUnsentMessageOptions(context, unsentText())
            .map { it.id }.any { it in serverIdOptions } shouldBe false
    }

    test("only an own row with no server id, with offline on, is unsent") {
        OfflineRowState.isUnsent(unsentText(), offlineOn = true) shouldBe true
        OfflineRowState.isUnsent(unsentText(), offlineOn = false) shouldBe false
        OfflineRowState.isUnsent(unsentText().apply { id = 5L }, offlineOn = true) shouldBe false
        OfflineRowState.isUnsent(textTo().apply { sender = User("peer", "P") }, offlineOn = true) shouldBe false
    }
})
