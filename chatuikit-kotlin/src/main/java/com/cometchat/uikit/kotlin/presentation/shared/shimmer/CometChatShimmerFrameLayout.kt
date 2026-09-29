package com.cometchat.uikit.kotlin.presentation.shared.shimmer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import com.cometchat.uikit.kotlin.R

/**
 * CometChatShimmerFrameLayout is a FrameLayout that displays a shimmer effect
 * over its children. It's used to show loading placeholders.
 */
public class CometChatShimmerFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    public companion object {
        private val TAG = CometChatShimmerFrameLayout::class.java.simpleName
    }

    private val contentPaint = Paint()
    private val shimmerDrawable = CometChatShimmerDrawable()

    private var showShimmer = true
    private var stoppedShimmerBecauseVisibility = false

    init {
        setWillNotDraw(false)
        shimmerDrawable.callback = this

        if (attrs == null) {
            setShimmer(CometChatShimmer.AlphaHighlightBuilder().build())
        } else {
            val a = context.obtainStyledAttributes(attrs, R.styleable.CometChatShimmerFrameLayout, 0, 0)
            try {
                val shimmerBuilder = if (a.hasValue(R.styleable.CometChatShimmerFrameLayout_cometchatShimmerColored) &&
                    a.getBoolean(R.styleable.CometChatShimmerFrameLayout_cometchatShimmerColored, false)
                ) {
                    CometChatShimmer.ColorHighlightBuilder()
                } else {
                    CometChatShimmer.AlphaHighlightBuilder()
                }
                setShimmer(shimmerBuilder.consumeAttributes(a).build())
            } finally {
                a.recycle()
            }
        }
    }

    public fun setShimmer(shimmer: CometChatShimmer?): CometChatShimmerFrameLayout {
        shimmerDrawable.setShimmer(shimmer)
        if (shimmer != null && shimmer.clipToChildren) {
            setLayerType(LAYER_TYPE_HARDWARE, contentPaint)
        } else {
            setLayerType(LAYER_TYPE_NONE, null)
        }
        return this
    }

    public fun getShimmer(): CometChatShimmer? = shimmerDrawable.shimmer

    public fun startShimmer() {
        if (isAttachedToWindow) {
            shimmerDrawable.setShimmer(CometChatShimmerUtils.getCometChatShimmerConfig(context))
            shimmerDrawable.startShimmer()
        }
    }

    public fun stopShimmer() {
        stoppedShimmerBecauseVisibility = false
        shimmerDrawable.stopShimmer()
    }

    public fun isShimmerStarted(): Boolean = shimmerDrawable.isShimmerStarted()

    public fun showShimmer(startShimmer: Boolean) {
        showShimmer = true
        if (startShimmer) {
            startShimmer()
        }
        invalidate()
    }

    public fun hideShimmer() {
        stopShimmer()
        showShimmer = false
        invalidate()
    }

    public fun isShimmerVisible(): Boolean = showShimmer

    public fun isShimmerRunning(): Boolean = shimmerDrawable.isShimmerRunning()

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        shimmerDrawable.setBounds(0, 0, width, height)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility != View.VISIBLE) {
            if (isShimmerStarted()) {
                stopShimmer()
                stoppedShimmerBecauseVisibility = true
            }
        } else if (stoppedShimmerBecauseVisibility) {
            shimmerDrawable.maybeStartShimmer()
            stoppedShimmerBecauseVisibility = false
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        shimmerDrawable.maybeStartShimmer()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopShimmer()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (showShimmer) {
            shimmerDrawable.draw(canvas)
        }
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return super.verifyDrawable(who) || who == shimmerDrawable
    }

    public fun setStaticAnimationProgress(value: Float) {
        shimmerDrawable.setStaticAnimationProgress(value)
    }

    public fun clearStaticAnimationProgress() {
        shimmerDrawable.clearStaticAnimationProgress()
    }

    public fun setCustomLayout(@LayoutRes shimmerLayoutDesign: Int) {
        removeAllViews()
        val view = inflate(context, shimmerLayoutDesign, null)
        addView(view)
    }
}
