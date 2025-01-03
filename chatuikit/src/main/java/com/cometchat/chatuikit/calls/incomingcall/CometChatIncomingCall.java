package com.cometchat.chatuikit.calls.incomingcall;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.RawRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.cometchat.calls.core.CometChatCalls;
import com.cometchat.chat.constants.CometChatConstants;
import com.cometchat.chat.core.Call;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.databinding.CometchatIncomingCallLayoutBinding;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.interfaces.OnClick;
import com.cometchat.chatuikit.shared.interfaces.OnError;
import com.cometchat.chatuikit.shared.resources.soundmanager.CometChatSoundManager;
import com.cometchat.chatuikit.shared.resources.soundmanager.Sound;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.google.android.material.card.MaterialCardView;

/**
 * CometChatIncomingCall is a custom view that represents the UI for an incoming
 * call.
 *
 * <p>
 * It extends the MaterialCardView class and provides methods for setting
 * call-related properties and styles.
 */
public class CometChatIncomingCall extends MaterialCardView {
    private static final String TAG = CometChatIncomingCall.class.getSimpleName();

    // Binding and ViewModel references
    private CometchatIncomingCallLayoutBinding binding;
    private IncomingCallViewModel viewModel;
    private Call call;
    private User user;

    // Callbacks
    private OnError onError;
    private OnClick onDeclineCallClick, onAcceptCallClick;

    // Sound configuration
    private CometChatSoundManager soundManager;
    private @RawRes int customSoundForCalls;
    private boolean disableSoundForCall;

    // Call settings builder
    private CometChatCalls.CallSettingsBuilder callSettingsBuilder;

    // UI Appearance Properties
    private @ColorInt int titleTextColor;
    private @ColorInt int subtitleTextColor;
    private @StyleRes int titleTextAppearance;
    private @StyleRes int subtitleTextAppearance;
    private @ColorInt int iconTint;
    private Drawable voiceCallIcon;
    private Drawable videoCallIcon;
    private @StyleRes int avatarStyle;

    // Reject Call Button Properties
    private Drawable rejectCallIcon;
    private @ColorInt int rejectCallIconTint;
    private @ColorInt int rejectCallButtonBackgroundColor;

    // Accept Call Button Properties
    private Drawable acceptCallIcon;
    private @ColorInt int acceptCallIconTint;
    private @ColorInt int acceptCallButtonBackgroundColor;

    // Background and Style Properties
    private @ColorInt int backgroundColor;
    private @Dimension int cornerRadius;
    private @Dimension int strokeWidth;
    private @ColorInt int strokeColor;
    private @StyleRes int style;

    /**
     * Constructs a new CometChatIncomingCall with the given context.
     *
     * @param context The context in which the view is created.
     */
    public CometChatIncomingCall(Context context) {
        this(context, null);
    }

    /**
     * Constructs a new CometChatIncomingCall with the given context and attribute
     * set.
     *
     * @param context The context in which the view is created.
     * @param attrs   The attribute set for the view.
     */
    public CometChatIncomingCall(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.cometchatIncomingCallStyle);
    }

    /**
     * Constructs a new CometChatIncomingCall with the given context, attribute set,
     * and default style.
     *
     * @param context      The context in which the view is created.
     * @param attrs        The attribute set for the view.
     * @param defStyleAttr The default style resource.
     */
    public CometChatIncomingCall(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateAndInitializeView(context, attrs, defStyleAttr);
    }

    /**
     * Initializes the incoming call layout and binds view elements. Sets up
     * ViewModel observers for call accept and reject actions, and error handling.
     *
     * @param context      the Context in which the view is running
     * @param attrs        the attribute set containing custom attributes
     * @param defStyleAttr the default style to apply to this view
     */
    private void inflateAndInitializeView(Context context, AttributeSet attrs, int defStyleAttr) {
        // Keep screen on during the call
        ((Activity) context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Utils.initMaterialCard(this);

        // Inflate binding for layout
        binding = CometchatIncomingCallLayoutBinding.inflate(LayoutInflater.from(getContext()), this, true);

        // Sound manager setup
        soundManager = new CometChatSoundManager(context);

        // Initialize ViewModel and observe call state
        viewModel = new ViewModelProvider.NewInstanceFactory().create(IncomingCallViewModel.class);
        viewModel.getAcceptedCall().observe((AppCompatActivity) context, this::acceptedCall);
        viewModel.getRejectCall().observe((AppCompatActivity) context, this::rejectedCall);
        viewModel.getException().observe((AppCompatActivity) context, this::showError);

        // End call button setup
        binding.endCall.getButton().setOnClickListener(view -> {
            binding.endCall.getButton().setEnabled(false);
            if (onDeclineCallClick == null) {
                viewModel.rejectCall(call);
            } else {
                onDeclineCallClick.onClick();
            }
        });

        // Accept call button setup
        binding.acceptCall.getButton().setOnClickListener(view -> {
            binding.acceptCall.getButton().setEnabled(false);
            if (onAcceptCallClick == null) {
                viewModel.acceptCall(call.getSessionId());
            } else {
                onAcceptCallClick.onClick();
            }
        });

        // Apply style attributes to customize the view
        applyStyleAttributes(attrs, defStyleAttr);
    }

    /**
     * Handles acceptance of the call and initiates the ongoing call screen.
     *
     * @param call the call object representing the accepted call.
     */
    public void acceptedCall(Call call) {
        launchOnGoingScreen(call);
    }

    /**
     * Ends the current call and finishes the activity.
     *
     * @param call the call object representing the rejected call.
     */
    public void rejectedCall(Call call) {
        ((Activity) getContext()).finish();
    }

    /**
     * Displays an error message when an exception occurs. If {@code onError} is not
     * set, it will close the activity.
     *
     * @param e the exception to be displayed or handled.
     */
    private void showError(CometChatException e) {
        if (onError == null) {
            ((Activity) getContext()).finish();
        } else {
            onError.onError(getContext(), e);
        }
    }

    /**
     * Applies style attributes based on the XML layout or theme.
     *
     * @param attrs        The attribute set containing customization.
     * @param defStyleAttr The default style attribute.
     */
    private void applyStyleAttributes(AttributeSet attrs, int defStyleAttr) {
        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CometChatIncomingCall, defStyleAttr, 0);
        @StyleRes int style = typedArray.getResourceId(R.styleable.CometChatIncomingCall_cometchatIncomingCallStyle, 0);
        typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CometChatIncomingCall, defStyleAttr, style);
        extractAttributesAndApplyDefaults(typedArray);
    }

    public void launchOnGoingScreen(Call call) {
        binding.incomingCallLayout.setVisibility(GONE);
        binding.ongoingCall.setCallWorkFlow(call
                                                .getReceiverType()
                                                .equalsIgnoreCase(UIKitConstants.ReceiverType.GROUP) ? UIKitConstants.CallWorkFlow.MEETING : UIKitConstants.CallWorkFlow.DEFAULT);
        binding.ongoingCall.setSessionId(call.getSessionId());
        binding.ongoingCall.setCallType(call.getType());
        binding.ongoingCall.setCallSettingsBuilder(callSettingsBuilder);
        binding.ongoingCall.startCall();
        binding.ongoingCall.setVisibility(VISIBLE);
        soundManager.pauseSilently();
    }

    /**
     * Extracts attributes from the given {@link TypedArray} and applies default
     * values.
     *
     * @param typedArray The TypedArray containing the view's attributes.
     */
    private void extractAttributesAndApplyDefaults(TypedArray typedArray) {
        try {
            setTitleTextColor(typedArray.getColor(R.styleable.CometChatIncomingCall_cometchatIncomingCallTitleTextColor, 0));
            setTitleTextAppearance(typedArray.getResourceId(R.styleable.CometChatIncomingCall_cometchatIncomingCallTitleTextAppearance, 0));
            setSubtitleTextColor(typedArray.getColor(R.styleable.CometChatIncomingCall_cometchatIncomingCallSubtitleTextColor, 0));
            setSubtitleTextAppearance(typedArray.getResourceId(R.styleable.CometChatIncomingCall_cometchatIncomingCallSubtitleTextAppearance, 0));
            setIconTint(typedArray.getColor(R.styleable.CometChatIncomingCall_cometchatIncomingCallIconTint, 0));
            setVoiceCallIcon(typedArray.getDrawable(R.styleable.CometChatIncomingCall_cometchatIncomingCallVoiceCallIcon));
            setVideoCallIcon(typedArray.getDrawable(R.styleable.CometChatIncomingCall_cometchatIncomingCallVideoCallIcon));
            setAcceptCallIcon(typedArray.getDrawable(R.styleable.CometChatIncomingCall_cometchatIncomingCallAcceptCallIcon));
            setAcceptCallIconTint(typedArray.getColor(R.styleable.CometChatIncomingCall_cometchatIncomingCallAcceptCallIconTint, 0));
            setAcceptCallButtonBackgroundColor(typedArray.getColor(R.styleable.CometChatIncomingCall_cometchatIncomingCallAcceptCallButtonBackgroundColor,
                                                                   0
            ));

            setRejectCallIcon(typedArray.getDrawable(R.styleable.CometChatIncomingCall_cometchatIncomingCallRejectCallIcon));
            setRejectCallIconTint(typedArray.getColor(R.styleable.CometChatIncomingCall_cometchatIncomingCallRejectCallIconTint, 0));
            setRejectCallButtonBackgroundColor(typedArray.getColor(R.styleable.CometChatIncomingCall_cometchatIncomingCallRejectCallButtonBackgroundColor,
                                                                   0
            ));
            setBackgroundColor(typedArray.getColor(R.styleable.CometChatIncomingCall_cometchatIncomingCallBackgroundColor, 0));
            setCornerRadius(typedArray.getDimensionPixelSize(R.styleable.CometChatIncomingCall_cometchatIncomingCallCornerRadius, 0));
            setStrokeWidth(typedArray.getDimensionPixelSize(R.styleable.CometChatIncomingCall_cometchatIncomingCallStrokeWidth, 0));
            setStrokeColor(typedArray.getColor(R.styleable.CometChatIncomingCall_cometchatIncomingCallStrokeColor, 0));
            setAvatarStyle(typedArray.getResourceId(R.styleable.CometChatIncomingCall_cometchatIncomingCallAvatarStyle, 0));
        } finally {
            typedArray.recycle();
        }
    }

    /**
     * Sets the stroke color for this component.
     *
     * @param strokeColor the color to set as the stroke color.
     */
    @Override
    public void setStrokeColor(@ColorInt int strokeColor) {
        this.strokeColor = strokeColor;
        super.setStrokeColor(strokeColor);
    }

    /**
     * Retrieves the stroke color as a {@code ColorStateList} for the call card
     * border.
     *
     * @return the stroke color as a {@code ColorStateList}.
     */
    public ColorStateList getStrokeColorStateList() {
        return ColorStateList.valueOf(strokeColor);
    }

    /**
     * Sets the call associated with the incoming call view.
     *
     * @param call The Call object representing the incoming call.
     */
    public void setCall(Call call) {
        if (call != null) {
            this.call = call;
            setUser(call.getSender());
            binding.callTypeIcon.setImageDrawable(call.getType().equals(CometChatConstants.CALL_TYPE_AUDIO) ? voiceCallIcon : videoCallIcon);
            binding.subtitleText.setText(getResources().getString(R.string.cometchat_incoming) + " " + call.getType() + " " + getResources().getString(
                R.string.cometchat_call));
        }
    }

    /**
     * Sets the user associated with the incoming call.
     *
     * @param user The User object representing the user making the incoming call.
     */
    public void setUser(User user) {
        if (user != null) {
            this.user = user;
            binding.titleText.setText(user.getName());
            binding.avatar.setAvatar(user.getName(), user.getAvatar());
        }
    }

    /**
     * Enables or disables sound for incoming calls.
     *
     * @param disableSoundForCall {@code true} to disable sound, {@code false} otherwise.
     */
    public void disableSoundForCall(boolean disableSoundForCall) {
        this.disableSoundForCall = disableSoundForCall;
    }

    /**
     * Removes listeners and pauses the call sound when the view is detached from
     * the window.
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        viewModel.removeListeners();
        pauseSound();
    }

    /**
     * Pauses the call sound.
     */
    public void pauseSound() {
        soundManager.pauseSilently();
    }

    /**
     * Retrieves the {@code IncomingCallViewModel} associated with this component.
     *
     * @return the {@code IncomingCallViewModel} instance.
     */
    public IncomingCallViewModel getViewModel() {
        return viewModel;
    }

    /**
     * Retrieves the {@code OnError} callback for handling errors.
     *
     * @return the {@code OnError} instance.
     */
    public OnError getOnError() {
        return onError;
    }

    /**
     * Sets a custom error handling callback.
     *
     * @param onError the {@code OnError} callback to execute when an error occurs.
     */
    public void setOnError(OnError onError) {
        if (onError != null) this.onError = onError;
    }

    /**
     * Retrieves the {@code OnClick} callback for the decline call button action.
     *
     * @return the {@code OnClick} instance for decline call.
     */
    public OnClick getOnDeclineCallClick() {
        return onDeclineCallClick;
    }

    /**
     * Sets a custom action for when the decline call button is clicked.
     *
     * @param click the {@code OnClick} action to execute on decline call button
     *              click.
     */
    public void setOnDeclineCallClick(OnClick click) {
        if (click != null) this.onDeclineCallClick = click;
    }

    /**
     * Retrieves the {@code OnClick} callback for the accept call button action.
     *
     * @return the {@code OnClick} instance for accept call.
     */
    public OnClick getOnAcceptCallClick() {
        return onAcceptCallClick;
    }    /**
     * Sets the stroke width for this component.
     *
     * @param strokeWidth the width in pixels to set for the stroke.
     */
    public void setStrokeWidth(@Dimension int strokeWidth) {
        this.strokeWidth = strokeWidth;
        super.setStrokeWidth(strokeWidth);
    }

    /**
     * Sets a custom action for when the accept call button is clicked.
     *
     * @param click the {@code OnClick} action to execute on accept call button click.
     */
    public void setOnAcceptCallClick(OnClick click) {
        if (click != null) this.onAcceptCallClick = click;
    }

    /**
     * Retrieves the sound manager for handling call sounds.
     *
     * @return the {@code CometChatSoundManager} instance.
     */
    public CometChatSoundManager getSoundManager() {
        return soundManager;
    }

    /**
     * Gets the resource ID of the custom sound for calls.
     *
     * @return the resource ID of the custom call sound.
     */
    public int getCustomSoundForCalls() {
        return customSoundForCalls;
    }

    /**
     * Sets a custom sound resource for incoming calls.
     *
     * @param customSoundForCalls the resource ID of the custom sound.
     */
    public void setCustomSoundForCalls(@RawRes int customSoundForCalls) {
        if (customSoundForCalls != 0) this.customSoundForCalls = customSoundForCalls;
    }

    /**
     * Indicates whether sound is disabled for incoming calls.
     *
     * @return {@code true} if sound is disabled, {@code false} otherwise.
     */
    public boolean isDisableSoundForCall() {
        return disableSoundForCall;
    }

    /**
     * Retrieves the call settings builder used for configuring call settings.
     *
     * @return the {@code CallSettingsBuilder} instance.
     */
    public CometChatCalls.CallSettingsBuilder getCallSettingsBuilder() {
        return callSettingsBuilder;
    }    /**
     * Plays the incoming call sound if sound is not disabled.
     */
    public void playSound() {
        if (!disableSoundForCall) soundManager.play(Sound.incomingCall, customSoundForCalls);
    }

    /**
     * Sets the call settings builder to configure ongoing call settings.
     *
     * @param callSettingsBuilder the call settings builder for custom call configurations.
     */
    public void setCallSettingsBuilder(CometChatCalls.CallSettingsBuilder callSettingsBuilder) {
        if (callSettingsBuilder != null) {
            this.callSettingsBuilder = callSettingsBuilder;
        }
    }

    /**
     * Retrieves the text color for the call title.
     *
     * @return the color of the title text.
     */
    public int getTitleTextColor() {
        return titleTextColor;
    }

    /**
     * Sets the text color for the title text view.
     *
     * @param titleTextColor the color to set as the title text color.
     */
    public void setTitleTextColor(@ColorInt int titleTextColor) {
        this.titleTextColor = titleTextColor;
        binding.titleText.setTextColor(titleTextColor);
    }

    /**
     * Retrieves the text color for the call subtitle.
     *
     * @return the color of the subtitle text.
     */
    public int getSubtitleTextColor() {
        return subtitleTextColor;
    }

    /**
     * Sets the text color for the subtitle text view.
     *
     * @param color the color to set as the subtitle text color.
     */
    public void setSubtitleTextColor(@ColorInt int color) {
        this.subtitleTextColor = color;
        binding.subtitleText.setTextColor(color);
    }

    /**
     * Retrieves the text appearance style resource for the title text.
     *
     * @return the resource ID of the title text appearance.
     */
    public int getTitleTextAppearance() {
        return titleTextAppearance;
    }

    /**
     * Sets the text appearance style for the title text view.
     *
     * @param titleTextAppearance the resource ID of the text appearance style.
     */
    public void setTitleTextAppearance(@StyleRes int titleTextAppearance) {
        this.titleTextAppearance = titleTextAppearance;
        binding.titleText.setTextAppearance(titleTextAppearance);
    }

    /**
     * Retrieves the text appearance style resource for the subtitle text.
     *
     * @return the resource ID of the subtitle text appearance.
     */
    public int getSubtitleTextAppearance() {
        return subtitleTextAppearance;
    }

    /**
     * Sets the text appearance style for the subtitle text view.
     *
     * @param appearance the resource ID of the text appearance style.
     */
    public void setSubtitleTextAppearance(@StyleRes int appearance) {
        this.subtitleTextAppearance = appearance;
        binding.subtitleText.setTextAppearance(appearance);
    }

    /**
     * Retrieves the tint color applied to the call type icon.
     *
     * @return the tint color of the icon.
     */
    public int getIconTint() {
        return iconTint;
    }    /**
     * Adds listeners when the view is attached to the window.
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        viewModel.addListeners();
        if (user != null) playSound();
    }

    /**
     * Sets the tint color for the call type icon.
     *
     * @param iconTint the color to set as the icon tint.
     */
    public void setIconTint(@ColorInt int iconTint) {
        this.iconTint = iconTint;
        binding.callTypeIcon.setColorFilter(iconTint);
    }

    /**
     * Retrieves the drawable icon for voice calls.
     *
     * @return the {@code Drawable} for the voice call icon.
     */
    public Drawable getVoiceCallIcon() {
        return voiceCallIcon;
    }

    /**
     * Sets the icon for voice call functionality.
     *
     * @param voiceCallIcon the drawable to set as the voice call icon.
     */
    public void setVoiceCallIcon(Drawable voiceCallIcon) {
        this.voiceCallIcon = voiceCallIcon;
    }

    /**
     * Retrieves the drawable icon for video calls.
     *
     * @return the {@code Drawable} for the video call icon.
     */
    public Drawable getVideoCallIcon() {
        return videoCallIcon;
    }

    /**
     * Sets the icon for video call functionality.
     *
     * @param videoCallIcon the drawable to set as the video call icon.
     */
    public void setVideoCallIcon(Drawable videoCallIcon) {
        this.videoCallIcon = videoCallIcon;
    }

    /**
     * Retrieves the style resource ID for the avatar.
     *
     * @return the avatar style resource ID.
     */
    public int getAvatarStyle() {
        return avatarStyle;
    }

    /**
     * Sets the style resource for the avatar view.
     *
     * @param avatarStyle the resource ID of the style to apply to the avatar.
     */
    public void setAvatarStyle(@StyleRes int avatarStyle) {
        this.avatarStyle = avatarStyle;
        binding.avatar.setStyle(avatarStyle);
    }

    /**
     * Retrieves the drawable icon for the reject call button.
     *
     * @return the {@code Drawable} for the reject call icon.
     */
    public Drawable getRejectCallIcon() {
        return rejectCallIcon;
    }

    /**
     * Sets the icon for the reject call button.
     *
     * @param rejectCallIcon the drawable to set as the reject call icon.
     */
    public void setRejectCallIcon(Drawable rejectCallIcon) {
        this.rejectCallIcon = rejectCallIcon;
        binding.endCall.setButtonIcon(rejectCallIcon);
    }

    /**
     * Retrieves the tint color for the reject call icon.
     *
     * @return the tint color for the reject call icon.
     */
    public int getRejectCallIconTint() {
        return rejectCallIconTint;
    }

    /**
     * Sets the tint color for the reject call button icon.
     *
     * @param rejectCallIconTint the color to set as the reject call icon tint.
     */
    public void setRejectCallIconTint(@ColorInt int rejectCallIconTint) {
        this.rejectCallIconTint = rejectCallIconTint;
        binding.endCall.setButtonIconTint(rejectCallIconTint);
    }

    /**
     * Retrieves the background color for the reject call button.
     *
     * @return the background color of the reject call button.
     */
    public int getRejectCallButtonBackgroundColor() {
        return rejectCallButtonBackgroundColor;
    }

    /**
     * Sets the background color for the reject call button.
     *
     * @param rejectCallButtonBackgroundColor the color to set as the reject call button background color.
     */
    public void setRejectCallButtonBackgroundColor(@ColorInt int rejectCallButtonBackgroundColor) {
        this.rejectCallButtonBackgroundColor = rejectCallButtonBackgroundColor;
        binding.endCall.setButtonBackgroundColor(rejectCallButtonBackgroundColor);
    }

    /**
     * Retrieves the drawable icon for the accept call button.
     *
     * @return the {@code Drawable} for the accept call icon.
     */
    public Drawable getAcceptCallIcon() {
        return acceptCallIcon;
    }

    /**
     * Sets the icon for the accept call button.
     *
     * @param acceptCallIcon the drawable to set as the accept call icon.
     */
    public void setAcceptCallIcon(Drawable acceptCallIcon) {
        this.acceptCallIcon = acceptCallIcon;
        binding.acceptCall.setButtonIcon(acceptCallIcon);
    }

    /**
     * Retrieves the tint color for the accept call icon.
     *
     * @return the tint color for the accept call icon.
     */
    public int getAcceptCallIconTint() {
        return acceptCallIconTint;
    }

    /**
     * Sets the tint color for the accept call button icon.
     *
     * @param acceptCallIconTint the color to set as the accept call icon tint.
     */
    public void setAcceptCallIconTint(@ColorInt int acceptCallIconTint) {
        this.acceptCallIconTint = acceptCallIconTint;
        binding.acceptCall.setButtonIconTint(acceptCallIconTint);
    }

    /**
     * Retrieves the background color for the accept call button.
     *
     * @return the background color of the accept call button.
     */
    public int getAcceptCallButtonBackgroundColor() {
        return acceptCallButtonBackgroundColor;
    }

    /**
     * Sets the background color for the accept call button.
     *
     * @param acceptCallButtonBackgroundColor the color to set as the accept call button background color.
     */
    public void setAcceptCallButtonBackgroundColor(@ColorInt int acceptCallButtonBackgroundColor) {
        this.acceptCallButtonBackgroundColor = acceptCallButtonBackgroundColor;
        binding.acceptCall.setButtonBackgroundColor(acceptCallButtonBackgroundColor);
    }

    /**
     * Retrieves the background color of the call card.
     *
     * @return the background color of the call card.
     */
    public int getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Sets the background color for this component.
     *
     * @param backgroundColor the color to set as the background color.
     */
    @Override
    public void setBackgroundColor(@ColorInt int backgroundColor) {
        this.backgroundColor = backgroundColor;
        super.setCardBackgroundColor(backgroundColor);
    }

    /**
     * Retrieves the corner radius for the call card.
     *
     * @return the corner radius of the card in dimension units.
     */
    public float getCornerRadius() {
        return cornerRadius;
    }

    /**
     * Sets the corner radius for this component.
     *
     * @param cornerRadius the radius in pixels to set for the corners.
     */
    public void setCornerRadius(@Dimension int cornerRadius) {
        this.cornerRadius = cornerRadius;
        super.setRadius(cornerRadius);
    }

    /**
     * Retrieves the style resource ID applied to the call card.
     *
     * @return the style resource ID.
     */
    public int getStyle() {
        return style;
    }

    /**
     * Sets the style of the text bubble from a specific style resource.
     *
     * @param style The resource ID of the style to apply.
     */
    public void setStyle(@StyleRes int style) {
        if (style != 0) {
            this.style = style;
            TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(style, R.styleable.CometChatIncomingCall);
            extractAttributesAndApplyDefaults(typedArray);
        }
    }








    /**
     * Retrieves the stroke width for the call card border.
     *
     * @return the stroke width of the card border.
     */
    @Override
    public int getStrokeWidth() {
        return strokeWidth;
    }


}
