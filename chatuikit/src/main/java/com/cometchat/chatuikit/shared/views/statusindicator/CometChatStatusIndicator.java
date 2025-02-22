package com.cometchat.chatuikit.shared.views.statusindicator;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import com.cometchat.chatuikit.CometChatTheme;
import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.google.android.material.card.MaterialCardView;

/**
 * CometChatStatusIndicator is a custom MaterialCardView that represents the
 * status indicator
 *
 * <p>
 * It provides methods to customize the appearance of the status indicator
 *
 * <p>
 * such as setting the stroke color, stroke width, corner radius, background
 * color, and background image. Created on: 06 September 2024 Modified on: 10
 * September 2024
 */
@SuppressWarnings("unused")
public class CometChatStatusIndicator extends MaterialCardView {
    private static final String TAG = CometChatStatusIndicator.class.getSimpleName();
    private @Dimension float statusIndicatorStrokeWidth;
    private @ColorInt int statusIndicatorStrokeColor;
    private @Dimension float statusIndicatorCornerRadius;
    @Nullable
    private Drawable statusIndicatorOnlineIcon;
    private Drawable statusIndicatorPrivateGroupIcon;
    private Drawable statusIndicatorProtectedGroupIcon;

    private StatusIndicator statusIndicator = StatusIndicator.ONLINE;
    private ImageView imageView;

    /**
     * Constructs a new CometChatStatusIndicator with the given context.
     *
     * @param context The context of the view.
     */
    public CometChatStatusIndicator(Context context) {
        this(context, null);
    }

    /**
     * Constructs a new CometChatStatusIndicator with the given context and
     * attribute set.
     *
     * @param context The context of the view.
     * @param attrs   The attribute set for the view.
     */
    public CometChatStatusIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.cometchatStatusIndicatorStyle);
    }

    /**
     * Constructs a new CometChatStatusIndicator with the given context, attribute
     * set, and default style attribute.
     *
     * @param context      The context of the view.
     * @param attrs        The attribute set for the view.
     * @param defStyleAttr The default style attribute.
     */
    public CometChatStatusIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
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
        Utils.initMaterialCard(this);
        imageView = new ImageView(getContext());
        addView(imageView);
        applyStyleAttributes(attrs, defStyleAttr);
    }

    /**
     * Applies the style attributes from XML, allowing direct attribute overrides.
     *
     * @param attrs        The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr The default style to apply to this view.
     */
    private void applyStyleAttributes(AttributeSet attrs, int defStyleAttr) {
        TypedArray directAttributes = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CometChatStatusIndicator, defStyleAttr, 0);
        @StyleRes int styleResId = directAttributes.getResourceId(R.styleable.CometChatStatusIndicator_cometchatStatusIndicatorStyle, 0);
        directAttributes = styleResId != 0 ? getContext()
            .getTheme()
            .obtainStyledAttributes(attrs, R.styleable.CometChatStatusIndicator, defStyleAttr, styleResId) : null;
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
            statusIndicatorStrokeColor = typedArray.getColor(R.styleable.CometChatStatusIndicator_cometchatStatusIndicatorStrokeColor,
                                                             CometChatTheme.getBackgroundColor1(getContext()));
            statusIndicatorStrokeWidth = typedArray.getDimension(R.styleable.CometChatStatusIndicator_cometchatStatusIndicatorStrokeWidth,
                                                                 statusIndicatorStrokeWidth);
            statusIndicatorCornerRadius = typedArray.getDimension(R.styleable.CometChatStatusIndicator_cometchatStatusIndicatorCornerRadius,
                                                                  statusIndicatorCornerRadius);
            statusIndicatorOnlineIcon = typedArray.getDrawable(R.styleable.CometChatStatusIndicator_cometchatStatusIndicatorOnlineIcon);
            statusIndicatorPrivateGroupIcon = typedArray.getDrawable(R.styleable.CometChatStatusIndicator_cometchatStatusIndicatorPrivateGroupIcon);
            statusIndicatorProtectedGroupIcon = typedArray.getDrawable(R.styleable.CometChatStatusIndicator_cometchatStatusIndicatorProtectedGroupIcon);
            // Apply default styles
            applyDefault();
        } finally {
            typedArray.recycle();
        }
    }

    /**
     * Applies the extracted or default values to the CometChatStatusIndicator.
     */
    private void applyDefault() {
        setStatusIndicatorStrokeColor(statusIndicatorStrokeColor);
        setStatusIndicatorStrokeWidth(statusIndicatorStrokeWidth);
        setStatusIndicatorCornerRadius(statusIndicatorCornerRadius);
        setStatusIndicatorOnlineIcon(statusIndicatorOnlineIcon);
        setStatusIndicatorPrivateGroupIcon(statusIndicatorPrivateGroupIcon);
        setStatusIndicatorProtectedGroupIcon(statusIndicatorProtectedGroupIcon);
        setStatusIndicator(statusIndicator);
    }

    /**
     * Sets the status indicator with either a background color or an icon.
     *
     * @param drawable the {@link Drawable} to be used as a background image.
     */
    private void setStatus(Drawable drawable) {
        if (drawable == null) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
            setStatusIndicatorBackgroundImage(drawable);
        }
    }

    /**
     * Sets the background image of the status indicator.
     *
     * @param image The drawable resource ID of the background image.
     */
    public void setStatusIndicatorBackgroundImage(Drawable image) {
        if (image != null) {
            imageView.setImageDrawable(image);
        }
    }

    /**
     * Sets the style for the CometChatStatusIndicator view by applying a style
     * resource.
     *
     * @param style The style resource to apply.
     */
    public void setStyle(@StyleRes int style) {
        if (style != 0) {
            TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(style, R.styleable.CometChatStatusIndicator);
            extractAttributesAndApplyDefaults(typedArray);
        }
    }

    /**
     * Gets the stroke width of the status indicator.
     *
     * @return The width of the stroke.
     */
    public @Dimension float getStatusIndicatorStrokeWidth() {
        return statusIndicatorStrokeWidth;
    }

    /**
     * Sets the stroke width of the status indicator.
     *
     * @param width The width of the stroke.
     */
    public void setStatusIndicatorStrokeWidth(@Dimension float width) {
        statusIndicatorStrokeWidth = width;
        setStrokeWidth((int) width);
    }

    /**
     * Gets the stroke color of the status indicator.
     *
     * @return The color of the stroke.
     */
    public @ColorInt int getStatusIndicatorStrokeColor() {
        return statusIndicatorStrokeColor;
    }

    /**
     * Sets the stroke color of the status indicator.
     *
     * @param color The color to set for the stroke.
     */
    public void setStatusIndicatorStrokeColor(@ColorInt int color) {
        statusIndicatorStrokeColor = color;
        setStrokeColor(color);
    }

    /**
     * Gets the corner radius of the status indicator.
     *
     * @return The corner radius of the status indicator.
     */
    public @Dimension float getStatusIndicatorCornerRadius() {
        return statusIndicatorCornerRadius;
    }

    /**
     * Sets the corner radius of the status indicator.
     *
     * @param radius The corner radius to set.
     */
    public void setStatusIndicatorCornerRadius(@Dimension float radius) {
        statusIndicatorCornerRadius = radius;
        setRadius(radius);
    }

    /**
     * Sets the background color of the status indicator.
     *
     * @param color The color to set for the background.
     */
    public void setStatusIndicatorBackgroundColor(@ColorInt int color) {
        setCardBackgroundColor(color);
    }

    /**
     * Retrieves the drawable for the status indicator when the user is online.
     *
     * @return The {@link Drawable} representing the online status indicator icon.
     */
    @Nullable
    public Drawable getStatusIndicatorOnlineIcon() {
        return statusIndicatorOnlineIcon;
    }

    /**
     * Sets the drawable for the status indicator when the user is online.
     *
     * @param statusIndicatorOnlineIcon The {@link Drawable} representing the online status indicator icon
     *                                  to be set.
     */
    public void setStatusIndicatorOnlineIcon(@Nullable Drawable statusIndicatorOnlineIcon) {
        this.statusIndicatorOnlineIcon = statusIndicatorOnlineIcon;
    }

    /**
     * Gets the icon used for the private group status indicator.
     *
     * @return the {@link Drawable} representing the private group status indicator
     * icon.
     */
    public Drawable getStatusIndicatorPrivateGroupIcon() {
        return statusIndicatorPrivateGroupIcon;
    }

    /**
     * Sets the icon for the private group status indicator.
     *
     * @param statusIndicatorPrivateGroupIcon the {@link Drawable} to be used as the private group status
     *                                        indicator icon.
     */
    public void setStatusIndicatorPrivateGroupIcon(Drawable statusIndicatorPrivateGroupIcon) {
        this.statusIndicatorPrivateGroupIcon = statusIndicatorPrivateGroupIcon;
    }

    /**
     * Gets the icon used for the protected group status indicator.
     *
     * @return the {@link Drawable} representing the protected group status
     * indicator icon.
     */
    public Drawable getStatusIndicatorProtectedGroupIcon() {
        return statusIndicatorProtectedGroupIcon;
    }

    /**
     * Sets the icon for the protected group status indicator.
     *
     * @param statusIndicatorProtectedGroupIcon the {@link Drawable} to be used as the protected group status
     *                                          indicator icon.
     */
    public void setStatusIndicatorProtectedGroupIcon(Drawable statusIndicatorProtectedGroupIcon) {
        this.statusIndicatorProtectedGroupIcon = statusIndicatorProtectedGroupIcon;
    }

    /**
     * Gets the current status indicator.
     *
     * @return the {@link StatusIndicator} representing the current status.
     */
    public StatusIndicator getStatusIndicator() {
        return statusIndicator;
    }

    /**
     * Sets the status indicator based on the given {@link StatusIndicator} value.
     *
     * @param statusIndicator the {@link StatusIndicator} to be set. Changes the visual
     *                        representation based on the status type, such as setting an icon
     *                        for private or protected group status, or changing background
     *                        color for online/offline status.
     */
    public void setStatusIndicator(StatusIndicator statusIndicator) {
        this.statusIndicator = statusIndicator;
        if (StatusIndicator.ONLINE.equals(statusIndicator)) {
            setStatus(statusIndicatorOnlineIcon);
        } else if (StatusIndicator.OFFLINE.equals(statusIndicator)) {
            setStatus(null);
        } else if (StatusIndicator.PUBLIC_GROUP.equals(statusIndicator)) {
            setStatus(null);
        } else if (StatusIndicator.PRIVATE_GROUP.equals(statusIndicator)) {
            setStatus(statusIndicatorPrivateGroupIcon);
        } else if (StatusIndicator.PROTECTED_GROUP.equals(statusIndicator)) {
            setStatus(statusIndicatorProtectedGroupIcon);
        }
    }
}
