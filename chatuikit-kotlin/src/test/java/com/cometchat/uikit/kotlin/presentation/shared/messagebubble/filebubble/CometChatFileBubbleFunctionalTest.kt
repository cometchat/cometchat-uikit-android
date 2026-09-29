package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.filebubble

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.MediaMessage
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.shared.interfaces.OnClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The View file bubble is bound and re-bound by a RecyclerView adapter, so the thing
 * worth pinning is that a bind leaves no residue from the previous row.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatFileBubbleFunctionalTest {

    private fun fileMessage(
        count: Int = 1,
        mimeType: String = "application/pdf",
        extension: String = "pdf",
        sizeBytes: Int? = null,
        caption: String? = null,
    ): MediaMessage = MockFactory.createMediaMessage(
        count = count,
        type = CometChatConstants.MESSAGE_TYPE_FILE,
        mimeType = mimeType,
        extension = extension,
        caption = caption,
    ).apply { if (count == 1 && sizeBytes != null) attachment.fileSize = sizeBytes }

    private fun withBubble(block: (CometChatFileBubble) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            block(CometChatFileBubble(activity))
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
    fun setMessage_showsTheFileName() = withBubble { bubble ->
        bubble.setMessage(fileMessage())
        assertEquals("media_1.pdf", bubble.getTitle().text.toString())
    }

    @Test
    fun setMessage_showsSizeAndType() = withBubble { bubble ->
        bubble.setMessage(fileMessage())
        assertTrue(
            "subtitle should carry size and type, was '${bubble.getSubtitle().text}'",
            bubble.getSubtitle().text.contains("3.1 MB"),
        )
    }

    @Test
    fun setMessage_withASmallFile_formatsInKilobytes() = withBubble { bubble ->
        bubble.setMessage(fileMessage(sizeBytes = 204_800))
        assertTrue(bubble.getSubtitle().text.contains("200 KB"))
    }

    @Test
    fun rebindingReplacesTheTitleRatherThanAppending() = withBubble { bubble ->
        bubble.setMessage(fileMessage())
        bubble.setMessage(fileMessage(mimeType = "application/zip", extension = "zip"))
        assertEquals("media_1.zip", bubble.getTitle().text.toString())
    }

    @Test
    fun setMessage_withNull_isHarmless() = withBubble { bubble ->
        bubble.setMessage(null)
    }

    @Test
    fun setFileUrl_setsTitleAndSubtitleDirectly() = withBubble { bubble ->
        bubble.setFileUrl("https://cdn.example.com/a.pdf", "a.pdf", "1 MB • PDF")
        assertEquals("a.pdf", bubble.getTitleText())
        assertEquals("1 MB • PDF", bubble.getSubTitleText())
        assertEquals("https://cdn.example.com/a.pdf", bubble.getFileUrl())
    }

    @Test
    fun titleAndSubtitleSettersReachTheViews() = withBubble { bubble ->
        bubble.setTitleText("report.pdf")
        bubble.setSubtitleText("2 MB • PDF")
        assertEquals("report.pdf", bubble.getTitle().text.toString())
        assertEquals("2 MB • PDF", bubble.getSubtitle().text.toString())
    }

    @Test
    fun theSingleFileBubbleDropsCaptions() = withBubble { bubble ->
        // Neither toolkit's single file bubble renders a caption -- there is no caption
        // parameter on the compose one and no caption handling here. Captions ride the
        // Files bubble, which is what a file message reaches by default. Pinned so the
        // asymmetry is a decision on record rather than a surprise.
        bubble.setMessage(fileMessage(caption = "the signed copy"))
        assertFalse(bubble.treeText().contains("the signed copy"))
    }

    @Test
    fun theDownloadAffordanceIsPresent() = withBubble { bubble ->
        bubble.setMessage(fileMessage())
        assertEquals(View.VISIBLE, bubble.getDownloadImageView().visibility)
    }

    @Test
    fun theFileIconIsPresent() = withBubble { bubble ->
        bubble.setMessage(fileMessage())
        assertEquals(View.VISIBLE, bubble.getFileIcon().visibility)
    }

    @Test
    fun theOnClickHandlerIsHeldAndHandedBack() = withBubble { bubble ->
        val handler = OnClick { }
        bubble.setOnClick(handler)
        assertTrue(handler === bubble.getOnClick())
        bubble.setOnClick(null)
        assertEquals(null, bubble.getOnClick())
    }

    @Test
    fun styleGettersReportWhatTheStyleHolds() = withBubble { bubble ->
        bubble.setCornerRadius(24)
        bubble.setBubbleStrokeWidth(3)
        assertEquals(24f, bubble.getBubbleCornerRadius(), 0.01f)
        assertEquals(3f, bubble.getBubbleStrokeWidth(), 0.01f)
    }
}
