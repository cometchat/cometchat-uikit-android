package com.cometchat.uikit.kotlin.presentation.shared.mediaviewer

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * Functional layer for [CometChatImagePreview] and [CometChatImagePreviewUtils].
 *
 * Both were entirely untested. Despite the name, this is not a view — it is the gesture
 * handler behind the full-screen image viewer: an `OnTouchListener` + `OnLayoutChangeListener`
 * that owns pinch-zoom, double-tap-zoom, pan, and drag/fling-to-dismiss. Its Compose
 * namesake is a different thing altogether (an internal composable), so there is no shared
 * spec across the toolkits here and no point pretending there is one.
 *
 * **What these tests deliberately do and don't cover.** Driving the physics — flings,
 * momentum, animator-driven dismissal — through synthetic `MotionEvent`s under Robolectric
 * would assert the gesture detectors and the animation framework far more than it asserts
 * this class, and would be flaky about it. So the assertions here are on the parts that are
 * deterministic and genuinely this component's own:
 *
 * - what construction does to the views handed to it, which is more than it looks;
 * - the guard conditions in `onTouch` that decide whether a gesture is considered at all;
 * - `setupLayout`'s refusal to prepare when there is nothing to measure;
 * - the two `setDragToDismissDistance` overloads, which take **different units**;
 * - the `CometChatImagePreviewUtils` adapters, which are pure delegation.
 *
 * The dismiss threshold is read back by reflection. That is deliberate: the two overloads
 * are `(Float)` as a *fraction of view height* and `(Int)` as *dp*, they are one character
 * apart at a call site, and confusing them silently changes how far a user must drag. The
 * alternative — proving it through a full drag gesture — would bury that arithmetic under
 * exactly the physics this file avoids.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatImagePreviewFunctionalTest {

    private class Harness(
        val preview: CometChatImagePreview,
        val imageView: ImageView,
        val container: ViewGroup,
        val activity: ComponentActivity,
    )

    /**
     * A laid-out ImageView inside a container that itself has a parent — `onTouch` calls
     * `container.parent.requestDisallowInterceptTouchEvent`, so an unattached container
     * would fail before reaching anything worth testing.
     */
    private fun withHarness(
        withDrawable: Boolean = true,
        block: (Harness) -> Unit,
    ) {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setTheme(R.style.CometChatTheme_DayNight)

                val container = FrameLayout(activity)
                val imageView = ImageView(activity)
                container.addView(imageView)
                activity.setContentView(container)

                if (withDrawable) imageView.setImageDrawable(ColorDrawable(Color.RED))
                ShadowLooper.idleMainLooper()

                val preview = CometChatImagePreviewUtils.createImagePreview(imageView, container)
                block(Harness(preview, imageView, container, activity))
            }
        }
    }

    /** See the class comment: the two overloads differ in units, which is the point. */
    private fun CometChatImagePreview.dismissThreshold(): Float {
        val field = CometChatImagePreview::class.java.getDeclaredField("dragToDismissThreshold")
        field.isAccessible = true
        return field.getFloat(this)
    }

    /** `isReadyToDraw` is the only outward sign that setupLayout accepted its input. */
    private fun CometChatImagePreview.isReady(): Boolean {
        val field = CometChatImagePreview::class.java.getDeclaredField("isReadyToDraw")
        field.isAccessible = true
        return field.getBoolean(this)
    }

    private fun down(x: Float = 10f, y: Float = 10f): MotionEvent =
        MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, x, y, 0)

    // ── construction is not passive ─────────────────────────────────────────

    @Test
    fun creatingAPreviewReturnsAnInstance() {
        withHarness { h -> assertNotNull(h.preview) }
    }

    @Test
    fun constructionTakesOverTheImageViewsScaleType() {
        // The whole class drives the image through an imageMatrix, which only has any
        // effect under ScaleType.MATRIX. An integrator setting FIT_CENTER beforehand is
        // overridden rather than respected.
        withHarness { h ->
            assertEquals(ImageView.ScaleType.MATRIX, h.imageView.scaleType)
        }
    }

    @Test
    fun constructionResetsAnyVerticalOffsetOnTheImageView() {
        // Re-entering the viewer after a partial drag would otherwise inherit the old
        // translation.
        withHarness { h ->
            h.imageView.y = 120f
            val second = CometChatImagePreviewUtils.createImagePreview(h.imageView, h.container)
            assertNotNull(second)
            assertEquals(0f, h.imageView.y, 0.001f)
        }
    }

    @Test
    fun constructionBlackensTheHostWindow() {
        // A side effect on the *activity*, not on either view passed in — surprising
        // enough to be worth pinning, since it outlives the viewer if it is not undone.
        withHarness { h ->
            val background = h.activity.window.decorView.background
            assertTrue(
                "expected a colour drawable, was ${background?.javaClass?.simpleName}",
                background is ColorDrawable,
            )
            assertEquals(Color.BLACK, (background as ColorDrawable).color)
        }
    }

    @Test
    fun constructionAttachesItselfToTheContainer() {
        // The container, not the image view, is the touch target: the drag area is the
        // whole screen rather than the image's bounds.
        withHarness { h ->
            assertTrue(
                "the container should have become touch-responsive",
                h.container.hasOnClickListeners() || h.container.dispatchTouchEvent(down()),
            )
        }
    }

    // ── the guards in onTouch ───────────────────────────────────────────────

    @Test
    fun aDisabledImageViewSuppressesGesturesEntirely() {
        // The image view carries the enabled flag even though the container receives the
        // touches, so this is the one switch an integrator has to turn the viewer inert.
        withHarness { h ->
            h.imageView.isEnabled = false
            assertFalse(h.preview.onTouch(h.container, down()))
        }
    }

    @Test
    fun anEnabledImageViewConsumesTheGesture() {
        withHarness { h ->
            h.imageView.isEnabled = true
            assertTrue(h.preview.onTouch(h.container, down()))
        }
    }

    // ── setupLayout refuses to prepare on nothing ───────────────────────────

    @Test
    fun layoutWithoutADrawableLeavesTheViewerUnprepared() {
        // The viewer lays out before Coil has delivered anything, so this is the ordinary
        // first call rather than an edge case. It must not arm itself on nothing: with no
        // drawable there are no intrinsic dimensions to build a scale range from.
        withHarness(withDrawable = false) { h ->
            h.preview.setupLayout(0, 0, h.imageView.width, h.imageView.height)
            assertFalse(
                "no drawable means nothing to measure, so it should not be ready to draw",
                h.preview.isReady(),
            )
        }
    }

    @Test
    fun layoutWithADrawableArmsTheViewer() {
        // The other half of the pair — without it the assertion above would also pass on a
        // component that never becomes ready at all.
        withHarness(withDrawable = true) { h ->
            h.preview.setupLayout(0, 0, h.imageView.width, h.imageView.height)
            assertTrue("a measured view with a drawable should be ready", h.preview.isReady())
        }
    }

    // ── the two dismiss-distance overloads take different units ─────────────

    @Test
    fun theRatioOverloadIsAFractionOfTheImageViewHeight() {
        // Measured against the height the host actually gave the view: the activity's own
        // layout pass overrides anything set by hand, so a hardcoded expectation here
        // would be asserting the test harness rather than the component.
        withHarness { h ->
            h.preview.setDragToDismissDistance(0.5f)
            assertEquals(h.imageView.height * 0.5f, h.preview.dismissThreshold(), 0.001f)
        }
    }

    @Test
    fun aRatioOfOneMeansTheFullHeight() {
        withHarness { h ->
            h.preview.setDragToDismissDistance(1.0f)
            assertEquals(h.imageView.height.toFloat(), h.preview.dismissThreshold(), 0.001f)
        }
    }

    @Test
    fun theIntOverloadIsDpAndNotAFraction() {
        // The trap: setDragToDismissDistance(1) is one dp, not the whole height, while
        // setDragToDismissDistance(1f) is the whole height. Same name, same call site
        // shape, wildly different results.
        withHarness { h ->
            val density = h.imageView.context.resources.displayMetrics.density
            h.preview.setDragToDismissDistance(96)
            assertEquals(96f * density, h.preview.dismissThreshold(), 0.001f)
        }
    }

    @Test
    fun theTwoOverloadsDisagreeForTheSameNumber() {
        // Stated as a test because the compiler will happily pick either.
        withHarness { h ->
            h.preview.setDragToDismissDistance(1.0f)
            val asRatio = h.preview.dismissThreshold()
            h.preview.setDragToDismissDistance(1)
            val asDp = h.preview.dismissThreshold()

            assertTrue(
                "a ratio of 1 should dwarf a distance of 1dp, was $asRatio vs $asDp",
                asRatio > asDp,
            )
        }
    }

    // ── the Utils adapters are pure delegation ──────────────────────────────

    @Test
    fun theUtilsFactoryProducesAWorkingPreview() {
        withHarness { h ->
            assertEquals(ImageView.ScaleType.MATRIX, h.imageView.scaleType)
            assertTrue(h.preview.onTouch(h.container, down()))
        }
    }

    @Test
    fun theTranslateAdapterForwardsEveryCallbackToTheCallersListener() {
        // The adapter re-declares all four methods by hand, so a missed one would be a
        // silent no-op for that event only.
        withHarness { h ->
            val seen = mutableListOf<String>()
            CometChatImagePreviewUtils.setOnViewTranslateListener(
                h.preview,
                object : CometChatImagePreviewUtils.OnViewTranslateListener {
                    override fun onStart(view: ImageView) { seen += "start" }
                    override fun onViewTranslate(view: ImageView, amount: Float) { seen += "translate" }
                    override fun onRestore(view: ImageView) { seen += "restore" }
                    override fun onDismiss(view: ImageView) { seen += "dismiss" }
                },
            )

            val installed = installedTranslateListener(h.preview)
            assertNotNull("the adapter should have installed a listener", installed)
            installed!!.onStart(h.imageView)
            installed.onViewTranslate(h.imageView, 0.25f)
            installed.onRestore(h.imageView)
            installed.onDismiss(h.imageView)

            assertEquals(listOf("start", "translate", "restore", "dismiss"), seen)
        }
    }

    @Test
    fun theScaleAdapterForwardsItsArgumentsUnchanged() {
        withHarness { h ->
            var factor = 0f
            var fx = 0f
            var fy = 0f
            CometChatImagePreviewUtils.setOnScaleChangedListener(
                h.preview,
                object : CometChatImagePreviewUtils.OnScaleChangedListener {
                    override fun onScaleChange(scaleFactor: Float, focusX: Float, focusY: Float) {
                        factor = scaleFactor; fx = focusX; fy = focusY
                    }
                },
            )

            val installed = installedScaleListener(h.preview)
            assertNotNull(installed)
            installed!!.onScaleChange(2.5f, 10f, 20f)

            assertEquals(2.5f, factor, 0.001f)
            assertEquals(10f, fx, 0.001f)
            assertEquals(20f, fy, 0.001f)
        }
    }

    private fun installedTranslateListener(
        preview: CometChatImagePreview,
    ): CometChatImagePreview.OnViewTranslateListener? {
        val field = CometChatImagePreview::class.java.getDeclaredField("onViewTranslateListener")
        field.isAccessible = true
        return field.get(preview) as CometChatImagePreview.OnViewTranslateListener?
    }

    private fun installedScaleListener(
        preview: CometChatImagePreview,
    ): CometChatImagePreview.OnScaleChangedListener? {
        val field = CometChatImagePreview::class.java.getDeclaredField("onScaleChangedListener")
        field.isAccessible = true
        return field.get(preview) as CometChatImagePreview.OnScaleChangedListener?
    }
}
