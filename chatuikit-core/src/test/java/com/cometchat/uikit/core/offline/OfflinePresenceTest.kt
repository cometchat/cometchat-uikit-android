package com.cometchat.uikit.core.offline

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.mockito.Mockito

/**
 * ENG-41465: [OfflinePresence] hides "online" only when offline is on and the device is not
 * connected; offline off or connected is today's presence, untouched.
 */
class OfflinePresenceTest : FunSpec({

    val online = CometChatConstants.USER_STATUS_ONLINE
    val offline = CometChatConstants.USER_STATUS_OFFLINE

    afterTest {
        OfflineGate.testOverride = null
        OfflinePresence.resetForTest()
    }

    test("ENG-41551 A: presenceChanges ticks only when the hideOnline answer flips") {
        OfflinePresence.resetForTest()
        val changes = OfflinePresence.presenceChanges()
        val start = changes.value
        OfflinePresence.onConnectionChanged(hidden = true) // disconnected
        changes.value shouldBe start + 1
        OfflinePresence.onConnectionChanged(hidden = true) // still not connected: no re-bind
        changes.value shouldBe start + 1
        OfflinePresence.onConnectionChanged(hidden = false) // reconnected
        changes.value shouldBe start + 2
    }

    test("ENG-41551 A: offline on registers one connection listener; its callbacks tick on a flip") {
        OfflinePresence.resetForTest()
        OfflineGate.testOverride = true
        Mockito.mockStatic(CometChat::class.java).use { m ->
            m.`when`<String> { CometChat.getConnectionStatus() }.thenReturn(CometChatConstants.WS_STATE_CONNECTED)
            val changes = OfflinePresence.presenceChanges()
            OfflinePresence.presenceChanges()
            val captor = org.mockito.ArgumentCaptor.forClass(CometChat.ConnectionListener::class.java)
            m.verify({ CometChat.addConnectionListener(Mockito.anyString(), captor.capture()) }, Mockito.times(1))
            val start = changes.value
            captor.value.onDisconnected()
            changes.value shouldBe start + 1
            captor.value.onConnected()
            changes.value shouldBe start + 2
        }
    }

    test("ENG-41551 A: offline off registers nothing and never ticks") {
        OfflinePresence.resetForTest()
        OfflineGate.testOverride = false
        Mockito.mockStatic(CometChat::class.java).use { m ->
            OfflinePresence.presenceChanges().value shouldBe 0
            m.verify({ CometChat.addConnectionListener(Mockito.anyString(), Mockito.any()) }, Mockito.never())
        }
    }

    test("rule: offline on + disconnected hides online") {
        OfflinePresence.hideOnline(offlineOn = true, connected = false) shouldBe true
    }

    test("rule: connected shows presence as today") {
        OfflinePresence.hideOnline(offlineOn = true, connected = true) shouldBe false
    }

    test("rule: offline off shows presence as today, connected or not") {
        OfflinePresence.hideOnline(offlineOn = false, connected = false) shouldBe false
        OfflinePresence.hideOnline(offlineOn = false, connected = true) shouldBe false
    }

    test("hideOnline(): offline on and the SDK not connected → hidden") {
        OfflineGate.testOverride = true
        Mockito.mockStatic(CometChat::class.java).use { m ->
            m.`when`<String> { CometChat.getConnectionStatus() }.thenReturn("disconnected")
            OfflinePresence.hideOnline() shouldBe true
        }
    }

    test("hideOnline(): offline on and the SDK connected → as status") {
        OfflineGate.testOverride = true
        Mockito.mockStatic(CometChat::class.java).use { m ->
            m.`when`<String> { CometChat.getConnectionStatus() }.thenReturn(CometChatConstants.WS_STATE_CONNECTED)
            OfflinePresence.hideOnline() shouldBe false
        }
    }

    test("hideOnline(): offline off never reads the connection") {
        OfflineGate.testOverride = false
        Mockito.mockStatic(CometChat::class.java).use { m ->
            m.`when`<String> { CometChat.getConnectionStatus() }.thenReturn("disconnected")
            OfflinePresence.hideOnline() shouldBe false
            m.verify({ CometChat.getConnectionStatus() }, Mockito.never())
        }
    }

    test("hideOnline(): a failing connection read counts as not connected") {
        OfflineGate.testOverride = true
        Mockito.mockStatic(CometChat::class.java).use { m ->
            m.`when`<String> { CometChat.getConnectionStatus() }.thenThrow(RuntimeException("not initialised"))
            OfflinePresence.hideOnline() shouldBe true
        }
    }

    test("shownStatus: hidden turns online into offline, leaves every other value") {
        OfflinePresence.shownStatus(online, hideOnline = true) shouldBe offline
        OfflinePresence.shownStatus(offline, hideOnline = true) shouldBe offline
        OfflinePresence.shownStatus(null, hideOnline = true) shouldBe null
    }

    test("shownStatus: not hidden returns the status as is") {
        OfflinePresence.shownStatus(online, hideOnline = false) shouldBe online
        OfflinePresence.shownStatus(offline, hideOnline = false) shouldBe offline
        OfflinePresence.shownStatus(null, hideOnline = false) shouldBe null
    }
})
