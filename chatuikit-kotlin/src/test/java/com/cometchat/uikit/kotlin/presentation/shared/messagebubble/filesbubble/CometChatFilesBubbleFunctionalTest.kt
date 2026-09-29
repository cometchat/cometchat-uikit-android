package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.filesbubble

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.MediaMessage
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Complements the existing overflow-toggle tests: the binding contract an adapter
 * depends on, and the padding rule the timestamp row drives.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatFilesBubbleFunctionalTest {

    private fun files(count: Int, id: Long = 1L, caption: String? = null): MediaMessage =
        MockFactory.createMediaMessage(
            count = count,
            type = CometChatConstants.MESSAGE_TYPE_FILE,
            mimeType = "application/pdf",
            extension = "pdf",
            caption = caption,
        ).apply { this.id = id }

    private fun withBubble(block: (CometChatFilesBubble) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            block(CometChatFilesBubble(activity))
        }
        scenario.close()
    }

    private fun View.treeText(): String {
        val out = StringBuilder()
        fun walk(v: View) {
            if (v is TextView) out.append(v.text).append(' ')
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(this)
        return out.toString()
    }

    @Test
    fun oneFile_rendersItsName() = withBubble { bubble ->
        bubble.setMessage(files(1))
        assertTrue(bubble.treeText().contains("media_1.pdf"))
    }

    @Test
    fun severalFiles_renderEveryName() = withBubble { bubble ->
        bubble.setMessage(files(3))
        val text = bubble.treeText()
        assertTrue(text.contains("media_1.pdf"))
        assertTrue(text.contains("media_2.pdf"))
        assertTrue(text.contains("media_3.pdf"))
    }

    @Test
    fun aCaptionRenders() = withBubble { bubble ->
        bubble.setMessage(files(2, caption = "both drafts"))
        assertTrue(bubble.treeText().contains("both drafts"))
    }

    @Test
    fun rebindingReplacesTheList() = withBubble { bubble ->
        bubble.setMessage(files(3))
        bubble.setMessage(files(1, id = 2L))
        assertFalse("the previous row's third file must not survive", bubble.treeText().contains("media_3.pdf"))
    }

    @Test
    fun theBottomPaddingClosesUpWhileTheTimestampRowProvidesTheGap() = withBubble { bubble ->
        bubble.setMessage(files(2))
        assertEquals(0, bubble.paddingBottom)
    }

    @Test
    fun hidingTheTimestampRowRestoresTheBottomPadding() = withBubble { bubble ->
        bubble.setMessage(files(2))
        bubble.setStatusInfoVisible(false)
        assertTrue(bubble.paddingBottom > 0)
    }

    @Test
    fun rebindingRestoresTheDefaultPadding() = withBubble { bubble ->
        bubble.setMessage(files(2))
        bubble.setStatusInfoVisible(false)
        bubble.setMessage(files(2, id = 2L))
        assertEquals(0, bubble.paddingBottom)
    }

    @Test
    fun setOutgoingIsAcceptedOnBothSides() = withBubble { bubble ->
        bubble.setOutgoing(true)
        bubble.setMessage(files(2))
        assertTrue(bubble.treeText().contains("media_1.pdf"))
        bubble.setOutgoing(false)
        bubble.setMessage(files(2, id = 3L))
        assertTrue(bubble.treeText().contains("media_1.pdf"))
    }

    @Test
    fun textFormattersAreAcceptedBeforeBinding() = withBubble { bubble ->
        bubble.setTextFormatters(emptyList(), UIKitConstants.MessageBubbleAlignment.RIGHT)
        bubble.setMessage(files(1, caption = "with formatters"))
        assertTrue(bubble.treeText().contains("with formatters"))
    }

    @Test
    fun withNoAttachments_rendersNothing() = withBubble { bubble ->
        bubble.setMessage(files(0))
        assertFalse(bubble.treeText().contains("media_"))
    }
}
