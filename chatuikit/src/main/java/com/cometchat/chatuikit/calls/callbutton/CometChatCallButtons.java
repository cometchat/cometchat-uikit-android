package com.cometchat.chatuikit.calls.callbutton;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.cometchat.calls.core.CometChatCalls;
import com.cometchat.chat.constants.CometChatConstants;
import com.cometchat.chat.core.Call;
import com.cometchat.chat.core.CometChat;
import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.calls.CallingExtension;
import com.cometchat.chatuikit.calls.CometChatCallActivity;
import com.cometchat.chatuikit.calls.outgoingcall.OutgoingCallConfiguration;
import com.cometchat.chatuikit.shared.interfaces.Function3;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.cometchat.chatuikit.shared.views.button.CometChatButton;
import com.google.android.material.card.MaterialCardView;

/**
 * Custom view class for displaying call buttons in the CometChat application.
 *
 * <p>
 * It extends MaterialCardView to provide a card-like appearance.
 *
 * <p>
 * Use this class to display and customize the call buttons.
 */
public class CometChatCallButtons extends MaterialCardView {
    private static final String TAG = CometChatCallButtons.class.getSimpleName();
    // Context and resources
    private Context context;
    private @StyleRes int style;

    // UI components for voice and video calls
    private CometChatButton voiceCall, videoCall;
    private Drawable voiceCallIcon, videoCallIcon;

    // Colors and styles for the call buttons
    private @ColorInt int voiceCallIconTint, videoCallIconTint;
    private @ColorInt int voiceCallTextColor, videoCallTextColor;
    private @StyleRes int voiceCallTextAppearance, videoCallTextAppearance;
    private @ColorInt int voiceCallBackgroundColor, videoCallBackgroundColor;

    // Event handlers for voice and video call button clicks
    private OnClick onVideoCallClick, onVoiceCallClick;

    // User and group data for the call
    private User user;
    private Group group;

    // ViewModel to manage the call buttons logic
    private CallButtonsViewModel callButtonsViewModel;

    // Layout parameters for the buttons
    private LinearLayout.LayoutParams params;

    // Call configuration and builder callback
    private OutgoingCallConfiguration outgoingCallConfiguration;
    private Function3<User, Group, Boolean, CometChatCalls.CallSettingsBuilder> callSettingsBuilderCallback;
    private CometChatCalls.CallSettingsBuilder callSettingsBuilder;

    /**
     * Constructor to create CometChatCallButtons with just a Context.
     *
     * @param context The context to use. Usually your Activity or Application context.
     */
    public CometChatCallButtons(Context context) {
        this(context, null);
    }

    /**
     * Constructor to create CometChatCallButtons with a Context and AttributeSet.
     *
     * @param context The context to use. Usually your Activity or Application context.
     * @param attrs   The AttributeSet containing custom XML attributes for this view.
     */
    public CometChatCallButtons(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.cometchatCallButtonsStyle);
    }

    /**
     * Constructor to create CometChatCallButtons with a Context, AttributeSet, and
     * a default style attribute.
     *
     * @param context      The context to use. Usually your Activity or Application context.
     * @param attrs        The AttributeSet containing custom XML attributes for this view.
     * @param defStyleAttr An attribute in the current theme that contains a reference to a
     *                     style resource that provides default values for the view. Can be 0
     *                     to not look for defaults.
     */
    public CometChatCallButtons(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateAndInitializeView(context, attrs, defStyleAttr);
    }

    /**
     * Inflates the view for CometChat call buttons and initializes the components.
     * Sets up the voice and video call buttons, binds data to the ViewModel, and
     * applies any custom style attributes.
     *
     * @param context      The context to use for inflating the view and initializing
     *                     resources.
     * @param attrs        The AttributeSet containing custom XML attributes.
     * @param defStyleAttr The default style attribute to apply if no attributes are
     *                     provided.
     */
    private void inflateAndInitializeView(Context context, AttributeSet attrs, int defStyleAttr) {
        // Initialize the material design components for this view
        Utils.initMaterialCard(this);

        // Store the context
        this.context = context;

        // Initialize the ViewModel to manage call button states
        callButtonsViewModel = new CallButtonsViewModel();

        // Inflate the layout for the call buttons
        View view = View.inflate(context, R.layout.cometchat_call_button, null);

        // Initialize the voice and video call buttons
        voiceCall = view.findViewById(R.id.voice_call);
        videoCall = view.findViewById(R.id.video_call);

        // Set layout parameters for the buttons
        params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.weight = 1;

        // Observe call status updates from ViewModel
        callButtonsViewModel.getCallInitiated().observe((AppCompatActivity) context, this::callInitiated);
        callButtonsViewModel.getStartDirectCall().observe((AppCompatActivity) context, this::startDirectCall);

        // Set click listeners for voice and video call buttons
        voiceCall.setOnClickListener(view12 -> {
            if (onVoiceCallClick != null) {
                // Custom voice call click action
                onVoiceCallClick.onClick(user, group);
            } else {
                // Default action - invoke call settings and initiate audio call
                invokeCallSettingsBuilderCallback(true);
                callButtonsViewModel.initiateCall(CometChatConstants.CALL_TYPE_AUDIO);
            }
        });

        videoCall.setOnClickListener(view1 -> {
            if (onVideoCallClick != null) {
                // Custom video call click action
                onVideoCallClick.onClick(user, group);
            } else {
                // Default action - invoke call settings and initiate video call
                invokeCallSettingsBuilderCallback(false);
                callButtonsViewModel.initiateCall(CometChatConstants.CALL_TYPE_VIDEO);
            }
        });

        // Apply custom style attributes if any
        applyStyleAttributes(attrs, defStyleAttr, 0);

        // Add the inflated view to this layout
        addView(view);
    }

    /**
     * Applies style attributes based on the XML layout or theme.
     *
     * @param attrs        The attribute set containing customization.
     * @param defStyleAttr The default style attribute.
     * @param defStyleRes  The default style resource.
     */
    private void applyStyleAttributes(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CometChatCallButtons, defStyleAttr, defStyleRes);
        @StyleRes int style = typedArray.getResourceId(R.styleable.CometChatCallButtons_cometchatCallButtonsStyle, 0);
        typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CometChatCallButtons, defStyleAttr, style);
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
            setVideoCallIcon(typedArray.getDrawable(R.styleable.CometChatCallButtons_cometchatCallButtonsVideoCallIcon));
            setVoiceCallIcon(typedArray.getDrawable(R.styleable.CometChatCallButtons_cometchatCallButtonsVoiceCallIcon));

            setVideoCallIconTint(typedArray.getColor(R.styleable.CometChatCallButtons_cometchatCallButtonsVideoCallIconTint, 0));
            setVoiceCallIconTint(typedArray.getColor(R.styleable.CometChatCallButtons_cometchatCallButtonsVoiceCallIconTint, 0));

            setVideoCallTextColor(typedArray.getColor(R.styleable.CometChatCallButtons_cometchatCallButtonsVideoCallTextColor, 0));
            setVoiceCallTextColor(typedArray.getColor(R.styleable.CometChatCallButtons_cometchatCallButtonsVoiceCallTextColor, 0));

            setVideoCallTextAppearance(typedArray.getResourceId(R.styleable.CometChatCallButtons_cometchatCallButtonsVideoCallTextAppearance, 0));
            setVoiceCallTextAppearance(typedArray.getResourceId(R.styleable.CometChatCallButtons_cometchatCallButtonsVoiceCallTextAppearance, 0));

            setVoiceCallBackgroundColor(typedArray.getColor(R.styleable.CometChatCallButtons_cometchatCallButtonsVoiceCallBackgroundColor, 0));
            setVideoCallBackgroundColor(typedArray.getColor(R.styleable.CometChatCallButtons_cometchatCallButtonsVideoCallBackgroundColor, 0));
        } finally {
            typedArray.recycle();
        }
    }

    /**
     * Sets the style of the text bubble from a specific style resource.
     *
     * @param style The resource ID of the style to apply.
     */
    public void setStyle(@StyleRes int style) {
        if (style != 0) {
            this.style = style;
            TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(style, R.styleable.CometChatCallButtons);
            extractAttributesAndApplyDefaults(typedArray);
        }
    }

    /**
     * Sets the margins for the voice and video call buttons.
     *
     * @param marginLeft   The margin on the left side of the button (in pixels).
     * @param marginTop    The margin on the top side of the button (in pixels).
     * @param marginRight  The margin on the right side of the button (in pixels).
     * @param marginBottom The margin on the bottom side of the button (in pixels).
     */
    public void setButtonMargin(int marginLeft, int marginTop, int marginRight, int marginBottom) {
        params.setMargins(marginLeft, marginTop, marginRight, marginBottom);
        // Apply the updated layout parameters to both call buttons
        videoCall.setLayoutParams(params);
        voiceCall.setLayoutParams(params);
    }

    /**
     * Starts a direct call (such as a conference call) using the provided
     * BaseMessage. Launches the conference call screen with the current context and
     * call settings.
     *
     * @param baseMessage The message object containing the call data needed to initiate the
     *                    call.
     */
    private void startDirectCall(BaseMessage baseMessage) {
        CometChatCallActivity.launchConferenceCallScreen(context, baseMessage, callSettingsBuilder);
    }

    /**
     * Callback method when a call is initiated. Configures the outgoing call
     * settings and launches the outgoing call screen.
     *
     * @param call The Call object that represents the call being initiated.
     */
    private void callInitiated(Call call) {
        // Ensure the outgoing call configuration is properly set up
        if (outgoingCallConfiguration == null) {
            outgoingCallConfiguration = new OutgoingCallConfiguration().setCallSettingsBuilder(callSettingsBuilder);
        } else {
            if (outgoingCallConfiguration.getCallSettingBuilder() == null) {
                outgoingCallConfiguration.setCallSettingsBuilder(callSettingsBuilder);
            }
        }
        // Launch the outgoing call screen with the configured settings
        CometChatCallActivity.launchOutgoingCallScreen(context, call, outgoingCallConfiguration);
    }

    /**
     * Sets the outgoing call configuration for the call buttons. If the provided
     * configuration is non-null, it replaces the current configuration.
     *
     * @param outgoingCallConfiguration The configuration object that contains settings for outgoing
     *                                  calls.
     */
    public void setOutgoingCallConfiguration(OutgoingCallConfiguration outgoingCallConfiguration) {
        if (outgoingCallConfiguration != null) {
            this.outgoingCallConfiguration = outgoingCallConfiguration;
        }
    }

    /**
     * Called when the view is attached to a window. Checks if there is an active
     * call and disables the buttons if a call is active, or enables the buttons if
     * there is no active call. It also adds the necessary listeners.
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Add listeners to observe call status changes
        callButtonsViewModel.addListener();
    }

    /**
     * Called when the view is detached from a window. Removes listeners to avoid
     * memory leaks and stops observing call status changes.
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Remove listeners to prevent memory leaks when the view is detached
        callButtonsViewModel.removeListener();
    }

    /**
     * Hides or shows the voice call button.
     *
     * @param hide True to hide the voice call button, false to show it.
     */
    public void hideVoiceCall(boolean hide) {
        voiceCall.setVisibility(hide ? GONE : VISIBLE);
    }

    /**
     * Hides or shows the video call button.
     *
     * @param hide True to hide the video call button, false to show it.
     */
    public void hideVideoCall(boolean hide) {
        videoCall.setVisibility(hide ? GONE : VISIBLE);
    }

    /**
     * Sets the user associated with the call buttons.
     *
     * @param user The User object representing the user associated with the buttons.
     */
    public void setUser(User user) {
        if (user != null) {
            this.user = user;
            hideVoiceCall(false);
            hideVideoCall(false);
            callButtonsViewModel.setUser(user);
        }
    }

    /**
     * Sets the group associated with the call buttons.
     *
     * @param group The Group object representing the group associated with the
     *              buttons.
     */
    public void setGroup(Group group) {
        if (group != null) {
            this.group = group;
            callButtonsViewModel.setGroup(group);
        }
    }

    /**
     * Sets the background color of the voice call button and applies it.
     *
     * @param voiceCallBackgroundColor The color to set as the background of the voice call button.
     */
    public void setVoiceCallBackgroundColor(@ColorInt int voiceCallBackgroundColor) {
        this.voiceCallBackgroundColor = voiceCallBackgroundColor;
        // Apply the background color to the voice call button
        voiceCall.setButtonBackgroundColor(voiceCallBackgroundColor);
    }

    /**
     * Sets the background color of the video call button and applies it.
     *
     * @param videoCallBackgroundColor The color to set as the background of the video call button.
     */
    public void setVideoCallBackgroundColor(@ColorInt int videoCallBackgroundColor) {
        this.videoCallBackgroundColor = videoCallBackgroundColor;
        // Apply the background color to the video call button
        videoCall.setButtonBackgroundColor(videoCallBackgroundColor);
    }

    /**
     * Sets the icon for the voice call button.
     *
     * @param voiceCallIcon The resource ID of the drawable representing the icon for the
     *                      voice call button.
     */
    public void setVoiceCallIcon(Drawable voiceCallIcon) {
        this.voiceCallIcon = voiceCallIcon;
        voiceCall.setButtonIcon(voiceCallIcon);
        voiceCall.hideButtonBackground(true);
    }

    /**
     * Sets the icon for the video call button.
     *
     * @param videoCallIcon The resource ID of the drawable representing the icon for the
     *                      video call button.
     */
    public void setVideoCallIcon(Drawable videoCallIcon) {
        this.videoCallIcon = videoCallIcon;
        videoCall.setButtonIcon(videoCallIcon);
        videoCall.hideButtonBackground(true);
    }

    /**
     * Sets the tint color for the voice call button icon.
     *
     * @param voiceCallIconTint The color resource ID for the voice call button icon tint.
     */
    public void setVoiceCallIconTint(@ColorInt int voiceCallIconTint) {
        this.voiceCallIconTint = voiceCallIconTint;
        voiceCall.setButtonIconTint(voiceCallIconTint);
    }

    /**
     * Sets the tint color for the video call button icon.
     *
     * @param videoCallIconTint The color resource ID for the video call button icon tint.
     */
    public void setVideoCallIconTint(@ColorInt int videoCallIconTint) {
        this.videoCallIconTint = videoCallIconTint;
        videoCall.setButtonIconTint(videoCallIconTint);
    }

    /**
     * Sets the text color for the voice call button.
     *
     * @param voiceCallTextColor The color resource ID for the voice call button text.
     */
    public void setVoiceCallTextColor(@ColorInt int voiceCallTextColor) {
        this.voiceCallTextColor = voiceCallTextColor;
        voiceCall.setButtonTextColor(voiceCallTextColor);
    }

    /**
     * Sets the text color for the video call button.
     *
     * @param videoCallTextColor The color resource ID for the video call button text.
     */
    public void setVideoCallTextColor(@ColorInt int videoCallTextColor) {
        this.videoCallTextColor = videoCallTextColor;
        videoCall.setButtonTextColor(videoCallTextColor);
    }

    /**
     * Sets the text appearance for the voice call button.
     *
     * @param voiceCallTextAppearance The style resource ID for the voice call button text appearance.
     */
    public void setVoiceCallTextAppearance(@StyleRes int voiceCallTextAppearance) {
        this.voiceCallTextAppearance = voiceCallTextAppearance;
        voiceCall.setButtonTextAppearance(voiceCallTextAppearance);
    }

    /**
     * Sets the text appearance for the video call button.
     *
     * @param videoCallTextAppearance The style resource ID for the video call button text appearance.
     */
    public void setVideoCallTextAppearance(@StyleRes int videoCallTextAppearance) {
        this.videoCallTextAppearance = videoCallTextAppearance;
        videoCall.setButtonTextAppearance(videoCallTextAppearance);
    }

    /**
     * Sets the text for the voice call button.
     *
     * @param voiceCallText The text to be displayed on the voice call button.
     */
    public void setVoiceButtonText(String voiceCallText) {
        if (voiceCallText != null && !voiceCallText.isEmpty()) {
            voiceCall.setVisibility(VISIBLE);
            voiceCall.setButtonText(voiceCallText);
        }
    }

    /**
     * Sets the text for the video call button.
     *
     * @param videoCallText The text to be displayed on the video call button.
     */
    public void setVideoButtonText(String videoCallText) {
        if (videoCallText != null && !videoCallText.isEmpty()) {
            videoCall.setVisibility(VISIBLE);
            videoCall.setButtonText(videoCallText);
        }
    }

    /**
     * Hides or shows the text on the call buttons.
     *
     * @param hide True to hide the text on the buttons, false to show it.
     */
    public void hideButtonText(boolean hide) {
        voiceCall.hideButtonText(hide);
        videoCall.hideButtonText(hide);
    }

    /**
     * Hides or shows the icon on the call buttons.
     *
     * @param hide True to hide the icon on the buttons, false to show it.
     */
    public void hideButtonIcon(boolean hide) {
        videoCall.hideButtonIcon(hide);
        voiceCall.hideButtonIcon(hide);
    }

    /**
     * Sets the click listener for the video call button.
     *
     * @param onVideoCallClick The OnClick listener to be invoked when the video call button is
     *                         clicked.
     */
    public void setOnVideoCallClick(OnClick onVideoCallClick) {
        if (onVideoCallClick != null) this.onVideoCallClick = onVideoCallClick;
    }

    /**
     * Sets the click listener for the voice call button.
     *
     * @param onVoiceCallClick The OnClick listener to be invoked when the voice call button is
     *                         clicked.
     */
    public void setOnVoiceCallClick(OnClick onVoiceCallClick) {
        if (onVoiceCallClick != null) this.onVoiceCallClick = onVoiceCallClick;
    }

    /**
     * Sets the call settings builder callback function. This callback is
     * responsible for configuring the call settings based on the user, group, and
     * call type (audio/video).
     *
     * @param callSettingsBuilder The callback function that accepts User, Group, and a Boolean
     *                            (isAudio) and returns a CallSettingsBuilder instance.
     */
    public void setCallSettingsBuilder(Function3<User, Group, Boolean, CometChatCalls.CallSettingsBuilder> callSettingsBuilder) {
        if (callSettingsBuilder != null) {
            this.callSettingsBuilderCallback = callSettingsBuilder;
        }
    }

    /**
     * Invokes the call settings builder callback to retrieve the
     * CallSettingsBuilder. It checks if the callback is set and whether there is a
     * user or group. Based on the call type (audio or video), it creates the
     * appropriate call settings.
     *
     * @param isAudio A boolean flag indicating whether the call is audio (true) or
     *                video (false).
     */
    private void invokeCallSettingsBuilderCallback(boolean isAudio) {
        if (callSettingsBuilderCallback != null) {
            // Ensure either a user or group is available
            if (user != null || group != null) {
                // Apply the callback to generate the call settings
                this.callSettingsBuilder = callSettingsBuilderCallback.apply(user, group, isAudio);
            }
        }
    }

    /**
     * Retrieves the voice call button.
     *
     * @return The CometChatButton instance representing the voice call button.
     */
    public CometChatButton getVoiceCallButton() {
        return voiceCall;
    }

    /**
     * Retrieves the video call button.
     *
     * @return The CometChatButton instance representing the video call button.
     */
    public CometChatButton getVideoCallButton() {
        return videoCall;
    }

    /**
     * Retrieves the style resource identifier for the call buttons.
     *
     * @return The style resource identifier as an int.
     */
    public @StyleRes int getStyle() {
        return style;
    }

    /**
     * Retrieves the drawable icon for the voice call button.
     *
     * @return The Drawable instance representing the voice call icon.
     */
    public Drawable getVoiceCallIcon() {
        return voiceCallIcon;
    }

    /**
     * Retrieves the drawable icon for the video call button.
     *
     * @return The Drawable instance representing the video call icon.
     */
    public Drawable getVideoCallIcon() {
        return videoCallIcon;
    }

    /**
     * Retrieves the tint color for the voice call icon.
     *
     * @return The tint color as an int.
     */
    public @ColorInt int getVoiceCallIconTint() {
        return voiceCallIconTint;
    }

    /**
     * Retrieves the tint color for the video call icon.
     *
     * @return The tint color as an int.
     */
    public @ColorInt int getVideoCallIconTint() {
        return videoCallIconTint;
    }

    /**
     * Retrieves the text color for the voice call button.
     *
     * @return The text color as an int.
     */
    public @ColorInt int getVoiceCallTextColor() {
        return voiceCallTextColor;
    }

    /**
     * Retrieves the text color for the video call button.
     *
     * @return The text color as an int.
     */
    public @ColorInt int getVideoCallTextColor() {
        return videoCallTextColor;
    }

    /**
     * Retrieves the text appearance resource identifier for the voice call button.
     *
     * @return The text appearance resource identifier as an int.
     */
    public @StyleRes int getVoiceCallTextAppearance() {
        return voiceCallTextAppearance;
    }

    /**
     * Retrieves the text appearance resource identifier for the video call button.
     *
     * @return The text appearance resource identifier as an int.
     */
    public @StyleRes int getVideoCallTextAppearance() {
        return videoCallTextAppearance;
    }

    /**
     * Retrieves the background color for the voice call button.
     *
     * @return The background color as an int.
     */
    public @ColorInt int getVoiceCallBackgroundColor() {
        return voiceCallBackgroundColor;
    }

    /**
     * Retrieves the background color for the video call button.
     *
     * @return The background color as an int.
     */
    public @ColorInt int getVideoCallBackgroundColor() {
        return videoCallBackgroundColor;
    }

    /**
     * Interface for handling click events on call buttons.
     */
    public interface OnClick {
        /**
         * Method called when a call button is clicked.
         *
         * @param user  The User object associated with the click event.
         * @param group The Group object associated with the click event.
         */
        void onClick(User user, Group group);
    }
}
