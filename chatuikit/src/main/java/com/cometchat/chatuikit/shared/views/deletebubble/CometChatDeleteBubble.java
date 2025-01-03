package com.cometchat.chatuikit.shared.views.deletebubble;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.StyleRes;

import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.cometchat.chatuikit.shared.spans.MentionMovementMethod;
import com.google.android.material.card.MaterialCardView;

public class CometChatDeleteBubble extends MaterialCardView {
    private static final String TAG = CometChatDeleteBubble.class.getSimpleName();
    private TextView textView;
    private ImageView deleteImageView;
    private @ColorInt int textColor;
    private @StyleRes int textAppearance;
    private @ColorInt int backgroundColor;
    private int cornerRadius;
    private int borderWidth;
    private @ColorInt int strokeColor;
    private Drawable backgroundDrawable;
    private @StyleRes int style;

    public CometChatDeleteBubble(Context context) {
        this(context, null);
    }

    public CometChatDeleteBubble(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.cometchatDeleteBubbleStyle);
    }

    public CometChatDeleteBubble(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateAndInitializeView(attrs, defStyleAttr);
    }

    /**
     * Initializes the view by inflating the layout and setting up the text bubble.
     *
     * @param attrs        The attribute set for customization.
     * @param defStyleAttr The default style attribute.
     */
    private void inflateAndInitializeView(AttributeSet attrs, int defStyleAttr) {
        Utils.initMaterialCard(this);
        View view = View.inflate(getContext(), R.layout.cometchat_delete_bubble, null);
        textView = view.findViewById(R.id.cometchat_delete_bubble_text_view);
        deleteImageView = view.findViewById(R.id.cometchat_delete_bubble_image_view);
        addView(view);
        applyStyleAttributes(attrs, defStyleAttr, 0);
    }

    /**
     * Applies style attributes based on the XML layout or theme.
     *
     * @param attrs        The attribute set containing customization.
     * @param defStyleAttr The default style attribute.
     * @param defStyleRes  The default style resource.
     */
    private void applyStyleAttributes(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CometChatDeleteBubble, defStyleAttr, defStyleRes);
        @StyleRes int style = typedArray.getResourceId(R.styleable.CometChatDeleteBubble_cometchatDeleteBubbleStyle, 0);
        typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CometChatDeleteBubble, defStyleAttr, style);
        extractAttributesAndApplyDefaults(typedArray);
    }

    /**
     * Extracts attributes from the given {@link TypedArray} and applies default
     * values.
     *
     * @param typedArray The TypedArray containing the view's attributes.
     */
    private void extractAttributesAndApplyDefaults(TypedArray typedArray) {
        try {
            setTextColor(typedArray.getColor(R.styleable.CometChatDeleteBubble_cometchatDeleteBubbleTextColor, 0));
            setTextAppearance(typedArray.getResourceId(R.styleable.CometChatDeleteBubble_cometchatDeleteBubbleTextAppearance, 0));
            setDeleteIcon(R.drawable.cometchat_ic_delete_bubble);
            setDeleteIconTint(typedArray.getColor(R.styleable.CometChatDeleteBubble_cometchatDeleteBubbleIconTint, 0));
        } finally {
            typedArray.recycle();
        }
    }

    /**
     * Sets the text color of the text bubble.
     *
     * @param color The color to set for the text.
     */
    public void setTextColor(@ColorInt int color) {
        textColor = color;
        textView.setTextColor(color);
    }

    /**
     * Sets the text appearance (style) of the text bubble.
     *
     * @param appearance The resource ID of the text appearance.
     */
    public void setTextAppearance(@StyleRes int appearance) {
        textAppearance = appearance;
        textView.setTextAppearance(appearance);
    }

    /**
     * Sets the background color of the text bubble.
     *
     * @param color The color to set for the background.
     */
    @Override
    public void setBackgroundColor(@ColorInt int color) {
        backgroundColor = color;
        setCardBackgroundColor(color);
    }

    /**
     * Sets the corner radius of the text bubble.
     *
     * @param radius The radius to set for the corners, in pixels.
     */
    public void setCornerRadius(@Dimension int radius) {
        cornerRadius = radius;
        setRadius(radius);
    }

    /**
     * Sets the border width of the text bubble.
     *
     * @param width The width of the border, in pixels.
     */
    public void setStrokeWidth(@Dimension int width) {
        borderWidth = width;
        super.setStrokeWidth(width);
    }

    /**
     * Sets the border color of the text bubble.
     *
     * @param color The color to set for the border.
     */
    public void setStrokeColor(@ColorInt int color) {
        strokeColor = color;
        super.setStrokeColor(color);
    }

    /**
     * Sets the background drawable for the text bubble.
     *
     * @param backgroundDrawable The drawable to set as the background.
     */
    @Override
    public void setBackgroundDrawable(Drawable backgroundDrawable) {
        this.backgroundDrawable = backgroundDrawable;
        super.setBackgroundDrawable(backgroundDrawable);
    }

    /**
     * Sets the text content of the text bubble.
     *
     * @param text The text to display in the bubble.
     */
    public void setText(String text) {
        textView.setText(text);
    }

    /**
     * Sets the text content of the text bubble using a {@link SpannableString}.
     *
     * @param text The spannable string to display in the bubble.
     */
    public void setText(SpannableString text) {
        textView.setText(text, TextView.BufferType.SPANNABLE);
        textView.setMovementMethod(MentionMovementMethod.getInstance());
    }

    /**
     * Gets the {@link TextView} associated with this text bubble.
     *
     * @return The TextView within the bubble.
     */
    public TextView getTextView() {
        return textView;
    }

    /**
     * Sets the compound drawables for the delete bubble.
     *
     * @param icon The drawable to set at the start of the text.
     */
    public void setDeleteIcon(@DrawableRes int icon) {
        deleteImageView.setImageResource(icon);
    }

    /**
     * Sets the tint color for the delete icon .
     *
     * @param color The color to apply to the drawables.
     */
    public void setDeleteIconTint(@ColorInt int color) {
        deleteImageView.setImageTintList(ColorStateList.valueOf(color));
    }

    /**
     * Sets the style of the text bubble from a specific style resource.
     *
     * @param style The resource ID of the style to apply.
     */
    public void setStyle(@StyleRes int style) {
        this.style = style;
        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(style, R.styleable.CometChatDeleteBubble);
        extractAttributesAndApplyDefaults(typedArray);
    }

    // Getters for testing or direct access if needed
    public int getTextColor() {
        return textColor;
    }

    public int getTextAppearance() {
        return textAppearance;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    public int getBorderWidth() {
        return borderWidth;
    }

    public int getStrokeColor() {
        return strokeColor;
    }

    public Drawable getBackgroundDrawable() {
        return backgroundDrawable;
    }

    public int getStyle() {
        return style;
    }
}
