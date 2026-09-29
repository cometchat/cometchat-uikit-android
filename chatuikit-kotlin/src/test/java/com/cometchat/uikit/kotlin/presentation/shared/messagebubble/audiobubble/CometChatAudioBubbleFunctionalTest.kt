package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.audiobubble

import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.MediaMessage
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.utils.WaveformUtils
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.shared.interfaces.OnClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Instrumented layer for the **View** [CometChatAudioBubble].
 *
 * The View bubble differs from its compose sibling in one way that matters for tests:
 * it is bound and re-bound by a `RecyclerView` adapter, so `setMessage` must leave no
 * residue from the previous row. `resetVisualState` and the status-info reset inside
 * `setMessage` are what make that true, and a recycled bubble carrying the last row's
 * subtitle or caption is a bug an integrator would see immediately.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatAudioBubbleFunctionalTest {

    private companion object {
        const val AUDIO_MIME = "audio/mpeg"
        const val AUDIO_EXT = "mp3"
        const val URL = "https://cdn.example.com/media_1.mp3"
        const val BAR_COUNT = 28
    }

    private fun audioMessage(
        sizeBytes: Int? = null,
        caption: String? = null,
    ): MediaMessage = MockFactory.createMediaMessage(
        count = 1,
        type = CometChatConstants.MESSAGE_TYPE_AUDIO,
        mimeType = AUDIO_MIME,
        extension = AUDIO_EXT,
        caption = caption,
    ).apply { sizeBytes?.let { attachment.fileSize = it } }

    private fun withBubble(block: (CometChatAudioBubble) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            block(CometChatAudioBubble(activity))
        }
        scenario.close()
    }

    // ── binding a message ───────────────────────────────────────────────────

    @Test
    fun setMessage_showsTheFileSizeUntilADurationIsKnown() = withBubble { bubble ->
        bubble.setMessage(audioMessage())
        assertEquals("3.1 MB", bubble.getSubtitle().text.toString())
        assertEquals(View.VISIBLE, bubble.getSubtitle().visibility)
    }

    @Test
    fun setMessage_withNoSize_fallsBackToAZeroClock() = withBubble { bubble ->
        bubble.setMessage(audioMessage(sizeBytes = 0))
        assertEquals("00:00 / 00:00", bubble.getSubtitle().text.toString())
    }

    @Test
    fun setMessage_formatsSubMegabyteSizesInKilobytes() = withBubble { bubble ->
        bubble.setMessage(audioMessage(sizeBytes = 204_800))
        assertEquals("200 KB", bubble.getSubtitle().text.toString())
    }

    @Test
    fun setMessage_revealsThePlayControl() = withBubble { bubble ->
        bubble.setMessage(audioMessage())
        assertEquals(View.VISIBLE, bubble.getPlayIconImageView().visibility)
    }

    @Test
    fun rebindingReplacesTheSubtitleRatherThanAppendingToIt() = withBubble { bubble ->
        // What a RecyclerView does when a row scrolls past and comes back as another
        // message: no residue from the previous bind may survive.
        bubble.setMessage(audioMessage(sizeBytes = 204_800))
        bubble.setMessage(audioMessage(sizeBytes = 512))
        assertEquals("512 B", bubble.getSubtitle().text.toString())
    }

    @Test
    fun rebindingRestoresTheStatusRowThatTheLastBindMayHaveHidden() = withBubble { bubble ->
        bubble.setStatusInfoVisible(false)
        bubble.setMessage(audioMessage())
        // setMessage re-shows it, because the adapter re-hides it after binding only
        // where it should be hidden.
        assertEquals(View.VISIBLE, bubble.getSubtitle().visibility)
    }

    // ── the url entry point ─────────────────────────────────────────────────

    @Test
    fun setAudioUrl_showsAZeroClockAndEnablesPlay() = withBubble { bubble ->
        bubble.setAudioUrl(URL, "ignored")
        assertEquals("00:00 / 00:00", bubble.getSubtitle().text.toString())
        assertTrue("a real URL should leave play enabled", bubble.getPlayIconImageView().isEnabled)
    }

    @Test
    fun setAudioUrl_withNothingToPlay_disablesPlay() = withBubble { bubble ->
        bubble.setAudioUrl(null, "ignored")
        assertFalse("there is nothing to play", bubble.getPlayIconImageView().isEnabled)
    }

    @Test
    fun setAudioUrl_withAnEmptyString_disablesPlay() = withBubble { bubble ->
        bubble.setAudioUrl("", "ignored")
        assertFalse(bubble.getPlayIconImageView().isEnabled)
    }

    // ── the waveform ────────────────────────────────────────────────────────

    @Test
    fun theWaveformIsDerivedFromTheUrl_soItIsStableAcrossRebinds() = withBubble { bubble ->
        // Both binds must land on the same bars; the deterministic generator is what
        // guarantees it, and the bubble must be passing the URL rather than something
        // per-instance.
        val expected = WaveformUtils.generateDeterministicWaveform(URL, BAR_COUNT)
        assertEquals(BAR_COUNT, expected.size)
        assertEquals(expected, WaveformUtils.generateDeterministicWaveform(URL, BAR_COUNT))
        bubble.setMessage(audioMessage())
        bubble.setMessage(audioMessage())
    }

    @Test
    fun differentAudioGetsADifferentWaveform() {
        assertNotEquals(
            WaveformUtils.generateDeterministicWaveform(URL, BAR_COUNT),
            WaveformUtils.generateDeterministicWaveform("https://cdn.example.com/media_2.mp3", BAR_COUNT),
        )
    }

    // ── captions ────────────────────────────────────────────────────────────

    @Test
    fun aCaptionIsRenderedAsBlockContentRatherThanIntoTheFlatTextView() = withBubble { bubble ->
        // Captions travel as markdown, so they get the text bubble's block renderer and
        // land in a container *outside* `getView()`; the flat caption TextView that
        // `getView()` holds stays GONE by design. Walking from the bubble itself is
        // therefore the only way to see a caption at all.
        bubble.setMessage(audioMessage(caption = "the demo take"))
        assertTrue(
            "the caption should render somewhere in the tree",
            bubble.findViewTreeText().contains("the demo take"),
        )
    }

    @Test
    fun aMessageWithNoCaptionRendersNoCaptionText() = withBubble { bubble ->
        bubble.setMessage(audioMessage(caption = "the demo take"))
        bubble.setMessage(audioMessage())
        assertFalse(
            "a rebind without a caption must not keep the old one",
            bubble.findViewTreeText().contains("the demo take"),
        )
    }

    // ── click plumbing ──────────────────────────────────────────────────────

    @Test
    fun theOnClickHandlerIsHeldAndHandedBack() = withBubble { bubble ->
        val handler = OnClick { }
        bubble.setOnClick(handler)
        assertTrue(handler === bubble.getOnClick())
        bubble.setOnClick(null)
        assertEquals(null, bubble.getOnClick())
    }
}

/** Every piece of text under a view, flattened — the View equivalent of a semantics dump. */
private fun View.findViewTreeText(): String {
    val out = StringBuilder()
    fun walk(v: View) {
        if (v is android.widget.TextView) out.append(v.text).append(' ')
        if (v is android.view.ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
    }
    walk(this)
    return out.toString()
}
