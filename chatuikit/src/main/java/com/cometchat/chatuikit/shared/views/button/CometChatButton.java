package com.cometchat.chatuikit.shared.views.button;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.shared.resources.utils.FontUtils;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Custom implementation of a CometChat button that extends the MaterialCardView
 * class.
 *
 * <p>
 * This button provides various customization options for styling and
 * appearance.
 */
public class CometChatButton extends MaterialCardView {
    private static final String TAG = CometChatButton.class.getSimpleName();
    private Context context;
    private FloatingActionButton button;
    private TextView buttonText;
    private ImageView icon;
    private RelativeLayout buttonContainer;

    /**
     * Constructs a CometChatButton object with the given context.
     *
     * @param context The context in which the button is created.
     */
    public CometChatButton(Context context) {
        super(context);
        init(context);
    }

    /**
     * Initializes the CometChatButton by inflating the layout and initializing its
     * components.
     *
     * @param context The context in which the button is created.
     */
    private void init(Context context) {
        this.context = context;
        Utils.initMaterialCard(this);
        View view = View.inflate(context, R.layout.cometchat_button, null);
        button = view.findViewById(R.id.button);
        buttonText = view.findViewById(R.id.button_text);
        icon = view.findViewById(R.id.icon);
        buttonContainer = view.findViewById(R.id.button_container);
        addView(view);
    }

    /**
     * ]
     * Constructs a CometChatButton object with the given context and attributes.
     *
     * @param context The context in which the button is created.
     * @param attrs   The attributes of the XML tag that is inflating the button.
     */
    public CometChatButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * Constructs a CometChatButton object with the given context, attributes, and
     * default style.
     *
     * @param context      The context in which the button is created.
     * @param attrs        The attributes of the XML tag that is inflating the button.
     * @param defStyleAttr An attribute in the current theme that contains a reference to a
     *                     style resource.
     */
    public CometChatButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * Sets the visibility of the button icon.
     *
     * @param hide Boolean indicating whether to hide the button icon (true) or show
     *             it (false).
     */
    public void hideButtonIcon(boolean hide) {
        icon.setVisibility(hide ? GONE : VISIBLE);
    }

    /**
     * Sets the icon for the button.
     *
     * @param icon The drawable resource ID for the button icon.
     */
    public void setButtonIcon(Drawable icon) {
        if (icon != null) {
            this.icon.setImageDrawable(icon);
        }
    }

    /**
     * Sets the visibility of the button text.
     *
     * @param hideText Boolean indicating whether to hide the button text (true) or show
     *                 it (false).
     */
    public void hideButtonText(boolean hideText) {
        buttonText.setVisibility(hideText ? GONE : VISIBLE);
    }

    /**
     * Sets the visibility of the button background.
     *
     * @param hide Boolean indicating whether to hide the button background (true) or
     *             show it (false).
     */
    public void hideButtonBackground(boolean hide) {
        button.setVisibility(hide ? GONE : VISIBLE);
    }

    /**
     * Applies the specified style to the button.
     *
     * @param style The ButtonStyle object containing the style properties to be
     *              applied.
     * @see ButtonStyle
     */
    public void setStyle(ButtonStyle style) {
        if (style != null) {
            setButtonTextFont(style.getButtonTextFont());
            setButtonTextAppearance(style.getButtonTextAppearance());
            setButtonTextColor(style.getButtonTextColor());
            setButtonIconTint(style.getButtonIconTint());
            setButtonBackgroundColor(style.getButtonBackgroundColor());
            setButtonBackgroundDrawable(style.getButtonBackgroundDrawable());
            setButtonSize(style.getWidth(), style.getHeight());

            if (style.getDrawableBackground() != null)
                this.setBackground(style.getDrawableBackground());
            else if (style.getBackground() != 0) this.setCardBackgroundColor(style.getBackground());
            if (style.getStrokeWidth() >= 0) this.setStrokeWidth(style.getStrokeWidth());
            if (style.getCornerRadius() >= 0) this.setRadius(style.getCornerRadius());
            if (style.getStrokeColor() != 0) this.setStrokeColor(style.getStrokeColor());
        }
    }

    /**
     * Sets the font for the button text.
     *
     * @param fonts The name of the font file in the assets directory.
     */
    public void setButtonTextFont(String fonts) {
        if (fonts != null && !fonts.isEmpty())
            buttonText.setTypeface(FontUtils.getInstance().getTypeFace(fonts, getContext()));
    }

    /**
     * Sets the appearance of the button text.
     *
     * @param appearance The style resource for the button text appearance.
     */
    public void setButtonTextAppearance(int appearance) {
        if (buttonText != null && appearance != 0) buttonText.setTextAppearance(appearance);
    }

    /**
     * Sets the color of the button text.
     *
     * @param color The color resource for the button text.
     */
    public void setButtonTextColor(int color) {
        if (color != 0) buttonText.setTextColor(color);
    }

    /**
     * Sets the tint color for the button icon.
     *
     * @param color The color resource for the button icon tint.
     */
    public void setButtonIconTint(@ColorInt int color) {
        if (color != 0) {
            this.icon.setImageTintList(ColorStateList.valueOf(color));
        }
    }

    /**
     * Sets the background color for the button.
     *
     * @param color The color resource for the button background.
     */
    public void setButtonBackgroundColor(@ColorInt int color) {
        if (color != 0) {
            button.setBackgroundTintList(ColorStateList.valueOf(color));
        }
    }

    /**
     * Sets the background drawable for the button.
     *
     * @param drawable The drawable resource ID for the button background.
     */
    public void setButtonBackgroundDrawable(@DrawableRes int drawable) {
        if (drawable != 0) {
            button.setBackgroundResource(drawable);
        }
    }

    /**
     * Sets the size of the button.
     *
     * @param width  The width of the button in density-independent pixels (dp).
     * @param height The height of the button in density-independent pixels (dp).
     */
    public void setButtonSize(int width, int height) {
        if (width > 0 && height > 0) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(Utils.convertDpToPx(context, width),
                                                                                 Utils.convertDpToPx(context, height));
            button.setLayoutParams(params);
            params = new RelativeLayout.LayoutParams(Utils.convertDpToPx(context, width - 10), Utils.convertDpToPx(context, height - 10));
            params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            icon.setLayoutParams(params);
        }
    }

    public void setButtonPadding(int padding) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                                                         LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = padding;
        params.bottomMargin = padding;
        params.leftMargin = padding;
        params.rightMargin = padding;

        buttonContainer.setLayoutParams(params);
    }

    public FloatingActionButton getButton() {
        return button;
    }

    public TextView getButtonText() {
        return buttonText;
    }

    /**
     * Sets the text for the button.
     *
     * @param titleStr The text to be displayed on the button.
     */
    public void setButtonText(@Nullable String titleStr) {
        if (titleStr != null && !titleStr.isEmpty()) {
            buttonText.setText(titleStr);
            buttonText.setVisibility(VISIBLE);
        } else buttonText.setVisibility(GONE);
    }

    public ImageView getIcon() {
        return icon;
    }
}
