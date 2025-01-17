package com.cometchat.chatuikit.shared.views.messagebubble;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.StyleRes;

import com.cometchat.chatuikit.CometChatTheme;
import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.google.android.material.card.MaterialCardView;

/**
 * The CometChatMessageBubble class is a custom view that represents a message
 * bubble used in a chat interface. It extends the MaterialCardView class,
 * providing additional customization and functionality.
 *
 * <p>
 * The message bubble can be aligned to the left, right, or center of the screen
 * using the setMessageAlignment method. It supports various views such as
 * leading view, header view, reply view, content view, bottom view, thread
 * view, and footer view. These views can be set using the corresponding setter
 * methods (setLeadingView, setHeaderView, etc.) to customize the appearance and
 * content of the message bubble.
 *
 * <p>
 * The style of the message bubble can be customized using the setStyle method,
 * which takes a MessageBubbleStyle object as a parameter. The style allows
 * customization of background color, border width, corner radius, and border
 * color.
 *
 * <p>
 * Overall, the CometChatMessageBubble class provides a flexible and
 * customizable message bubble component for chat interfaces.
 */
public class CometChatMessageBubble extends MaterialCardView {
    private static final String TAG = CometChatMessageBubble.class.getSimpleName();
    private View view;
    private LinearLayout parent, leadingView, headerView, replyView, messageContainer, contentView, bottomView, statusInfoView, threadView, footerView;
    private MaterialCardView messageBubble;
    private View customLeadingView, customHeaderView, customReplyView, customContentView, customBottomView, customStatusInfoView, customThreadView, customFooterView;

    private @ColorInt int backgroundColor;
    private @Dimension int strokeWidth;
    private @ColorInt int strokeColor;
    private @Dimension int cornerRadius;
    private Drawable backgroundDrawable = null;
    private @StyleRes int style;

    /**
     * Constructs a new CometChatMessageBubble with the provided context.
     *
     * @param context The context in which the view is created.
     */
    public CometChatMessageBubble(Context context) {
        this(context, null);
    }

    /**
     * Constructs a new CometChatMessageBubble with the provided context and
     * attribute set.
     *
     * @param context The context in which the view is created.
     * @param attrs   The attribute set to apply to the view.
     */
    public CometChatMessageBubble(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.cometchatMessageBubbleStyle);
    }

    /**
     * Constructs a new CometChatMessageBubble with the provided context, attribute
     * set, and default style.
     *
     * @param context      The context in which the view is created.
     * @param attrs        The attribute set to apply to the view.
     * @param defStyleAttr The default style resource ID to apply to the view.
     */
    public CometChatMessageBubble(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateAndInitializeView(attrs, defStyleAttr);
    }

    private void inflateAndInitializeView(AttributeSet attrs, int defStyleAttr) {
        Utils.initMaterialCard(this);
        applyStyleAttributes(attrs, defStyleAttr, 0);
    }

    private void applyStyleAttributes(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CometChatMessageBubble, defStyleAttr, defStyleRes);
        @StyleRes int styleResId = typedArray.getResourceId(R.styleable.CometChatMessageBubble_cometchatMessageBubbleStyle, 0);
        typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CometChatMessageBubble, defStyleRes, styleResId);
        extractAttributesAndApplyDefaults(typedArray);
    }

    private void extractAttributesAndApplyDefaults(TypedArray typedArray) {
        try {
            setBackgroundDrawable(typedArray.getDrawable(R.styleable.CometChatMessageBubble_cometchatMessageBubbleBackgroundDrawable));
            setBackgroundColor(typedArray.getColor(R.styleable.CometChatMessageBubble_cometchatMessageBubbleBackgroundColor,
                                                   CometChatTheme.getPrimaryColor(getContext())));
            setStrokeWidth((int) typedArray.getDimension(R.styleable.CometChatMessageBubble_cometchatMessageBubbleStrokeWidth, 0));
            setStrokeColor(typedArray.getColor(R.styleable.CometChatMessageBubble_cometchatMessageBubbleStrokeColor, 0));
            setCornerRadius((int) typedArray.getDimension(R.styleable.CometChatMessageBubble_cometchatMessageBubbleCornerRadius, 0));
        } finally {
            typedArray.recycle();
        }
    }

    private void initView(View view) {
        parent = view.findViewById(R.id.parent);
        messageBubble = view.findViewById(R.id.message_bubble);
        Utils.initMaterialCard(messageBubble);
        leadingView = view.findViewById(R.id.leading_view);
        headerView = view.findViewById(R.id.header_view_layout);
        replyView = view.findViewById(R.id.reply_bubble);
        contentView = view.findViewById(R.id.content_view);
        bottomView = view.findViewById(R.id.bottom_view);
        statusInfoView = view.findViewById(R.id.status_info_view);
        threadView = view.findViewById(R.id.view_replies);
        footerView = view.findViewById(R.id.footer_view_layout);
        messageContainer = view.findViewById(R.id.message_container);
        setLeadingView(customLeadingView);
        setHeaderView(customHeaderView);
        setReplyView(customReplyView);
        setContentView(customContentView);
        setThreadView(customThreadView);
        setFooterView(customFooterView);
        setBottomView(customBottomView);
        setStatusInfoView(customStatusInfoView);
        addView(view);
        applyStyle();
    }

    /**
     * Sets the alignment of the message bubble.
     *
     * @param mAlignment The alignment of the message bubble.
     * @see UIKitConstants.MessageBubbleAlignment
     */
    public void setMessageAlignment(UIKitConstants.MessageBubbleAlignment mAlignment) {
        if (UIKitConstants.MessageBubbleAlignment.LEFT.equals(mAlignment)) {
            view = View.inflate(getContext(), R.layout.cometchat_message_bubble_left, null);
        } else if (UIKitConstants.MessageBubbleAlignment.RIGHT.equals(mAlignment)) {
            view = View.inflate(getContext(), R.layout.cometchat_message_bubble_right, null);
        } else if (UIKitConstants.MessageBubbleAlignment.CENTER.equals(mAlignment)) {
            view = View.inflate(getContext(), R.layout.cometchat_message_bubble_center, null);
        }
        removeAllViewsInLayout();
        initView(view);
    }

    /**
     * Sets a custom view as the bottom view.
     *
     * @param view The custom bottom view to set.
     */
    public void setBottomView(View view) {
        this.customBottomView = view;
        handleView(bottomView, view);
    }    /**
     * Sets the stroke width of the message bubble.
     *
     * <p>
     * This method updates the stroke width of the message bubble. The stroke width
     * must be non-negative. If the message bubble is not null, it applies the
     * specified stroke width to the bubble.
     *
     * @param strokeWidth The width of the stroke in pixels. This value should be greater
     *                    than or equal to 0.
     */
    @Override
    public void setStrokeWidth(@Dimension int strokeWidth) {
        if (strokeWidth >= 0) {
            this.strokeWidth = strokeWidth;
            if (messageBubble != null) {
                messageBubble.setStrokeWidth(strokeWidth);
            }
        }
    }

    /**
     * Sets the visibility of the footer view.
     *
     * <p>
     * This method updates the visibility of the footer view to the specified
     * visibility state. The visibility can be set to View.VISIBLE, View.INVISIBLE,
     * or View.GONE.
     *
     * @param visibility The visibility state to set for the footer view. This should be
     *                   one of the constants from the View class.
     */
    public void setFooterViewVisibility(int visibility) {
        footerView.setVisibility(visibility);
    }    /**
     * Sets the stroke color of the message bubble.
     *
     * <p>
     * This method updates the stroke color of the message bubble. If the message
     * bubble is not null, it applies the specified stroke color to the bubble.
     *
     * @param strokeColor The color to set as the stroke. This should be a color resource ID
     *                    or a color value.
     */
    @Override
    public void setStrokeColor(@ColorInt int strokeColor) {
        if (messageBubble != null) {
            this.strokeColor = strokeColor;
            messageBubble.setStrokeColor(strokeColor);
        }
    }

    /**
     * Sets the visibility of the thread view.
     *
     * <p>
     * This method updates the visibility of the thread view to the specified
     * visibility state. The visibility can be set to View.VISIBLE, View.INVISIBLE,
     * or View.GONE.
     *
     * @param visibility The visibility state to set for the thread view. This should be
     *                   one of the constants from the View class.
     */
    public void setThreadViewVisibility(int visibility) {
        threadView.setVisibility(visibility);
    }

    /**
     * Sets the visibility of the bottom view.
     *
     * <p>
     * This method updates the visibility of the bottom view to the specified
     * visibility state. The visibility can be set to View.VISIBLE, View.INVISIBLE,
     * or View.GONE.
     *
     * @param visibility The visibility state to set for the bottom view. This should be
     *                   one of the constants from the View class.
     */
    public void setBottomViewVisibility(int visibility) {
        bottomView.setVisibility(visibility);
    }

    /**
     * Sets the visibility of the status info view.
     *
     * <p>
     * This method updates the visibility of the status info view to the specified
     * visibility state. The visibility can be set to View.VISIBLE, View.INVISIBLE,
     * or View.GONE.
     *
     * @param visibility The visibility state to set for the status info view. This should
     *                   be one of the constants from the View class.
     */
    public void setStatusInfoViewVisibility(int visibility) {
        statusInfoView.setVisibility(visibility);
    }

    /**
     * Sets the visibility of the reply view.
     *
     * <p>
     * This method updates the visibility of the reply view to the specified
     * visibility state. The visibility can be set to View.VISIBLE, View.INVISIBLE,
     * or View.GONE.
     *
     * @param visibility The visibility state to set for the reply view. This should be one
     *                   of the constants from the View class.
     */
    public void setReplyViewVisibility(int visibility) {
        replyView.setVisibility(visibility);
    }

    /**
     * Sets the visibility of the header view.
     *
     * <p>
     * This method updates the visibility of the header view to the specified
     * visibility state. The visibility can be set to View.VISIBLE, View.INVISIBLE,
     * or View.GONE.
     *
     * @param visibility The visibility state to set for the header view. This should be
     *                   one of the constants from the View class.
     */
    public void setHeaderViewVisibility(int visibility) {
        headerView.setVisibility(visibility);
    }

    /**
     * Sets the visibility of the leading view.
     *
     * <p>
     * This method updates the visibility of the leading view to the specified
     * visibility state. The visibility can be set to View.VISIBLE, View.INVISIBLE,
     * or View.GONE.
     *
     * @param visibility The visibility state to set for the leading view. This should be
     *                   one of the constants from the View class.
     */
    public void setLeadingViewVisibility(int visibility) {
        leadingView.setVisibility(visibility);
    }

    /**
     * Sets the visibility of the content view.
     *
     * <p>
     * This method updates the visibility of the content view to the specified
     * visibility state. The visibility can be set to View.VISIBLE, View.INVISIBLE,
     * or View.GONE.
     *
     * @param visibility The visibility state to set for the content view. This should be
     *                   one of the constants from the View class.
     */
    public void setContentViewVisibility(int visibility) {
        contentView.setVisibility(visibility);
    }

    /**
     * Handles the display of a specific view within a given layout.
     *
     * <p>
     * This method checks if both the layout and the view are not null. If they are
     * valid, it removes any existing views from the layout, detaches the view from
     * its current parent (if it has one), sets the layout's visibility to VISIBLE,
     * and then adds the specified view to the layout. If either the layout or the
     * view is null, the layout's visibility is set to GONE.
     *
     * @param layout The LinearLayout in which the view will be displayed. This layout
     *               will have any existing views removed and the new view added to it.
     * @param view   The View to be displayed in the specified layout. If this view is
     *               null, the layout will be set to GONE.
     */
    private void handleView(LinearLayout layout, View view) {
        if (view != null && layout != null) {
            layout.removeAllViews();
            removeParentFromView(view);
            layout.setVisibility(VISIBLE);
            layout.addView(view);
        } else {
            if (layout != null) {
                layout.setVisibility(GONE);
            }
        }
    }

    /**
     * Removes the parent view from the specified view if it has one.
     *
     * <p>
     * This method checks if the provided view has a parent. If it does, the method
     * retrieves the parent and removes the view from it. This ensures that the view
     * can be added to a different parent layout without any issues.
     *
     * @param view The View whose parent is to be removed. This view will be detached
     *             from its current parent if it exists.
     */
    private void removeParentFromView(View view) {
        if (view.getParent() != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null) {
                parent.removeView(view);
            }
        }
    }

    /**
     * Sets the padding of the message container.
     *
     * <p>
     * This method updates the margins of the message container based on the
     * specified values for left, top, right, and bottom. If any of the values are
     * less than zero, they will be treated as zero. The padding is applied by
     * modifying the layout parameters of the message container.
     *
     * @param left   The left margin to set, or -1 for no change.
     * @param top    The top margin to set, or -1 for no change.
     * @param right  The right margin to set, or -1 for no change.
     * @param bottom The bottom margin to set, or -1 for no change.
     */
    public void setPadding(int left, int top, int right, int bottom) {
        ViewGroup.LayoutParams layoutParams = messageContainer.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
            marginLayoutParams.setMargins(left > -1 ? left : 0, top > -1 ? top : 0, right > -1 ? right : 0, bottom > -1 ? bottom : 0);
            messageContainer.setLayoutParams(marginLayoutParams);
        }
    }

    /**
     * Applies the current style properties to the view. This method sets the
     * background color, background drawable, stroke width, stroke color, and corner
     * radius of the view using the previously defined style attributes. It
     * centralizes the application of these styles to ensure that the view reflects
     * the desired appearance whenever the style properties are modified.
     */
    private void applyStyle() {
        this.setBackgroundColor(backgroundColor);
        this.setBackgroundDrawable(backgroundDrawable);
        this.setStrokeWidth(strokeWidth);
        this.setStrokeColor(strokeColor);
        this.setCornerRadius(cornerRadius);
    }

    // Getters for testing or direct access if needed
    public LinearLayout getView() {
        return parent;
    }

    public LinearLayout getLeadingView() {
        return leadingView;
    }

    /**
     * Sets a custom view as the leading view.
     *
     * @param view The custom leading view to set.
     */
    public void setLeadingView(View view) {
        this.customLeadingView = view;
        handleView(leadingView, view);
    }

    public LinearLayout getHeaderView() {
        return headerView;
    }

    /**
     * Sets a custom view as the header view.
     *
     * @param view The custom header view to set.
     */
    public void setHeaderView(View view) {
        this.customHeaderView = view;
        handleView(headerView, view);
    }

    public LinearLayout getReplyView() {
        return replyView;
    }

    /**
     * Sets a custom view as the reply view.
     *
     * @param view The custom reply view to set.
     */
    public void setReplyView(View view) {
        this.customReplyView = view;
        handleView(replyView, view);
    }

    public LinearLayout getContentView() {
        return contentView;
    }

    /**
     * Sets a custom view as the content view.
     *
     * @param view The custom content view to set.
     */
    public void setContentView(View view) {
        this.customContentView = view;
        handleView(contentView, view);
    }

    public LinearLayout getThreadView() {
        return threadView;
    }

    /**
     * Sets a custom view as the thread view.
     *
     * @param view The custom thread view to set.
     */
    public void setThreadView(View view) {
        this.customThreadView = view;
        handleView(threadView, view);
    }

    public LinearLayout getFooterView() {
        return footerView;
    }

    /**
     * Sets a custom view as the footer view.
     *
     * @param view The custom footer view to set.
     */
    public void setFooterView(View view) {
        this.customFooterView = view;
        handleView(footerView, view);
    }

    public View getStatusInfoView() {
        return statusInfoView;
    }

    /**
     * Sets a custom view as the status info view.
     *
     * @param view The custom status info view to set.
     */
    public void setStatusInfoView(View view) {
        this.customStatusInfoView = view;
        handleView(statusInfoView, customStatusInfoView);
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Sets the background color of the message bubble.
     *
     * <p>
     * This method updates the background color of the message bubble to the
     * specified color. If the message bubble is not null, it applies the color to
     * the bubble.
     *
     * @param backgroundColor The color to set as the background. This should be a color
     *                        resource ID or a color value.
     */
    @Override
    public void setBackgroundColor(@ColorInt int backgroundColor) {
        if (messageBubble != null) {
            this.backgroundColor = backgroundColor;
            messageBubble.setCardBackgroundColor(backgroundColor);
        }
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    /**
     * Sets the corner radius of the message bubble.
     *
     * <p>
     * This method updates the corner radius of the message bubble. The radius must
     * be non-negative. If the message bubble is not null, it applies the specified
     * radius to the bubble.
     *
     * @param cornerRadius The radius of the corners in pixels. This value should be greater
     *                     than or equal to 0.
     */
    public void setCornerRadius(@Dimension int cornerRadius) {
        if (cornerRadius >= 0) {
            if (messageBubble != null) {
                this.cornerRadius = cornerRadius;
                messageBubble.setRadius(cornerRadius);
            }
        }
    }

    public Drawable getBackgroundDrawable() {
        return backgroundDrawable;
    }

    /**
     * Sets the background drawable of the message bubble.
     *
     * <p>
     * This method updates the background drawable of the message bubble. If the
     * provided drawable is not null, and the message bubble is not null, it applies
     * the drawable to the bubble.
     *
     * @param backgroundDrawable The drawable to set as the background of the message bubble. This
     *                           can be any valid drawable resource.
     */
    @Override
    public void setBackgroundDrawable(Drawable backgroundDrawable) {
        if (backgroundDrawable != null) {
            this.backgroundDrawable = backgroundDrawable;
            if (messageBubble != null) {
                messageBubble.setBackgroundDrawable(backgroundDrawable);
            }
        }
    }

    public int getStyle() {
        return style;
    }

    /**
     * Applies the specified style to the message bubble.
     *
     * <p>
     * This method extracts attributes from the specified style resource and applies
     * them to the message bubble. The attributes are obtained from the current
     * theme's styled attributes based on the provided style resource ID.
     *
     * @param style The resource ID of the style to apply to the message bubble. This
     *              style should define attributes for the bubble.
     */
    public void setStyle(@StyleRes int style) {
        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(style, R.styleable.CometChatMessageBubble);
        extractAttributesAndApplyDefaults(typedArray);
    }    @Override
    public int getStrokeWidth() {
        return strokeWidth;
    }

    @Override
    public int getStrokeColor() {
        return strokeColor;
    }






}
