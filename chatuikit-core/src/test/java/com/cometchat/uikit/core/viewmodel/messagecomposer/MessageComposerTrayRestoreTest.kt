package com.cometchat.uikit.core.viewmodel.messagecomposer

import android.util.Log
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.SettingsRepo
import com.cometchat.chat.core.UploadFileRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Attachment
import com.cometchat.chat.models.User
import com.cometchat.chat.upload.UploadFileItem
import com.cometchat.chat.upload.UploadFileItemStatus
import com.cometchat.chat.upload.UploadFileListener
import com.cometchat.uikit.core.domain.usecase.EditMessageUseCase
import com.cometchat.uikit.core.domain.usecase.SendCustomMessageUseCase
import com.cometchat.uikit.core.domain.usecase.SendMediaMessageUseCase
import com.cometchat.uikit.core.domain.usecase.SendTextMessageUseCase
import com.cometchat.uikit.core.models.AttachmentUploadStatus
import com.cometchat.uikit.core.models.StagedAttachmentInput
import com.cometchat.uikit.core.models.isCopyMissing
import com.cometchat.uikit.core.offline.OfflineGate
import com.cometchat.uikit.core.viewmodel.CometChatMessageComposerViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * ENG-41136 (W10) — BEH-UPLOAD-14 composer tray restore-on-open.
 *
 * The restore is posted past the configuration frame, so these tests run Main on a
 * [StandardTestDispatcher]: nothing restores until [advanceUntilIdle] drains the frame. The SDK is
 * intercepted at `CometChat.getExistingUploadFileRequest`; the offline mode at [OfflineGate].
 *
 * Run: ./gradlew :chatuikit-core:testDebugUnitTest --tests "*MessageComposerTrayRestoreTest"
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MessageComposerTrayRestoreTest : FunSpec({

    lateinit var testDispatcher: TestDispatcher
    lateinit var logMock: MockedStatic<Log>
    lateinit var cometChatMock: MockedStatic<CometChat>
    lateinit var settingsRepoMock: MockedStatic<SettingsRepo>

    val alice = User().apply { uid = "alice" }
    val bob = User().apply { uid = "bob" }

    fun createViewModel() = CometChatMessageComposerViewModel(
        sendTextMessageUseCase = mock<SendTextMessageUseCase>(),
        sendMediaMessageUseCase = mock<SendMediaMessageUseCase>(),
        sendCustomMessageUseCase = mock<SendCustomMessageUseCase>(),
        editMessageUseCase = mock<EditMessageUseCase>(),
        enableListeners = false
    )

    fun attachment(name: String) = Attachment().apply {
        fileUrl = "https://cdn.example.com/$name"
        fileName = name
        fileMimeType = "image/jpeg"
    }

    fun item(
        fileId: String,
        state: UploadFileItemStatus.State,
        mime: String = "image/jpeg",
        localCopyPath: String? = "/copies/$fileId",
        attachment: Attachment? = null,
        error: CometChatException? = null
    ) = UploadFileItemStatus(fileId, "$fileId.bin", 100L, mime, state, error, attachment, localCopyPath)

    fun restoredRequest(batchId: String, items: List<UploadFileItemStatus>): UploadFileRequest {
        val request = mock<UploadFileRequest>()
        whenever(request.batchId).thenReturn(batchId)
        whenever(request.items).thenReturn(items)
        return request
    }

    fun existing(uid: String, parent: Long, request: UploadFileRequest?) {
        cometChatMock.`when`<UploadFileRequest?> {
            CometChat.getExistingUploadFileRequest(uid, "user", parent)
        }.thenReturn(request)
    }

    fun verifyLookups(uid: String, parent: Long, times: Int) {
        cometChatMock.verify(
            { CometChat.getExistingUploadFileRequest(uid, "user", parent) },
            Mockito.times(times)
        )
    }

    fun runFrame(block: suspend TestScope.() -> Unit) = runTest(testDispatcher) { block() }

    beforeTest {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        logMock = Mockito.mockStatic(Log::class.java)
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        settingsRepoMock = Mockito.mockStatic(SettingsRepo::class.java)
        OfflineGate.testOverride = true
    }

    afterTest {
        OfflineGate.testOverride = null
        Dispatchers.resetMain()
        logMock.close()
        cometChatMock.close()
        settingsRepoMock.close()
    }

    // ==================== Settled-triple, single-shot restore ====================

    test("main composer restores once, after the configuration frame, with parentMessageId 0") {
        runFrame {
            existing("alice", 0L, restoredRequest("b1", listOf(item("b1_0", UploadFileItemStatus.State.FAILED))))
            val vm = createViewModel()
            vm.setUser(alice)
            verifyLookups("alice", 0L, 0) // not eagerly from the setter
            advanceUntilIdle()
            verifyLookups("alice", 0L, 1)
            vm.attachmentTiles.value.map { it.fileId } shouldBe listOf("b1_0")
        }
    }

    test("thread composer restores once on the final triple and never adopts the main batch") {
        runFrame {
            existing("alice", 0L, restoredRequest("main", listOf(item("main_0", UploadFileItemStatus.State.FAILED))))
            existing("alice", 42L, restoredRequest("thr", listOf(item("thr_0", UploadFileItemStatus.State.FAILED))))
            val vm = createViewModel()
            vm.setUser(alice)
            vm.setParentMessageId(42L)
            advanceUntilIdle()
            verifyLookups("alice", 42L, 1)
            verifyLookups("alice", 0L, 0)
            vm.attachmentTiles.value.map { it.fileId } shouldBe listOf("thr_0")
        }
    }

    test("null request leaves the tray empty") {
        runFrame {
            existing("alice", 0L, null)
            val vm = createViewModel()
            vm.setUser(alice)
            advanceUntilIdle()
            verifyLookups("alice", 0L, 1)
            vm.attachmentTiles.value.shouldBeEmpty()
            vm.hasStagedAttachments.value shouldBe false
        }
    }

    test("a lookup failure is swallowed and the tray stays empty") {
        runFrame {
            cometChatMock.`when`<UploadFileRequest?> {
                CometChat.getExistingUploadFileRequest(any<String>(), any<String>(), any<Long>())
            }.thenThrow(IllegalStateException("store closed"))
            val vm = createViewModel()
            vm.setUser(alice)
            advanceUntilIdle()
            vm.attachmentTiles.value.shouldBeEmpty()
        }
    }

    test("no receiver means no lookup") {
        runFrame {
            val vm = createViewModel()
            vm.setParentMessageId(42L)
            advanceUntilIdle()
            cometChatMock.verify(
                { CometChat.getExistingUploadFileRequest(any<String>(), any<String>(), any<Long>()) },
                never()
            )
        }
    }

    // ==================== State map ====================

    test("state map: UPLOADED done, REJECTED rejected, in flight uploading, FAILED failed; copy-missing flagged") {
        runFrame {
            val uploaded = attachment("u.jpg")
            existing(
                "alice", 0L,
                restoredRequest(
                    "b1",
                    listOf(
                        item("b1_0", UploadFileItemStatus.State.UPLOADED, attachment = uploaded),
                        item("b1_1", UploadFileItemStatus.State.REJECTED, attachment = attachment("x")),
                        item("b1_2", UploadFileItemStatus.State.QUEUED),
                        item("b1_3", UploadFileItemStatus.State.UPLOADING),
                        item("b1_4", UploadFileItemStatus.State.FAILED, mime = "application/pdf"),
                        item("b1_5", UploadFileItemStatus.State.FAILED, localCopyPath = null)
                    )
                )
            )
            val vm = createViewModel()
            vm.setUser(alice)
            runCurrent() // the restore frame only; in-flight files are read back later

            val tiles = vm.attachmentTiles.value
            tiles.map { it.fileId } shouldBe listOf("b1_0", "b1_1", "b1_2", "b1_3", "b1_4", "b1_5")
            tiles.map { it.status } shouldBe listOf(
                AttachmentUploadStatus.DONE,
                AttachmentUploadStatus.REJECTED,
                AttachmentUploadStatus.UPLOADING,
                AttachmentUploadStatus.UPLOADING,
                AttachmentUploadStatus.FAILED,
                AttachmentUploadStatus.FAILED
            )
            tiles[0].attachment shouldBe uploaded
            tiles.drop(1).all { it.attachment == null } shouldBe true // only UPLOADED carries one
            tiles[0].localUri shouldBe "/copies/b1_0"
            tiles[4].category shouldBe "file" // derived from mime, picker origin is not persisted
            tiles[0].category shouldBe "image"
            tiles.map { it.isCopyMissing } shouldBe listOf(false, false, false, false, false, true)
            vm.attachmentsAllUploaded.value shouldBe false
        }
    }

    // ==================== No duplicates ====================

    test("re-binding the same receiver (rotation) neither re-restores nor duplicates tiles") {
        runFrame {
            existing("alice", 0L, restoredRequest("b1", listOf(item("b1_0", UploadFileItemStatus.State.FAILED))))
            val vm = createViewModel()
            vm.setUser(alice)
            advanceUntilIdle()
            vm.setUser(alice)
            advanceUntilIdle()
            verifyLookups("alice", 0L, 1)
            vm.attachmentTiles.value shouldHaveSize 1
        }
    }

    test("reopen on a fresh ViewModel restores the batch once, without duplicate tiles") {
        runFrame {
            existing(
                "alice", 0L,
                restoredRequest(
                    "b1",
                    listOf(
                        item("b1_0", UploadFileItemStatus.State.FAILED),
                        item("b1_0", UploadFileItemStatus.State.FAILED),
                        item("b1_1", UploadFileItemStatus.State.UPLOADED, attachment = attachment("a"))
                    )
                )
            )
            val first = createViewModel()
            first.setUser(alice)
            advanceUntilIdle()
            val reopened = createViewModel()
            reopened.setUser(alice)
            advanceUntilIdle()
            reopened.attachmentTiles.value.map { it.fileId } shouldBe listOf("b1_0", "b1_1")
        }
    }

    test("a restore never replaces a tray staged before it ran") {
        runFrame {
            val live = mock<UploadFileRequest>()
            whenever(live.batchId).thenReturn("live")
            whenever(live.setBatchId(any<String>())).thenReturn(live)
            whenever(live.setParentMessageId(any<Long>())).thenReturn(live)
            cometChatMock.`when`<UploadFileRequest> {
                CometChat.createUploadFileRequest(any<String>(), any<String>())
            }.thenReturn(live)
            existing("alice", 0L, restoredRequest("b1", listOf(item("b1_0", UploadFileItemStatus.State.FAILED))))
            val vm = createViewModel()
            vm.setUser(alice)
            vm.stageAttachments(listOf(StagedAttachmentInput(File("/tmp/a.jpg"), "a.jpg", 1L, "image/jpeg")))
            advanceUntilIdle()
            vm.attachmentTiles.value.map { it.fileId } shouldBe listOf("live_0")
        }
    }

    // ==================== Thread vs main isolation ====================

    test("offline on: opening a thread detaches the main batch without destroying it") {
        runFrame {
            val main = restoredRequest("main", listOf(item("main_0", UploadFileItemStatus.State.FAILED)))
            existing("alice", 0L, main)
            existing("alice", 42L, null)
            val vm = createViewModel()
            vm.setUser(alice)
            advanceUntilIdle()
            vm.attachmentTiles.value shouldHaveSize 1

            vm.setParentMessageId(42L)
            vm.attachmentTiles.value.shouldBeEmpty() // detached at once
            advanceUntilIdle()
            verify(main, never()).clearAll()
            vm.attachmentTiles.value.shouldBeEmpty()
            verifyLookups("alice", 42L, 1)
        }
    }

    test("offline on: switching chats back restores each chat's own batch") {
        runFrame {
            existing("alice", 0L, restoredRequest("a", listOf(item("a_0", UploadFileItemStatus.State.FAILED))))
            existing("bob", 0L, restoredRequest("b", listOf(item("b_0", UploadFileItemStatus.State.FAILED))))
            val vm = createViewModel()
            vm.setUser(alice)
            advanceUntilIdle()
            vm.setUser(bob)
            advanceUntilIdle()
            vm.attachmentTiles.value.map { it.fileId } shouldBe listOf("b_0")
            vm.setUser(alice)
            advanceUntilIdle()
            vm.attachmentTiles.value.map { it.fileId } shouldBe listOf("a_0")
        }
    }

    // ==================== Offline-off baseline ====================

    test("offline off: a triple change destroys the batch as today and the lookup returns nothing") {
        runFrame {
            OfflineGate.testOverride = false
            val live = mock<UploadFileRequest>()
            whenever(live.batchId).thenReturn("live")
            whenever(live.setBatchId(any<String>())).thenReturn(live)
            whenever(live.setParentMessageId(any<Long>())).thenReturn(live)
            cometChatMock.`when`<UploadFileRequest> {
                CometChat.createUploadFileRequest(any<String>(), any<String>())
            }.thenReturn(live)
            // Offline off: the SDK returns null for every triple (mock default).
            val vm = createViewModel()
            vm.setUser(alice)
            advanceUntilIdle()
            vm.stageAttachments(listOf(StagedAttachmentInput(File("/tmp/a.jpg"), "a.jpg", 1L, "image/jpeg")))
            vm.attachmentTiles.value shouldHaveSize 1

            vm.setParentMessageId(42L)
            verify(live).clearAll()
            advanceUntilIdle()
            vm.attachmentTiles.value.shouldBeEmpty()
        }
    }

    test("offline off: retry of a live tile re-uploads the picked file, never the id-based retry") {
        runFrame {
            OfflineGate.testOverride = false
            val live = mock<UploadFileRequest>()
            whenever(live.batchId).thenReturn("live")
            whenever(live.setBatchId(any<String>())).thenReturn(live)
            whenever(live.setParentMessageId(any<Long>())).thenReturn(live)
            cometChatMock.`when`<UploadFileRequest> {
                CometChat.createUploadFileRequest(any<String>(), any<String>())
            }.thenReturn(live)
            val vm = createViewModel()
            vm.setUser(alice)
            advanceUntilIdle()
            val file = File("/tmp/a.jpg")
            vm.stageAttachments(listOf(StagedAttachmentInput(file, "a.jpg", 1L, "image/jpeg")))
            val tile = vm.attachmentTiles.value.single().copy(status = AttachmentUploadStatus.FAILED)
            vm.retryAttachment(tile)
            verify(live).uploadAttachment(eq("live_0"), eq(file), any<UploadFileListener>())
            verify(live, never()).retryAttachment(any<String>(), anyOrNull())
        }
    }

    // ==================== Retry / add-more on a restored tray ====================

    test("retry of a restored FAILED tile uses the id-based retry with the composer's listener") {
        runFrame {
            val request = restoredRequest(
                "b1",
                listOf(
                    item("b1_0", UploadFileItemStatus.State.FAILED),
                    item("b1_1", UploadFileItemStatus.State.FAILED, localCopyPath = null)
                )
            )
            existing("alice", 0L, request)
            val vm = createViewModel()
            vm.setUser(alice)
            advanceUntilIdle()

            // Each retry reads the item before and after the call. The SDK re-queues b1_0; it
            // declines the copy-less b1_1 (SDK 5.1.0 retry is a no-op for a FAILED file with no
            // bound input).
            val failedBoth = listOf(
                item("b1_0", UploadFileItemStatus.State.FAILED),
                item("b1_1", UploadFileItemStatus.State.FAILED, localCopyPath = null)
            )
            val requeued = listOf(
                item("b1_0", UploadFileItemStatus.State.QUEUED),
                item("b1_1", UploadFileItemStatus.State.FAILED, localCopyPath = null)
            )
            whenever(request.items).thenReturn(failedBoth, requeued, requeued, requeued)
            vm.retryAttachment(vm.attachmentTiles.value[0])
            vm.retryAttachment(vm.attachmentTiles.value[1])
            verify(request).retryAttachment(eq("b1_0"), any<UploadFileListener>())
            verify(request, never()).retryAttachment(eq("b1_1"), anyOrNull()) // D-306: remove-only
            verify(request, never()).uploadAttachment(any<String>(), any<File>(), any<UploadFileListener>())
            val tiles = vm.attachmentTiles.value
            tiles[0].status shouldBe AttachmentUploadStatus.UPLOADING
            tiles[1].status shouldBe AttachmentUploadStatus.FAILED // never left spinning
            tiles[1].isCopyMissing shouldBe true
        }
    }

    test("a restored retry whose SDK call throws puts the tile back to FAILED") {
        runFrame {
            val request = restoredRequest("b1", listOf(item("b1_0", UploadFileItemStatus.State.FAILED)))
            doThrow(IllegalStateException("boom"))
                .whenever(request).retryAttachment(any<String>(), any<UploadFileListener>())
            existing("alice", 0L, request)
            val vm = createViewModel()
            vm.setUser(alice)
            advanceUntilIdle()

            vm.retryAttachment(vm.attachmentTiles.value[0])
            vm.attachmentTiles.value[0].status shouldBe AttachmentUploadStatus.FAILED
            vm.attachmentTiles.value[0].error?.code shouldBe "RETRY_ERROR"
        }
    }

    // ==================== Same-session reopen while an upload runs ====================

    test("a restored file still uploading shows UPLOADING, then DONE with its attachment when it finishes") {
        runFrame {
            val request = restoredRequest("b1", listOf(item("b1_0", UploadFileItemStatus.State.UPLOADING)))
            existing("alice", 0L, request)
            val vm = createViewModel()
            vm.setUser(alice)
            runCurrent()
            vm.attachmentTiles.value.single().status shouldBe AttachmentUploadStatus.UPLOADING
            vm.attachmentsAllUploaded.value shouldBe false

            // The upload finishes under the old composer's listener; this one reads it back.
            val uploaded = attachment("b1_0.jpg")
            whenever(request.items).thenReturn(
                listOf(item("b1_0", UploadFileItemStatus.State.UPLOADED, attachment = uploaded))
            )
            advanceTimeBy(600)
            runCurrent()
            val tile = vm.attachmentTiles.value.single()
            tile.status shouldBe AttachmentUploadStatus.DONE
            tile.attachment shouldBe uploaded
            vm.attachmentsAllUploaded.value shouldBe true
            verify(request, never()).retryAttachment(any<String>(), anyOrNull())
        }
    }

    test("a restored in-flight file that fails is shown FAILED") {
        runFrame {
            val request = restoredRequest("b1", listOf(item("b1_0", UploadFileItemStatus.State.QUEUED)))
            existing("alice", 0L, request)
            val vm = createViewModel()
            vm.setUser(alice)
            runCurrent()
            whenever(request.items).thenReturn(listOf(item("b1_0", UploadFileItemStatus.State.FAILED)))
            advanceTimeBy(600)
            runCurrent()
            vm.attachmentTiles.value.single().status shouldBe AttachmentUploadStatus.FAILED
        }
    }

    test("a restored in-flight file still uploading after the read-back limit is shown FAILED") {
        runFrame {
            val request = restoredRequest("b1", listOf(item("b1_0", UploadFileItemStatus.State.UPLOADING)))
            existing("alice", 0L, request)
            val vm = createViewModel()
            vm.setUser(alice)
            advanceUntilIdle()
            vm.attachmentTiles.value.single().status shouldBe AttachmentUploadStatus.FAILED
        }
    }

    test("retry on a stale FAILED tile whose upload already finished shows DONE without retrying") {
        runFrame {
            val request = restoredRequest("b1", listOf(item("b1_0", UploadFileItemStatus.State.FAILED)))
            existing("alice", 0L, request)
            val vm = createViewModel()
            vm.setUser(alice)
            advanceUntilIdle()

            val uploaded = attachment("b1_0.jpg")
            whenever(request.items).thenReturn(
                listOf(item("b1_0", UploadFileItemStatus.State.UPLOADED, attachment = uploaded))
            )
            vm.retryAttachment(vm.attachmentTiles.value.single())
            verify(request, never()).retryAttachment(any<String>(), anyOrNull())
            vm.attachmentTiles.value.single().status shouldBe AttachmentUploadStatus.DONE
            vm.attachmentTiles.value.single().attachment shouldBe uploaded
            runCurrent() // let the derived send gate collect the new tile list
            vm.attachmentsAllUploaded.value shouldBe true
        }
    }

    test("retry on a stale FAILED tile whose upload is still running follows it to DONE without retrying") {
        runFrame {
            val request = restoredRequest("b1", listOf(item("b1_0", UploadFileItemStatus.State.FAILED)))
            existing("alice", 0L, request)
            val vm = createViewModel()
            vm.setUser(alice)
            advanceUntilIdle()

            whenever(request.items).thenReturn(listOf(item("b1_0", UploadFileItemStatus.State.UPLOADING)))
            vm.retryAttachment(vm.attachmentTiles.value.single())
            verify(request, never()).retryAttachment(any<String>(), anyOrNull())
            vm.attachmentTiles.value.single().status shouldBe AttachmentUploadStatus.UPLOADING

            val uploaded = attachment("b1_0.jpg")
            whenever(request.items).thenReturn(
                listOf(item("b1_0", UploadFileItemStatus.State.UPLOADED, attachment = uploaded))
            )
            advanceTimeBy(600)
            runCurrent()
            vm.attachmentTiles.value.single().status shouldBe AttachmentUploadStatus.DONE
            vm.attachmentTiles.value.single().attachment shouldBe uploaded
        }
    }

    test("a followed in-flight file stops being read back once the chat is left") {
        runFrame {
            val request = restoredRequest("b1", listOf(item("b1_0", UploadFileItemStatus.State.UPLOADING)))
            existing("alice", 0L, request)
            existing("bob", 0L, null)
            val vm = createViewModel()
            vm.setUser(alice)
            runCurrent()
            vm.setUser(bob)
            advanceUntilIdle()
            vm.attachmentTiles.value.shouldBeEmpty()
            verify(request, Mockito.times(1)).items // only the restore read; no read-back after leaving
        }
    }

    test("isCopyMissing is never true for a live or app-built FAILED tile without a preview") {
        val tile = com.cometchat.uikit.core.models.AttachmentUploadTile(
            fileId = "x", name = "x.jpg", size = 1L, mimeType = "image/jpeg",
            status = AttachmentUploadStatus.FAILED, localUri = null
        )
        tile.isCopyMissing shouldBe false
    }

    test("D-306: a copy-missing tile is remove-only (retry never reaches the SDK; remove drops it)") {
        runFrame {
            val request = restoredRequest(
                "b1", listOf(item("b1_0", UploadFileItemStatus.State.FAILED, localCopyPath = null))
            )
            existing("alice", 0L, request)
            val vm = createViewModel()
            vm.setUser(alice)
            advanceUntilIdle()

            val tile = vm.attachmentTiles.value.single()
            tile.isCopyMissing shouldBe true
            vm.retryAttachment(tile)
            verify(request, never()).retryAttachment(any<String>(), anyOrNull())
            vm.attachmentTiles.value.single().status shouldBe AttachmentUploadStatus.FAILED

            vm.removeAttachment(tile)
            verify(request).removeAttachment("b1_0")
            vm.attachmentTiles.value.shouldBeEmpty()
        }
    }

    test("a restored REJECTED tile is never retried") {
        runFrame {
            val request = restoredRequest("b1", listOf(item("b1_0", UploadFileItemStatus.State.REJECTED)))
            existing("alice", 0L, request)
            val vm = createViewModel()
            vm.setUser(alice)
            advanceUntilIdle()

            vm.retryAttachment(vm.attachmentTiles.value[0])
            verify(request, never()).retryAttachment(any<String>(), anyOrNull())
            vm.attachmentTiles.value[0].status shouldBe AttachmentUploadStatus.REJECTED
        }
    }

    test("files added after a restore join the restored batch with non-colliding fileIds") {
        runFrame {
            val request = restoredRequest(
                "b1",
                listOf(item("b1_0", UploadFileItemStatus.State.FAILED), item("b1_3", UploadFileItemStatus.State.FAILED))
            )
            var added: List<UploadFileItem>? = null
            whenever(request.uploadAttachments(any<List<UploadFileItem>>(), any<UploadFileListener>()))
                .thenAnswer { added = it.getArgument(0); null }
            existing("alice", 0L, request)
            val vm = createViewModel()
            vm.setUser(alice)
            advanceUntilIdle()

            vm.stageAttachments(listOf(StagedAttachmentInput(File("/tmp/c.jpg"), "c.jpg", 1L, "image/jpeg")))
            cometChatMock.verify(
                { CometChat.createUploadFileRequest(any<String>(), any<String>()) },
                never()
            )
            added shouldNotBe null
            added!!.single().fileId shouldBe "b1_4"
            vm.attachmentTiles.value.map { it.fileId } shouldBe listOf("b1_0", "b1_3", "b1_4")
        }
    }
})
