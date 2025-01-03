package com.cometchat.chatuikit.shimmer;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class CometChatShimmerDrawable extends Drawable {
    private static final String TAG = CometChatShimmerDrawable.class.getSimpleName();
    private final ValueAnimator.AnimatorUpdateListener mUpdateListener = animation -> invalidateSelf();

    private final Paint mShimmerPaint = new Paint();
    private final Rect mDrawRect = new Rect();
    private final Matrix mShaderMatrix = new Matrix();

    private @Nullable ValueAnimator mValueAnimator;
    private float mStaticAnimationProgress = -1f;

    private @Nullable CometChatShimmer mShimmer;

    public CometChatShimmerDrawable() {
        mShimmerPaint.setAntiAlias(true);
    }

    public void setShimmer(@Nullable CometChatShimmer shimmer) {
        mShimmer = shimmer;
        if (mShimmer != null) {
            mShimmerPaint.setXfermode(new PorterDuffXfermode(mShimmer.alphaShimmer ? PorterDuff.Mode.DST_IN : PorterDuff.Mode.SRC_IN));
        }
        updateShader();
        updateValueAnimator();
        invalidateSelf();
    }

    public @Nullable CometChatShimmer getShimmer() {
        return mShimmer;
    }

    public void startShimmer() {
        if (mValueAnimator != null && !isShimmerStarted() && getCallback() != null) {
            mValueAnimator.start();
        }
    }

    public void stopShimmer() {
        if (mValueAnimator != null && isShimmerStarted()) {
            mValueAnimator.cancel();
        }
    }

    public boolean isShimmerStarted() {
        return mValueAnimator != null && mValueAnimator.isStarted();
    }

    public boolean isShimmerRunning() {
        return mValueAnimator != null && mValueAnimator.isRunning();
    }

    @Override
    public void onBoundsChange(@NonNull Rect bounds) {
        super.onBoundsChange(bounds);
        mDrawRect.set(bounds);
        updateShader();
        maybeStartShimmer();
    }

    public void setStaticAnimationProgress(float value) {
        if (Float.compare(value, mStaticAnimationProgress) == 0 || (value < 0f && mStaticAnimationProgress < 0f)) {
            return;
        }
        mStaticAnimationProgress = Math.min(value, 1f);
        invalidateSelf();
    }

    public void clearStaticAnimationProgress() {
        setStaticAnimationProgress(-1f);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mShimmer == null || mShimmerPaint.getShader() == null) {
            return;
        }

        final float tiltTan = (float) Math.tan(Math.toRadians(mShimmer.tilt));
        final float translateHeight = mDrawRect.height() + tiltTan * mDrawRect.width();
        final float translateWidth = mDrawRect.width() + tiltTan * mDrawRect.height();
        final float dx;
        final float dy;
        final float animatedValue;

        if (mStaticAnimationProgress < 0f) {
            animatedValue = mValueAnimator != null ? (float) mValueAnimator.getAnimatedValue() : 0f;
        } else {
            animatedValue = mStaticAnimationProgress;
        }

        switch (mShimmer.direction) {
            default:
            case CometChatShimmer.Direction.LEFT_TO_RIGHT:
                dx = offset(-translateWidth, translateWidth, animatedValue);
                dy = 0;
                break;
            case CometChatShimmer.Direction.RIGHT_TO_LEFT:
                dx = offset(translateWidth, -translateWidth, animatedValue);
                dy = 0f;
                break;
            case CometChatShimmer.Direction.TOP_TO_BOTTOM:
                dx = 0f;
                dy = offset(-translateHeight, translateHeight, animatedValue);
                break;
            case CometChatShimmer.Direction.BOTTOM_TO_TOP:
                dx = 0f;
                dy = offset(translateHeight, -translateHeight, animatedValue);
                break;
        }

        mShaderMatrix.reset();
        if (mShimmer != null) {
            mShaderMatrix.setRotate(mShimmer.tilt, mDrawRect.width() / 2f, mDrawRect.height() / 2f);
        }
        mShaderMatrix.preTranslate(dx, dy);
        mShimmerPaint.getShader().setLocalMatrix(mShaderMatrix);
        canvas.drawRect(mDrawRect, mShimmerPaint);
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return mShimmer != null && (mShimmer.clipToChildren || mShimmer.alphaShimmer) ? PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE;
    }

    private float offset(float start, float end, float percent) {
        return start + (end - start) * percent;
    }

    private void updateValueAnimator() {
        if (mShimmer == null) {
            return;
        }

        final boolean started;
        if (mValueAnimator != null) {
            started = mValueAnimator.isStarted();
            mValueAnimator.cancel();
            mValueAnimator.removeAllUpdateListeners();
        } else {
            started = false;
        }

        mValueAnimator = ValueAnimator.ofFloat(0f, 1f + (float) (mShimmer.repeatDelay / mShimmer.animationDuration));
        mValueAnimator.setInterpolator(new LinearInterpolator());
        mValueAnimator.setRepeatMode(mShimmer.repeatMode);
        mValueAnimator.setStartDelay(mShimmer.startDelay);
        mValueAnimator.setRepeatCount(mShimmer.repeatCount);
        mValueAnimator.setDuration(mShimmer.animationDuration + mShimmer.repeatDelay);
        mValueAnimator.addUpdateListener(mUpdateListener);
        if (started) {
            mValueAnimator.start();
        }
    }

    void maybeStartShimmer() {
        if (mValueAnimator != null && !mValueAnimator.isStarted() && mShimmer != null && mShimmer.autoStart && getCallback() != null) {
            mValueAnimator.start();
        }
    }

    private void updateShader() {
        final Rect bounds = getBounds();
        final int boundsWidth = bounds.width();
        final int boundsHeight = bounds.height();
        if (boundsWidth == 0 || boundsHeight == 0 || mShimmer == null) {
            return;
        }
        final int width = mShimmer.width(boundsWidth);
        final int height = mShimmer.height(boundsHeight);

        final Shader shader;
        switch (mShimmer.shape) {
            default:
            case CometChatShimmer.Shape.LINEAR:
                boolean vertical = mShimmer.direction == CometChatShimmer.Direction.TOP_TO_BOTTOM || mShimmer.direction == CometChatShimmer.Direction.BOTTOM_TO_TOP;
                int endX = vertical ? 0 : width;
                int endY = vertical ? height : 0;
                shader = new LinearGradient(0, 0, endX, endY, mShimmer.colors, mShimmer.positions, Shader.TileMode.CLAMP);
                break;
            case CometChatShimmer.Shape.RADIAL:
                shader = new RadialGradient(width / 2f, height / 2f, (float) (Math.max(width, height) / Math.sqrt(2)), mShimmer.colors, mShimmer.positions, Shader.TileMode.CLAMP);
                break;
        }

        mShimmerPaint.setShader(shader);
    }
}
