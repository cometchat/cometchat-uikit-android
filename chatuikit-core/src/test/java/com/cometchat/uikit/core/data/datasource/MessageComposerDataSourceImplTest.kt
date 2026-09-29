package com.cometchat.uikit.core.data.datasource

import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.uikit.core.testutils.MockFactory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Tests for [MessageComposerDataSourceImpl] (ENG-38677 / L — re-pointed, partial).
 *
 * `editMessage` delegates to the SDK static `CometChat.editMessage(message,
 * callback)` and is re-pointed to the REAL Impl here (callback index 1, throws on
 * error via `resumeWithException`).
 *
 * NOTE (waiver — see ENG-38677-L-notes.md): `sendTextMessage`, `sendMediaMessage`
 * and `sendCustomMessage` delegate to the **CometChatUIKit `object`**
 * (non-`@JvmStatic` instance methods), which Mockito's `mockStatic` cannot
 * intercept. They require instrumented coverage (or a MockK `mockkObject`, which
 * is not on the classpath) and are intentionally not unit-tested here.
 */
class MessageComposerDataSourceImplTest : FunSpec({

    lateinit var cometChatMock: MockedStatic<CometChat>
    lateinit var dataSource: MessageComposerDataSourceImpl

    beforeTest {
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        dataSource = MessageComposerDataSourceImpl()
    }
    afterTest {
        cometChatMock.close()
    }

    test("editMessage returns the edited message on success") {
        runTest {
            val edited = mock<BaseMessage>()
            cometChatMock.`when`<Unit> {
                CometChat.editMessage(any(), any())
            }.thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<BaseMessage>>(1).onSuccess(edited)
                null
            }

            dataSource.editMessage(mock()) shouldBe edited
        }
    }

    test("editMessage throws the SDK exception on error") {
        runTest {
            val error = MockFactory.createCometChatException("ERR_EDIT", "boom")
            cometChatMock.`when`<Unit> {
                CometChat.editMessage(any(), any())
            }.thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<BaseMessage>>(1).onError(error)
                null
            }

            shouldThrow<CometChatException> { dataSource.editMessage(mock()) } shouldBe error
        }
    }
})
