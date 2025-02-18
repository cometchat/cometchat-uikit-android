package com.cometchat.chatuikit.shared.views.mediarecorder;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.google.android.material.card.MaterialCardView;

/**
 * A custom view that displays a circular ripple effect originating from a
 * central point. The ripples expand outward and can be animated to create a
 * visual effect.
 */
public class AudioCircleRippleView extends MaterialCardView {
    private static final String TAG = AudioCircleRippleView.class.getSimpleName();
    private static final int RIPPLE_COUNT = 3;
    private int[] rippleColors;
    private float radius;
    private float innerCircleRadius = 50f;

    private boolean isAnimating = false;

    private Paint paint;
    private ValueAnimator animator;

    private @ColorInt int color;

    /**
     * Constructs an AudioCircleRippleView with the specified context.
     *
     * @param context the context to use
     */
    public AudioCircleRippleView(Context context) {
        super(context);
        init();
    }

    /**
     * Initializes the view by setting up the paint object.
     */
    private void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
    }

    /**
     * Constructs an AudioCircleRippleView with the specified context and
     * attributes.
     *
     * @param context the context to use
     * @param attrs   the attribute set containing custom attributes
     */
    public AudioCircleRippleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Gets the color of the ripples.
     *
     * @return the ripple color as an integer
     */
    public @ColorInt int getColor() {
        return color;
    }

    /**
     * Sets the color of the ripples.
     *
     * @param color the ripple color as an integer
     */
    public void setColor(@ColorInt int color) {
        this.color = color;
        createRippleColors(color);
    }

    /**
     * Creates an array of ripple colors based on the specified color.
     *
     * @param color the base color to use for creating ripple colors
     */
    private void createRippleColors(@ColorInt int color) {
        rippleColors = new int[RIPPLE_COUNT];
        for (int i = 0; i < RIPPLE_COUNT; i++) {
            rippleColors[i] = Color.argb(i + 10, Color.red(color), Color.green(color), Color.blue(color));
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setupAnimator();
    }

    /**
     * Sets up the animator for creating the ripple effect.
     */
    private void setupAnimator() {
        innerCircleRadius = (float) (getMeasuredWidth() - (getMeasuredWidth() / 1.4));
        float maxRippleRadius = innerCircleRadius + 150; // Set max ripple radius to 100dp beyond the inner circle
        animator = ValueAnimator.ofFloat(innerCircleRadius, maxRippleRadius);
        animator.setDuration(1800); // Duration for one full ripple
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE); // Loop infinitely
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator animation) {
                radius = (float) animation.getAnimatedValue();
                invalidate(); // Redraw the view
            }
        });
    }

    /**
     * Starts the ripple animation.
     */
    public void startAnimation() {
        if (!isAnimating) {
            isAnimating = true;
            if (animator != null) new Handler().postDelayed(() -> animator.start(), 10);
        }
    }

    /**
     * Stops the ripple animation and resets the radius.
     */
    public void stopAnimation() {
        if (isAnimating) {
            isAnimating = false;
            animator.cancel();
            radius = innerCircleRadius; // Reset radius to the inner circle radius
            invalidate(); // Redraw to clear circles
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        // Draw the inner circle
        paint.setColor(color);
        canvas.drawCircle((float) getWidth() / 2, (float) getHeight() / 2, innerCircleRadius, paint);
        // Draw ripples
        if (isAnimating) {
            for (int i = 0; i < rippleColors.length; i++) {
                paint.setColor(rippleColors[i]);
                float currentRippleRadius = innerCircleRadius + (radius - innerCircleRadius) * (i + 3) / rippleColors.length; // Adjust the radius for each
                // layer
                canvas.drawCircle((float) getWidth() / 2, (float) getHeight() / 2, currentRippleRadius, paint);
            }
        }
    }
}
