package com.cometchat.uikit.kotlin.presentation.messagecomposer

import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.models.User
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.messagecomposer.ui.CometChatMessageComposer
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.NonDefaultValues
import com.cometchat.uikit.propmatrix.Prop
import com.cometchat.uikit.propmatrix.PropKind
import com.cometchat.uikit.propmatrix.ViewPropSweep
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * Reflective sweep over the View composer's `set*` surface -- the widest one in the
 * toolkit at 110 declared setters, of which the great majority are the flat styling
 * props (`setComposerBackgroundColor`, `setEditPreviewCloseIconTint`, ...) that an
 * integrator reaches for when they want to restyle the composer without building a
 * whole [com.cometchat.uikit.kotlin.presentation.messagecomposer.style.CometChatMessageComposerStyle].
 * Nothing previously constructed the component from a test in this module except the
 * screenshot suite, which drives four or five props and captures pixels, so the flat
 * setter surface had no layer that claimed it.
 *
 * The instance is attached, because the composer wires its click listeners and lays out
 * its two row variants on attach; a detached one accepts every setter without ever
 * reaching the views the setter is supposed to move.
 *
 * Three things the mechanical value can't express are registered by hand:
 *   - resource-id setters (`@StyleRes`), where the harness's default int is not a
 *     resolvable id;
 *   - the nine visibility ints, where an arbitrary int is *accepted* (the component
 *     only ever compares it against [View.VISIBLE]) but would assert nothing -- so a
 *     real visibility constant is registered, chosen to be the non-default one; and
 *   - the icon setters, which the harness has no producer for. Those are the reason
 *     `Drawable` is not treated as functional-pending here the way it is in the older
 *     matrices: a drawable prop on this component is a plain hand-it-to-the-ImageView
 *     value with a getter, so it belongs in the denominator, not on the pending list.
 *
 * [styleSettersRoundTripThroughTheirGetters] is the half the mechanical sweep cannot
 * do: "applied cleanly" only means the setter did not throw. Every flat styling prop
 * on this component has a paired getter reading the same field, so the pair is asserted
 * directly and a setter that silently drops its argument is caught.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatMessageComposerPropMatrixTest {

    private companion object {
        const val OWNER = "CometChatMessageComposer"

        /** The `@StyleRes` setters: a real, resolvable style id per prop. */
        val STYLE_RES_PROPS = listOf(
            "EditPreviewTitleTextAppearance",
            "EditPreviewMessageTextAppearance",
            "MessagePreviewTitleTextAppearance",
            "MessagePreviewSubtitleTextAppearance",
            "InputTextAppearance",
            "InputPlaceholderAppearance",
            "MentionTextStyle",
        )

        /**
         * The icon setters. A [ColorDrawable] is a perfectly good non-default icon for a
         * prop whose only contract is "hand this drawable to that ImageView", and each
         * prop gets its own instance so the round-trip below asserts identity rather than
         * the vacuous equality of two default-constructed drawables.
         */
        val DRAWABLE_PROPS = listOf(
            "AttachmentIcon",
            "VoiceRecordingIcon",
            "AIIcon",
            "StickerIcon",
            "SendButtonActiveIcon",
            "SendButtonInactiveIcon",
            "SendButtonStopIcon",
            "EditPreviewCloseIcon",
            "MessagePreviewCloseIcon",
        )

        /**
         * The attachment-option visibility setters. Every one defaults to shown, so
         * [View.GONE] is the non-default value.
         */
        val OPTION_VISIBILITY_PROPS = listOf(
            "CameraOptionVisibility",
            "ImageOptionVisibility",
            "VideoOptionVisibility",
            "AudioOptionVisibility",
            "FileOptionVisibility",
            "PollOptionVisibility",
            "CollaborativeDocumentOptionVisibility",
            "CollaborativeWhiteboardOptionVisibility",
        )
    }

    private lateinit var cometChat: MockedStatic<com.cometchat.chat.core.CometChat>

    @Before
    fun stubLoggedInUser() {
        cometChat = Mockito.mockStatic(com.cometchat.chat.core.CometChat::class.java)
        val me = mock<User>()
        whenever(me.uid).thenReturn("logged-in-user")
        whenever(me.name).thenReturn("Logged In User")
        cometChat.`when`<User?> { com.cometchat.chat.core.CometChat.getLoggedInUser() }
            .thenReturn(me)
    }

    @After
    fun tearDown() {
        cometChat.close()
        NonDefaultValues.clearRegistered()
    }

    private fun isFunctionalPending(paramType: Class<*>): Boolean =
        View::class.java.isAssignableFrom(paramType) ||
            Function::class.java.isAssignableFrom(paramType) ||
            List::class.java.isAssignableFrom(paramType) ||
            Map::class.java.isAssignableFrom(paramType) ||
            paramType.isInterface ||
            paramType.simpleName == "User" ||
            paramType.simpleName == "Group" ||
            paramType.simpleName == "BaseMessage" ||
            paramType.simpleName == "TextMessage" ||
            paramType.simpleName.endsWith("Style") ||
            paramType.simpleName.endsWith("Configuration") ||
            paramType.simpleName.endsWith("ViewModel") ||
            paramType.simpleName.endsWith("Listener") ||
            paramType.simpleName.endsWith("Callback")

    /** One stable drawable per icon prop, so set-then-get compares the same instance. */
    private val drawableFixtures: Map<String, ColorDrawable> =
        DRAWABLE_PROPS.withIndex().associate { (i, prop) -> prop to ColorDrawable(0xFF00FF00.toInt() + i) }

    /** Values the harness cannot derive from the Java type alone. See the class doc. */
    private fun registerHandRegisteredValues() {
        NonDefaultValues.registerProp(OWNER, "Style") { R.style.CometChatMessageComposerStyle }
        DRAWABLE_PROPS.forEach { prop ->
            NonDefaultValues.registerProp(OWNER, prop) { drawableFixtures.getValue(prop) }
        }
        STYLE_RES_PROPS.forEach { prop ->
            NonDefaultValues.registerProp(OWNER, prop) { R.style.CometChatTextAppearanceBody_Regular }
        }
        OPTION_VISIBILITY_PROPS.forEach { prop ->
            NonDefaultValues.registerProp(OWNER, prop) { View.GONE }
        }
        // The toolbar defaults to hidden, so VISIBLE is this one's non-default.
        NonDefaultValues.registerProp(OWNER, "RichTextToolbarVisibility") { View.VISIBLE }
    }

    private fun attachedComposer(activity: ComponentActivity): CometChatMessageComposer {
        activity.setTheme(R.style.CometChatTheme_DayNight)
        val composer = CometChatMessageComposer(activity)
        activity.setContentView(composer)
        ShadowLooper.idleMainLooper()
        return composer
    }

    @Test
    fun mechanicalValueMatrix_coversEveryValueProp() {
        registerHandRegisteredValues()
        val setters = ViewPropSweep.setters(CometChatMessageComposer::class.java)
        assertTrue("expected the widest setter surface in the toolkit, was ${setters.size}", setters.size > 90)

        val props = mutableListOf<Prop>()
        val functionalPending = mutableListOf<String>()

        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            val composer = attachedComposer(activity)
            setters.forEach { setter ->
                when {
                    isFunctionalPending(setter.paramType) ->
                        functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                    setter.supported ->
                        props += Prop(
                            OWNER,
                            setter.propName,
                            PropKind.VALUE,
                            covered = runCatching { setter.applyNonDefault(composer) }.isSuccess,
                        )
                    else -> functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                }
            }
        }
        scenario.close()

        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [composer view VALUE] ${cov.covered}/${cov.total}; functional-pending=${functionalPending.size} $functionalPending")
        if (uncovered.isNotEmpty()) println("  [composer view] NOT covered: $uncovered")

        assertEquals("every swept VALUE prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }

    /**
     * "Applied cleanly" only means the setter did not throw. The flat styling props all
     * write a field that a paired getter reads back, so the pair is asserted here: a
     * setter that swallows its argument, or writes the wrong field, fails this even
     * though the mechanical sweep above would call it covered.
     *
     * Props with no paired getter (the option visibilities forward to the ViewModel and
     * keep nothing) are counted and printed rather than asserted -- there is nothing on
     * the component to read.
     */
    @Test
    fun styleSettersRoundTripThroughTheirGetters() {
        registerHandRegisteredValues()
        val setters = ViewPropSweep.setters(CometChatMessageComposer::class.java)
            .filterNot { isFunctionalPending(it.paramType) }
            .filter { it.supported }

        val roundTripped = mutableListOf<String>()
        val notReadable = mutableListOf<String>()

        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            val composer = attachedComposer(activity)
            setters.forEach { setter ->
                val getter = runCatching {
                    CometChatMessageComposer::class.java.getDeclaredMethod("get${setter.propName}")
                }.getOrNull()

                if (getter == null || getter.returnType != setter.paramType) {
                    notReadable += setter.propName
                    return@forEach
                }

                val expected = NonDefaultValues.forProp(OWNER, setter.propName, setter.paramType)
                setter.applyNonDefault(composer)
                assertEquals(
                    "set${setter.propName} should be readable through get${setter.propName}",
                    expected,
                    getter.invoke(composer),
                )
                roundTripped += setter.propName
            }
        }
        scenario.close()

        println("  [composer view round-trip] ${roundTripped.size} readable; ${notReadable.size} write-only $notReadable")
        assertTrue(
            "the flat styling surface should be readable back; only ${roundTripped.size} were",
            roundTripped.size > 40,
        )
    }
}
