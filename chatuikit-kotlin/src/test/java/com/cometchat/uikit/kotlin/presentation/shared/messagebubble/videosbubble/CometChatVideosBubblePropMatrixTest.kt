package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.videosbubble

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.Attachment
import com.cometchat.chat.models.MediaMessage
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.shared.messagebubble.imagebubble.CometChatImageBubbleStyle
import com.cometchat.uikit.kotlin.presentation.shared.messagebubble.imagesbubble.CometChatImagesBubble
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.NonDefaultValues
import com.cometchat.uikit.propmatrix.Prop
import com.cometchat.uikit.propmatrix.PropKind
import com.cometchat.uikit.propmatrix.ViewPropSweep
import com.google.android.material.card.MaterialCardView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Property (prop-matrix) layer for the **View** [CometChatVideosBubble] — the port of
 * `CometChatVideosBubbleComposePropMatrixTest` to the View toolkit.
 *
 * The bubble is a subclass that overrides only `decorateTile`, so its whole integrator
 * surface is inherited from [CometChatImagesBubble]. That matters for how this file is
 * built: `ViewPropSweep` reflects over *declared* methods, so sweeping the subclass
 * directly reports an empty surface and a vacuous 100%. The sweep below therefore walks
 * the superclass chain up to `LinearLayout` and applies every prop **to a videos
 * instance** — which is the thing the sibling images matrix cannot check, since it only
 * ever proves the setters work on the parent.
 *
 * The mechanical sweep can only drive one of them (`statusInfoVisible`); the rest carry
 * a message, a style or a lambda. Those are measured for real in the observable matrix,
 * against a seven-attachment message — the only shape in which both `onMediaClickListener`
 * and `onMoreClickListener` are reachable, matching the Compose matrix.
 *
 * Two entries read differently from the Compose twin, and deliberately so:
 *
 *  - `style` is measured through `imageCornerRadius`, not a play-badge colour. The View
 *    bubble hardcodes its badge scrim (`0x73000000`) where Compose exposes
 *    `playBadgeBackgroundColor`, so there is no video-specific styling to assert here.
 *  - There is no `alignment`, `caption` or `onLongClick` entry: the View bubble folds
 *    alignment into `setTextFormatters`, takes the caption off the message, and forwards
 *    the long press to the row through `Utils.performAdapterClick`.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatVideosBubblePropMatrixTest {

    @After fun tearDown() = NonDefaultValues.clearRegistered()

    private companion object {
        const val OWNER = "CometChatVideosBubble"
        const val VIDEO_MIME = "video/mp4"
        const val VIDEO_EXT = "mp4"
        const val ATTACHMENT_COUNT = 7
        const val OVERFLOW_BADGE = "+3"
        const val CAPTION = "both takes"
        const val CORNER_RADIUS = 13f
        const val CAPTION_COLOR = 0xFFFF00FF.toInt()

        /** The grid draws at most this many tiles; the rest become the "+N" badge. */
        const val MAX_VISIBLE = 4
    }

    /** Params the mechanical sweep cannot meaningfully drive — reported, not counted. */
    private fun isFunctionalPending(paramType: Class<*>): Boolean =
        View::class.java.isAssignableFrom(paramType) ||
            android.graphics.drawable.Drawable::class.java.isAssignableFrom(paramType) ||
            Function::class.java.isAssignableFrom(paramType) ||
            List::class.java.isAssignableFrom(paramType) ||
            paramType.isInterface ||
            paramType.simpleName == "MediaMessage" ||
            paramType.simpleName.endsWith("Style") ||
            paramType.simpleName.endsWith("Alignment")

    /**
     * The component's own surface *plus* everything it inherits short of the framework —
     * `ViewPropSweep.setters` stops at `declaredMethods`, which reads as empty for a
     * subclass that adds no setters of its own.
     */
    private fun inheritedSetters(): List<com.cometchat.uikit.propmatrix.ViewSetter> {
        val out = mutableListOf<com.cometchat.uikit.propmatrix.ViewSetter>()
        var clazz: Class<*>? = CometChatVideosBubble::class.java
        while (clazz != null && clazz != LinearLayout::class.java) {
            out += ViewPropSweep.setters(clazz)
            clazz = clazz.superclass
        }
        return out.distinctBy { it.method.name + "/" + it.paramType.name }
    }

    private fun videos(count: Int, caption: String? = null): MediaMessage =
        MockFactory.createMediaMessage(
            count = count,
            type = CometChatConstants.MESSAGE_TYPE_VIDEO,
            mimeType = VIDEO_MIME,
            extension = VIDEO_EXT,
            caption = caption,
        )

    private fun View.descendants(): List<View> {
        val out = mutableListOf<View>()
        fun walk(v: View) {
            out += v
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(this)
        return out
    }

    private fun View.tiles(): List<MaterialCardView> = descendants().filterIsInstance<MaterialCardView>()

    private fun View.visibleText(): String {
        val out = StringBuilder()
        fun walk(v: View) {
            if (v.visibility != View.VISIBLE) return
            if (v is TextView) out.append(v.text).append(' ')
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(this)
        return out.toString()
    }

    /** Caption TextViews live under the caption container, never under a tile. */
    private fun View.captionViews(): List<TextView> = descendants()
        .filterIsInstance<TextView>()
        .filter { it.parent !is MaterialCardView && !it.text.isNullOrEmpty() }

    private fun withBubble(block: (ComponentActivity, CometChatVideosBubble) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            block(activity, CometChatVideosBubble(activity))
        }
        scenario.close()
    }

    // ── the mechanical sweep ────────────────────────────────────────────────

    @Test
    fun viewVideosBubble_mechanicalValueMatrix_coversEveryInheritedValueProp() {
        val setters = inheritedSetters()
        assertTrue("expected an inherited setter surface, was ${setters.size}", setters.isNotEmpty())

        val props = mutableListOf<Prop>()
        val functionalPending = mutableListOf<String>()

        withBubble { _, bubble ->
            setters.forEach { setter ->
                when {
                    isFunctionalPending(setter.paramType) ->
                        functionalPending += "${setter.propName}(${setter.paramType.simpleName})"

                    setter.supported ->
                        props += Prop(
                            OWNER,
                            setter.propName,
                            PropKind.VALUE,
                            covered = runCatching { setter.applyNonDefault(bubble) }.isSuccess,
                        )

                    else -> functionalPending += "${setter.propName}(${setter.paramType.simpleName})"
                }
            }
        }

        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [videosbubble view VALUE] ${cov.covered}/${cov.total}; functional-pending=${functionalPending.size} $functionalPending")
        if (uncovered.isNotEmpty()) println("  [videosbubble view] NOT covered: $uncovered")

        assertEquals("every swept VALUE prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }

    /**
     * The sweep above is only worth its assertions while the surface stays inherited. A
     * setter added to the subclass would be swept by the loop but would also mean the
     * bubble has grown a video-specific prop that deserves an observable entry below.
     */
    @Test
    fun theVideosBubbleStillDeclaresNoSetterSurfaceOfItsOwn() {
        assertTrue(
            "the videos bubble should subclass the images bubble",
            CometChatImagesBubble::class.java.isAssignableFrom(CometChatVideosBubble::class.java),
        )
        val declared = ViewPropSweep.setters(CometChatVideosBubble::class.java).map { it.propName }
        assertEquals(
            "the videos bubble gained setters; give each one an observable matrix entry. Found: $declared",
            emptyList<String>(),
            declared,
        )
    }

    // ── the observable matrix ───────────────────────────────────────────────

    /**
     * Every prop the mechanical sweep hands to the functional column, measured on the
     * videos bubble for real. A prop counts as covered when its exercise runs without
     * throwing, which is the same contract the Compose matrices use.
     */
    @Test
    fun viewVideosBubble_observableMatrix_coversEveryFunctionalProp() {
        val props = mutableListOf<Prop>()

        fun exercise(name: String, kind: PropKind = PropKind.VALUE, block: () -> Unit) {
            props += Prop(OWNER, name, kind, covered = runCatching(block).isSuccess)
        }

        exercise("Message") {
            withBubble { _, bubble ->
                bubble.setMessage(videos(ATTACHMENT_COUNT))
                assertEquals(
                    "seven videos cap at four tiles",
                    MAX_VISIBLE,
                    bubble.tiles().size,
                )
                assertTrue(
                    "expected an overflow badge, got '${bubble.visibleText()}'",
                    bubble.visibleText().contains(OVERFLOW_BADGE),
                )
            }
        }

        exercise("Style") {
            withBubble { activity, bubble ->
                // The tile reads the style when it is built, so the style has to land first.
                bubble.setStyle(
                    CometChatImageBubbleStyle.incoming(activity).copy(
                        imageCornerRadius = CORNER_RADIUS,
                        captionTextColor = CAPTION_COLOR,
                    )
                )
                bubble.setMessage(videos(ATTACHMENT_COUNT, caption = CAPTION))

                bubble.tiles().forEach {
                    assertEquals("every tile should take the styled radius", CORNER_RADIUS, it.radius, 0f)
                }
                assertTrue(
                    "the caption should take the styled colour",
                    bubble.captionViews().any { it.currentTextColor == CAPTION_COLOR },
                )
                assertTrue(
                    "incoming() and outgoing() should resolve differently",
                    CometChatImageBubbleStyle.incoming(activity) !=
                        CometChatImageBubbleStyle.outgoing(activity),
                )
            }
        }

        exercise("StatusInfoVisible") {
            withBubble { _, bubble ->
                bubble.setMessage(videos(2))
                val padded = bubble.paddingBottom
                bubble.setStatusInfoVisible(false)
                assertTrue(
                    "hiding the timestamp row should pad the bubble instead",
                    bubble.paddingBottom > padded,
                )
            }
        }

        exercise("TextFormatters") {
            withBubble { _, bubble ->
                // The formatter pipeline itself is the text bubble's to prove; what this
                // bubble owes is that the caption still renders once formatters are wired.
                bubble.setTextFormatters(emptyList(), UIKitConstants.MessageBubbleAlignment.RIGHT)
                bubble.setMessage(videos(2, caption = CAPTION))
                assertTrue(bubble.visibleText().contains(CAPTION))
            }
        }

        exercise("OnMediaClickListener", PropKind.CALLBACK) {
            withBubble { _, bubble ->
                var index: Int? = null
                var attachment: Attachment? = null
                bubble.setOnMediaClickListener { i, a -> index = i; attachment = a }
                bubble.setMessage(videos(ATTACHMENT_COUNT))

                bubble.tiles()[1].performClick()

                assertEquals("the second tile reports index 1", 1, index)
                assertEquals("media_2.$VIDEO_EXT", attachment?.fileName)
            }
        }

        exercise("OnMoreClickListener", PropKind.CALLBACK) {
            withBubble { _, bubble ->
                var mediaIndex: Int? = null
                var all: List<Attachment>? = null
                bubble.setOnMediaClickListener { i, _ -> mediaIndex = i }
                bubble.setOnMoreClickListener { all = it }
                bubble.setMessage(videos(ATTACHMENT_COUNT))

                bubble.tiles()[MAX_VISIBLE - 1].performClick()

                assertEquals(
                    "the overflow tile hands back every attachment",
                    ATTACHMENT_COUNT,
                    all?.size,
                )
                assertNull("and must not also fire the ordinary media callback", mediaIndex)
            }
        }

        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [videosbubble view observable] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [videosbubble view] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }

    /**
     * The two halves have to add up to the whole surface — otherwise a prop can be dropped
     * from the sweep's functional column and quietly never picked up by the observable one.
     */
    @Test
    fun theTwoHalvesBetweenThemCoverEveryInheritedSetter() {
        val swept = inheritedSetters().map { it.propName }.toSet()
        val observed = setOf(
            "Message",
            "Style",
            "StatusInfoVisible",
            "OnMediaClickListener",
            "OnMoreClickListener",
        )
        assertEquals(
            "an inherited setter is in neither half of this matrix",
            emptySet<String>(),
            swept - observed,
        )
        // `setTextFormatters` takes two params, so the single-arg sweep never sees it —
        // the observable half carries it alone.
        assertTrue("TextFormatters is not a swept single-arg setter", "TextFormatters" !in swept)
    }
}
