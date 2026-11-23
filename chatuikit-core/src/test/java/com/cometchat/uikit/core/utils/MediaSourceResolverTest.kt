package com.cometchat.uikit.core.utils

import android.util.Log
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.Attachment
import com.cometchat.chat.models.MediaCachingMode
import com.cometchat.chat.models.MediaMessage
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.offline.OfflineGate
import com.cometchat.uikit.core.offline.OfflineHub
import com.cometchat.uikit.core.offline.OfflineHubEvent
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * W5 (ENG-41090) — [MediaSourceResolver] (G9, BEH-MEDIA-13): local path → URL → placeholder,
 * URL only with caching OFF, today's path (null) with offline off, and the row re-resolving on the
 * hub's `MediaReady`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MediaSourceResolverTest : FunSpec({

    fun attachment(url: String? = "https://cdn/x/a.jpg?sig=1", local: String? = null) = Attachment().apply {
        fileUrl = url
        localPath = local
        fileName = "a.jpg"
        fileSize = 2048
    }

    fun resolve(
        a: Attachment?,
        on: Boolean = true,
        mode: MediaCachingMode? = MediaCachingMode.AUTO,
        online: Boolean = true,
        exists: Boolean = true
    ) = MediaSourceResolver.resolve(a, on, { mode }, { online }, { exists })

    test("offline off returns null so the bubble keeps today's code") {
        resolve(attachment(local = "/p/a.jpg"), on = false) shouldBe null
    }

    test("a local file wins over the URL") {
        resolve(attachment(local = "/p/a.jpg")) shouldBe MediaSource.Local("/p/a.jpg")
    }

    test("a missing local file falls back to the URL when online") {
        resolve(attachment(local = "/p/gone.jpg"), exists = false) shouldBe MediaSource.Remote("https://cdn/x/a.jpg?sig=1")
    }

    test("never downloaded and offline is a placeholder with name and size") {
        resolve(attachment(), online = false) shouldBe MediaSource.Placeholder("a.jpg", 2048)
    }

    test("no URL and no local file is a placeholder") {
        resolve(attachment(url = null)) shouldBe MediaSource.Placeholder("a.jpg", 2048)
    }

    test("caching OFF is the URL only, even with a local file and offline") {
        resolve(attachment(local = "/p/a.jpg"), mode = MediaCachingMode.OFF, online = false) shouldBe
            MediaSource.Remote("https://cdn/x/a.jpg?sig=1")
    }

    test("the MediaReady key is the URL without its query") {
        MediaSourceResolver.attachmentKey("https://cdn/x/a.jpg?sig=1&e=2") shouldBe "https://cdn/x/a.jpg"
        MediaSourceResolver.attachmentKey(null) shouldBe null
    }

    // ---- re-resolve on MediaReady ----

    val testDispatcher = UnconfinedTestDispatcher()
    lateinit var logMock: MockedStatic<Log>
    lateinit var cometChatMock: MockedStatic<CometChat>
    val stores = mutableListOf<androidx.lifecycle.ViewModelStore>()

    beforeSpec { Dispatchers.setMain(testDispatcher) }
    afterSpec { Thread.sleep(100); Dispatchers.resetMain() }
    beforeTest {
        OfflineGate.testOverride = true
        OfflineHub.resetForTest()
        logMock = Mockito.mockStatic(Log::class.java)
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
    }
    afterTest {
        stores.forEach { it.clear() }
        stores.clear()
        cometChatMock.close()
        logMock.close()
        OfflineGate.testOverride = null
    }

    test("MediaReady stamps the local path on the matching row; reconnect re-resolves shown media") {
        runTest {
            val att = attachment()
            val copy = mock<MediaMessage>()
            val row = mock<MediaMessage> {
                on { id } doReturn 42L
                on { attachment } doReturn att
                on { attachments } doReturn emptyList()
                on { clone() } doReturn copy
                on { receiverType } doReturn CometChatConstants.RECEIVER_TYPE_USER
            }
            val repository = mock<MessageListRepository> {
                on { hasMorePreviousMessages() } doReturn false
                onBlocking { fetchPreviousMessages() } doReturn Result.success(emptyList())
            }
            val store = androidx.lifecycle.ViewModelStore()
            stores += store
            val vm = androidx.lifecycle.ViewModelProvider(
                store,
                object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                        CometChatMessageListViewModel(repository = repository, enableListeners = true) as T
                }
            )[CometChatMessageListViewModel::class.java]
            vm.setUser(com.cometchat.chat.models.User().apply { uid = "peer" })
            advanceUntilIdle()
            vm.offlineSetRows(listOf(row))

            // Another attachment's MediaReady changes nothing.
            OfflineHub.publishForTest(OfflineHubEvent.MediaReady(OfflineHub.gen, "42", "https://cdn/x/other.jpg", "/p/o.jpg"))
            advanceUntilIdle()
            (vm.getItems().single() === row) shouldBe true

            OfflineHub.publishForTest(OfflineHubEvent.MediaReady(OfflineHub.gen, "42", "https://cdn/x/a.jpg", "/p/a.jpg"))
            advanceUntilIdle()
            (vm.getItems().single() === copy) shouldBe true
            verify(copy).setAttachment(argThat { localPath == "/p/a.jpg" && fileUrl == att.fileUrl })
            att.localPath shouldBe null // the shown attachment is cloned, never mutated

            // Reconnect: shown media rows re-resolve (Compose keys on the epoch; Views rebinds).
            val epochBefore = MediaSourceResolver.connectionEpoch.value
            val rebound = mutableListOf<com.cometchat.chat.models.BaseMessage>()
            val job = launch { vm.messageUpdated.collect { rebound += it } }
            advanceUntilIdle() // subscribe before the reconnect emits
            val conn = org.mockito.kotlin.argumentCaptor<CometChat.ConnectionListener>()
            cometChatMock.verify({ CometChat.addConnectionListener(any(), conn.capture()) }, org.mockito.kotlin.atLeastOnce())
            conn.lastValue.onConnected()
            advanceUntilIdle()
            MediaSourceResolver.connectionEpoch.value shouldBe epochBefore + 1
            (rebound.single() === copy) shouldBe true
            job.cancel()
        }
    }
})
