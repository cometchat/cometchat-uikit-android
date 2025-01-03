package com.cometchat.chatuikit.messageheader;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.cometchat.chat.constants.CometChatConstants;
import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.TypingIndicator;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.databinding.CometchatMessageHeaderBinding;
import com.cometchat.chatuikit.shared.framework.ChatConfigurator;
import com.cometchat.chatuikit.shared.interfaces.Function2;
import com.cometchat.chatuikit.shared.interfaces.Function3;
import com.cometchat.chatuikit.shared.interfaces.OnBackPress;
import com.cometchat.chatuikit.shared.models.AdditionParameter;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.cometchat.chatuikit.shared.views.cometchatstatusindicator.StatusIndicator;
import com.google.android.material.card.MaterialCardView;

import java.util.HashMap;
import java.util.Map;

/**
 * CometChatMessageHeader is a subclass of MaterialCardView which is used to
 * display the header of a chat conversation.
 */
public class CometChatMessageHeader extends MaterialCardView {
    private static final String TAG = CometChatMessageHeader.class.getSimpleName();
    private CometchatMessageHeaderBinding binding;

    private boolean disableTyping;
    private boolean disableUserPresence;

    private User user;
    private Group group;
    private View backButtonView;
    private OnBackPress onBackPress;

    private MessageHeaderViewModel messageHeaderViewModel;
    private Function3<Context, User, Group, View> customSubtitle;
    private Function3<Context, User, Group, View> tailView;
    private Function2<Context, User, String> customLastSeenText;

    private @ColorInt int titleTextColor;
    private @ColorInt int subtitleTextColor;
    private @ColorInt int backIconTint;
    private @ColorInt int backgroundColor;
    private @ColorInt int strokeColor;

    private @Dimension int cornerRadius;
    private @Dimension int strokeWidth;

    private @StyleRes int titleTextAppearance;
    private @StyleRes int subtitleTextAppearance;
    private @StyleRes int avatarStyle;
    private @StyleRes int statusIndicatorStyle;
    private @StyleRes int typingIndicatorStyle;
    private @StyleRes int callButtonsStyle;

    private Drawable backIcon;
    private AdditionParameter additionParameter;

    /**
     * Constructs a new CometChatMessageHeader with a given context.
     *
     * @param context The context in which the header is created.
     */
    public CometChatMessageHeader(@NonNull Context context) {
        this(context, null);
    }

    /**
     * Constructs a new CometChatMessageHeader with a given context and attribute
     * set.
     *
     * @param context The context in which the header is created.
     * @param attrs   The attribute set for the header.
     */
    public CometChatMessageHeader(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.cometchatMessageHeaderStyle);
    }

    /**
     * Constructs a new CometChatMessageHeader with a given context, attribute set,
     * and default style.
     *
     * @param context      The context in which the header is created.
     * @param attrs        The attribute set for the header.
     * @param defStyleAttr The default style attribute for the header.
     */
    public CometChatMessageHeader(Context context, AttributeSet attrs, int defStyleAttr) {
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
        // Inflate the layout for this view
        binding = CometchatMessageHeaderBinding.inflate(LayoutInflater.from(getContext()), this, true);
        // Reset the card view to default values
        Utils.initMaterialCard(this);
        // setting addition parameter for data source
        additionParameter = new AdditionParameter();
        // Init ViewModel
        init();
        // Apply style attributes
        applyStyleAttributes(attrs, defStyleAttr);
    }

    /**
     * Initializes the ViewModel and observes various LiveData objects for updates.
     * This method is typically called during the initialization phase of the
     * component.
     */
    private void init() {
        messageHeaderViewModel = new ViewModelProvider.NewInstanceFactory().create(MessageHeaderViewModel.class);
        messageHeaderViewModel.getMemberCount().observe((AppCompatActivity) getContext(), this::setMembersCount);
        messageHeaderViewModel.getUserPresenceStatus().observe((AppCompatActivity) getContext(), this::showUserStatusAndLastSeen);
        messageHeaderViewModel.getUpdatedGroup().observe((AppCompatActivity) getContext(), this::setGroup);
        messageHeaderViewModel.getUpdatedUser().observe((AppCompatActivity) getContext(), this::setUser);
        messageHeaderViewModel.getTyping().observe((AppCompatActivity) getContext(), this::setTypingIndicator);
        configureBackIcon();
    }

    /**
     * Applies the style attributes from XML, allowing direct attribute overrides.
     *
     * @param attrs        The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr The default style to apply to this view.
     */
    private void applyStyleAttributes(AttributeSet attrs, int defStyleAttr) {
        TypedArray directAttributes = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CometChatMessageHeader, defStyleAttr, 0);
        @StyleRes int styleResId = directAttributes.getResourceId(R.styleable.CometChatMessageHeader_cometchatMessageHeaderStyle, 0);
        directAttributes = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CometChatMessageHeader, defStyleAttr, styleResId);
        extractAttributesAndApplyDefaults(directAttributes);
    }

    /**
     * Updates the members count displayed in the message header subtitle.
     *
     * @param count The number of members to display.
     */
    private void setMembersCount(int count) {
        binding.tvMessageHeaderSubtitle.setText(count > 1 ? count + " " + getContext().getString(R.string.cometchat_members) : count + " " + getContext().getString(
            R.string.cometchat_member));
    }

    /**
     * Updates the user status and last seen time based on the provided User object.
     * If a custom last seen text function is set, it will be used to generate the
     * last seen text. Otherwise, the default last seen text will be displayed. If
     * user presence notifications are disabled, the subtitle will be hidden. If the
     * user is online, the subtitle will display "Online".
     *
     * @param mUser The User object containing the status and last seen time.
     */
    private void showUserStatusAndLastSeen(@NonNull User mUser) {
        if (customSubtitle == null) {
            if (disableUserPresence) {
                binding.tvMessageHeaderSubtitle.setVisibility(GONE);
            } else {
                if (mUser.getStatus().equals(CometChatConstants.USER_STATUS_ONLINE)) {
                    binding.tvMessageHeaderSubtitle.setText(getResources().getString(R.string.cometchat_online));
                } else {
                    if (customLastSeenText != null) {
                        binding.tvMessageHeaderSubtitle.setText(customLastSeenText.apply(getContext(), mUser));
                    } else {
                        if (mUser.getLastActiveAt() == 0) {
                            binding.tvMessageHeaderSubtitle.setText(getContext().getString(R.string.cometchat_offline));
                        } else {
                            String lastSeen = Utils.getLastSeenTime(getContext(), mUser.getLastActiveAt());
                            binding.tvMessageHeaderSubtitle.setText(lastSeen);
                            binding.tvMessageHeaderSubtitle.setSelected(true);
                        }
                    }
                }
            }
        }
    }

    /**
     * Updates the typing indicator based on the provided typing status.
     *
     * @param typingHashMap A map containing typing indicators and their corresponding status
     *                      (true for typing, false for not typing).
     */
    private void setTypingIndicator(@NonNull HashMap<TypingIndicator, Boolean> typingHashMap) {
        if (customSubtitle == null) {
            if (!disableTyping) {
                if (!typingHashMap.isEmpty()) {
                    for (Map.Entry<TypingIndicator, Boolean> map : typingHashMap.entrySet()) {
                        boolean isTyping = map.getValue();
                        TypingIndicator typingIndicator = map.getKey();
                        if (isTyping) {
                            binding.tvMessageHeaderSubtitle.setVisibility(GONE);
                            binding.tvMessageHeaderTypingIndicator.setVisibility(VISIBLE);
                            String typingMessage = typingIndicator
                                .getReceiverType()
                                .equals(CometChatConstants.RECEIVER_TYPE_USER) ? getContext().getString(R.string.cometchat_typing) : typingIndicator
                                .getSender()
                                .getName() + " " + getContext().getString(R.string.cometchat_is_typing);
                            binding.tvMessageHeaderTypingIndicator.setText(typingMessage);
                        } else {
                            binding.tvMessageHeaderSubtitle.setVisibility(VISIBLE);
                            binding.tvMessageHeaderTypingIndicator.setVisibility(GONE);
                        }
                    }
                }
            }
        }
    }

    /**
     * Configures the back icon's click behavior. When the back icon is clicked, it
     * checks if a custom onBackPressed callback is set. If so, it calls that
     * callback; otherwise, it invokes the default back button behavior.
     */
    private void configureBackIcon() {
        binding.ivMessageHeaderBack.setOnClickListener(view -> {
            if (onBackPress != null) onBackPress.onBack();
            else {
                ((Activity) getContext()).onBackPressed();
            }
        });
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
            titleTextColor = typedArray.getColor(R.styleable.CometChatMessageHeader_cometchatMessageHeaderTitleTextColor, 0);
            subtitleTextColor = typedArray.getColor(R.styleable.CometChatMessageHeader_cometchatMessageHeaderSubtitleTextColor, 0);
            backIconTint = typedArray.getColor(R.styleable.CometChatMessageHeader_cometchatMessageHeaderBackIconTint, 0);
            backgroundColor = typedArray.getColor(R.styleable.CometChatMessageHeader_cometchatMessageHeaderBackgroundColor, 0);
            strokeColor = typedArray.getColor(R.styleable.CometChatMessageHeader_cometchatMessageHeaderStrokeColor, 0);
            cornerRadius = typedArray.getDimensionPixelSize(R.styleable.CometChatMessageHeader_cometchatMessageHeaderCornerRadius, 0);
            strokeWidth = typedArray.getDimensionPixelSize(R.styleable.CometChatMessageHeader_cometchatMessageHeaderStrokeWidth, 0);
            titleTextAppearance = typedArray.getResourceId(R.styleable.CometChatMessageHeader_cometchatMessageHeaderTitleTextAppearance, 0);
            subtitleTextAppearance = typedArray.getResourceId(R.styleable.CometChatMessageHeader_cometchatMessageHeaderSubtitleTextAppearance, 0);
            avatarStyle = typedArray.getResourceId(R.styleable.CometChatMessageHeader_cometchatMessageHeaderAvatarStyle, 0);
            statusIndicatorStyle = typedArray.getResourceId(R.styleable.CometChatMessageHeader_cometchatMessageHeaderStatusIndicatorStyle, 0);
            typingIndicatorStyle = typedArray.getResourceId(R.styleable.CometChatMessageHeader_cometchatMessageHeaderTypingIndicatorStyle, 0);
            backIcon = typedArray.getDrawable(R.styleable.CometChatMessageHeader_cometchatMessageHeaderBackIcon);
            callButtonsStyle = typedArray.getResourceId(R.styleable.CometChatMessageHeader_cometchatMessageHeaderCallButtonsStyle, 0);
            // Apply default styles
            applyDefault();
        } finally {
            typedArray.recycle();
        }
    }

    /**
     * Updates the header data for the UI based on the provided Group object.
     *
     * @param group The Group object containing information to be displayed in the
     *              header.
     */
    private void setHeaderData(Group group) {
        binding.messageHeaderAvatarView.setAvatar(group.getName(), group.getIcon());
        binding.tvMessageHeaderName.setText(group.getName());
        if (customSubtitle == null) {
            setMembersCount(group.getMembersCount());
            if (group.getGroupType().equals(CometChatConstants.GROUP_TYPE_PASSWORD)) {
                binding.messageHeaderStatusIndicatorView.setStatusIndicator(StatusIndicator.PROTECTED_GROUP);
            } else if (group.getGroupType().equals(CometChatConstants.GROUP_TYPE_PRIVATE)) {
                binding.messageHeaderStatusIndicatorView.setStatusIndicator(StatusIndicator.PRIVATE_GROUP);
            } else {
                binding.messageHeaderStatusIndicatorView.setStatusIndicator(StatusIndicator.OFFLINE);
            }
        }
    }

    /**
     * Updates the header data for the UI based on the provided User object.
     *
     * @param user The User object containing information to be displayed in the
     *             header.
     */
    private void setHeaderData(User user) {
        binding.messageHeaderAvatarView.setAvatar(user.getName(), user.getAvatar());
        binding.tvMessageHeaderName.setText(user.getName());
        if (customSubtitle == null) {
            if (!Utils.isBlocked(user)) {
                binding.messageHeaderStatusIndicatorView.setVisibility(disableUserPresence ? GONE : VISIBLE);
                binding.tvMessageHeaderSubtitle.setVisibility(VISIBLE);
                binding.messageHeaderStatusIndicatorView.setStatusIndicator(StatusIndicator.OFFLINE);
                showUserStatusAndLastSeen(user);
            } else {
                binding.messageHeaderStatusIndicatorView.setVisibility(GONE);
                binding.tvMessageHeaderSubtitle.setVisibility(GONE);
            }
        }
    }

    /**
     * Applies the extracted or default values to the avatar's views.
     */
    private void applyDefault() {
        setTitleTextAppearance(titleTextAppearance);
        setSubtitleTextAppearance(subtitleTextAppearance);
        setTitleTextColor(titleTextColor);
        setSubtitleTextColor(subtitleTextColor);
        setBackIconTint(backIconTint);
        setBackgroundColor(backgroundColor);
        setStrokeColor(strokeColor);
        setCornerRadius(cornerRadius);
        setStrokeWidth(strokeWidth);
        setAvatarStyle(avatarStyle);
        setStatusIndicatorStyle(statusIndicatorStyle);
        setTypingIndicatorStyle(typingIndicatorStyle);
        setCallButtonsStyle(callButtonsStyle);
        setBackButtonView(backIcon);
    }

    /**
     * Sets the style for the CometChatMessageHeader view by applying a style
     * resource.
     *
     * @param style The style resource to apply.
     */
    public void setStyle(@StyleRes int style) {
        if (style != 0) {
            TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(style, R.styleable.CometChatMessageHeader);
            extractAttributesAndApplyDefaults(typedArray);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        messageHeaderViewModel.removeListeners();
    }

    /**
     * Sets a callback for handling back button presses.
     *
     * @param onBackPressed A callback that will be invoked when the back button is pressed.
     *                      If null, the default back button behavior will be used.
     */
    public void onBackButtonPressed(OnBackPress onBackPressed) {
        if (onBackPressed != null) {
            this.onBackPress = onBackPressed;
        }
    }

    /**
     * Retrieves the style resource for the message header call buttons.
     *
     * @return The current style resource ID for the message header call buttons.
     */
    public @StyleRes int getCallButtonsStyle() {
        return callButtonsStyle;
    }

    /**
     * Sets the style for the message header call buttons.
     *
     * @param callButtonsStyle The desired style resource for the message header call buttons.
     */
    public void setCallButtonsStyle(@StyleRes int callButtonsStyle) {
        this.callButtonsStyle = callButtonsStyle;
        additionParameter.setCallButtonStyle(callButtonsStyle);
    }

    /**
     * Retrieves the text color for the message header title.
     *
     * @return The current text color of the message header title as an integer
     * color value.
     */
    public @ColorInt int getTitleTextColor() {
        return titleTextColor;
    }

    /**
     * Sets the text color for the message header title.
     *
     * @param titleTextColor The desired color for the message header title text.
     */
    public void setTitleTextColor(@ColorInt int titleTextColor) {
        this.titleTextColor = titleTextColor;
        binding.tvMessageHeaderName.setTextColor(titleTextColor);
    }

    /**
     * Retrieves the text color for the message header subtitle.
     *
     * @return The current text color of the message header subtitle as an integer
     * color value.
     */
    public @ColorInt int getSubtitleTextColor() {
        return subtitleTextColor;
    }

    /**
     * Sets the text color for the message header subtitle.
     *
     * @param messageHeaderSubtitleTextColor The desired color for the message header subtitle text.
     */
    public void setSubtitleTextColor(@ColorInt int messageHeaderSubtitleTextColor) {
        this.subtitleTextColor = messageHeaderSubtitleTextColor;
        binding.tvMessageHeaderSubtitle.setTextColor(messageHeaderSubtitleTextColor);
    }

    /**
     * Retrieves the tint color for the message header back icon.
     *
     * @return The current tint color of the message header back icon as an integer
     * color value.
     */
    public @ColorInt int getBackIconTint() {
        return backIconTint;
    }

    /**
     * Sets the tint color for the message header back icon.
     *
     * @param backIconTint The desired tint color for the message header back icon.
     */
    public void setBackIconTint(@ColorInt int backIconTint) {
        this.backIconTint = backIconTint;
        binding.ivMessageHeaderBack.setColorFilter(backIconTint);
    }

    /**
     * Retrieves the background color for the message header.
     *
     * @return The current background color of the message header as an integer
     * color value.
     */
    public @ColorInt int getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Sets the background color for the message header.
     *
     * @param backgroundColor The desired background color for the message header.
     */
    public void setBackgroundColor(@ColorInt int backgroundColor) {
        this.backgroundColor = backgroundColor;
        setCardBackgroundColor(backgroundColor);
    }

    /**
     * Retrieves the corner radius for the message header.
     *
     * @return The current corner radius of the message header as a dimension value.
     */
    public @Dimension int getCornerRadius() {
        return cornerRadius;
    }

    /**
     * Sets the corner radius for the message header.
     *
     * @param cornerRadius The desired corner radius for the message header.
     */
    public void setCornerRadius(@Dimension int cornerRadius) {
        this.cornerRadius = cornerRadius;
        setRadius(cornerRadius);
    }

    /**
     * Retrieves the stroke width for the message header.
     *
     * @return The current stroke width of the message header as a dimension value.
     */
    public @Dimension int getStrokeWidth() {
        return strokeWidth;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (user != null) {
            setUser(messageHeaderViewModel.getUser());
        } else if (group != null) {
            setGroup(messageHeaderViewModel.getGroup());
        }
        messageHeaderViewModel.addListener();
    }

    /**
     * Retrieves the text appearance style resource for the message header title.
     *
     * @return The current text appearance style resource ID for the message header
     * title.
     */
    public @StyleRes int getTitleTextAppearance() {
        return titleTextAppearance;
    }

    /**
     * Sets the text appearance style for the message header title.
     *
     * @param titleTextAppearance The desired text appearance style resource for the message header
     *                            title.
     */
    public void setTitleTextAppearance(@StyleRes int titleTextAppearance) {
        this.titleTextAppearance = titleTextAppearance;
        binding.tvMessageHeaderName.setTextAppearance(titleTextAppearance);
    }

    /**
     * Retrieves the text appearance style resource for the message header subtitle.
     *
     * @return The current text appearance style resource ID for the message header
     * subtitle.
     */
    public @StyleRes int getSubtitleTextAppearance() {
        return subtitleTextAppearance;
    }

    /**
     * Sets the text appearance style for the message header subtitle.
     *
     * @param subtitleTextAppearance The desired text appearance style resource for the message header
     *                               subtitle.
     */
    public void setSubtitleTextAppearance(@StyleRes int subtitleTextAppearance) {
        this.subtitleTextAppearance = subtitleTextAppearance;
        binding.tvMessageHeaderSubtitle.setTextAppearance(subtitleTextAppearance);
    }

    /**
     * Retrieves the style resource for the message header avatar.
     *
     * @return The current style resource ID for the message header avatar.
     */
    public @StyleRes int getAvatarStyle() {
        return avatarStyle;
    }

    /**
     * Sets the style for the message header avatar.
     *
     * @param avatarStyle The desired style resource for the message header avatar.
     */
    public void setAvatarStyle(@StyleRes int avatarStyle) {
        this.avatarStyle = avatarStyle;
        binding.messageHeaderAvatarView.setStyle(avatarStyle);
    }

    /**
     * Retrieves the style resource for the message header status indicator.
     *
     * @return The current style resource ID for the message header status
     * indicator.
     */
    public @StyleRes int getStatusIndicatorStyle() {
        return statusIndicatorStyle;
    }

    /**
     * Sets the style for the message header status indicator.
     *
     * @param statusIndicatorStyle The desired style resource for the message header status
     *                             indicator.
     */
    public void setStatusIndicatorStyle(@StyleRes int statusIndicatorStyle) {
        this.statusIndicatorStyle = statusIndicatorStyle;
        binding.messageHeaderStatusIndicatorView.setStyle(statusIndicatorStyle);
    }

    /**
     * Retrieves the style resource for the message header typing indicator.
     *
     * @return The current style resource ID for the message header typing
     * indicator.
     */
    public @StyleRes int getTypingIndicatorStyle() {
        return typingIndicatorStyle;
    }

    /**
     * Sets the style for the message header typing indicator.
     *
     * @param typingIndicatorStyle The desired style resource for the message header typing
     *                             indicator.
     */
    public void setTypingIndicatorStyle(@StyleRes int typingIndicatorStyle) {
        this.typingIndicatorStyle = typingIndicatorStyle;
        try (TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(typingIndicatorStyle, R.styleable.CometChatTypingIndicator)) {
            binding.tvMessageHeaderTypingIndicator.setTextAppearance(typedArray.getResourceId(R.styleable.CometChatTypingIndicator_cometchatTypingIndicatorTextColor,
                                                                                              0));
            binding.tvMessageHeaderTypingIndicator.setTextColor(typedArray.getColor(R.styleable.CometChatTypingIndicator_cometchatTypingIndicatorTextColor,
                                                                                    0));
        }
    }

    /**
     * Retrieves the drawable resource for the message header back icon.
     *
     * @return The current drawable for the message header back icon.
     */
    public Drawable getBackIcon() {
        return backIcon;
    }

    /**
     * Sets the back icon view and updates its visibility.
     *
     * @param backIcon The View representing the back icon (must not be null).
     */
    public void setBackIcon(@NonNull View backIcon) {
        this.backButtonView = backIcon;
        Utils.handleView(binding.messageHeaderBackIconLayout, backIcon, true);
    }

    /**
     * Checks whether typing notifications are disabled.
     *
     * @return True if typing notifications are disabled, otherwise false.
     */
    public boolean isDisableTyping() {
        return disableTyping;
    }

    /**
     * Sets whether typing notifications should be disabled.
     *
     * @param disableTyping True to disable typing notifications, false to enable them.
     */
    public void disableTyping(boolean disableTyping) {
        this.disableTyping = disableTyping;
    }

    /**
     * Checks whether user presence notifications are disabled.
     *
     * @return True if user presence notifications are disabled, otherwise false.
     */
    public boolean isUserPresenceDisable() {
        return disableUserPresence;
    }

    /**
     * Sets whether user presence notifications should be disabled.
     *
     * @param disableUserPresence True to disable user presence notifications, false to enable them.
     */
    public void disableUserPresence(boolean disableUserPresence) {
        this.disableUserPresence = disableUserPresence;
    }

    /**
     * Retrieves the current User object.
     *
     * @return The User object representing the current user.
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the User object and updates the relevant views and data.
     *
     * @param user The User object to be set (must not be null).
     */
    public void setUser(@NonNull User user) {
        this.user = user;
        this.group = null;
        messageHeaderViewModel.setUser(user);
        if (tailView == null) {
            Utils.handleView(binding.messageHeaderTailView,
                             ChatConfigurator.getDataSource().getAuxiliaryHeaderMenu(getContext(), user, group, additionParameter),
                             true);
        }
        setHeaderData(user);
    }

    /**
     * Retrieves the current Group object.
     *
     * @return The Group object representing the current group.
     */
    public Group getGroup() {
        return group;
    }

    /**
     * Sets the Group object and updates the relevant views and data.
     *
     * @param group The Group object to be set (must not be null).
     */
    public void setGroup(@NonNull Group group) {
        this.group = group;
        this.user = null;
        messageHeaderViewModel.setGroup(group);
        if (tailView == null) {
            Utils.handleView(binding.messageHeaderTailView,
                             ChatConfigurator.getDataSource().getAuxiliaryHeaderMenu(getContext(), user, group, additionParameter),
                             true);
        } else {
            Utils.handleView(binding.messageHeaderTailView, tailView.apply(getContext(), user, group), true);
        }
        setHeaderData(group);
    }

    /**
     * Retrieves the MessageHeaderViewModel object associated with this instance.
     *
     * @return The MessageHeaderViewModel instance.
     */
    public MessageHeaderViewModel getMessageHeaderViewModel() {
        return messageHeaderViewModel;
    }

    /**
     * Retrieves the back icon view.
     *
     * @return The back icon view.
     */
    public View getBackButtonView() {
        return backButtonView;
    }

    /**
     * Sets the drawable resource for the message header back icon.
     *
     * @param messageHeaderBackIcon The desired drawable for the message header back icon.
     */
    public void setBackButtonView(Drawable messageHeaderBackIcon) {
        this.backIcon = messageHeaderBackIcon;
        if (backButtonView == null) {
            binding.ivMessageHeaderBack.setImageDrawable(messageHeaderBackIcon);
        }
    }

    /**
     * Retrieves the OnBackPress callback.
     *
     * @return The OnBackPress instance.
     */
    public OnBackPress getOnBackPress() {
        return onBackPress;
    }

    /**
     * Sets the OnBackPress callback.
     *
     * @param onBackPress The OnBackPress instance to be set.
     */
    public void setOnBackPress(OnBackPress onBackPress) {
        this.onBackPress = onBackPress;
    }

    /**
     * Retrieves the custom subtitle function.
     *
     * @return The Function3 that generates the custom subtitle view.
     */
    public Function3<Context, User, Group, View> getSubtitle() {
        return customSubtitle;
    }

    /**
     * Sets the custom subtitle function and updates the subtitle layout.
     *
     * @param subtitle The Function3 that generates the custom subtitle view (must not be
     *                 null).
     */
    public void setSubtitle(@NonNull Function3<Context, User, Group, View> subtitle) {
        this.customSubtitle = subtitle;
        Utils.handleView(binding.messageHeaderSubtitleLayout, subtitle.apply(getContext(), user, group), true);
    }

    /**
     * Retrieves the custom tail view function.
     *
     * @return The Function3 that generates the custom tail view.
     */
    public Function3<Context, User, Group, View> getTailView() {
        return tailView;
    }

    /**
     * Sets the custom tail view function and updates the tail view layout.
     *
     * @param tailView The Function3 that generates the custom tail view (must not be
     *                 null).
     */
    public void setTailView(@NonNull Function3<Context, User, Group, View> tailView) {
        this.tailView = tailView;
        Utils.handleView(binding.messageHeaderTailView, tailView.apply(getContext(), user, group), true);
    }

    /**
     * Retrieves the custom last seen text function.
     *
     * @return a Function2 that takes a Context and a User and returns a String.
     */
    public Function2<Context, User, String> getLastSeenText() {
        return customLastSeenText;
    }

    /**
     * Sets a custom last seen text function.
     *
     * @param customLastSeenText a Function2 that takes a Context and a User and returns a String.
     */
    public void setLastSeenText(@NonNull Function2<Context, User, String> customLastSeenText) {
        this.customLastSeenText = customLastSeenText;
        if (user != null) {
            showUserStatusAndLastSeen(user);
        }
    }

    /**
     * Retrieves the configured addition parameter object.
     *
     * @return the AdditionParameter.
     */
    public AdditionParameter getAdditionParameter() {
        return additionParameter;
    }

    public void setBackIconVisibility(int visibility) {
        binding.ivMessageHeaderBack.setVisibility(visibility);
    }
}
