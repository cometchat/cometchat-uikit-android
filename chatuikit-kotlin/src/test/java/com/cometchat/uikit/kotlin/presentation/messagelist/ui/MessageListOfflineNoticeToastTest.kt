package com.cometchat.uikit.kotlin.presentation.messagelist.ui

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.offline.OfflineNotice
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import com.cometchat.uikit.kotlin.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

/**
 * ENG-41640 (A): the View message list shows its offline notices as a toast, as the Compose list
 * does, and shows none when [CometChatMessageList.setHideOfflineNotices] is set. The notice is
 * raised on the real ViewModel the way `MessageListOfflineSendRows` raises it for a queue-full send.
 *
 * Run: ./gradlew :chatuikit-kotlin:testDebugUnitTest --tests "*MessageListOfflineNoticeToastTest"
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MessageListOfflineNoticeToastTest {

    private lateinit var cometChatMock: MockedStatic<CometChat>

    @Before
    fun setUp() {
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        cometChatMock.`when`<Boolean> { CometChat.isOfflineEnabled() }.thenReturn(true)
        ShadowToast.reset()
    }

    @After
    fun tearDown() {
        cometChatMock.close()
    }

    private fun user(): User = User().apply {
        uid = "test-user"
        name = "Test User"
    }

    private fun repository(): MessageListRepository = mock {
        onBlocking { fetchPreviousMessages() }.thenReturn(Result.success(emptyList<BaseMessage>()))
        on { hasMorePreviousMessages() }.thenReturn(false)
        onBlocking { markAsRead(any()) }.thenReturn(Result.success(Unit))
        onBlocking { markAsDelivered(any()) }.thenReturn(Result.success(Unit))
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    /** `raiseOfflineNotice` is internal to core (its JVM name is module-mangled): reach it by prefix. */
    private fun raise(vm: CometChatMessageListViewModel, notice: OfflineNotice) {
        val method = CometChatMessageListViewModel::class.java.declaredMethods
            .first { it.name.startsWith("raiseOfflineNotice") }
        method.isAccessible = true
        method.invoke(vm, notice)
    }

    private fun withList(setUp: (CometChatMessageList) -> Unit, block: (CometChatMessageListViewModel) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            // Listeners on: that is what starts the ViewModel's notice relay (offline on only).
            val viewModel = CometChatMessageListViewModel(repository = repository(), enableListeners = true)
            val list = CometChatMessageList(activity)
            activity.setContentView(list)
            setUp(list)
            list.setViewModel(viewModel)
            list.setUser(user())
            idle()
            block(viewModel)
        }
        scenario.close()
    }

    @Test
    fun `offline on - a queue-full notice shows the Too many messages waiting toast`() {
        withList(setUp = {}) { vm ->
            raise(vm, OfflineNotice.QueueFull)
            idle()
            assertEquals(
                ApplicationText.queueFull(),
                ShadowToast.getTextOfLatestToast()
            )
            assertEquals(1, ShadowToast.shownToastCount())
        }
    }

    @Test
    fun `default ViewModel, bound before attach as an XML layout does - the toast shows`() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val list = CometChatMessageList(activity)
            list.setUser(user())
            activity.setContentView(list)
            idle()
            raise(list.getViewModel()!!, OfflineNotice.QueueFull)
            idle()
            assertEquals(ApplicationText.queueFull(), ShadowToast.getTextOfLatestToast())
            assertEquals(1, ShadowToast.shownToastCount())
        }
        scenario.close()
    }

    @Test
    fun `hideOfflineNotices(true) - no toast for a queue-full notice`() {
        withList(setUp = { it.setHideOfflineNotices(true) }) { vm ->
            raise(vm, OfflineNotice.QueueFull)
            idle()
            assertNull(ShadowToast.getTextOfLatestToast())
            assertEquals(0, ShadowToast.shownToastCount())
        }
    }

    private object ApplicationText {
        fun queueFull(): String = androidx.test.core.app.ApplicationProvider
            .getApplicationContext<android.content.Context>()
            .getString(com.cometchat.uikit.core.R.string.cometchat_queue_full)
    }
}
