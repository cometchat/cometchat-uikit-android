package com.cometchat.uikit.kotlin.presentation.outgoingcall

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
 * ENG-38680 — CometChatOutgoingCall (View) functional surface. The end-call
 * callback fires on the real (programmatically-created) end button. No live calls
 * runtime — the callback is the UI-Kit's own OnClick lambda.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatOutgoingCallFunctionalTest {

    @Test
    fun onEndCallClick_firesOnEndButtonClick() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val v = CometChatOutgoingCall(activity)
            v.setCall(mock<Call>())
            var ended = false
            v.setOnEndCallClickListener(OnClick { ended = true })
            val f = CometChatOutgoingCall::class.java.getDeclaredField("endCallButton").apply { isAccessible = true }
            (f.get(v) as View).performClick()
            assertTrue("onEndCallClick should fire on end-button click", ended)
        }
        scenario.close()
    }
}
