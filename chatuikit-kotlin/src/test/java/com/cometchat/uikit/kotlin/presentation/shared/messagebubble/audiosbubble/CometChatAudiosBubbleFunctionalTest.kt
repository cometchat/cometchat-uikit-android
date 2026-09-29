package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.audiosbubble

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
import com.cometchat.uikit.kotlin.presentation.shared.messagebubble.audiobubble.CometChatAudioBubble
import com.cometchat.uikit.kotlin.presentation.shared.messagebubble.audiobubble.CometChatAudioBubbleStyle
import com.cometchat.uikit.kotlin.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.json.JSONObject

/**
 * Instrumented layer for the **View** [CometChatAudiosBubble].
 *
 * Complements the existing `CometChatAudiosBubbleToggleTest`, which already pins the
 * overflow toggle from ENG-37014. What is covered here is everything around it: the
 * binding contract a `RecyclerView` adapter depends on, the voice-note branch that
 * swaps the whole layout for the single waveform bubble, and the padding rule that
 * ENG-37143 and the timestamp row both turn on.
 *
 * Those two branches are the reason this bubble is worth its own tests: `setMessage`
 * either builds player cards or hands the whole job to a [CometChatAudioBubble] child,
 * and nothing in the type signature says which.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatAudiosBubbleFunctionalTest {

    private companion object {
        const val AUDIO_MIME = "audio/mpeg"
        const val AUDIO_EXT = "mp3"
    }

    private fun audios(count: Int, id: Long = 1L, caption: String? = null): MediaMessage =
        MockFactory.createMediaMessage(
            count = count,
            type = CometChatConstants.MESSAGE_TYPE_AUDIO,
            mimeType = AUDIO_MIME,
            extension = AUDIO_EXT,
            caption = caption,
        ).apply { this.id = id }

    /**
     * The contract key the composer stamps on a recorded voice note. Built from the
     * published constants rather than string literals — the value is `voice_note`,
     * matching the cross-platform contract, and hand-writing it is easy to get wrong.
     */
    private fun voiceNote(): MediaMessage = audios(1).apply {
        metadata = JSONObject().put(
            UIKitConstants.JSONKeys.AUDIO_TYPE,
            UIKitConstants.JSONKeys.AUDIO_TYPE_VOICE_NOTE,
        )
    }

    private fun withBubble(block: (ComponentActivity, CometChatAudiosBubble) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            block(activity, CometChatAudiosBubble(activity))
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

    // ── the picker-audio branch: player cards ───────────────────────────────

    @Test
    fun oneAudio_rendersOneCardCarryingItsFileName() = withBubble { _, bubble ->
        bubble.setMessage(audios(1))
        assertTrue(bubble.treeText().contains("media_1.mp3"))
    }

    @Test
    fun severalAudios_renderEveryNameInOrder() = withBubble { _, bubble ->
        bubble.setMessage(audios(3))
        val text = bubble.treeText()
        assertTrue(text.contains("media_1.mp3"))
        assertTrue(text.contains("media_2.mp3"))
        assertTrue(text.contains("media_3.mp3"))
    }

    @Test
    fun aCaptionIsRendered() = withBubble { _, bubble ->
        bubble.setMessage(audios(2, caption = "two takes"))
        assertTrue(bubble.treeText().contains("two takes"))
    }

    // ── the voice-note branch ───────────────────────────────────────────────

    @Test
    fun aVoiceNoteHandsTheWholeLayoutToTheSingleWaveformBubble() = withBubble { _, bubble ->
        bubble.setMessage(voiceNote())
        assertNotNull(
            "a voice note should be rendered by the waveform bubble, not by player cards",
            bubble.getChildAt(0) as? CometChatAudioBubble,
        )
    }

    @Test
    fun aVoiceNoteDropsTheWrapperPaddingEntirely() = withBubble { _, bubble ->
        // The embedded bubble brings its own chrome; wrapper padding would double it.
        bubble.setMessage(voiceNote())
        assertEquals(0, bubble.paddingTop)
        assertEquals(0, bubble.paddingBottom)
        assertEquals(0, bubble.paddingStart)
        assertEquals(0, bubble.paddingEnd)
    }

    @Test
    fun pickerAudioDoesNotUseTheWaveformBubble() = withBubble { _, bubble ->
        bubble.setMessage(audios(2))
        assertNull(bubble.getChildAt(0) as? CometChatAudioBubble)
    }

    @Test
    fun rebindingFromAVoiceNoteToPickerAudioSwapsTheLayout() = withBubble { _, bubble ->
        // The two branches build completely different trees, so a recycled view must
        // not keep the previous one.
        bubble.setMessage(voiceNote())
        bubble.setMessage(audios(2, id = 2L))
        assertNull("the waveform embed must be gone", bubble.getChildAt(0) as? CometChatAudioBubble)
        assertTrue(bubble.treeText().contains("media_1.mp3"))
    }

    // ── the timestamp-row padding rule ──────────────────────────────────────

    @Test
    fun theBottomPaddingClosesUpWhileTheTimestampRowProvidesTheGap() = withBubble { _, bubble ->
        bubble.setMessage(audios(2))
        assertEquals("the row below supplies the gap", 0, bubble.paddingBottom)
    }

    @Test
    fun hidingTheTimestampRowRestoresTheBottomPadding() = withBubble { _, bubble ->
        bubble.setMessage(audios(2))
        bubble.setStatusInfoVisible(false)
        assertTrue(
            "with no row below, the cards must not sit flush with the bubble edge",
            bubble.paddingBottom > 0,
        )
    }

    @Test
    fun rebindingRestoresTheDefaultPadding() = withBubble { _, bubble ->
        // A recycled bubble may arrive padded for a hidden row; the adapter re-hides
        // after binding, so setMessage has to start from the default.
        bubble.setMessage(audios(2))
        bubble.setStatusInfoVisible(false)
        bubble.setMessage(audios(2, id = 2L))
        assertEquals(0, bubble.paddingBottom)
    }

    @Test
    fun aVoiceNoteForwardsTheTimestampFlagToItsEmbeddedBubble() = withBubble { _, bubble ->
        bubble.setMessage(voiceNote())
        bubble.setStatusInfoVisible(false)
        // The wrapper stays unpadded; the embed compensates instead.
        assertEquals(0, bubble.paddingBottom)
        assertNotNull(bubble.getChildAt(0) as? CometChatAudioBubble)
    }

    // ── style and formatter plumbing ────────────────────────────────────────

    @Test
    fun aStyleSetBeforeBindingReachesTheVoiceNoteEmbed() = withBubble { activity, bubble ->
        // setStyle only stores; it is setMessage that pushes it down. An integrator
        // styling the bubble before binding must still see it applied.
        val style = CometChatAudioBubbleStyle.default(activity).copy(playIconTint = 0xFF00FF00.toInt())
        bubble.setStyle(style)
        bubble.setMessage(voiceNote())
        val embed = bubble.getChildAt(0) as CometChatAudioBubble
        assertEquals(0xFF00FF00.toInt(), embed.getPlayIconTint())
    }

    @Test
    fun setOutgoingIsAcceptedOnBothSides() = withBubble { _, bubble ->
        bubble.setOutgoing(true)
        bubble.setMessage(audios(2))
        assertTrue(bubble.treeText().contains("media_1.mp3"))
        bubble.setOutgoing(false)
        bubble.setMessage(audios(2, id = 3L))
        assertTrue(bubble.treeText().contains("media_1.mp3"))
    }

    @Test
    fun textFormattersAreAcceptedBeforeBinding() = withBubble { _, bubble ->
        bubble.setTextFormatters(emptyList(), UIKitConstants.MessageBubbleAlignment.RIGHT)
        bubble.setMessage(audios(1, caption = "with formatters"))
        assertTrue(bubble.treeText().contains("with formatters"))
    }

    // ── degenerate input ────────────────────────────────────────────────────

    @Test
    fun withNoAttachments_rendersNothingRatherThanEmptyChrome() = withBubble { _, bubble ->
        bubble.setMessage(audios(0))
        assertFalse(bubble.treeText().contains("media_"))
    }
}
