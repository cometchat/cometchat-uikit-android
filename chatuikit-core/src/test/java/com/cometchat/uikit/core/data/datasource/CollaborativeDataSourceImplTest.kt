package com.cometchat.uikit.core.data.datasource

import com.cometchat.chat.core.CometChat
import com.cometchat.uikit.core.testutils.MockFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

/**
 * Tests for [CollaborativeDataSourceImpl] (ENG-38677 / L — `data/datasource` was
 * at 9%).
 *
 * Both operations build a real `org.json.JSONObject` payload and call the static
 * `CometChat.callExtension(extension, method, path, json, callback)`. We build
 * the REAL Impl, mock the static seam (callback at index 4), and cover success
 * (`onSuccess`) and error (`onError` → `Result.failure`) for whiteboard and
 * document. Flat tests so the static mock registers once per leaf.
 */
class CollaborativeDataSourceImplTest : FunSpec({

    lateinit var cometChatMock: MockedStatic<CometChat>
    lateinit var dataSource: CollaborativeDataSourceImpl

    beforeTest {
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        dataSource = CollaborativeDataSourceImpl()
    }
    afterTest {
        cometChatMock.close()
    }

    test("createWhiteboard succeeds when the extension call succeeds") {
        runTest {
            cometChatMock.`when`<Unit> {
                CometChat.callExtension(eq("whiteboard"), eq("POST"), eq("/v1/create"), any(), any())
            }.thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<JSONObject>>(4).onSuccess(null)
                null
            }

            dataSource.createWhiteboard("user-1", "user", null).isSuccess shouldBe true
        }
    }

    test("createWhiteboard fails when the extension call errors") {
        runTest {
            val error = MockFactory.createCometChatException("ERR_WB", "boom")
            cometChatMock.`when`<Unit> {
                CometChat.callExtension(any(), any(), any(), any(), any())
            }.thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<JSONObject>>(4).onError(error)
                null
            }

            val result = dataSource.createWhiteboard("group-1", "group", 5L)

            result.isFailure shouldBe true
            result.exceptionOrNull() shouldBe error
        }
    }

    test("createDocument succeeds when the extension call succeeds") {
        runTest {
            cometChatMock.`when`<Unit> {
                CometChat.callExtension(eq("document"), eq("POST"), eq("/v1/create"), any(), any())
            }.thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<JSONObject>>(4).onSuccess(JSONObject())
                null
            }

            dataSource.createDocument("user-1", "user", null).isSuccess shouldBe true
        }
    }

    test("createDocument fails when the extension call errors") {
        runTest {
            val error = MockFactory.createCometChatException("ERR_DOC", "boom")
            cometChatMock.`when`<Unit> {
                CometChat.callExtension(any(), any(), any(), any(), any())
            }.thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<JSONObject>>(4).onError(error)
                null
            }

            val result = dataSource.createDocument("group-1", "group", 9L)

            result.isFailure shouldBe true
            result.exceptionOrNull() shouldBe error
        }
    }
})
