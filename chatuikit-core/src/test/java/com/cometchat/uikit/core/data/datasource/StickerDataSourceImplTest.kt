package com.cometchat.uikit.core.data.datasource

import com.cometchat.chat.core.CometChat
import com.cometchat.uikit.core.testutils.MockFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

/**
 * Tests for [StickerDataSourceImpl] (ENG-38677 / L — re-pointed).
 *
 * Previously exercised a fake stand-in; re-pointed to the REAL Impl driving the
 * static seams: the `CometChat.isExtensionEnabled("stickers")` gate and
 * `CometChat.callExtension("stickers","GET","/v1/fetch", null, callback)`
 * (callback index 4). Uses a real `org.json` payload to exercise the parser.
 * Flat tests so the static mock registers once per leaf.
 */
class StickerDataSourceImplTest : FunSpec({

    lateinit var cometChatMock: MockedStatic<CometChat>
    lateinit var dataSource: StickerDataSourceImpl

    beforeTest {
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        dataSource = StickerDataSourceImpl()
    }
    afterTest {
        cometChatMock.close()
    }

    fun enableExtension(enabled: Boolean) {
        cometChatMock.`when`<Boolean> { CometChat.isExtensionEnabled(eq("stickers")) }.thenReturn(enabled)
    }

    fun stubCallExtension(answer: (CometChat.CallbackListener<JSONObject>) -> Unit) {
        cometChatMock.`when`<Unit> {
            CometChat.callExtension(eq("stickers"), eq("GET"), eq("/v1/fetch"), anyOrNull(), any())
        }.thenAnswer { inv ->
            @Suppress("UNCHECKED_CAST")
            answer(inv.getArgument(4) as CometChat.CallbackListener<JSONObject>)
            null
        }
    }

    test("fetchStickers fails when the stickers extension is disabled") {
        runTest {
            enableExtension(false)

            val result = dataSource.fetchStickers()

            result.isFailure shouldBe true
            (result.exceptionOrNull() as com.cometchat.chat.exceptions.CometChatException).code shouldBe "ERR_EXTENSION_NOT_ENABLED"
        }
    }

    test("fetchStickers parses default stickers grouped by set on success") {
        runTest {
            enableExtension(true)
            val json = JSONObject().apply {
                put("data", JSONObject().apply {
                    put("defaultStickers", JSONArray().apply {
                        put(JSONObject().apply {
                            put("stickerName", "happy")
                            put("stickerUrl", "https://x/happy.png")
                            put("stickerSetName", "Emotions")
                        })
                        put(JSONObject().apply {
                            put("stickerName", "sad")
                            put("stickerUrl", "https://x/sad.png")
                            put("stickerSetName", "Emotions")
                        })
                    })
                })
            }
            stubCallExtension { it.onSuccess(json) }

            val result = dataSource.fetchStickers()

            result.isSuccess shouldBe true
            val sets = result.getOrNull()!!
            sets.size shouldBe 1
            sets[0].name shouldBe "Emotions"
            sets[0].stickers.size shouldBe 2
        }
    }

    test("fetchStickers returns an empty list when the response is null") {
        runTest {
            enableExtension(true)
            stubCallExtension { it.onSuccess(null) }

            val result = dataSource.fetchStickers()

            result.isSuccess shouldBe true
            result.getOrNull() shouldBe emptyList()
        }
    }

    test("fetchStickers returns failure when the extension call errors") {
        runTest {
            enableExtension(true)
            val error = MockFactory.createCometChatException("ERR_STICKERS", "boom")
            stubCallExtension { it.onError(error) }

            val result = dataSource.fetchStickers()

            result.isFailure shouldBe true
            result.exceptionOrNull() shouldBe error
        }
    }
})
