package com.cometchat.chatuikit.shimmer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cometchat.chatuikit.R;

public class CometChatShimmerFrameLayout extends FrameLayout {
    private static final String TAG = CometChatShimmerFrameLayout.class.getSimpleName();


    private final Paint mContentPaint = new Paint();
    private final CometChatShimmerDrawable mShimmerDrawable = new CometChatShimmerDrawable();

    private boolean mShowShimmer = true;
    private boolean mStoppedShimmerBecauseVisibility = false;

    public CometChatShimmerFrameLayout(Context context) {
        super(context);
        init(context, null);
    }

    public CometChatShimmerFrameLayout(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CometChatShimmerFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public CometChatShimmerFrameLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        setWillNotDraw(false);
        mShimmerDrawable.setCallback(this);

        if (attrs == null) {
            setShimmer(new CometChatShimmer.AlphaHighlightBuilder().build());
            return;
        }

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CometChatShimmerFrameLayout, 0, 0);
        try {
            CometChatShimmer.Builder shimmerBuilder = a.hasValue(R.styleable.CometChatShimmerFrameLayout_cometchatShimmerColored) && a.getBoolean(R.styleable.CometChatShimmerFrameLayout_cometchatShimmerColored, false) ? new CometChatShimmer.ColorHighlightBuilder() : new CometChatShimmer.AlphaHighlightBuilder();
            setShimmer(shimmerBuilder.consumeAttributes(a).build());
        } finally {
            a.recycle();
        }
    }

    public CometChatShimmerFrameLayout setShimmer(@Nullable CometChatShimmer shimmer) {
        mShimmerDrawable.setShimmer(shimmer);
        if (shimmer != null && shimmer.clipToChildren) {
            setLayerType(LAYER_TYPE_HARDWARE, mContentPaint);
        } else {
            setLayerType(LAYER_TYPE_NONE, null);
        }
        return this;
    }

    public @Nullable CometChatShimmer getShimmer() {
        return mShimmerDrawable.getShimmer();
    }

    public void startShimmer() {
        if (isAttachedToWindow()) {
            mShimmerDrawable.setShimmer(CometChatShimmerUtils.getCometChatShimmerConfig(getContext()));
            mShimmerDrawable.startShimmer();
        }
    }

    public void stopShimmer() {
        mStoppedShimmerBecauseVisibility = false;
        mShimmerDrawable.stopShimmer();
    }

    public boolean isShimmerStarted() {
        return mShimmerDrawable.isShimmerStarted();
    }

    public void showShimmer(boolean startShimmer) {
        mShowShimmer = true;
        if (startShimmer) {
            startShimmer();
        }
        invalidate();
    }

    public void hideShimmer() {
        stopShimmer();
        mShowShimmer = false;
        invalidate();
    }

    public boolean isShimmerVisible() {
        return mShowShimmer;
    }

    public boolean isShimmerRunning() {
        return mShimmerDrawable.isShimmerRunning();
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        final int width = getWidth();
        final int height = getHeight();
        mShimmerDrawable.setBounds(0, 0, width, height);
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        // View's constructor directly invokes this method, in which case no fields on
        // this class have been fully initialized yet.
        if (mShimmerDrawable == null) {
            return;
        }
        if (visibility != View.VISIBLE) {
            // GONE or INVISIBLE
            if (isShimmerStarted()) {
                stopShimmer();
                mStoppedShimmerBecauseVisibility = true;
            }
        } else if (mStoppedShimmerBecauseVisibility) {
            mShimmerDrawable.maybeStartShimmer();
            mStoppedShimmerBecauseVisibility = false;
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mShimmerDrawable.maybeStartShimmer();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopShimmer();
    }

    @Override
    public void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mShowShimmer) {
            mShimmerDrawable.draw(canvas);
        }
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return super.verifyDrawable(who) || who == mShimmerDrawable;
    }

    public void setStaticAnimationProgress(float value) {
        mShimmerDrawable.setStaticAnimationProgress(value);
    }

    public void clearStaticAnimationProgress() {
        mShimmerDrawable.clearStaticAnimationProgress();
    }

    public void setCustomLayout(int simmerLayoutDesign) {
        removeAllViews();
        View view = inflate(getContext(), simmerLayoutDesign, null);
        addView(view);
    }
}
