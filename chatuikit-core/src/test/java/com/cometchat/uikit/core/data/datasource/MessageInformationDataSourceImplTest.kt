package com.cometchat.uikit.core.data.datasource

import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.MessageReceipt
import com.cometchat.uikit.core.testutils.MockFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Tests for [MessageInformationDataSourceImpl] (ENG-38677 / L — re-pointed).
 *
 * Previously exercised a stand-in; re-pointed to the REAL Impl driving the
 * static seam `CometChat.getMessageReceipts(messageId, callback)` (callback
 * index 1), which resumes with `Result.success`/`Result.failure`. Flat tests so
 * the static mock registers once per leaf.
 */
class MessageInformationDataSourceImplTest : FunSpec({

    lateinit var cometChatMock: MockedStatic<CometChat>
    lateinit var dataSource: MessageInformationDataSourceImpl

    beforeTest {
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        dataSource = MessageInformationDataSourceImpl()
    }
    afterTest {
        cometChatMock.close()
    }

    test("getMessageReceipts returns the receipts on success") {
        runTest {
            val receipts = listOf(mock<MessageReceipt>(), mock<MessageReceipt>())
            cometChatMock.`when`<Unit> {
                CometChat.getMessageReceipts(eq(42L), any())
            }.thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<List<MessageReceipt>>>(1).onSuccess(receipts)
                null
            }

            val result = dataSource.getMessageReceipts(42L)

            result.isSuccess shouldBe true
            result.getOrNull() shouldBe receipts
        }
    }

    test("getMessageReceipts returns failure on error") {
        runTest {
            val error = MockFactory.createCometChatException("ERR_RECEIPTS", "boom")
            cometChatMock.`when`<Unit> {
                CometChat.getMessageReceipts(eq(7L), any())
            }.thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<List<MessageReceipt>>>(1).onError(error)
                null
            }

            val result = dataSource.getMessageReceipts(7L)

            result.isFailure shouldBe true
            result.exceptionOrNull() shouldBe error
        }
    }
})
