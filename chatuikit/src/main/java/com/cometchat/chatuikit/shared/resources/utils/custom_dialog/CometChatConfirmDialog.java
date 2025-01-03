package com.cometchat.chatuikit.shared.resources.utils.custom_dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import com.cometchat.chatuikit.CometChatTheme;
import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.databinding.CometchatConfirmDialogBinding;
import com.google.android.material.card.MaterialCardView;

public class CometChatConfirmDialog extends Dialog {
    private static final String TAG = CometChatConfirmDialog.class.getSimpleName();


    private final CometchatConfirmDialogBinding binding;

    private boolean hideTitle = false;
    private boolean hideSubtitle = false;
    private boolean hideDialogIcon = false;
    private boolean hideIconBackground = false;
    private boolean hidePositiveButton = false;
    private boolean hideNegativeButton = false;
    private boolean hideNegativeButtonProgressBar = true;
    private boolean hidePositiveButtonProgressBar = true;

    private String titleText;
    private String subtitleText;
    private String positiveButtonText;
    private String negativeButtonText;

    private View.OnClickListener onPositiveButtonClick;
    private View.OnClickListener onNegativeButtonClick;

    private @ColorInt int confirmDialogStrokeColor;
    private @Dimension int confirmDialogStrokeWidth;
    private @Dimension int confirmDialogCornerRadius;
    private @Dimension int confirmDialogElevation;
    private @ColorInt int confirmDialogBackgroundColor;

    @Nullable
    private Drawable confirmDialogIcon;
    private @ColorInt int confirmDialogIconBackgroundColor;
    private @ColorInt int confirmDialogIconTint;
    private @StyleRes int confirmDialogTitleTextAppearance;
    private @ColorInt int confirmDialogTitleTextColor;
    private @StyleRes int confirmDialogSubtitleTextAppearance;
    private @ColorInt int confirmDialogSubtitleTextColor;

    private @StyleRes int confirmDialogPositiveButtonTextAppearance;
    private @ColorInt int confirmDialogPositiveButtonTextColor;
    private @ColorInt int confirmDialogPositiveButtonBackgroundColor;
    private @ColorInt int confirmDialogPositiveButtonStrokeWidth;
    private @ColorInt int confirmDialogPositiveButtonStrokeColor;
    private @ColorInt int confirmDialogPositiveButtonRadius;

    private @StyleRes int confirmDialogNegativeButtonTextAppearance;
    private @ColorInt int confirmDialogNegativeButtonTextColor;
    private @ColorInt int confirmDialogNegativeButtonBackgroundColor;
    private @ColorInt int confirmDialogNegativeButtonStrokeWidth;
    private @ColorInt int confirmDialogNegativeButtonStrokeColor;
    private @ColorInt int confirmDialogNegativeButtonRadius;

    /**
     * Constructor for the CometChatConfirmDialog.
     *
     * @param context    The context in which the dialog should run.
     * @param themeResId A resource ID of a style resource defining the theme for the
     *                   dialog.
     */
    public CometChatConfirmDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
        binding = CometchatConfirmDialogBinding.inflate(getLayoutInflater());
        applyDefaultValues();
    }

    /**
     * Called when the dialog is created. This method sets the content view for the
     * dialog and initializes the click listeners for the positive and negative
     * buttons.
     *
     * @param savedInstanceState If the dialog is being re-initialized after previously being shut
     *                           down then this Bundle contains the data it most recently supplied
     *                           in onSaveInstanceState. Note: Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(binding.getRoot());
        initClickListener();
    }

    /**
     * Initializes the click listeners for the positive and negative buttons in the
     * dialog. When a button is clicked, the corresponding click listener is invoked
     * if it is not null.
     */
    private void initClickListener() {
        binding.btnPositive.setOnClickListener(v -> {
            if (onPositiveButtonClick != null) {
                onPositiveButtonClick.onClick(v);
            }
        });

        binding.btnNegative.setOnClickListener(v -> {
            if (onNegativeButtonClick != null) {
                onNegativeButtonClick.onClick(v);
            }
        });
    }

    /**
     * Applies default values for the dialog's appearance and behavior. This
     * includes setting colors for the dialog's background, stroke, title text,
     * subtitle text, and button styles. It also obtains custom attributes defined
     * in XML for this dialog.
     */
    private void applyDefaultValues() {
        configureDialogWindow();

        confirmDialogStrokeColor = CometChatTheme.getStrokeColorLight(getContext());
        confirmDialogBackgroundColor = CometChatTheme.getBackgroundColor1(getContext());
        confirmDialogIconBackgroundColor = CometChatTheme.getBackgroundColor2(getContext());

        confirmDialogTitleTextColor = CometChatTheme.getTextColorPrimary(getContext());
        confirmDialogSubtitleTextColor = CometChatTheme.getTextColorSecondary(getContext());

        confirmDialogNegativeButtonTextColor = CometChatTheme.getTextColorPrimary(getContext());
        confirmDialogNegativeButtonBackgroundColor = CometChatTheme.getTextColorWhite(getContext());
        confirmDialogNegativeButtonStrokeColor = CometChatTheme.getStrokeColorDark(getContext());

        confirmDialogPositiveButtonTextColor = CometChatTheme.getColorWhite(getContext());
        confirmDialogPositiveButtonBackgroundColor = CometChatTheme.getErrorColor(getContext());

        TypedArray typedArray = getContext().obtainStyledAttributes(R.styleable.CometChatConfirmDialog);
        applyCustomAttributes(typedArray);
    }

    /**
     * Configures the dialog window's layout and appearance. This method sets the
     * width to match the parent, height to wrap content, applies a transparent
     * background, and sets padding and gravity for the dialog window.
     */
    private void configureDialogWindow() {
        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.getDecorView().setPadding(getContext().getResources().getDimensionPixelSize(com.cometchat.chatuikit.R.dimen.cometchat_margin_4), 0, getContext().getResources().getDimensionPixelSize(com.cometchat.chatuikit.R.dimen.cometchat_margin_4), 0);
            window.setGravity(Gravity.CENTER); // Center the dialog
        }
    }

    /**
     * Sets the style of the dialog using the specified style resource ID. This
     * method obtains the styled attributes from the provided resource ID and
     * applies them to the dialog.
     *
     * @param styleResId A resource ID of a style resource defining the style for the
     *                   dialog.
     */
    public void setStyle(@StyleRes int styleResId) {
        TypedArray typedArray = getContext().obtainStyledAttributes(styleResId, R.styleable.CometChatConfirmDialog);
        applyCustomAttributes(typedArray);
    }

    /**
     * Applies custom attributes from the provided TypedArray to the dialog. This
     * method extracts various attributes such as stroke color, width, corner
     * radius, elevation, background color, icon, text appearance, and button styles
     * from the TypedArray, applying defaults where necessary.
     *
     * @param typedArray The TypedArray containing the custom attributes for the dialog.
     */
    private void applyCustomAttributes(TypedArray typedArray) {
        try {
            // Extract attributes or apply default values
            confirmDialogStrokeColor = typedArray.getColor(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogStrokeColor, confirmDialogStrokeColor);
            confirmDialogStrokeWidth = typedArray.getDimensionPixelSize(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogStrokeWidth, confirmDialogStrokeWidth);
            confirmDialogCornerRadius = typedArray.getDimensionPixelSize(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogCornerRadius, confirmDialogCornerRadius);
            confirmDialogElevation = typedArray.getDimensionPixelSize(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogElevation, confirmDialogElevation);

            confirmDialogBackgroundColor = typedArray.getColor(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogBackgroundColor, confirmDialogBackgroundColor);
            confirmDialogIcon = typedArray.getDrawable(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogIcon) == null ? confirmDialogIcon : typedArray.getDrawable(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogIcon);
            confirmDialogIconBackgroundColor = typedArray.getColor(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogIconBackgroundColor, confirmDialogIconBackgroundColor);
            confirmDialogIconTint = typedArray.getColor(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogIconTint, confirmDialogIconTint);

            confirmDialogTitleTextAppearance = typedArray.getResourceId(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogTitleTextAppearance, confirmDialogTitleTextAppearance);
            confirmDialogTitleTextColor = typedArray.getColor(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogTitleTextColor, confirmDialogTitleTextColor);
            confirmDialogSubtitleTextAppearance = typedArray.getResourceId(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogSubtitleTextAppearance, confirmDialogSubtitleTextAppearance);
            confirmDialogSubtitleTextColor = typedArray.getColor(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogSubtitleTextColor, confirmDialogSubtitleTextColor);

            confirmDialogNegativeButtonTextAppearance = typedArray.getResourceId(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogNegativeButtonTextAppearance, confirmDialogNegativeButtonTextAppearance);
            confirmDialogNegativeButtonTextColor = typedArray.getColor(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogNegativeButtonTextColor, confirmDialogNegativeButtonTextColor);
            confirmDialogNegativeButtonBackgroundColor = typedArray.getColor(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogNegativeButtonBackgroundColor, confirmDialogNegativeButtonBackgroundColor);
            confirmDialogNegativeButtonStrokeWidth = typedArray.getDimensionPixelSize(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogNegativeButtonStrokeWidth, confirmDialogNegativeButtonStrokeWidth);
            confirmDialogNegativeButtonStrokeColor = typedArray.getColor(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogNegativeButtonStrokeColor, confirmDialogNegativeButtonStrokeColor);
            confirmDialogNegativeButtonRadius = typedArray.getDimensionPixelSize(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogNegativeButtonRadius, confirmDialogNegativeButtonRadius);

            confirmDialogPositiveButtonTextAppearance = typedArray.getResourceId(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogPositiveButtonTextAppearance, confirmDialogPositiveButtonTextAppearance);
            confirmDialogPositiveButtonTextColor = typedArray.getColor(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogPositiveButtonTextColor, confirmDialogPositiveButtonTextColor);
            confirmDialogPositiveButtonBackgroundColor = typedArray.getColor(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogPositiveButtonBackgroundColor, confirmDialogPositiveButtonBackgroundColor);
            confirmDialogPositiveButtonStrokeWidth = typedArray.getDimensionPixelSize(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogPositiveButtonStrokeWidth, confirmDialogPositiveButtonStrokeWidth);
            confirmDialogPositiveButtonStrokeColor = typedArray.getColor(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogPositiveButtonStrokeColor, confirmDialogPositiveButtonStrokeColor);
            confirmDialogPositiveButtonRadius = typedArray.getDimensionPixelSize(R.styleable.CometChatConfirmDialog_cometchatConfirmDialogPositiveButtonRadius, confirmDialogPositiveButtonRadius);

            updateUI();
        } finally {
            typedArray.recycle();
        }
    }

    /**
     * Updates the user interface elements of the confirmation dialog by applying
     * the current attribute values to their respective views. This method sets the
     * stroke color, stroke width, corner radius, elevation, background color, icon,
     * and text appearances for both title and subtitle, as well as for the positive
     * and negative buttons.
     */
    private void updateUI() {
        setConfirmDialogStrokeColor(confirmDialogStrokeColor);
        setConfirmDialogStrokeWidth(confirmDialogStrokeWidth);
        setConfirmDialogCornerRadius(confirmDialogCornerRadius);
        setConfirmDialogElevation(confirmDialogElevation);

        setConfirmDialogBackgroundColor(confirmDialogBackgroundColor);
        setConfirmDialogIcon(confirmDialogIcon);
        setConfirmDialogIconBackgroundColor(confirmDialogIconBackgroundColor);
        setConfirmDialogIconTint(confirmDialogIconTint);

        setConfirmDialogTitleTextAppearance(confirmDialogTitleTextAppearance);
        setConfirmDialogTitleTextColor(confirmDialogTitleTextColor);
        setConfirmDialogSubtitleTextAppearance(confirmDialogSubtitleTextAppearance);
        setConfirmDialogSubtitleTextColor(confirmDialogSubtitleTextColor);

        setConfirmDialogNegativeButtonTextAppearance(confirmDialogNegativeButtonTextAppearance);
        setConfirmDialogNegativeButtonTextColor(confirmDialogNegativeButtonTextColor);
        setConfirmDialogNegativeButtonBackgroundColor(confirmDialogNegativeButtonBackgroundColor);
        setConfirmDialogNegativeButtonStrokeWidth(confirmDialogNegativeButtonStrokeWidth);
        setConfirmDialogNegativeButtonStrokeColor(confirmDialogNegativeButtonStrokeColor);
        setConfirmDialogNegativeButtonRadius(confirmDialogNegativeButtonRadius);

        setConfirmDialogPositiveButtonTextAppearance(confirmDialogPositiveButtonTextAppearance);
        setConfirmDialogPositiveButtonTextColor(confirmDialogPositiveButtonTextColor);
        setConfirmDialogPositiveButtonBackgroundColor(confirmDialogPositiveButtonBackgroundColor);
        setConfirmDialogPositiveButtonStrokeWidth(confirmDialogPositiveButtonStrokeWidth);
        setConfirmDialogPositiveButtonStrokeColor(confirmDialogPositiveButtonStrokeColor);
        setConfirmDialogPositiveButtonRadius(confirmDialogPositiveButtonRadius);
    }

    /**
     * Retrieves the stroke color of the confirmation dialog.
     *
     * @return The current stroke color of the confirmation dialog.
     */
    public @ColorInt int getConfirmDialogStrokeColor() {
        return confirmDialogStrokeColor;
    }

    /**
     * Sets the stroke color for the confirmation dialog.
     *
     * @param confirmDialogStrokeColor The color to set as the stroke color.
     */
    public void setConfirmDialogStrokeColor(@ColorInt int confirmDialogStrokeColor) {
        this.confirmDialogStrokeColor = confirmDialogStrokeColor;
        if (binding != null) {
            binding.parentCard.setStrokeColor(confirmDialogStrokeColor);
        }
    }

    /**
     * Retrieves the stroke width of the confirmation dialog.
     *
     * @return The current stroke width of the confirmation dialog.
     */
    public @Dimension int getConfirmDialogStrokeWidth() {
        return confirmDialogStrokeWidth;
    }

    /**
     * Sets the stroke width for the confirmation dialog.
     *
     * @param confirmDialogStrokeWidth The width to set as the stroke width.
     */
    public void setConfirmDialogStrokeWidth(@Dimension int confirmDialogStrokeWidth) {
        this.confirmDialogStrokeWidth = confirmDialogStrokeWidth;
        if (binding != null) {
            binding.parentCard.setStrokeWidth(confirmDialogStrokeWidth);
        }
    }

    /**
     * Retrieves the corner radius of the confirmation dialog.
     *
     * @return The current corner radius of the confirmation dialog.
     */
    public @Dimension int getConfirmDialogCornerRadius() {
        return confirmDialogCornerRadius;
    }

    /**
     * Sets the corner radius for the confirmation dialog.
     *
     * @param confirmDialogCornerRadius The radius to set for the corners.
     */
    public void setConfirmDialogCornerRadius(@Dimension int confirmDialogCornerRadius) {
        this.confirmDialogCornerRadius = confirmDialogCornerRadius;
        if (binding != null) {
            binding.parentCard.setRadius(confirmDialogCornerRadius);
        }
    }

    /**
     * Retrieves the elevation of the confirmation dialog.
     *
     * @return The current elevation of the confirmation dialog.
     */
    public @Dimension int getConfirmDialogElevation() {
        return confirmDialogElevation;
    }

    /**
     * Sets the elevation for the confirmation dialog.
     *
     * @param confirmDialogElevation The elevation to set for the dialog.
     */
    public void setConfirmDialogElevation(@Dimension int confirmDialogElevation) {
        this.confirmDialogElevation = confirmDialogElevation;
        if (binding != null) {
            binding.parentCard.setElevation(confirmDialogElevation);
        }
    }

    /**
     * Retrieves the background color of the confirmation dialog.
     *
     * @return The current background color of the confirmation dialog.
     */
    public @ColorInt int getConfirmDialogBackgroundColor() {
        return confirmDialogBackgroundColor;
    }

    /**
     * Sets the background color for the confirmation dialog.
     *
     * @param confirmDialogBackgroundColor The color to set as the background color.
     */
    public void setConfirmDialogBackgroundColor(@ColorInt int confirmDialogBackgroundColor) {
        this.confirmDialogBackgroundColor = confirmDialogBackgroundColor;
        if (binding != null) {
            binding.parentCard.setCardBackgroundColor(confirmDialogBackgroundColor);
        }
    }

    /**
     * Retrieves the icon drawable of the confirmation dialog.
     *
     * @return The current drawable used as the confirmation dialog icon.
     */
    @Nullable
    public Drawable getConfirmDialogIcon() {
        return confirmDialogIcon;
    }

    /**
     * Sets the icon drawable for the confirmation dialog.
     *
     * @param confirmDialogIcon The drawable to set as the confirmation dialog icon.
     */
    public void setConfirmDialogIcon(Drawable confirmDialogIcon) {
        this.confirmDialogIcon = confirmDialogIcon;
        if (binding != null) {
            binding.ivDialogIcon.setImageDrawable(confirmDialogIcon);
        }
    }

    public void setConfirmDialogIconTint(@ColorInt int color) {
        this.confirmDialogIconTint = color;
        binding.ivDialogIcon.setColorFilter(color);
    }

    /**
     * Retrieves the background color of the confirmation dialog icon.
     *
     * @return The current background color of the confirmation dialog icon.
     */
    public @ColorInt int getConfirmDialogIconBackgroundColor() {
        return confirmDialogIconBackgroundColor;
    }

    /**
     * Sets the background color for the confirmation dialog icon.
     *
     * @param confirmDialogIconBackgroundColor The color to set as the icon background.
     */
    public void setConfirmDialogIconBackgroundColor(@ColorInt int confirmDialogIconBackgroundColor) {
        this.confirmDialogIconBackgroundColor = confirmDialogIconBackgroundColor;
        if (binding != null) {
            binding.cardDialogIcon.setStrokeColor(getContext().getResources().getColor(R.color.cometchat_color_transparent, getContext().getTheme()));
            binding.cardDialogIcon.setElevation(0);
            binding.cardDialogIcon.setCardBackgroundColor(confirmDialogIconBackgroundColor);
        }
    }

    /**
     * Retrieves the text appearance style resource ID for the confirmation dialog
     * title.
     *
     * @return The current style resource ID for the title text appearance.
     */
    public @StyleRes int getConfirmDialogTitleTextAppearance() {
        return confirmDialogTitleTextAppearance;
    }

    /**
     * Sets the text appearance style for the confirmation dialog title.
     *
     * @param confirmDialogTitleTextAppearance The style resource ID to apply to the title text.
     */
    public void setConfirmDialogTitleTextAppearance(@StyleRes int confirmDialogTitleTextAppearance) {
        this.confirmDialogTitleTextAppearance = confirmDialogTitleTextAppearance;
        if (binding != null) {
            binding.tvDialogTitle.setTextAppearance(confirmDialogTitleTextAppearance);
        }
    }

    /**
     * Retrieves the text color of the confirmation dialog title.
     *
     * @return The current color of the title text in the confirmation dialog.
     */
    public @ColorInt int getConfirmDialogTitleTextColor() {
        return confirmDialogTitleTextColor;
    }

    /**
     * Sets the text color for the confirmation dialog title.
     *
     * @param confirmDialogTitleTextColor The color to set for the title text.
     */
    public void setConfirmDialogTitleTextColor(@ColorInt int confirmDialogTitleTextColor) {
        this.confirmDialogTitleTextColor = confirmDialogTitleTextColor;
        if (binding != null) {
            binding.tvDialogTitle.setTextColor(confirmDialogTitleTextColor);
        }
    }

    /**
     * Retrieves the text appearance style resource ID for the confirmation dialog
     * subtitle.
     *
     * @return The current style resource ID for the subtitle text appearance.
     */
    public @StyleRes int getConfirmDialogSubtitleTextAppearance() {
        return confirmDialogSubtitleTextAppearance;
    }

    /**
     * Sets the text appearance style for the confirmation dialog subtitle.
     *
     * @param confirmDialogSubtitleTextAppearance The style resource ID to apply to the subtitle text.
     */
    public void setConfirmDialogSubtitleTextAppearance(@StyleRes int confirmDialogSubtitleTextAppearance) {
        this.confirmDialogSubtitleTextAppearance = confirmDialogSubtitleTextAppearance;
        if (binding != null) {
            binding.tvDialogSubtitle.setTextAppearance(confirmDialogSubtitleTextAppearance);
        }
    }

    /**
     * Retrieves the text color of the confirmation dialog subtitle.
     *
     * @return The current color of the subtitle text in the confirmation dialog.
     */
    public @ColorInt int getConfirmDialogSubtitleTextColor() {
        return confirmDialogSubtitleTextColor;
    }

    /**
     * Sets the text color for the confirmation dialog subtitle.
     *
     * @param confirmDialogSubtitleTextColor The color to set for the subtitle text.
     */
    public void setConfirmDialogSubtitleTextColor(@ColorInt int confirmDialogSubtitleTextColor) {
        this.confirmDialogSubtitleTextColor = confirmDialogSubtitleTextColor;
        if (binding != null) {
            binding.tvDialogSubtitle.setTextColor(confirmDialogSubtitleTextColor);
        }
    }

    /**
     * Retrieves the text appearance style resource ID for the positive button in
     * the confirmation dialog.
     *
     * @return The current style resource ID for the positive button text
     * appearance.
     */
    public @StyleRes int getConfirmDialogPositiveButtonTextAppearance() {
        return confirmDialogPositiveButtonTextAppearance;
    }

    /**
     * Sets the text appearance style for the positive button in the confirmation
     * dialog.
     *
     * @param confirmDialogPositiveButtonTextAppearance The style resource ID to apply to the positive button text.
     */
    public void setConfirmDialogPositiveButtonTextAppearance(@StyleRes int confirmDialogPositiveButtonTextAppearance) {
        this.confirmDialogPositiveButtonTextAppearance = confirmDialogPositiveButtonTextAppearance;
        if (binding != null) {
            binding.tvPositiveButton.setTextAppearance(confirmDialogPositiveButtonTextAppearance);
        }
    }

    /**
     * Retrieves the text color of the confirmation dialog positive button.
     *
     * @return The current color of the positive button text in the confirmation
     * dialog.
     */
    public @ColorInt int getConfirmDialogPositiveButtonTextColor() {
        return confirmDialogPositiveButtonTextColor;
    }

    /**
     * Sets the text color for the positive button in the confirmation dialog.
     *
     * @param confirmDialogPositiveButtonTextColor The color to set for the positive button text.
     */
    public void setConfirmDialogPositiveButtonTextColor(@ColorInt int confirmDialogPositiveButtonTextColor) {
        this.confirmDialogPositiveButtonTextColor = confirmDialogPositiveButtonTextColor;
        if (binding != null) {
            binding.tvPositiveButton.setTextColor(confirmDialogPositiveButtonTextColor);
        }
    }

    /**
     * Retrieves the background color of the confirmation dialog positive button.
     *
     * @return The current background color of the positive button in the
     * confirmation dialog.
     */
    public @ColorInt int getConfirmDialogPositiveButtonBackgroundColor() {
        return confirmDialogPositiveButtonBackgroundColor;
    }

    /**
     * Sets the background color for the positive button in the confirmation dialog.
     *
     * @param confirmDialogPositiveButtonBackgroundColor The color to set as the positive button background.
     */
    public void setConfirmDialogPositiveButtonBackgroundColor(@ColorInt int confirmDialogPositiveButtonBackgroundColor) {
        this.confirmDialogPositiveButtonBackgroundColor = confirmDialogPositiveButtonBackgroundColor;
        if (binding != null) {
            binding.btnPositive.setCardBackgroundColor(confirmDialogPositiveButtonBackgroundColor);
        }
    }

    /**
     * Retrieves the stroke width of the confirmation dialog positive button.
     *
     * @return The current stroke width of the positive button in the confirmation
     * dialog.
     */
    public @Dimension int getConfirmDialogPositiveButtonStrokeWidth() {
        return confirmDialogPositiveButtonStrokeWidth;
    }

    /**
     * Sets the stroke width for the positive button in the confirmation dialog.
     *
     * @param confirmDialogPositiveButtonStrokeWidth The stroke width to set for the positive button.
     */
    public void setConfirmDialogPositiveButtonStrokeWidth(@Dimension int confirmDialogPositiveButtonStrokeWidth) {
        this.confirmDialogPositiveButtonStrokeWidth = confirmDialogPositiveButtonStrokeWidth;
        if (binding != null) {
            binding.btnPositive.setStrokeWidth(confirmDialogPositiveButtonStrokeWidth);
        }
    }

    /**
     * Retrieves the stroke color of the confirmation dialog positive button.
     *
     * @return The current stroke color of the positive button in the confirmation
     * dialog.
     */
    public @ColorInt int getConfirmDialogPositiveButtonStrokeColor() {
        return confirmDialogPositiveButtonStrokeColor;
    }

    /**
     * Sets the stroke color for the positive button in the confirmation dialog.
     *
     * @param confirmDialogPositiveButtonStrokeColor The color to set as the stroke for the positive button.
     */
    public void setConfirmDialogPositiveButtonStrokeColor(@ColorInt int confirmDialogPositiveButtonStrokeColor) {
        this.confirmDialogPositiveButtonStrokeColor = confirmDialogPositiveButtonStrokeColor;
        if (binding != null) {
            binding.btnPositive.setStrokeColor(confirmDialogPositiveButtonStrokeColor);
        }
    }

    /**
     * Retrieves the corner radius of the confirmation dialog positive button.
     *
     * @return The current corner radius of the positive button in the confirmation
     * dialog.
     */
    public @Dimension int getConfirmDialogPositiveButtonRadius() {
        return confirmDialogPositiveButtonRadius;
    }

    /**
     * Sets the corner radius for the positive button in the confirmation dialog.
     *
     * @param confirmDialogPositiveButtonRadius The radius to set for the corners of the positive button.
     */
    public void setConfirmDialogPositiveButtonRadius(@Dimension int confirmDialogPositiveButtonRadius) {
        this.confirmDialogPositiveButtonRadius = confirmDialogPositiveButtonRadius;
        if (binding != null) {
            binding.btnPositive.setRadius(confirmDialogPositiveButtonRadius);
        }
    }

    /**
     * Retrieves the text appearance style resource ID for the negative button in
     * the confirmation dialog.
     *
     * @return The current style resource ID for the negative button text
     * appearance.
     */
    public @StyleRes int getConfirmDialogNegativeButtonTextAppearance() {
        return confirmDialogNegativeButtonTextAppearance;
    }

    /**
     * Sets the text appearance style for the negative button in the confirmation
     * dialog.
     *
     * @param confirmDialogNegativeButtonTextAppearance The style resource ID to apply to the negative button text.
     */
    public void setConfirmDialogNegativeButtonTextAppearance(@StyleRes int confirmDialogNegativeButtonTextAppearance) {
        this.confirmDialogNegativeButtonTextAppearance = confirmDialogNegativeButtonTextAppearance;
        if (binding != null) {
            binding.tvNegativeButton.setTextAppearance(confirmDialogNegativeButtonTextAppearance);
        }
    }

    /**
     * Retrieves the text color of the confirmation dialog negative button.
     *
     * @return The current color of the negative button text in the confirmation
     * dialog.
     */
    public @ColorInt int getConfirmDialogNegativeButtonTextColor() {
        return confirmDialogNegativeButtonTextColor;
    }

    /**
     * Sets the text color for the negative button in the confirmation dialog.
     *
     * @param confirmDialogNegativeButtonTextColor The color to set for the negative button text.
     */
    public void setConfirmDialogNegativeButtonTextColor(@ColorInt int confirmDialogNegativeButtonTextColor) {
        this.confirmDialogNegativeButtonTextColor = confirmDialogNegativeButtonTextColor;
        if (binding != null) {
            binding.tvNegativeButton.setTextColor(confirmDialogNegativeButtonTextColor);
        }
    }

    /**
     * Retrieves the background color of the confirmation dialog negative button.
     *
     * @return The current background color of the negative button in the
     * confirmation dialog.
     */
    public @ColorInt int getConfirmDialogNegativeButtonBackgroundColor() {
        return confirmDialogNegativeButtonBackgroundColor;
    }

    /**
     * Sets the background color for the negative button in the confirmation dialog.
     *
     * @param confirmDialogNegativeButtonBackgroundColor The color to set as the negative button background.
     */
    public void setConfirmDialogNegativeButtonBackgroundColor(@ColorInt int confirmDialogNegativeButtonBackgroundColor) {
        this.confirmDialogNegativeButtonBackgroundColor = confirmDialogNegativeButtonBackgroundColor;
        if (binding != null) {
            binding.btnNegative.setCardBackgroundColor(confirmDialogNegativeButtonBackgroundColor);
        }
    }

    /**
     * Retrieves the stroke width of the confirmation dialog negative button.
     *
     * @return The current stroke width of the negative button in the confirmation
     * dialog.
     */
    public @Dimension int getConfirmDialogNegativeButtonStrokeWidth() {
        return confirmDialogNegativeButtonStrokeWidth;
    }

    /**
     * Sets the stroke width for the negative button in the confirmation dialog.
     *
     * @param confirmDialogNegativeButtonStrokeWidth The stroke width to set for the negative button.
     */
    public void setConfirmDialogNegativeButtonStrokeWidth(@Dimension int confirmDialogNegativeButtonStrokeWidth) {
        this.confirmDialogNegativeButtonStrokeWidth = confirmDialogNegativeButtonStrokeWidth;
        if (binding != null) {
            binding.btnNegative.setStrokeWidth(confirmDialogNegativeButtonStrokeWidth);
        }
    }

    /**
     * Retrieves the stroke color of the confirmation dialog negative button.
     *
     * @return The current stroke color of the negative button in the confirmation
     * dialog.
     */
    public @ColorInt int getConfirmDialogNegativeButtonStrokeColor() {
        return confirmDialogNegativeButtonStrokeColor;
    }

    /**
     * Sets the stroke color for the negative button in the confirmation dialog.
     *
     * @param confirmDialogNegativeButtonStrokeColor The color to set as the stroke for the negative button.
     */
    public void setConfirmDialogNegativeButtonStrokeColor(@ColorInt int confirmDialogNegativeButtonStrokeColor) {
        this.confirmDialogNegativeButtonStrokeColor = confirmDialogNegativeButtonStrokeColor;
        if (binding != null) {
            binding.btnNegative.setStrokeColor(confirmDialogNegativeButtonStrokeColor);
        }
    }

    /**
     * Retrieves the corner radius of the confirmation dialog negative button.
     *
     * @return The current corner radius of the negative button in the confirmation
     * dialog.
     */
    public @Dimension int getConfirmDialogNegativeButtonRadius() {
        return confirmDialogNegativeButtonRadius;
    }

    /**
     * Sets the corner radius for the negative button in the confirmation dialog.
     *
     * @param confirmDialogNegativeButtonRadius The radius to set for the corners of the negative button.
     */
    public void setConfirmDialogNegativeButtonRadius(@Dimension int confirmDialogNegativeButtonRadius) {
        this.confirmDialogNegativeButtonRadius = confirmDialogNegativeButtonRadius;
        if (binding != null) {
            binding.btnNegative.setRadius(confirmDialogNegativeButtonRadius);
        }
    }

    /**
     * Checks whether the title of the confirmation dialog is hidden.
     *
     * @return True if the title is hidden, otherwise false.
     */
    public boolean isHideTitle() {
        return hideTitle;
    }

    /**
     * Sets the visibility of the title in the confirmation dialog.
     *
     * @param hideTitle True to hide the title, false to show it.
     */
    public void setHideTitle(boolean hideTitle) {
        this.hideTitle = hideTitle;
        if (binding != null) {
            binding.tvDialogTitle.setVisibility(hideTitle ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Checks whether the subtitle of the confirmation dialog is hidden.
     *
     * @return True if the subtitle is hidden, otherwise false.
     */
    public boolean isHideSubtitle() {
        return hideSubtitle;
    }

    /**
     * Sets the visibility of the subtitle in the confirmation dialog.
     *
     * @param hideSubtitle True to hide the subtitle, false to show it.
     */
    public void setHideSubtitle(boolean hideSubtitle) {
        this.hideSubtitle = hideSubtitle;
        if (binding != null) {
            binding.tvDialogSubtitle.setVisibility(hideSubtitle ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Checks whether the dialog icon is hidden.
     *
     * @return True if the dialog icon is hidden, otherwise false.
     */
    public boolean isHideDialogIcon() {
        return hideDialogIcon;
    }

    /**
     * Sets the visibility of the dialog icon.
     *
     * @param hideDialogIcon True to hide the dialog icon, false to show it.
     */
    public void setHideDialogIcon(boolean hideDialogIcon) {
        this.hideDialogIcon = hideDialogIcon;
        if (binding != null) {
            binding.cardDialogIcon.setVisibility(hideDialogIcon ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Checks whether the background of the icon is hidden.
     *
     * @return True if the icon background is hidden, otherwise false.
     */
    public boolean isHideIconBackground() {
        return hideIconBackground;
    }

    /**
     * Sets the visibility of the icon background.
     *
     * @param hideIconBackground True to hide the icon background, false to show it.
     */
    public void setHideIconBackground(boolean hideIconBackground) {
        this.hideIconBackground = hideIconBackground;
        if (binding != null) {
            binding.cardDialogIcon.setStrokeColor(getContext().getResources().getColor(R.color.cometchat_color_transparent, getContext().getTheme()));
            binding.cardDialogIcon.setElevation(0);
            binding.cardDialogIcon.setCardBackgroundColor(getContext().getResources().getColor(R.color.cometchat_color_transparent, getContext().getTheme()));
        }
    }

    /**
     * Checks whether the positive button is hidden.
     *
     * @return True if the positive button is hidden, otherwise false.
     */
    public boolean isHidePositiveButton() {
        return hidePositiveButton;
    }

    /**
     * Sets the visibility of the positive button in the confirmation dialog.
     *
     * @param hidePositiveButton True to hide the positive button, false to show it.
     */
    public void setHidePositiveButton(boolean hidePositiveButton) {
        this.hidePositiveButton = hidePositiveButton;
        if (binding != null) {
            binding.btnPositive.setVisibility(hidePositiveButton ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Checks whether the negative button is hidden.
     *
     * @return True if the negative button is hidden, otherwise false.
     */
    public boolean isHideNegativeButton() {
        return hideNegativeButton;
    }

    /**
     * Sets the visibility of the negative button in the confirmation dialog.
     *
     * @param hideNegativeButton True to hide the negative button, false to show it.
     */
    public void setHideNegativeButton(boolean hideNegativeButton) {
        this.hideNegativeButton = hideNegativeButton;
        if (binding != null) {
            binding.btnNegative.setVisibility(hideNegativeButton ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Retrieves the text of the title in the confirmation dialog.
     *
     * @return The current title text in the confirmation dialog.
     */
    public String getTitleText() {
        return titleText;
    }

    /**
     * Sets the title text for the confirmation dialog.
     *
     * @param titleText The text to set as the title in the confirmation dialog.
     */
    public void setTitleText(String titleText) {
        this.titleText = titleText;
        if (binding != null) {
            binding.tvDialogTitle.setText(titleText);
        }
    }

    /**
     * Retrieves the text of the subtitle in the confirmation dialog.
     *
     * @return The current subtitle text in the confirmation dialog.
     */
    public String getSubtitleText() {
        return subtitleText;
    }

    /**
     * Sets the subtitle text for the confirmation dialog.
     *
     * @param subtitleText The text to set as the subtitle in the confirmation dialog.
     */
    public void setSubtitleText(String subtitleText) {
        this.subtitleText = subtitleText;
        if (binding != null) {
            binding.tvDialogSubtitle.setText(subtitleText);
        }
    }

    /**
     * Retrieves the text of the positive button in the confirmation dialog.
     *
     * @return The current positive button text in the confirmation dialog.
     */
    public String getPositiveButtonText() {
        return positiveButtonText;
    }

    /**
     * Sets the text for the positive button in the confirmation dialog.
     *
     * @param positiveButtonText The text to set for the positive button.
     */
    public void setPositiveButtonText(String positiveButtonText) {
        this.positiveButtonText = positiveButtonText;
        if (binding != null) {
            binding.tvPositiveButton.setText(positiveButtonText);
        }
    }

    /**
     * Retrieves the text of the negative button in the confirmation dialog.
     *
     * @return The current negative button text in the confirmation dialog.
     */
    public String getNegativeButtonText() {
        return negativeButtonText;
    }

    /**
     * Sets the text for the negative button in the confirmation dialog.
     *
     * @param negativeButtonText The text to set for the negative button.
     */
    public void setNegativeButtonText(String negativeButtonText) {
        this.negativeButtonText = negativeButtonText;
        if (binding != null) {
            binding.tvNegativeButton.setText(negativeButtonText);
        }
    }

    /**
     * Retrieves the click listener for the negative button.
     *
     * @return The current click listener for the negative button.
     */
    public View.OnClickListener getOnNegativeButtonClick() {
        return onNegativeButtonClick;
    }

    /**
     * Sets the click listener for the negative button in the confirmation dialog.
     *
     * @param onNegativeButtonClick The click listener to set for the negative button.
     */
    public void setOnNegativeButtonClick(View.OnClickListener onNegativeButtonClick) {
        this.onNegativeButtonClick = onNegativeButtonClick;
    }

    /**
     * Retrieves the click listener for the positive button.
     *
     * @return The current click listener for the positive button.
     */
    public View.OnClickListener getOnPositiveButtonClick() {
        return onPositiveButtonClick;
    }

    /**
     * Sets the click listener for the positive button in the confirmation dialog.
     *
     * @param onPositiveButtonClick The click listener to set for the positive button.
     */
    public void setOnPositiveButtonClick(View.OnClickListener onPositiveButtonClick) {
        this.onPositiveButtonClick = onPositiveButtonClick;
    }

    /**
     * Retrieves the dialog view.
     *
     * @return The parent MaterialCardView of the confirmation dialog.
     */
    public MaterialCardView getDialogView() {
        return binding.parentCard;
    }

    /**
     * Checks whether the negative button's progress bar is hidden.
     *
     * @return true if the negative button's progress bar is hidden; false
     * otherwise.
     */
    public boolean hideNegativeButtonProgressBar() {
        return hideNegativeButtonProgressBar;
    }

    /**
     * Sets the visibility of the negative button's progress bar.
     *
     * @param hideNegativeButtonProgressBar true to hide the progress bar, false to show it.
     */
    public void hideNegativeButtonProgressBar(boolean hideNegativeButtonProgressBar) {
        this.hideNegativeButtonProgressBar = hideNegativeButtonProgressBar;
        if (hideNegativeButtonProgressBar) {
            binding.tvNegativeButton.setVisibility(View.VISIBLE);
            binding.progressBarNegativeButton.setVisibility(View.GONE);
        } else {
            binding.tvNegativeButton.setVisibility(View.GONE);
            binding.progressBarNegativeButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Checks whether the positive button's progress bar is hidden.
     *
     * @return true if the positive button's progress bar is hidden; false
     * otherwise.
     */
    public boolean hidePositiveButtonProgressBar() {
        return hidePositiveButtonProgressBar;
    }

    /**
     * Sets the visibility of the positive button's progress bar.
     *
     * @param hidePositiveButtonProgressBar true to hide the progress bar, false to show it.
     */
    public void hidePositiveButtonProgressBar(boolean hidePositiveButtonProgressBar) {
        this.hidePositiveButtonProgressBar = hidePositiveButtonProgressBar;
        if (hidePositiveButtonProgressBar) {
            binding.tvPositiveButton.setVisibility(View.VISIBLE);
            binding.progressBarPositiveButton.setVisibility(View.GONE);
        } else {
            binding.tvPositiveButton.setVisibility(View.GONE);
            binding.progressBarPositiveButton.setVisibility(View.VISIBLE);
        }
    }
}
