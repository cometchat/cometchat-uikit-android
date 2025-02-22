package com.cometchat.chatuikit.shared.views.reaction;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.cometchat.chatuikit.CometChatTheme;
import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.databinding.CometchatReactionItemLayoutBinding;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.google.android.material.card.MaterialCardView;

/**
 * The CometChatReaction class is a custom view in Android that represents a
 * reaction, typically used for displaying a reaction emoji and the count of
 * reactions. It extends the MaterialCardView class and provides various
 * customization options such as setting the emoji, count, background color,
 * stroke color, stroke width, corner radius, and elevation. The reaction can be
 * set with an emoji and a count, and if no count is available, it can display
 * only the emoji. The class uses the MaterialCardView for displaying the
 * reaction and supports applying styles to the reaction using a style resource.
 * Created on: 13 September 2024 Modified on:
 */
@SuppressWarnings("unused")
public class CometChatReaction extends MaterialCardView {
    private static final String TAG = CometChatReaction.class.getSimpleName();


    private CometchatReactionItemLayoutBinding binding;

    private String emoji;
    private int count;
    private boolean isReactedByMe;
    private View.OnClickListener onClickListener;
    private View.OnLongClickListener onLongClickListener;

    private @StyleRes int reactionEmojiTextAppearance;
    private @ColorInt int reactionEmojiTextColor;
    private @StyleRes int reactionCountTextAppearance;
    private @ColorInt int reactionCountTextColor;
    private @ColorInt int reactionBackgroundColor;
    private @Dimension int reactionStrokeWidth;
    private @ColorInt int reactionStrokeColor;
    private @Dimension int reactionCornerRadius;
    private @Dimension int reactionElevation;
    private @Dimension int activeReactionBackgroundColor;
    private @Dimension int activeReactionStrokeWidth;
    private @ColorInt int activeReactionStrokeColor;

    /**
     * Constructor for creating a CometChatMessageReceipt without any attribute.
     *
     * @param context The context in which the CometChatMessageReceipt is created.
     */
    public CometChatReaction(Context context) {
        this(context, null);
    }

    /**
     * Constructor for creating a CometChatMessageReceipt with attributes.
     *
     * @param context The context in which the CometChatMessageReceipt is created.
     * @param attrs   The attribute set containing the custom attributes.
     */
    public CometChatReaction(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.cometchatReactionStyle);
    }

    /**
     * Constructor for creating a CometChatMessageReceipt with attributes and a
     * default style.
     *
     * @param context      The context in which the CometChatMessageReceipt is created.
     * @param attrs        The attribute set containing the custom attributes.
     * @param defStyleAttr The default style resource.
     */
    public CometChatReaction(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode()) {
            inflateAndInitializeView(attrs, defStyleAttr);
        }
    }

    /**
     * Inflates and initializes the view by setting up the layout, retrieving the
     * attributes, and applying styles.
     *
     * @param attrs        The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr The default style to apply to this view.
     */
    private void inflateAndInitializeView(AttributeSet attrs, int defStyleAttr) {
        // Reset the card view
        Utils.initMaterialCard(this);
        // Inflate the view
        binding = CometchatReactionItemLayoutBinding.inflate(LayoutInflater.from(getContext()), this, true);
        // Set the default values
        setDefaultValues();
        // Apply the style attributes
        applyStyleAttributes(attrs, defStyleAttr);
    }

    /**
     * Sets the default values for the CometChatMessageReceipt.
     */
    private void setDefaultValues() {
        reactionEmojiTextColor = CometChatTheme.getTextColorPrimary(getContext());
        reactionStrokeColor = CometChatTheme.getStrokeColorLight(getContext());
        reactionBackgroundColor = CometChatTheme.getBackgroundColor1(getContext());
        activeReactionStrokeColor = CometChatTheme.getExtendedPrimaryColor300(getContext());
        activeReactionBackgroundColor = CometChatTheme.getExtendedPrimaryColor100(getContext());
    }

    /**
     * Applies the style attributes from XML, allowing direct attribute overrides.
     *
     * @param attrs        The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr The default style to apply to this view.
     */
    private void applyStyleAttributes(AttributeSet attrs, int defStyleAttr) {
        TypedArray directAttributes = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CometChatReaction, defStyleAttr, 0);
        @StyleRes int styleResId = directAttributes.getResourceId(R.styleable.CometChatReaction_cometchatReactionStyle, 0);
        directAttributes = styleResId != 0 ? getContext()
            .getTheme()
            .obtainStyledAttributes(attrs, R.styleable.CometChatReaction, defStyleAttr, styleResId) : null;
        extractAttributesAndApplyDefaults(directAttributes);
    }

    /**
     * Extracts the attributes and applies the default values if they are not set in
     * the XML.
     *
     * @param typedArray The TypedArray containing the attributes to be extracted.
     */
    private void extractAttributesAndApplyDefaults(TypedArray typedArray) {
        if (typedArray == null) return;
        try {
            // Extract attributes or apply default values
            reactionEmojiTextColor = typedArray.getColor(R.styleable.CometChatReaction_cometchatReactionEmojiTextColor, reactionEmojiTextColor);
            reactionCountTextColor = typedArray.getColor(R.styleable.CometChatReaction_cometchatReactionCountTextColor, reactionCountTextColor);
            reactionStrokeColor = typedArray.getColor(R.styleable.CometChatReaction_cometchatReactionStrokeColor, reactionStrokeColor);
            reactionBackgroundColor = typedArray.getColor(R.styleable.CometChatReaction_cometchatReactionBackgroundColor, reactionBackgroundColor);
            activeReactionStrokeColor = typedArray.getColor(R.styleable.CometChatReaction_cometchatActiveReactionStrokeColor,
                                                            activeReactionStrokeColor);
            activeReactionBackgroundColor = typedArray.getColor(R.styleable.CometChatReaction_cometchatActiveReactionBackgroundColor,
                                                                activeReactionBackgroundColor);
            reactionEmojiTextAppearance = typedArray.getResourceId(R.styleable.CometChatReaction_cometchatReactionEmojiTextAppearance,
                                                                   reactionEmojiTextAppearance);
            reactionCountTextAppearance = typedArray.getResourceId(R.styleable.CometChatReaction_cometchatReactionCountTextAppearance,
                                                                   reactionCountTextAppearance);
            reactionStrokeWidth = typedArray.getDimensionPixelSize(R.styleable.CometChatReaction_cometchatReactionStrokeWidth, reactionStrokeWidth);
            reactionCornerRadius = typedArray.getDimensionPixelSize(R.styleable.CometChatReaction_cometchatReactionCornerRadius,
                                                                    reactionCornerRadius);
            reactionElevation = typedArray.getDimensionPixelSize(R.styleable.CometChatReaction_cometchatReactionElevation, reactionElevation);
            activeReactionStrokeWidth = typedArray.getDimensionPixelSize(R.styleable.CometChatReaction_cometchatActiveReactionStrokeWidth,
                                                                         activeReactionStrokeWidth);
            // Apply default styles
            applyDefault();
        } finally {
            typedArray.recycle();
        }
    }

    /**
     * Applies the extracted or default values to the CometChatMessageReceipt.
     */
    private void applyDefault() {
        configureReactionTexts();
        setReactionEmojiTextColor(reactionEmojiTextColor);
        setReactionCountTextColor(reactionEmojiTextColor);
        setReactionStrokeColor(reactionStrokeColor);
        setReactionBackgroundColor(reactionBackgroundColor);
        setActiveReactionStrokeColor(activeReactionStrokeColor);
        setActiveReactionBackgroundColor(activeReactionBackgroundColor);
        setReactionEmojiTextAppearance(reactionEmojiTextAppearance);
        setReactionCountTextAppearance(reactionEmojiTextAppearance);
        setReactionStrokeWidth(reactionStrokeWidth);
        setReactionCornerRadius(reactionCornerRadius);
        setReactionElevation(reactionElevation);
        setActiveReactionStrokeWidth(activeReactionStrokeWidth);
    }

    /**
     * Configures the reaction texts based on the emoji and count.
     */
    private void configureReactionTexts() {
        if (emoji != null) {
            if (emoji.equals("+")) {
                String countText = "+" + count;
                binding.tvReactionCount.setText(countText);
                binding.tvReactionEmoji.setVisibility(GONE);
                binding.spaceBetween.setVisibility(GONE);
                binding.tvReactionCount.setVisibility(VISIBLE);
            } else {
                binding.tvReactionEmoji.setText(emoji);
                binding.tvReactionEmoji.setVisibility(VISIBLE);
                binding.spaceBetween.setVisibility(GONE);
                binding.tvReactionCount.setVisibility(GONE);
                if (count > 0) {
                    binding.tvReactionCount.setText(String.valueOf(count));
                    binding.spaceBetween.setVisibility(VISIBLE);
                    binding.tvReactionCount.setVisibility(VISIBLE);
                }
            }
        }
    }

    /**
     * Builds the CometChatReaction view with the specified emoji, count, and
     * reaction status.
     *
     * @param emoji               The emoji to display in the reaction.
     * @param count               The count of the reaction.
     * @param isReactedByMe       The reaction status indicating whether the reaction is reacted by
     *                            the current user.
     * @param onClickListener     The click listener to handle the click event on the reaction.
     * @param onLongClickListener The long click listener to handle the long click event on the
     *                            reaction.
     * @return The built CometChatReaction view.
     */
    public View buildReactionView(@NonNull String emoji,
                                  int count,
                                  boolean isReactedByMe,
                                  View.OnClickListener onClickListener,
                                  View.OnLongClickListener onLongClickListener) {
        // Set the reaction status
        this.emoji = emoji;
        this.count = count;
        this.isReactedByMe = isReactedByMe;
        // Set the click listeners
        this.onClickListener = onClickListener;
        this.onLongClickListener = onLongClickListener;
        setOnClickListener(onClickListener);
        setOnLongClickListener(onLongClickListener);
        applyDefault();
        // Add the start margin to the view
        ViewGroup.MarginLayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                                                                  LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMarginStart(getContext().getResources().getDimensionPixelSize(R.dimen.cometchat_margin));
        setLayoutParams(layoutParams);
        // Return the view
        return this;
    }

    /**
     * Sets the style for the CometChatReaction view by applying a style resource.
     *
     * @param style The style resource to apply.
     */
    public void setStyle(@StyleRes int style) {
        if (style != 0) {
            TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(style, R.styleable.CometChatReaction);
            extractAttributesAndApplyDefaults(typedArray);
        }
    }

    /**
     * Get the reaction background color.
     *
     * @return The background color of the reaction.
     */
    public @ColorInt int getReactionBackgroundColor() {
        return reactionBackgroundColor;
    }

    /**
     * Set the reaction background color.
     *
     * @param reactionBackgroundColor The background color of the reaction.
     */
    public void setReactionBackgroundColor(@ColorInt int reactionBackgroundColor) {
        this.reactionBackgroundColor = reactionBackgroundColor;
        if (!isReactedByMe) {
            setCardBackgroundColor(reactionBackgroundColor);
        }
    }

    /**
     * Get the reaction emoji text style.
     *
     * @return The text style resource of the reaction emoji.
     */
    public @StyleRes int getReactionEmojiTextAppearance() {
        return reactionEmojiTextAppearance;
    }

    /**
     * Set the reaction emoji text style.
     *
     * @param reactionEmojiTextAppearance The text style resource of the reaction emoji.
     */
    public void setReactionEmojiTextAppearance(@StyleRes int reactionEmojiTextAppearance) {
        this.reactionEmojiTextAppearance = reactionEmojiTextAppearance;
        binding.tvReactionEmoji.setTextAppearance(reactionEmojiTextAppearance);
    }

    /**
     * Get the reaction emoji text color.
     *
     * @return The text color of the reaction emoji.
     */
    public @ColorInt int getReactionEmojiTextColor() {
        return reactionEmojiTextColor;
    }

    /**
     * Set the reaction emoji text color.
     *
     * @param reactionEmojiTextColor The text color of the reaction emoji.
     */
    public void setReactionEmojiTextColor(@ColorInt int reactionEmojiTextColor) {
        this.reactionEmojiTextColor = reactionEmojiTextColor;
        binding.tvReactionEmoji.setTextColor(reactionEmojiTextColor);
    }

    /**
     * Get the reaction count text style.
     *
     * @return The text style resource of the reaction count.
     */
    public @StyleRes int getReactionCountTextAppearance() {
        return reactionCountTextAppearance;
    }

    /**
     * Set the reaction count text style.
     *
     * @param reactionCountTextAppearance The text style resource of the reaction count.
     */
    public void setReactionCountTextAppearance(@StyleRes int reactionCountTextAppearance) {
        this.reactionCountTextAppearance = reactionCountTextAppearance;
        binding.tvReactionCount.setTextAppearance(reactionCountTextAppearance);
    }

    /**
     * Get the reaction count text color.
     *
     * @return The text color of the reaction count.
     */
    public @ColorInt int getReactionCountTextColor() {
        return reactionCountTextColor;
    }

    /**
     * Set the reaction count text color.
     *
     * @param reactionCountTextColor The text color of the reaction count.
     */
    public void setReactionCountTextColor(@ColorInt int reactionCountTextColor) {
        this.reactionCountTextColor = reactionCountTextColor;
        binding.tvReactionCount.setTextColor(reactionCountTextColor);
    }

    /**
     * Get the reaction stroke width.
     *
     * @return The stroke width of the reaction.
     */
    public @Dimension int getReactionStrokeWidth() {
        return reactionStrokeWidth;
    }

    /**
     * Set the reaction stroke width.
     *
     * @param reactionStrokeWidth The stroke width of the reaction.
     */
    public void setReactionStrokeWidth(@Dimension int reactionStrokeWidth) {
        this.reactionStrokeWidth = reactionStrokeWidth;
        if (!isReactedByMe) {
            setStrokeWidth(reactionStrokeWidth);
        }
    }

    /**
     * Get the reaction stroke color.
     *
     * @return The stroke color of the reaction.
     */
    public @ColorInt int getReactionStrokeColor() {
        return reactionStrokeColor;
    }

    /**
     * Set the reaction stroke color.
     *
     * @param reactionStrokeColor The stroke color of the reaction.
     */
    public void setReactionStrokeColor(@ColorInt int reactionStrokeColor) {
        this.reactionStrokeColor = reactionStrokeColor;
        if (!isReactedByMe) {
            setStrokeColor(reactionStrokeColor);
        }
    }

    /**
     * Get the reaction corner radius.
     *
     * @return The corner radius of the reaction.
     */
    public @Dimension int getReactionCornerRadius() {
        return reactionCornerRadius;
    }

    /**
     * Set the reaction corner radius.
     *
     * @param reactionCornerRadius The corner radius of the reaction.
     */
    public void setReactionCornerRadius(@Dimension int reactionCornerRadius) {
        this.reactionCornerRadius = reactionCornerRadius;
        setRadius(reactionCornerRadius);
    }

    /**
     * Get the reaction elevation.
     *
     * @return The elevation of the reaction.
     */
    public @Dimension int getReactionElevation() {
        return reactionElevation;
    }

    /**
     * Set the reaction elevation.
     *
     * @param reactionElevation The elevation of the reaction.
     */
    public void setReactionElevation(@Dimension int reactionElevation) {
        this.reactionElevation = reactionElevation;
        setCardElevation(reactionElevation);
    }

    /**
     * Get the active reaction background color.
     *
     * @return The background color of the active reaction.
     */
    public @ColorInt int getActiveReactionBackgroundColor() {
        return activeReactionBackgroundColor;
    }

    /**
     * Set the active reaction background color.
     *
     * @param activeReactionBackgroundColor The background color of the active reaction.
     */
    public void setActiveReactionBackgroundColor(@ColorInt int activeReactionBackgroundColor) {
        this.activeReactionBackgroundColor = activeReactionBackgroundColor;
        if (isReactedByMe) {
            setCardBackgroundColor(activeReactionBackgroundColor);
        }
    }

    /**
     * Get the active reaction stroke width.
     *
     * @return The stroke width of the active reaction.
     */
    public @Dimension int getActiveReactionStrokeWidth() {
        return activeReactionStrokeWidth;
    }

    /**
     * Set the active reaction stroke width.
     *
     * @param activeReactionStrokeWidth The stroke width of the active reaction.
     */
    public void setActiveReactionStrokeWidth(@Dimension int activeReactionStrokeWidth) {
        this.activeReactionStrokeWidth = activeReactionStrokeWidth;
        if (isReactedByMe) {
            setStrokeWidth(activeReactionStrokeWidth);
        }
    }

    /**
     * Get the active reaction stroke color.
     *
     * @return The stroke color of the active reaction.
     */
    public @ColorInt int getActiveReactionStrokeColor() {
        return activeReactionStrokeColor;
    }

    /**
     * Set the active reaction stroke color.
     *
     * @param activeReactionStrokeColor The stroke color of the active reaction.
     */
    public void setActiveReactionStrokeColor(@ColorInt int activeReactionStrokeColor) {
        this.activeReactionStrokeColor = activeReactionStrokeColor;
        if (isReactedByMe) {
            setStrokeColor(activeReactionStrokeColor);
        }
    }

    /**
     * Get the click listener for the reaction.
     *
     * @return The click listener for the reaction.
     */
    public OnClickListener getOnClickListener() {
        return onClickListener;
    }

    /**
     * Get the long click listener for the reaction.
     *
     * @return The long click listener for the reaction.
     */
    public OnLongClickListener getOnLongClickListener() {
        return onLongClickListener;
    }

    /**
     * Get the emoji of the reaction.
     *
     * @return The emoji of the reaction.
     */
    public String getEmoji() {
        return emoji;
    }

    /**
     * Get the count of the reaction.
     *
     * @return The count of the reaction.
     */
    public int getCount() {
        return count;
    }

    /**
     * Get the reaction status indicating whether the reaction is reacted by the
     * current user.
     *
     * @return The reaction status indicating whether the reaction is reacted by the
     * current user.
     */
    public boolean isReactedByMe() {
        return isReactedByMe;
    }
}
