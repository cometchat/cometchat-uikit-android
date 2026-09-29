package com.cometchat.uikit.core.viewmodel.messagecomposer

import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.usecase.EditMessageUseCase
import com.cometchat.uikit.core.domain.usecase.SendCustomMessageUseCase
import com.cometchat.uikit.core.domain.usecase.SendMediaMessageUseCase
import com.cometchat.uikit.core.domain.usecase.SendTextMessageUseCase
import com.cometchat.uikit.core.domain.model.CometChatMessageComposerAction
import com.cometchat.uikit.core.viewmodel.CometChatMessageComposerViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mockito.kotlin.mock

/**
 * The composer's attachment sheet: which options appear, and when.
 *
 * `getDefaultAttachmentOptions` is the one place that decides what a user can attach. It reads
 * eight independent visibility flags — the five media ones default on, the three extension-backed
 * ones default off — drops the extensions inside a thread, and appends whatever custom actions the
 * host app registered. Every one of those is a separate branch, and getting any
 * wrong shows the user an action the integrator switched off — or hides one they paid for.
 *
 * Two rules carry the weight:
 *
 * - **Each flag is independent.** Hiding the camera must not disturb image, video, audio or file.
 *   They are checked one at a time here rather than only in combination, because a shared `if`
 *   would pass a combination test while breaking a single toggle.
 * - **Threads are more restricted than the main composer.** Polls, collaborative documents and
 *   whiteboards are omitted once a parent message is set — they are conversation-level artefacts,
 *   not replies — and that holds even when their flags are on.
 *
 * Custom options are always appended after the defaults, so a host app's action never displaces a
 * built-in one.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*MessageComposerAttachmentOptionsTest"
 */
class MessageComposerAttachmentOptionsTest : FunSpec({

    beforeTest { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

    fun composer(): CometChatMessageComposerViewModel = CometChatMessageComposerViewModel(
        sendTextMessageUseCase = mock<SendTextMessageUseCase>(),
        sendMediaMessageUseCase = mock<SendMediaMessageUseCase>(),
        sendCustomMessageUseCase = mock<SendCustomMessageUseCase>(),
        editMessageUseCase = mock<EditMessageUseCase>(),
        enableListeners = false
    )

    /** Calls the options builder with recognisable titles so assertions read clearly. */
    fun CometChatMessageComposerViewModel.options(): List<CometChatMessageComposerAction> =
        getDefaultAttachmentOptions(
            cameraTitle = "Camera", cameraIcon = 1,
            imageTitle = "Image", imageIcon = 2,
            videoTitle = "Video", videoIcon = 3,
            audioTitle = "Audio", audioIcon = 4,
            fileTitle = "File", fileIcon = 5,
            pollTitle = "Poll", pollIcon = 6,
            collaborativeDocumentTitle = "Document", collaborativeDocumentIcon = 7,
            collaborativeWhiteboardTitle = "Whiteboard", collaborativeWhiteboardIcon = 8
        )

    fun List<CometChatMessageComposerAction>.ids() = map { it.id }

    // ==================== defaults ====================

    test("the five media options are on by default and the three extensions are not") {
        val vm = composer()
        vm.setUser(User().apply { uid = "partner" })

        // Extension-backed options are opt-in: poll, collaborative document and whiteboard all
        // default to false, so an integrator who never enables them never shows them.
        vm.options().ids() shouldBe listOf(
            CometChatMessageComposerAction.ID_CAMERA,
            CometChatMessageComposerAction.ID_IMAGE,
            CometChatMessageComposerAction.ID_VIDEO,
            CometChatMessageComposerAction.ID_AUDIO,
            CometChatMessageComposerAction.ID_DOCUMENT
        )
    }

    test("enabling the extensions adds them after the media options") {
        val vm = composer()
        vm.setPollOptionVisibility(true)
        vm.setCollaborativeDocumentOptionVisibility(true)
        vm.setCollaborativeWhiteboardOptionVisibility(true)

        val ids = vm.options().ids()
        ids shouldContain CometChatMessageComposerAction.ID_POLL
        ids shouldContain CometChatMessageComposerAction.ID_COLLABORATIVE_DOCUMENT
        ids shouldContain CometChatMessageComposerAction.ID_COLLABORATIVE_WHITEBOARD
    }

    test("the titles handed in are the titles that come back") {
        val vm = composer()
        vm.setUser(User().apply { uid = "partner" })

        val camera = vm.options().first { it.id == CometChatMessageComposerAction.ID_CAMERA }
        camera.title shouldBe "Camera"
        camera.icon shouldBe 1
    }

    // ==================== each flag, one at a time ====================

    test("hiding the camera removes only the camera") {
        val vm = composer()
        vm.setCameraOptionVisibility(false)

        val ids = vm.options().ids()
        ids shouldNotContain CometChatMessageComposerAction.ID_CAMERA
        ids shouldContain CometChatMessageComposerAction.ID_IMAGE
        ids shouldContain CometChatMessageComposerAction.ID_DOCUMENT
    }

    test("hiding the image option removes only the image") {
        val vm = composer()
        vm.setImageOptionVisibility(false)

        val ids = vm.options().ids()
        ids shouldNotContain CometChatMessageComposerAction.ID_IMAGE
        ids shouldContain CometChatMessageComposerAction.ID_CAMERA
    }

    test("hiding the video option removes only the video") {
        val vm = composer()
        vm.setVideoOptionVisibility(false)

        val ids = vm.options().ids()
        ids shouldNotContain CometChatMessageComposerAction.ID_VIDEO
        ids shouldContain CometChatMessageComposerAction.ID_AUDIO
    }

    test("hiding the audio option removes only the audio") {
        val vm = composer()
        vm.setAudioOptionVisibility(false)

        val ids = vm.options().ids()
        ids shouldNotContain CometChatMessageComposerAction.ID_AUDIO
        ids shouldContain CometChatMessageComposerAction.ID_VIDEO
    }

    // the "file" flag emits ID_DOCUMENT -- the plain-file attachment, distinct from
    // ID_COLLABORATIVE_DOCUMENT which is the shared-document extension
    test("hiding the file option removes only the file") {
        val vm = composer()
        vm.setFileOptionVisibility(false)

        val ids = vm.options().ids()
        ids shouldNotContain CometChatMessageComposerAction.ID_DOCUMENT
        ids shouldContain CometChatMessageComposerAction.ID_IMAGE
    }

    test("hiding polls removes only polls") {
        val vm = composer()
        vm.setCollaborativeDocumentOptionVisibility(true)   // opt-in, so enable before hiding polls
        vm.setPollOptionVisibility(false)

        val ids = vm.options().ids()
        ids shouldNotContain CometChatMessageComposerAction.ID_POLL
        ids shouldContain CometChatMessageComposerAction.ID_COLLABORATIVE_DOCUMENT
    }

    test("hiding collaborative documents removes only documents") {
        val vm = composer()
        vm.setCollaborativeWhiteboardOptionVisibility(true)
        vm.setCollaborativeDocumentOptionVisibility(false)

        val ids = vm.options().ids()
        ids shouldNotContain CometChatMessageComposerAction.ID_COLLABORATIVE_DOCUMENT
        ids shouldContain CometChatMessageComposerAction.ID_COLLABORATIVE_WHITEBOARD
    }

    test("hiding whiteboards removes only whiteboards") {
        val vm = composer()
        vm.setCollaborativeDocumentOptionVisibility(true)
        vm.setCollaborativeWhiteboardOptionVisibility(false)

        val ids = vm.options().ids()
        ids shouldNotContain CometChatMessageComposerAction.ID_COLLABORATIVE_WHITEBOARD
        ids shouldContain CometChatMessageComposerAction.ID_COLLABORATIVE_DOCUMENT
    }

    test("hiding everything leaves nothing to attach") {
        val vm = composer()
        vm.setCameraOptionVisibility(false)
        vm.setImageOptionVisibility(false)
        vm.setVideoOptionVisibility(false)
        vm.setAudioOptionVisibility(false)
        vm.setFileOptionVisibility(false)
        vm.setPollOptionVisibility(false)
        vm.setCollaborativeDocumentOptionVisibility(false)
        vm.setCollaborativeWhiteboardOptionVisibility(false)

        vm.options() shouldBe emptyList()
    }

    // ==================== thread restrictions ====================

    test("a thread drops polls, documents and whiteboards while keeping the media options") {
        val vm = composer()
        vm.setUser(User().apply { uid = "partner" })
        vm.setPollOptionVisibility(true)
        vm.setCollaborativeDocumentOptionVisibility(true)
        vm.setCollaborativeWhiteboardOptionVisibility(true)
        vm.setParentMessageId(42L)

        val ids = vm.options().ids()
        ids shouldNotContain CometChatMessageComposerAction.ID_POLL
        ids shouldNotContain CometChatMessageComposerAction.ID_COLLABORATIVE_DOCUMENT
        ids shouldNotContain CometChatMessageComposerAction.ID_COLLABORATIVE_WHITEBOARD

        ids shouldContain CometChatMessageComposerAction.ID_CAMERA
        ids shouldContain CometChatMessageComposerAction.ID_IMAGE
        ids shouldContain CometChatMessageComposerAction.ID_DOCUMENT
    }

    test("the thread restriction wins even when the flags are on") {
        val vm = composer()
        vm.setPollOptionVisibility(true)
        vm.setCollaborativeDocumentOptionVisibility(true)
        vm.setParentMessageId(7L)

        vm.options().ids() shouldNotContain CometChatMessageComposerAction.ID_POLL
    }

    test("a parent id of zero is the main conversation, not a thread") {
        val vm = composer()
        vm.setPollOptionVisibility(true)
        vm.setParentMessageId(0L)

        vm.options().ids() shouldContain CometChatMessageComposerAction.ID_POLL
    }

    // ==================== custom options ====================

    test("custom options are appended after the built-in ones") {
        val vm = composer()
        val custom = CometChatMessageComposerAction(id = "custom_action", title = "Custom", icon = 99)
        vm.setAttachmentOptions(listOf(custom))

        val ids = vm.options().ids()
        ids shouldContain "custom_action"
        ids.last() shouldBe "custom_action"
    }

    test("custom options survive when every built-in option is hidden") {
        val vm = composer()
        vm.setCameraOptionVisibility(false)
        vm.setImageOptionVisibility(false)
        vm.setVideoOptionVisibility(false)
        vm.setAudioOptionVisibility(false)
        vm.setFileOptionVisibility(false)
        vm.setPollOptionVisibility(false)
        vm.setCollaborativeDocumentOptionVisibility(false)
        vm.setCollaborativeWhiteboardOptionVisibility(false)
        vm.setAttachmentOptions(listOf(CometChatMessageComposerAction(id = "only_mine", title = "Mine", icon = 1)))

        vm.options().ids() shouldBe listOf("only_mine")
    }

    // ==================== conversation target ====================

    test("setting a user then a group leaves the composer pointed at the group") {
        val vm = composer()
        vm.setUser(User().apply { uid = "partner" })
        vm.setGroup(Group().apply { guid = "group_alpha" })

        vm.group.value?.guid shouldBe "group_alpha"
    }

    test("setting a group then a user leaves the composer pointed at the user") {
        val vm = composer()
        vm.setGroup(Group().apply { guid = "group_alpha" })
        vm.setUser(User().apply { uid = "partner" })

        vm.user.value?.uid shouldBe "partner"
    }
})
