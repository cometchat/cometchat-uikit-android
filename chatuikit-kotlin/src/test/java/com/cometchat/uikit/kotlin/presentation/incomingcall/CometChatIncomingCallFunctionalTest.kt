package com.cometchat.uikit.kotlin.presentation.incomingcall

import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.Call
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.shared.interfaces.OnClick
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config

/**
 * ENG-38680 — CometChatIncomingCall (View) functional surface. accept/decline
 * callbacks fire on the real (programmatically-created) buttons. No live calls
 * runtime — the callbacks are the UI-Kit's own OnClick lambdas.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatIncomingCallFunctionalTest {

    private fun withIncoming(block: (CometChatIncomingCall) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val v = CometChatIncomingCall(activity)
            v.setCall(mock<Call>())
            block(v)
        }
        scenario.close()
    }

    private fun button(v: CometChatIncomingCall, name: String): View {
        val f = CometChatIncomingCall::class.java.getDeclaredField(name).apply { isAccessible = true }
        return f.get(v) as View
    }

    @Test
    fun onAcceptClick_firesOnAcceptButtonClick() {
        withIncoming { v ->
            var accepted = false
            v.setOnAcceptClickListener(OnClick { accepted = true })
            button(v, "acceptButton").performClick()
            assertTrue("onAcceptClick should fire on accept-button click", accepted)
        }
    }

    @Test
    fun onRejectClick_firesOnDeclineButtonClick() {
        withIncoming { v ->
            var rejected = false
            v.setOnRejectClickListener(OnClick { rejected = true })
            button(v, "declineButton").performClick()
            assertTrue("onRejectClick should fire on decline-button click", rejected)
        }
    }
}
