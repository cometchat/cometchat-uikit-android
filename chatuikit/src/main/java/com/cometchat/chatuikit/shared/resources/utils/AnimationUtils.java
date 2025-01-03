package com.cometchat.chatuikit.shared.resources.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;

public class AnimationUtils {
    private static final String TAG = AnimationUtils.class.getSimpleName();


    public static void animateVisibilityVisible(final View view) {
        // restores it height to wrap content as it was set to 0 after visibility is
        // gone
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        view.setLayoutParams(params);
        // Make the view visible before starting the animation
        view.setVisibility(View.VISIBLE);

        // Use ViewTreeObserver to ensure the view is measured
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Remove the listener to prevent repeated calls
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                // Get the final height of the view after it has been laid out
                final int finalHeight = view.getMeasuredHeight();

                // Set the height to 0 for the animation
                ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                layoutParams.height = 0;
                view.setLayoutParams(layoutParams);

                // Create an animation to increase the height from 0 to finalHeight
                ValueAnimator animator = ValueAnimator.ofInt(0, finalHeight);
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                        // Update the height of the view
                        layoutParams.height = (int) valueAnimator.getAnimatedValue();
                        view.setLayoutParams(layoutParams);
                    }
                });

                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // Optional: Do something after the animation ends
                    }
                });

                // Set the duration of the animation and start it
                animator.setDuration(300); // Adjust the duration as needed
                animator.start();
            }
        });
    }

    public static void animateVisibilityGone(final View view) {
        // Set the initial height to the current height
        int initialHeight = view.getHeight();

        // Create an animation to reduce the height to zero
        ValueAnimator animator = ValueAnimator.ofInt(initialHeight, 0);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                // Update the height of the view
                int animatedValue = (int) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                layoutParams.height = animatedValue;
                view.setLayoutParams(layoutParams);
            }
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Set the view visibility to GONE after the animation ends
                view.setVisibility(View.GONE);
            }
        });

        // Set the duration of the animation and start it
        animator.setDuration(300); // You can adjust the duration
        animator.start();
    }
}
