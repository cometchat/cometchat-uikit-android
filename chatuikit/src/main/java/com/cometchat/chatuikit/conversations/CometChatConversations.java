package com.cometchat.chatuikit.conversations;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.cometchat.chat.core.ConversationsRequest;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chat.models.Conversation;
import com.cometchat.chat.models.TypingIndicator;
import com.cometchat.chatuikit.CometChatTheme;
import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.databinding.CometchatConversationsListViewBinding;
import com.cometchat.chatuikit.databinding.CometchatConversationsPopMenuBinding;
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.formatters.CometChatMentionsFormatter;
import com.cometchat.chatuikit.shared.formatters.CometChatTextFormatter;
import com.cometchat.chatuikit.shared.interfaces.Function1;
import com.cometchat.chatuikit.shared.interfaces.OnError;
import com.cometchat.chatuikit.shared.resources.soundmanager.CometChatSoundManager;
import com.cometchat.chatuikit.shared.resources.soundmanager.Sound;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.cometchat.chatuikit.shared.resources.utils.custom_dialog.CometChatConfirmDialog;
import com.cometchat.chatuikit.shared.resources.utils.recycler_touch.ClickListener;
import com.cometchat.chatuikit.shared.resources.utils.recycler_touch.RecyclerTouchListener;
import com.cometchat.chatuikit.shared.viewholders.ConversationsViewHolderListener;
import com.cometchat.chatuikit.shimmer.CometChatShimmerAdapter;
import com.cometchat.chatuikit.shimmer.CometChatShimmerUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CometChatConversations extends MaterialCardView {
    private static final String TAG = CometChatConversations.class.getSimpleName();
    private final HashMap<Conversation, Boolean> hashMap = new HashMap<>();
    private final List<CometChatTextFormatter> textFormatters = new ArrayList<>();
    private CometchatConversationsListViewBinding binding;
    private boolean showShimmer = true;
    private boolean isConversationListEmpty = true;
    private boolean hideError;
    private boolean hideToolBar = false;
    private boolean hideBackIcon = true;
    private boolean hideOverflowMenu = true;
    private boolean disableSoundForMessages;
    private String customErrorStateTitleText;
    private String customErrorStateSubtitleText;
    private String customEmptyStateTitleText;
    private String customEmptyStateSubtitleText;
    private Conversation conversationTemp;
    private ConversationsAdapter conversationsAdapter;
    /**
     * Observer for monitoring changes in the conversation list. Updates the
     * conversations adapter with the new list of conversations.
     */
    Observer<List<Conversation>> listObserver = new Observer<List<Conversation>>() {
        @Override
        public void onChanged(List<Conversation> conversations) {
            isConversationListEmpty = conversations.isEmpty();
            conversationsAdapter.setList(conversations);
        }
    };
    /**
     * Observer for updating a specific conversation in the list. Notifies the
     * adapter to refresh the item at the given position.
     */
    Observer<Integer> updateConversation = new Observer<Integer>() {
        @Override
        public void onChanged(Integer integer) {
            conversationsAdapter.notifyItemChanged(integer);
        }
    };
    /**
     * Observer for monitoring typing indicators in conversations. Updates the
     * adapter with the current typing indicators.
     */
    Observer<HashMap<Conversation, TypingIndicator>> typing = new Observer<HashMap<Conversation, TypingIndicator>>() {
        @Override
        public void onChanged(HashMap<Conversation, TypingIndicator> typingIndicatorHashMap) {
            conversationsAdapter.typing(typingIndicatorHashMap);
        }
    };
    /**
     * Observer for removing a conversation from the list. Notifies the adapter to
     * remove the item at the specified position.
     */
    Observer<Integer> remove = new Observer<Integer>() {
        @Override
        public void onChanged(Integer integer) {
            conversationsAdapter.notifyItemRemoved(integer);
        }
    };
    private CometChatSoundManager soundManager;
    private ConversationsViewModel conversationsViewModel;
    private RecyclerView.LayoutManager layoutManager;
    /**
     * Observer for inserting a new conversation at the top of the list. Notifies
     * the adapter and scrolls to the top of the list.
     */
    Observer<Integer> insertAtTop = new Observer<Integer>() {
        @Override
        public void onChanged(Integer integer) {
            conversationsAdapter.notifyItemInserted(integer);
            scrollToTop();
        }
    };
    /**
     * Observer for moving a conversation to the top of the list. Notifies the
     * adapter of data changes and scrolls to the top.
     */
    Observer<Integer> moveToTop = new Observer<Integer>() {
        @Override
        public void onChanged(Integer integer) {
            conversationsAdapter.notifyDataSetChanged();
            scrollToTop();
        }
    };
    private CometChatMentionsFormatter cometchatMentionsFormatter;
    @Nullable
    private View customEmptyView = null;
    private View customErrorView = null;
    private View customLoadingView = null;
    /**
     * Observer for handling conversation states. Depending on the state, it
     * triggers appropriate methods to handle each state.
     */
    Observer<UIKitConstants.States> stateChangeObserver = states -> {
        hideAllStates();

        switch (states) {
            case LOADING:
                handleLoadingState();
                break;
            case NON_EMPTY:
                setRecyclerViewVisibility(View.VISIBLE);
                break;
            case ERROR:
                handleErrorState();
                break;
            case EMPTY:
                handleEmptyState();
                break;
            default:
                break;
        }
    };
    private View overflowMenu = null;
    private OnError onError;
    /**
     * Observer for handling CometChat exceptions. Calls the onError callback if an
     * exception occurs.
     */
    Observer<CometChatException> cometchatExceptionObserver = exception -> {
        if (onError != null) onError.onError(getContext(), exception);
    };
    private OnSelection onSelection;
    private UIKitConstants.SelectionMode selectionMode = UIKitConstants.SelectionMode.NONE;
    private @RawRes int customSoundForMessage = 0;
    private @ColorInt int conversationsBackIconTint;
    private @ColorInt int conversationsStrokeColor;
    private @ColorInt int conversationsBackgroundColor;
    private @ColorInt int conversationsTitleTextColor;
    private @ColorInt int conversationsEmptyStateTitleTextColor;
    private @ColorInt int conversationsEmptyStateSubtitleTextColor;
    private @ColorInt int conversationsErrorStateTitleTextColor;
    private @ColorInt int conversationsErrorStateSubtitleTextColor;
    private @ColorInt int conversationsItemTitleTextColor;
    private @ColorInt int conversationsItemSubtitleTextColor;
    private @ColorInt int conversationsItemMessageTypeIconTint;
    private @Dimension int conversationsStrokeWidth;
    private @Dimension int conversationsCornerRadius;
    private @StyleRes int conversationsTitleTextAppearance;
    private @StyleRes int conversationsEmptyStateTextTitleAppearance;
    private @StyleRes int conversationsEmptyStateTextSubtitleAppearance;
    private @StyleRes int conversationsErrorStateTextTitleAppearance;
    private @StyleRes int conversationsErrorStateTextSubtitleAppearance;
    private @StyleRes int conversationsItemTitleTextAppearance;
    private @StyleRes int conversationsItemSubtitleTextAppearance;
    private @StyleRes int conversationsAvatarStyle;
    private @StyleRes int conversationsStatusIndicatorStyle;
    private @StyleRes int conversationsDateStyle;
    private @StyleRes int conversationsBadgeStyle;
    private @StyleRes int conversationsReceiptStyle;
    private @StyleRes int conversationsTypingIndicatorStyle;
    private @StyleRes int conversationsMentionsStyle;
    private @Dimension int conversationsSeparatorHeight;
    private @ColorInt int conversationsSeparatorColor;
    private Drawable conversationsBackIcon;
    private CometChatConfirmDialog deleteAlertDialog;
    /**
     * Observer for monitoring the conversation deletion state. Displays a progress
     * dialog based on the current deletion state.
     */
    Observer<UIKitConstants.DeleteState> conversationDeleteObserver = new Observer<UIKitConstants.DeleteState>() {
        @Override
        public void onChanged(UIKitConstants.DeleteState progressState) {
            if (UIKitConstants.DeleteState.SUCCESS_DELETE.equals(progressState)) {
                if (deleteAlertDialog != null) deleteAlertDialog.dismiss();
                conversationTemp = null;
            } else if (UIKitConstants.DeleteState.FAILURE_DELETE.equals(progressState)) {
                if (deleteAlertDialog != null) deleteAlertDialog.dismiss();
                Toast.makeText(getContext(), getContext().getString(R.string.cometchat_conversation_delete_error), Toast.LENGTH_SHORT).show();
            } else if (UIKitConstants.DeleteState.INITIATED_DELETE.equals(progressState)) {
                deleteAlertDialog.hidePositiveButtonProgressBar(false);
            }
        }
    };
    private OnItemClick onItemClick;
    private OnItemLongClick onItemLongClick;

    /**
     * Constructs a new CometChatConversations object with the given context.
     *
     * @param context The context of the view.
     */
    public CometChatConversations(@NonNull Context context) {
        this(context, null);
    }

    /**
     * Constructs a new CometChatConversations object with the given context and
     * attribute set.
     *
     * @param context The context of the view.
     * @param attrs   The attribute set for the view.
     */
    public CometChatConversations(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.cometchatConversationsStyle);
    }

    /**
     * Constructs a new CometChatConversations object with the given context,
     * attribute set, and default style attribute.
     *
     * @param context      The context of the view.
     * @param attrs        The attribute set for the view.
     * @param defStyleAttr The default style attribute.
     */
    public CometChatConversations(Context context, AttributeSet attrs, int defStyleAttr) {
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
        binding = CometchatConversationsListViewBinding.inflate(LayoutInflater.from(getContext()), this, true);
        // Reset the card view to default values
        Utils.initMaterialCard(this);
        // Set default values
        init();
        // Apply style attributes
        applyStyleAttributes(attrs, defStyleAttr);
    }

    /**
     * Sets the default values for the CometChatAvatar view.
     */
    private void init() {
        soundManager = new CometChatSoundManager(getContext());
        initRecyclerView();
        getDefaultMentionsFormatter();
        initViewModels();
        clickEvents();
    }

    /**
     * Applies the style attributes from XML, allowing direct attribute overrides.
     *
     * @param attrs        The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr The default style to apply to this view.
     */
    private void applyStyleAttributes(AttributeSet attrs, int defStyleAttr) {
        TypedArray directAttributes = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CometChatConversations, defStyleAttr, 0);
        @StyleRes int styleResId = directAttributes.getResourceId(R.styleable.CometChatConversations_cometchatConversationsStyle, 0);
        directAttributes = styleResId != 0 ? getContext()
            .getTheme()
            .obtainStyledAttributes(attrs, R.styleable.CometChatConversations, defStyleAttr, styleResId) : null;
        extractAttributesAndApplyDefaults(directAttributes);
    }

    /**
     * Initializes the RecyclerView with a LinearLayoutManager and sets up the
     * adapter. Disables change animations for the RecyclerView's ItemAnimator. Adds
     * a scroll listener to fetch more conversations when scrolled to the bottom.
     */
    private void initRecyclerView() {
        layoutManager = new LinearLayoutManager(getContext());
        conversationsAdapter = new ConversationsAdapter(getContext());
        RecyclerView.ItemAnimator animator = binding.recyclerviewConversationsList.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false); // Disables change animations
        }
        binding.recyclerviewConversationsList.setLayoutManager(layoutManager);
        binding.recyclerviewConversationsList.setAdapter(conversationsAdapter);
        binding.recyclerviewConversationsList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (!binding.recyclerviewConversationsList.canScrollVertically(1)) {
                    conversationsViewModel.fetchConversation();
                }
            }
        });
    }

    /**
     * Retrieves the default mentions formatter from the available text formatters
     * and adds it to the list. This method searches through the available text
     * formatters and assigns the first instance of CometChatMentionsFormatter found
     * to cometchatMentionsFormatter.
     */
    private void getDefaultMentionsFormatter() {
        for (CometChatTextFormatter textFormatter : CometChatUIKit.getDataSource().getTextFormatters(getContext())) {
            if (textFormatter instanceof CometChatMentionsFormatter) {
                cometchatMentionsFormatter = (CometChatMentionsFormatter) textFormatter;
                break;
            }
        }
        this.textFormatters.add(cometchatMentionsFormatter);
        processFormatters();
    }

    /**
     * Initializes the ViewModels for managing conversation data and observing
     * changes. Sets up observers to handle various state changes and data updates
     * related to conversations.
     */
    private void initViewModels() {
        conversationsViewModel = new ViewModelProvider.NewInstanceFactory().create(ConversationsViewModel.class);
        conversationsViewModel.getMutableConversationList().observe((AppCompatActivity) getContext(), listObserver);
        conversationsViewModel.getStates().observe((AppCompatActivity) getContext(), stateChangeObserver);
        conversationsViewModel.insertAtTop().observe((AppCompatActivity) getContext(), insertAtTop);
        conversationsViewModel.moveToTop().observe((AppCompatActivity) getContext(), moveToTop);
        conversationsViewModel.getTyping().observe((AppCompatActivity) getContext(), typing);
        conversationsViewModel.updateConversation().observe((AppCompatActivity) getContext(), updateConversation);
        conversationsViewModel.playSound().observe((AppCompatActivity) getContext(), this::playSound);
        conversationsViewModel.remove().observe((AppCompatActivity) getContext(), remove);
        conversationsViewModel.progressState().observe((AppCompatActivity) getContext(), conversationDeleteObserver);
        conversationsViewModel.getCometChatException().observe((AppCompatActivity) getContext(), cometchatExceptionObserver);
    }

    /**
     * Sets up click events for the conversations list. It adds an item touch
     * listener to the RecyclerView to handle single and multiple selection modes
     * for conversations.
     */
    private void clickEvents() {
        binding.recyclerviewConversationsList.addOnItemTouchListener(new RecyclerTouchListener(
            getContext(),
            binding.recyclerviewConversationsList,
            new ClickListener() {
                @Override
                public void onClick(View view, int position) {
                    Conversation conversation = (Conversation) view.getTag(
                        R.string.cometchat_conversation);
                    if (UIKitConstants.SelectionMode.SINGLE.equals(
                        selectionMode)) {
                        hashMap.clear();
                        hashMap.put(conversation, true);
                        conversationsAdapter.selectConversation(hashMap);
                    } else if (UIKitConstants.SelectionMode.MULTIPLE.equals(
                        selectionMode)) {
                        if (hashMap.containsKey(conversation))
                            hashMap.remove(conversation);
                        else hashMap.put(conversation, true);
                        conversationsAdapter.selectConversation(hashMap);
                    }
                    if (onItemClick != null) {
                        onItemClick.click(
                            view,
                            position,
                            conversation
                        );
                    }
                }

                @Override
                public void onLongClick(View view, int position) {
                    Conversation conversation = (Conversation) view.getTag(
                        R.string.cometchat_conversation);
                    if (onItemLongClick != null) {
                        onItemLongClick.longClick(
                            view,
                            position,
                            conversation
                        );
                    } else {
                        deleteAlertDialog = new CometChatConfirmDialog(
                            getContext(),
                            R.style.CometChatConfirmDialogStyle
                        );
                        showPopupMenu(view, position);
                    }
                }
            }
        ));
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
            // Colors
            conversationsBackIconTint = typedArray.getColor(R.styleable.CometChatConversations_cometchatConversationsBackIconTint, 0);
            conversationsStrokeColor = typedArray.getColor(R.styleable.CometChatConversations_cometchatConversationsStrokeColor, 0);
            conversationsBackgroundColor = typedArray.getColor(R.styleable.CometChatConversations_cometchatConversationsBackgroundColor, 0);
            conversationsTitleTextColor = typedArray.getColor(R.styleable.CometChatConversations_cometchatConversationsTitleTextColor, 0);
            conversationsEmptyStateTitleTextColor = typedArray.getColor(
                R.styleable.CometChatConversations_cometchatConversationsEmptyStateTitleTextColor,
                0
            );
            conversationsEmptyStateSubtitleTextColor = typedArray.getColor(
                R.styleable.CometChatConversations_cometchatConversationsEmptyStateSubtitleTextColor,
                0
            );
            conversationsErrorStateTitleTextColor = typedArray.getColor(
                R.styleable.CometChatConversations_cometchatConversationsErrorStateTitleTextColor,
                0
            );
            conversationsErrorStateSubtitleTextColor = typedArray.getColor(
                R.styleable.CometChatConversations_cometchatConversationsErrorStateSubtitleTextColor,
                0
            );
            conversationsItemTitleTextColor = typedArray.getColor(R.styleable.CometChatConversations_cometchatConversationsItemTitleTextColor, 0);
            conversationsItemSubtitleTextColor = typedArray.getColor(
                R.styleable.CometChatConversations_cometchatConversationsItemSubtitleTextColor,
                0
            );
            conversationsItemMessageTypeIconTint = typedArray.getColor(
                R.styleable.CometChatConversations_cometchatConversationsItemMessageTypeIconTint,
                0
            );
            // Dimensions
            conversationsStrokeWidth = typedArray.getDimensionPixelSize(R.styleable.CometChatConversations_cometchatConversationsStrokeWidth, 0);
            conversationsCornerRadius = typedArray.getDimensionPixelSize(R.styleable.CometChatConversations_cometchatConversationsCornerRadius, 0);
            // Styles
            conversationsTitleTextAppearance = typedArray.getResourceId(
                R.styleable.CometChatConversations_cometchatConversationsTitleTextAppearance,
                0
            );
            conversationsEmptyStateTextTitleAppearance = typedArray.getResourceId(
                R.styleable.CometChatConversations_cometchatConversationsEmptyStateTextTitleAppearance,
                0
            );
            conversationsEmptyStateTextSubtitleAppearance = typedArray.getResourceId(
                R.styleable.CometChatConversations_cometchatConversationsEmptyStateTextSubtitleAppearance,
                0
            );
            conversationsErrorStateTextTitleAppearance = typedArray.getResourceId(
                R.styleable.CometChatConversations_cometchatConversationsErrorStateTextTitleAppearance,
                0
            );
            conversationsErrorStateTextSubtitleAppearance = typedArray.getResourceId(
                R.styleable.CometChatConversations_cometchatConversationsErrorStateTextSubtitleAppearance,
                0
            );
            conversationsItemTitleTextAppearance = typedArray.getResourceId(
                R.styleable.CometChatConversations_cometchatConversationsItemTitleTextAppearance,
                0
            );
            conversationsItemSubtitleTextAppearance = typedArray.getResourceId(
                R.styleable.CometChatConversations_cometchatConversationsItemSubtitleTextAppearance,
                0
            );
            conversationsAvatarStyle = typedArray.getResourceId(R.styleable.CometChatConversations_cometchatConversationsAvatarStyle, 0);
            conversationsStatusIndicatorStyle = typedArray.getResourceId(
                R.styleable.CometChatConversations_cometchatConversationsStatusIndicatorStyle,
                0
            );
            conversationsDateStyle = typedArray.getResourceId(R.styleable.CometChatConversations_cometchatConversationsDateStyle, 0);
            conversationsBadgeStyle = typedArray.getResourceId(R.styleable.CometChatConversations_cometchatConversationsBadgeStyle, 0);
            conversationsReceiptStyle = typedArray.getResourceId(R.styleable.CometChatConversations_cometchatConversationsReceiptStyle, 0);
            conversationsTypingIndicatorStyle = typedArray.getResourceId(
                R.styleable.CometChatConversations_cometchatConversationsTypingIndicatorStyle,
                0
            );
            conversationsMentionsStyle = typedArray.getResourceId(R.styleable.CometChatConversations_cometchatConversationsMentionsStyle, 0);
            conversationsSeparatorColor = typedArray.getColor(R.styleable.CometChatConversations_cometchatConversationsSeparatorColor, 0);
            conversationsSeparatorHeight = typedArray.getDimensionPixelSize(
                R.styleable.CometChatConversations_cometchatConversationsSeparatorHeight,
                1
            );
            // Drawables
            conversationsBackIcon = typedArray.getDrawable(R.styleable.CometChatConversations_cometchatConversationsBackIcon);
            // Apply default styles
            updateUI();
        } finally {
            typedArray.recycle();
        }
    }

    /**
     * Processes the current list of text formatters and updates the conversations
     * adapter.
     */
    private void processFormatters() {
        conversationsAdapter.setTextFormatters(textFormatters);
    }

    /**
     * Plays a sound based on the provided boolean value.
     *
     * @param play true to play the sound, false otherwise.
     */
    private void playSound(Boolean play) {
        if (play) playSound();
    }

    /**
     * Displays a popup menu when an item in the RecyclerView is clicked. The menu
     * includes options for the selected conversation.
     *
     * @param anchorView The view that was clicked.
     * @param position   The position of the item in the RecyclerView.
     */
    private void showPopupMenu(View anchorView, int position) {
        CometchatConversationsPopMenuBinding popupMenuBinding = CometchatConversationsPopMenuBinding.inflate(LayoutInflater.from(getContext()));
        final PopupWindow popupWindow = new PopupWindow(
            popupMenuBinding.getRoot(),
            getResources().getDimensionPixelSize(com.cometchat.chatuikit.R.dimen.cometchat_160dp),
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        );

        popupMenuBinding.tvLogout.setOnClickListener(view -> {
            Conversation conversation = conversationsAdapter.getConversation(position);
            showDeleteConversationAlertDialog(conversation);
            popupWindow.dismiss();
        });

        popupWindow.setElevation(10);

        int endMargin = getResources().getDimensionPixelSize(com.cometchat.chatuikit.R.dimen.cometchat_margin_2);
        int anchorWidth = anchorView.getWidth();
        int offsetX = anchorWidth - popupWindow.getWidth() - endMargin;
        int offsetY = 0;
        popupWindow.showAsDropDown(anchorView, offsetX, offsetY);
    }

    /**
     * Applies the extracted or default values to the avatar's views.
     */
    private void updateUI() {
        // Styles
        setConversationsTitleTextAppearance(conversationsTitleTextAppearance);
        setConversationsEmptyStateTextTitleAppearance(conversationsEmptyStateTextTitleAppearance);
        setConversationsEmptyStateTextSubtitleAppearance(conversationsEmptyStateTextSubtitleAppearance);
        setConversationsErrorStateTextTitleAppearance(conversationsErrorStateTextTitleAppearance);
        setConversationsErrorStateTextSubtitleAppearance(conversationsErrorStateTextSubtitleAppearance);
        setConversationsItemTitleTextAppearance(conversationsItemTitleTextAppearance);
        setConversationsItemSubtitleTextAppearance(conversationsItemSubtitleTextAppearance);
        setConversationsAvatarStyle(conversationsAvatarStyle);
        setConversationsStatusIndicatorStyle(conversationsStatusIndicatorStyle);
        setConversationsDateStyle(conversationsDateStyle);
        setConversationsBadgeStyle(conversationsBadgeStyle);
        setConversationsReceiptStyle(conversationsReceiptStyle);
        setConversationsTypingIndicatorStyle(conversationsTypingIndicatorStyle);
        setConversationsMentionsStyle(conversationsMentionsStyle);
        // Colors
        setConversationsBackIconTint(conversationsBackIconTint);
        setConversationsStrokeColor(conversationsStrokeColor);
        setConversationsBackgroundColor(conversationsBackgroundColor);
        setConversationsTitleTextColor(conversationsTitleTextColor);
        setConversationsEmptyStateTitleTextColor(conversationsEmptyStateTitleTextColor);
        setConversationsEmptyStateSubtitleTextColor(conversationsEmptyStateSubtitleTextColor);
        setConversationsErrorStateTitleTextColor(conversationsErrorStateTitleTextColor);
        setConversationsErrorStateSubtitleTextColor(conversationsErrorStateSubtitleTextColor);
        setConversationsItemTitleTextColor(conversationsItemTitleTextColor);
        setConversationsItemSubtitleTextColor(conversationsItemSubtitleTextColor);
        setConversationsItemMessageTypeIconTint(conversationsItemMessageTypeIconTint);
        setConversationsSeparatorHeight(conversationsSeparatorHeight);
        setConversationsSeparatorColor(conversationsSeparatorColor);
        // Dimensions
        setConversationsStrokeWidth(conversationsStrokeWidth);
        setConversationsCornerRadius(conversationsCornerRadius);
        // Drawables
        setConversationsBackIcon(conversationsBackIcon);

        // View Handing
        hideToolBar(hideToolBar);
        hideBackIcon(hideBackIcon);
        hideOverflowMenu(hideOverflowMenu);
    }

    /**
     * Plays a sound for incoming messages if sound is not disabled.
     */
    private void playSound() {
        if (!disableSoundForMessages) soundManager.play(Sound.incomingMessageFromOther, customSoundForMessage);
    }

    private void showDeleteConversationAlertDialog(Conversation conversation) {
        deleteAlertDialog.setConfirmDialogIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.cometchat_ic_delete, null));
        deleteAlertDialog.setTitleText(getContext().getString(R.string.cometchat_conversation_delete_message_title));
        deleteAlertDialog.setSubtitleText(getContext().getString(R.string.cometchat_conversation_delete_message_subtitle));
        deleteAlertDialog.setPositiveButtonText(getContext().getString(R.string.cometchat_delete));
        deleteAlertDialog.setNegativeButtonText(getContext().getString(R.string.cometchat_cancel));
        deleteAlertDialog.setOnPositiveButtonClick(v -> {
            conversationTemp = conversation;
            conversationsViewModel.deleteConversation(conversation);
        });
        deleteAlertDialog.setOnNegativeButtonClick(v -> deleteAlertDialog.dismiss());
        deleteAlertDialog.setConfirmDialogElevation(0);
        deleteAlertDialog.setCancelable(false);
        deleteAlertDialog.show();
    }

    /**
     * Sets the style resource ID for the conversations date.
     *
     * @param conversationsDateStyle the style resource ID to use for the date.
     */
    public void setConversationsDateStyle(@StyleRes int conversationsDateStyle) {
        this.conversationsDateStyle = conversationsDateStyle;
        conversationsAdapter.setConversationsDateStyle(conversationsDateStyle);
    }

    /**
     * Hides or shows the toolbar based on the provided boolean value.
     *
     * @param hideToolBar true to hide the toolbar, false to show it.
     */
    public void hideToolBar(boolean hideToolBar) {
        this.hideToolBar = hideToolBar;
        binding.toolbarLayout.setVisibility(hideToolBar ? GONE : VISIBLE);
    }

    /**
     * Hides or shows the back icon based on the provided boolean value.
     *
     * @param hideBackIcon true to hide the back icon, false to show it.
     */
    public void hideBackIcon(boolean hideBackIcon) {
        this.hideBackIcon = hideBackIcon;
        binding.toolbarBackIcon.setVisibility(hideBackIcon ? GONE : VISIBLE);
    }

    /**
     * Hides or shows the overflow menu based on the provided boolean value.
     *
     * @param hideOverflowMenu true to hide the overflow menu, false to show it.
     */
    public void hideOverflowMenu(boolean hideOverflowMenu) {
        this.hideOverflowMenu = hideOverflowMenu;
        binding.toolbarOverflowMenu.setVisibility(hideOverflowMenu ? GONE : VISIBLE);
    }

    /**
     * Sets the style for the CometChatConversations view by applying a style
     * resource.
     *
     * @param style The style resource to apply.
     */
    public void setStyle(@StyleRes int style) {
        if (style != 0) {
            TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(style, R.styleable.CometChatConversations);
            extractAttributesAndApplyDefaults(typedArray);
        }
    }

    /**
     * Gets the tint color of the back icon in the conversations toolbar.
     *
     * @return the color used for tinting the back icon.
     */
    public @ColorInt int getConversationsBackIconTint() {
        return conversationsBackIconTint;
    }

    /**
     * Sets the tint color for the back icon in the conversations toolbar.
     *
     * @param conversationsBackIconTint the color to tint the back icon.
     */
    public void setConversationsBackIconTint(@ColorInt int conversationsBackIconTint) {
        this.conversationsBackIconTint = conversationsBackIconTint;
        binding.toolbarBackIcon.setImageTintList(ColorStateList.valueOf(conversationsBackIconTint));
    }

    /**
     * Gets the stroke color for the conversations card.
     *
     * @return the stroke color used for the conversations card.
     */
    public @ColorInt int getConversationsStrokeColor() {
        return conversationsStrokeColor;
    }

    /**
     * Sets the stroke color for the conversations card.
     *
     * @param conversationsStrokeColor the color to use for the card's stroke.
     */
    public void setConversationsStrokeColor(@ColorInt int conversationsStrokeColor) {
        this.conversationsStrokeColor = conversationsStrokeColor;
        setStrokeColor(conversationsStrokeColor);
    }

    /**
     * Gets the background color for the conversations card.
     *
     * @return the background color used for the conversations card.
     */
    public @ColorInt int getConversationsBackgroundColor() {
        return conversationsBackgroundColor;
    }

    /**
     * Sets the background color for the conversations card.
     *
     * @param conversationsBackgroundColor the color to use for the card's background.
     */
    public void setConversationsBackgroundColor(@ColorInt int conversationsBackgroundColor) {
        this.conversationsBackgroundColor = conversationsBackgroundColor;
        setCardBackgroundColor(conversationsBackgroundColor);
    }

    /**
     * Gets the text color for the title in the conversations toolbar.
     *
     * @return the text color used for the title.
     */
    public @ColorInt int getConversationsTitleTextColor() {
        return conversationsTitleTextColor;
    }

    /**
     * Sets the text color for the title in the conversations toolbar.
     *
     * @param conversationsTitleTextColor the color to use for the title text.
     */
    public void setConversationsTitleTextColor(@ColorInt int conversationsTitleTextColor) {
        this.conversationsTitleTextColor = conversationsTitleTextColor;
        binding.toolbarTitle.setTextColor(conversationsTitleTextColor);
    }

    /**
     * Gets the text color for the title in the empty state of conversations.
     *
     * @return the text color used for the empty state title.
     */
    public @ColorInt int getConversationsEmptyStateTitleTextColor() {
        return conversationsEmptyStateTitleTextColor;
    }

    /**
     * Sets the text color for the title in the empty state of conversations.
     *
     * @param conversationsEmptyStateTitleTextColor the color to use for the empty state title text.
     */
    public void setConversationsEmptyStateTitleTextColor(@ColorInt int conversationsEmptyStateTitleTextColor) {
        this.conversationsEmptyStateTitleTextColor = conversationsEmptyStateTitleTextColor;
        binding.tvEmptyConversationsTitle.setTextColor(conversationsEmptyStateTitleTextColor);
    }

    /**
     * Gets the text color for the subtitle in the empty state of conversations.
     *
     * @return the text color used for the empty state subtitle.
     */
    public @ColorInt int getConversationsEmptyStateSubtitleTextColor() {
        return conversationsEmptyStateSubtitleTextColor;
    }

    /**
     * Sets the text color for the subtitle in the empty state of conversations.
     *
     * @param conversationsEmptyStateSubtitleTextColor the color to use for the empty state subtitle text.
     */
    public void setConversationsEmptyStateSubtitleTextColor(@ColorInt int conversationsEmptyStateSubtitleTextColor) {
        this.conversationsEmptyStateSubtitleTextColor = conversationsEmptyStateSubtitleTextColor;
        binding.tvEmptyConversationsSubtitle.setTextColor(conversationsEmptyStateSubtitleTextColor);
    }

    /**
     * Gets the text color for the title in the error state of conversations.
     *
     * @return the text color used for the error state title.
     */
    public @ColorInt int getConversationsErrorStateTitleTextColor() {
        return conversationsErrorStateTitleTextColor;
    }

    /**
     * Sets the text color for the title in the error state of conversations.
     *
     * @param conversationsErrorStateTitleTextColor the color to use for the error state title text.
     */
    public void setConversationsErrorStateTitleTextColor(@ColorInt int conversationsErrorStateTitleTextColor) {
        this.conversationsErrorStateTitleTextColor = conversationsErrorStateTitleTextColor;
        binding.tvErrorTitle.setTextColor(conversationsErrorStateTitleTextColor);
    }

    /**
     * Gets the text color for the subtitle in the error state of conversations.
     *
     * @return the text color used for the error state subtitle.
     */
    public @ColorInt int getConversationsErrorStateSubtitleTextColor() {
        return conversationsErrorStateSubtitleTextColor;
    }

    /**
     * Sets the text color for the subtitle in the error state of conversations.
     *
     * @param conversationsErrorStateSubtitleTextColor the color to use for the error state subtitle text.
     */
    public void setConversationsErrorStateSubtitleTextColor(@ColorInt int conversationsErrorStateSubtitleTextColor) {
        this.conversationsErrorStateSubtitleTextColor = conversationsErrorStateSubtitleTextColor;
        binding.tvErrorSubtitle.setTextColor(conversationsErrorStateSubtitleTextColor);
    }

    /**
     * Gets the text color for the item title in the conversations list.
     *
     * @return the text color used for item titles.
     */
    public @ColorInt int getConversationsItemTitleTextColor() {
        return conversationsItemTitleTextColor;
    }

    /**
     * Sets the text color for item titles in the conversations list.
     *
     * @param conversationsItemTitleTextColor the color to use for item title text.
     */
    public void setConversationsItemTitleTextColor(@ColorInt int conversationsItemTitleTextColor) {
        this.conversationsItemTitleTextColor = conversationsItemTitleTextColor;
        conversationsAdapter.setConversationsItemTitleTextColor(conversationsItemTitleTextColor);
    }

    /**
     * Gets the text color for item subtitles in the conversations list.
     *
     * @return the text color used for item subtitles.
     */
    public @ColorInt int getConversationsItemSubtitleTextColor() {
        return conversationsItemSubtitleTextColor;
    }

    /**
     * Sets the text color for item subtitles in the conversations list.
     *
     * @param conversationsItemSubtitleTextColor the color to use for item subtitle text.
     */
    public void setConversationsItemSubtitleTextColor(@ColorInt int conversationsItemSubtitleTextColor) {
        this.conversationsItemSubtitleTextColor = conversationsItemSubtitleTextColor;
        conversationsAdapter.setConversationsItemSubtitleTextColor(conversationsItemSubtitleTextColor);
    }

    /**
     * Gets the tint color for the message type icon in the conversations list.
     *
     * @return the tint color used for message type icons.
     */
    public @ColorInt int getConversationsItemMessageTypeIconTint() {
        return conversationsItemMessageTypeIconTint;
    }

    /**
     * Sets the tint color for the message type icon in the conversations list.
     *
     * @param conversationsItemMessageTypeIconTint the color to use for the message type icon tint.
     */
    public void setConversationsItemMessageTypeIconTint(@ColorInt int conversationsItemMessageTypeIconTint) {
        this.conversationsItemMessageTypeIconTint = conversationsItemMessageTypeIconTint;
        conversationsAdapter.setConversationsItemMessageTypeIconTint(conversationsItemMessageTypeIconTint);
    }

    /**
     * Gets the color of the conversations separator.
     *
     * @return the color of the separator as an integer annotated with @ColorInt.
     */
    public @ColorInt int getConversationsSeparatorColor() {
        return conversationsSeparatorColor;
    }

    /**
     * Sets the color of the conversations separator.
     *
     * @param conversationsSeparatorColor the color to set for the separator, annotated with @ColorInt.
     */
    public void setConversationsSeparatorColor(@ColorInt int conversationsSeparatorColor) {
        this.conversationsSeparatorColor = conversationsSeparatorColor;
        binding.viewSeparator.setBackgroundColor(conversationsSeparatorColor);
    }

    /**
     * Gets the height of the conversations separator.
     *
     * @return the height of the separator as an integer annotated with @Dimension.
     */
    public @Dimension int getConversationsSeparatorHeight() {
        return conversationsSeparatorHeight;
    }

    /**
     * Sets the height of the conversations separator.
     *
     * @param conversationsSeparatorHeight the height to set for the separator, annotated with @Dimension.
     */
    public void setConversationsSeparatorHeight(@Dimension int conversationsSeparatorHeight) {
        this.conversationsSeparatorHeight = conversationsSeparatorHeight;
        binding.viewSeparator.getLayoutParams().height = conversationsSeparatorHeight;
    }

    /**
     * Gets the stroke width for the conversations card.
     *
     * @return the stroke width used for the conversations card.
     */
    public @Dimension int getConversationsStrokeWidth() {
        return conversationsStrokeWidth;
    }

    /**
     * Sets the stroke width for the conversations card.
     *
     * @param conversationsStrokeWidth the width to use for the card's stroke.
     */
    public void setConversationsStrokeWidth(@Dimension int conversationsStrokeWidth) {
        this.conversationsStrokeWidth = conversationsStrokeWidth;
        setStrokeWidth(conversationsStrokeWidth);
    }

    /**
     * Gets the corner radius for the conversations card.
     *
     * @return the corner radius used for the conversations card.
     */
    public @Dimension int getConversationsCornerRadius() {
        return conversationsCornerRadius;
    }

    /**
     * Sets the corner radius for the conversations card.
     *
     * @param conversationsCornerRadius the radius to use for the card's corners.
     */
    public void setConversationsCornerRadius(@Dimension int conversationsCornerRadius) {
        this.conversationsCornerRadius = conversationsCornerRadius;
        super.setRadius(conversationsCornerRadius);
    }

    /**
     * Gets the text appearance style resource ID for the title in the conversations
     * toolbar.
     *
     * @return the style resource ID used for the title text appearance.
     */
    public @StyleRes int getConversationsTitleTextAppearance() {
        return conversationsTitleTextAppearance;
    }

    /**
     * Sets the text appearance style for the title in the conversations toolbar.
     *
     * @param conversationsTitleTextAppearance the style resource ID to use for the title text appearance.
     */
    public void setConversationsTitleTextAppearance(@StyleRes int conversationsTitleTextAppearance) {
        this.conversationsTitleTextAppearance = conversationsTitleTextAppearance;
        binding.toolbarTitle.setTextAppearance(conversationsTitleTextAppearance);
    }

    /**
     * Gets the text appearance style resource ID for the title in the empty state
     * of conversations.
     *
     * @return the style resource ID used for the empty state title text appearance.
     */
    public @StyleRes int getConversationsEmptyStateTextTitleAppearance() {
        return conversationsEmptyStateTextTitleAppearance;
    }

    /**
     * Sets the text appearance style for the title in the empty state of
     * conversations.
     *
     * @param conversationsEmptyStateTextTitleAppearance the style resource ID to use for the empty state title text
     *                                                   appearance.
     */
    public void setConversationsEmptyStateTextTitleAppearance(@StyleRes int conversationsEmptyStateTextTitleAppearance) {
        this.conversationsEmptyStateTextTitleAppearance = conversationsEmptyStateTextTitleAppearance;
        binding.tvEmptyConversationsTitle.setTextAppearance(conversationsEmptyStateTextTitleAppearance);
    }

    /**
     * Gets the text appearance style resource ID for the subtitle in the empty
     * state of conversations.
     *
     * @return the style resource ID used for the empty state subtitle text
     * appearance.
     */
    public @StyleRes int getConversationsEmptyStateTextSubtitleAppearance() {
        return conversationsEmptyStateTextSubtitleAppearance;
    }

    /**
     * Sets the text appearance style for the subtitle in the empty state of
     * conversations.
     *
     * @param conversationsEmptyStateTextSubtitleAppearance the style resource ID to use for the empty state subtitle text
     *                                                      appearance.
     */
    public void setConversationsEmptyStateTextSubtitleAppearance(@StyleRes int conversationsEmptyStateTextSubtitleAppearance) {
        this.conversationsEmptyStateTextSubtitleAppearance = conversationsEmptyStateTextSubtitleAppearance;
        binding.tvEmptyConversationsSubtitle.setTextAppearance(conversationsEmptyStateTextSubtitleAppearance);
    }

    /**
     * Gets the text appearance style resource ID for the title in the error state
     * of conversations.
     *
     * @return the style resource ID used for the error state title text appearance.
     */
    public @StyleRes int getConversationsErrorStateTextTitleAppearance() {
        return conversationsErrorStateTextTitleAppearance;
    }

    /**
     * Sets the text appearance style for the title in the error state of
     * conversations.
     *
     * @param conversationsErrorStateTextTitleAppearance the style resource ID to use for the error state title text
     *                                                   appearance.
     */
    public void setConversationsErrorStateTextTitleAppearance(@StyleRes int conversationsErrorStateTextTitleAppearance) {
        this.conversationsErrorStateTextTitleAppearance = conversationsErrorStateTextTitleAppearance;
        binding.tvErrorTitle.setTextAppearance(conversationsErrorStateTextTitleAppearance);
    }

    /**
     * Gets the text appearance style resource ID for the subtitle in the error
     * state of conversations.
     *
     * @return the style resource ID used for the error state subtitle text
     * appearance.
     */
    public @StyleRes int getConversationsErrorStateTextSubtitleAppearance() {
        return conversationsErrorStateTextSubtitleAppearance;
    }

    /**
     * Sets the text appearance style for the subtitle in the error state of
     * conversations.
     *
     * @param conversationsErrorStateTextSubtitleAppearance the style resource ID to use for the error state subtitle text
     *                                                      appearance.
     */
    public void setConversationsErrorStateTextSubtitleAppearance(@StyleRes int conversationsErrorStateTextSubtitleAppearance) {
        this.conversationsErrorStateTextSubtitleAppearance = conversationsErrorStateTextSubtitleAppearance;
        binding.tvErrorSubtitle.setTextAppearance(conversationsErrorStateTextSubtitleAppearance);
    }

    /**
     * Gets the text appearance style resource ID for the title in the conversations
     * item.
     *
     * @return the style resource ID used for item title text appearance.
     */
    public @StyleRes int getConversationsItemTitleTextAppearance() {
        return conversationsItemTitleTextAppearance;
    }

    /**
     * Sets the text appearance style for the title in the conversations item.
     *
     * @param conversationsItemTitleTextAppearance the style resource ID to use for the item title text appearance.
     */
    public void setConversationsItemTitleTextAppearance(@StyleRes int conversationsItemTitleTextAppearance) {
        this.conversationsItemTitleTextAppearance = conversationsItemTitleTextAppearance;
        conversationsAdapter.setConversationsItemTitleTextAppearance(conversationsItemTitleTextAppearance);
    }

    /**
     * Gets the text appearance style resource ID for the subtitle in the
     * conversations item.
     *
     * @return the style resource ID used for item subtitle text appearance.
     */
    public @StyleRes int getConversationsItemSubtitleTextAppearance() {
        return conversationsItemSubtitleTextAppearance;
    }

    /**
     * Sets the text appearance style for the subtitle in the conversations item.
     *
     * @param conversationsItemSubtitleTextAppearance the style resource ID to use for the item subtitle text
     *                                                appearance.
     */
    public void setConversationsItemSubtitleTextAppearance(@StyleRes int conversationsItemSubtitleTextAppearance) {
        this.conversationsItemSubtitleTextAppearance = conversationsItemSubtitleTextAppearance;
        conversationsAdapter.setConversationsItemSubtitleTextAppearance(conversationsItemSubtitleTextAppearance);
    }

    /**
     * Gets the style resource ID for the conversations avatar.
     *
     * @return the style resource ID used for the avatar.
     */
    public @StyleRes int getConversationsAvatarStyle() {
        return conversationsAvatarStyle;
    }

    /**
     * Sets the style resource ID for the conversations avatar.
     *
     * @param conversationsAvatarStyle the style resource ID to use for the avatar.
     */
    public void setConversationsAvatarStyle(@StyleRes int conversationsAvatarStyle) {
        this.conversationsAvatarStyle = conversationsAvatarStyle;
        conversationsAdapter.setConversationsAvatarStyle(conversationsAvatarStyle);
    }

    /**
     * Gets the style resource ID for the conversations status indicator.
     *
     * @return the style resource ID used for the status indicator.
     */
    public @StyleRes int getConversationsStatusIndicatorStyle() {
        return conversationsStatusIndicatorStyle;
    }

    /**
     * Sets the style resource ID for the conversations status indicator.
     *
     * @param conversationsStatusIndicatorStyle the style resource ID to use for the status indicator.
     */
    public void setConversationsStatusIndicatorStyle(@StyleRes int conversationsStatusIndicatorStyle) {
        this.conversationsStatusIndicatorStyle = conversationsStatusIndicatorStyle;
        conversationsAdapter.setConversationsStatusIndicatorStyle(conversationsStatusIndicatorStyle);
    }

    /**
     * Gets the style resource ID for the conversations badge.
     *
     * @return the style resource ID used for the badge.
     */
    public @StyleRes int getConversationsBadgeStyle() {
        return conversationsBadgeStyle;
    }

    /**
     * Sets the style resource ID for the conversations badge.
     *
     * @param conversationsBadgeStyle the style resource ID to use for the badge.
     */
    public void setConversationsBadgeStyle(@StyleRes int conversationsBadgeStyle) {
        this.conversationsBadgeStyle = conversationsBadgeStyle;
        conversationsAdapter.setConversationsBadgeStyle(conversationsBadgeStyle);
    }

    /**
     * Gets the style resource ID for the conversations receipt.
     *
     * @return the style resource ID used for the receipt.
     */
    public @StyleRes int getConversationsReceiptStyle() {
        return conversationsReceiptStyle;
    }

    /**
     * Sets the style resource ID for the conversations receipt.
     *
     * @param conversationsReceiptStyle the style resource ID to use for the receipt.
     */
    public void setConversationsReceiptStyle(@StyleRes int conversationsReceiptStyle) {
        this.conversationsReceiptStyle = conversationsReceiptStyle;
        conversationsAdapter.setConversationsReceiptStyle(conversationsReceiptStyle);
    }

    /**
     * Gets the style resource ID for the typing indicator in conversations.
     *
     * @return the style resource ID used for the typing indicator.
     */
    public @StyleRes int getConversationsTypingIndicatorStyle() {
        return conversationsTypingIndicatorStyle;
    }

    /**
     * Sets the style resource ID for the typing indicator in conversations.
     *
     * @param conversationsTypingIndicatorStyle the style resource ID to use for the typing indicator.
     */
    public void setConversationsTypingIndicatorStyle(@StyleRes int conversationsTypingIndicatorStyle) {
        this.conversationsTypingIndicatorStyle = conversationsTypingIndicatorStyle;
        conversationsAdapter.setConversationsTypingIndicatorStyle(conversationsTypingIndicatorStyle);
    }

    /**
     * Gets the style resource ID for conversation mentions.
     *
     * @return the style resource ID used for conversation mentions.
     */
    public @StyleRes int getConversationsMentionsStyle() {
        return conversationsMentionsStyle;
    }

    /**
     * Sets the style resource ID for conversation mentions.
     *
     * @param conversationsMentionsStyle the style resource ID to use for conversation mentions.
     */
    public void setConversationsMentionsStyle(@StyleRes int conversationsMentionsStyle) {
        this.conversationsMentionsStyle = conversationsMentionsStyle;
        cometchatMentionsFormatter.setConversationsMentionTextStyle(getContext(), conversationsMentionsStyle);
        conversationsAdapter.notifyDataSetChanged();
    }

    /**
     * Gets the drawable for the back icon in conversations.
     *
     * @return the drawable used for the back icon.
     */
    public Drawable getConversationsBackIcon() {
        return conversationsBackIcon;
    }

    /**
     * Sets the drawable for the back icon in conversations.
     *
     * @param conversationsBackIcon the drawable to use for the back icon.
     */
    public void setConversationsBackIcon(Drawable conversationsBackIcon) {
        this.conversationsBackIcon = conversationsBackIcon;
        binding.toolbarBackIcon.setImageDrawable(conversationsBackIcon);
    }

    /**
     * Sets the list of text formatters to use for formatting messages.
     *
     * @param cometchatTextFormatters the list of text formatters to apply.
     */
    public void setTextFormatters(List<CometChatTextFormatter> cometchatTextFormatters) {
        if (cometchatTextFormatters != null) {
            this.textFormatters.addAll(cometchatTextFormatters);
            processFormatters();
        }
    }

    /**
     * Enables or disables mentions in the conversations.
     *
     * @param disable true to disable mentions, false to enable.
     */
    public void setDisableMentions(boolean disable) {
        if (disable) {
            textFormatters.remove(cometchatMentionsFormatter);
            processFormatters();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        processFormatters();
        conversationsViewModel.addListener();
        conversationsViewModel.fetchConversation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        conversationsViewModel.removeListener();
    }

    /**
     * Sets the layout resource for the empty state view.
     *
     * @param id The layout resource ID for the empty state view.
     */
    public void setEmptyStateView(@LayoutRes int id) {
        if (id != 0) {
            try {
                customEmptyView = View.inflate(getContext(), id, null);
            } catch (Exception e) {
                customEmptyView = null;
            }
        }
    }

    /**
     * Sets the layout resource for the error state view.
     *
     * @param id The layout resource ID for the error state view.
     */
    public void setErrorStateView(@LayoutRes int id) {
        if (id != 0) {
            try {
                customErrorView = View.inflate(getContext(), id, null);
            } catch (Exception e) {
                customErrorView = null;
            }
        }
    }

    /**
     * Sets the layout resource for the loading state view.
     *
     * @param id The layout resource ID for the loading state view.
     */
    public void setLoadingStateView(@LayoutRes int id) {
        if (id != 0) {
            try {
                customLoadingView = View.inflate(getContext(), id, null);
            } catch (Exception e) {
                customLoadingView = null;
            }
        }
    }

    /**
     * Hides or shows the error state message.
     *
     * @param hideError true to hide the error state message, false to show it.
     */
    public void hideError(boolean hideError) {
        this.hideError = hideError;
    }

    /**
     * Disables or enables sound for incoming messages.
     *
     * @param disableSoundForMessages true to disable sound for incoming messages, false to enable it.
     */
    public void disableSoundForMessages(boolean disableSoundForMessages) {
        this.disableSoundForMessages = disableSoundForMessages;
    }

    /**
     * Sets a custom sound for incoming messages.
     *
     * @param customSoundForMessages The resource ID of the custom sound for incoming messages.
     */
    public void setCustomSoundForMessages(@RawRes int customSoundForMessages) {
        this.customSoundForMessage = customSoundForMessages;
    }

    /**
     * Disables or enables users' presence indicators in the conversations view.
     *
     * @param disableUsersPresence true to disable users' presence indicators, false to enable them.
     */
    public void disableUsersPresence(boolean disableUsersPresence) {
        conversationsAdapter.setDisableUsersPresence(disableUsersPresence);
    }

    /**
     * Sets the callback for handling errors in the conversations view.
     *
     * @param onError The OnError callback for handling errors.
     */
    public void setOnError(OnError onError) {
        this.onError = onError;
    }

    /**
     * Hide or show the read receipt in the conversations view.
     *
     * @param hideReceipt true to hide the read receipt, false to show it.
     */
    public void hideReceipt(boolean hideReceipt) {
        conversationsAdapter.disableReadReceipt(hideReceipt);
    }

    /**
     * Disables or enables the typing indicator in the conversations view.
     *
     * @param hideTyping true to disable the typing indicator, false to enable it.
     */
    public void hideTyping(boolean hideTyping) {
        conversationsAdapter.disableTyping(hideTyping);
    }

    /**
     * Sets the date pattern for displaying dates in the conversations view.
     *
     * @param datePattern The function that formats the date pattern for conversations.
     */
    public void setDatePattern(Function1<Conversation, String> datePattern) {
        conversationsAdapter.setDatePattern(datePattern);
    }

    /**
     * Sets the custom view for the subtitle area within conversation items in the
     * list.
     *
     * @param subtitleView The listener interface that defines callbacks for interactions
     *                     with the subtitle view.
     *                     <p>
     *                     This method allows you to specify a custom view to be displayed
     *                     below the main title or name of each conversation item in the
     *                     list. The provided `ConversationsViewHolderListener` interface
     *                     defines callbacks that will be invoked when various interactions
     *                     occur with the subtitle view, similar to the subtitle views in
     *                     user and group lists.
     *                     <p>
     *                     By implementing the `ConversationsViewHolderListener` interface
     *                     and passing an instance to this method, you can customize the
     *                     appearance and behavior of the subtitle area within each
     *                     conversation item according to your specific needs.
     */
    public void setSubtitleView(ConversationsViewHolderListener subtitleView) {
        conversationsAdapter.setSubtitleView(subtitleView);
    }

    /**
     * Sets the custom view for the tail element at the end of the conversation
     * list.
     *
     * @param tailView The listener interface that defines callbacks for interactions
     *                 with the tail view.
     *                 <p>
     *                 This method allows you to specify a custom view to be displayed at
     *                 the end of the conversation list item.
     *                 <p>
     *                 The provided `ConversationsViewHolderListener` interface defines
     *                 callbacks that will be invoked when various interactions occur
     *                 with the tail view, allowing you to customize its behavior based
     *                 on user actions.
     */
    public void setTailView(ConversationsViewHolderListener tailView) {
        conversationsAdapter.setTailView(tailView);
    }

    /**
     * Sets the custom view for each conversation item in the list.
     *
     * @param viewHolderListener The listener interface that defines callbacks for interactions
     *                           with the conversation list item view.
     *                           <p>
     *                           This method allows you to specify a custom view to be used for
     *                           each item in the conversation list. The provided
     *                           `ConversationsViewHolderListener` interface defines callbacks that
     *                           will be invoked when various interactions occur with the list item
     *                           view, such as:
     *                           <p>
     *                           * Clicking on the conversation item * Long pressing on the
     *                           conversation item * Triggering other actions specific to the
     *                           conversation item view
     *                           <p>
     *                           By implementing the `ConversationsViewHolderListener` interface
     *                           and passing an instance to this method, you can customize the
     *                           appearance and behavior of each conversation item in the list
     *                           according to your specific needs.
     */
    public void setListItemView(ConversationsViewHolderListener viewHolderListener) {
        conversationsAdapter.setListItemView(viewHolderListener);
    }

    /**
     * Sets the listener for conversation item selection events.
     *
     * @param onSelection The listener to handle conversation item selection events.
     */
    public void setOnSelection(OnSelection onSelection) {
        this.onSelection = onSelection;
    }

    /**
     * Sets the selection mode for conversations.
     *
     * @param selectionMode The selection mode to be applied to conversations.
     */
    public void setSelectionMode(UIKitConstants.SelectionMode selectionMode) {
        hashMap.clear();
        conversationsAdapter.selectConversation(hashMap);
        this.selectionMode = selectionMode;
        if (!UIKitConstants.SelectionMode.NONE.equals(selectionMode) && selectionMode != null) {
            setOverflowMenu();
        }
    }

    private void setOverflowMenu() {
        if (overflowMenu == null) {
            ImageView icon = new ImageView(getContext());
            icon.setImageResource(R.drawable.cometchat_ic_check_primary);
            icon.setImageTintList(ColorStateList.valueOf(CometChatTheme.getPrimaryColor(getContext())));
            setOverflowMenu(icon);
            icon.setOnClickListener(v -> {
                if (onSelection != null) {
                    onSelection.onSelection(getSelectedConversation());
                }
            });
        } else {
            hideOverflowMenu(true);
        }
    }

    public void setOverflowMenu(View view) {
        this.overflowMenu = view;
        if (view != null) {
            Utils.handleView(binding.toolbarOverflowMenu, view, true);
        }
    }

    /**
     * Retrieves the selected conversations from the view.
     *
     * @return The list of selected Conversation objects.
     */
    public List<Conversation> getSelectedConversation() {
        List<Conversation> conversationList = new ArrayList<>();
        for (HashMap.Entry<Conversation, Boolean> entry : hashMap.entrySet()) {
            conversationList.add(entry.getKey());
        }
        return conversationList;
    }

    /**
     * Returns the RecyclerView used in the conversations view.
     *
     * @return The RecyclerView instance.
     */
    public RecyclerView getRecyclerView() {
        return binding.recyclerviewConversationsList;
    }

    /**
     * Returns the ViewModel associated with the conversations view.
     *
     * @return The ConversationsViewModel instance.
     */
    public ConversationsViewModel getViewModel() {
        return conversationsViewModel;
    }

    /**
     * Returns the ConversationsAdapter used in the conversations view.
     *
     * @return The ConversationsAdapter instance.
     */
    public ConversationsAdapter getConversationsAdapter() {
        return conversationsAdapter;
    }

    /**
     * Sets the adapter for the conversations view.
     *
     * @param adapter The RecyclerView.Adapter to be set.
     */
    public void setAdapter(ConversationsAdapter adapter) {
        if (adapter != null) {
            conversationsAdapter = adapter;
            binding.recyclerviewConversationsList.setAdapter(adapter);
        }
    }

    /**
     * Scrolls to the top of the conversation list if the first visible item
     * position is less than 5.
     */
    private void scrollToTop() {
        if (((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition() < 5) layoutManager.scrollToPosition(0);
    }

    /**
     * Handles the loading state by displaying a loading view or the shimmer effect.
     */
    private void handleLoadingState() {
        if (customLoadingView != null) {
            Utils.handleView(binding.customLayout, customLoadingView, true);
        } else if (showShimmer) {
            showShimmer = false;
            setShimmerVisibility(View.VISIBLE);
        }
    }

    /**
     * Sets the visibility of the shimmer effect, which is used to display a loading
     * state.
     *
     * @param visibility Visibility constant (View.VISIBLE, View.GONE, etc.).
     */
    private void setShimmerVisibility(int visibility) {
        if (visibility == View.GONE) {
            binding.shimmerEffectFrame.stopShimmer();
        } else {
            CometChatShimmerAdapter adapter = new CometChatShimmerAdapter(30, R.layout.shimmer_list_base);
            binding.shimmerRecyclerview.setAdapter(adapter);
            binding.shimmerEffectFrame.setShimmer(CometChatShimmerUtils.getCometChatShimmerConfig(getContext()));
            binding.shimmerEffectFrame.startShimmer();
        }
        binding.shimmerParentLayout.setVisibility(visibility);
    }

    /**
     * Handles the error state by displaying a custom error view or default error
     * message if available.
     */
    private void handleErrorState() {
        if (customErrorView != null) {
            Utils.handleView(binding.errorStateView, customErrorView, true);
        } else if (!hideError) {
            setErrorStateVisibility(View.VISIBLE);
        }
    }

    /**
     * Sets the visibility of the error state view.
     *
     * @param visibility Visibility constant (View.VISIBLE, View.GONE, etc.).
     */
    private void setErrorStateVisibility(int visibility) {
        binding.errorStateView.setVisibility(visibility);
    }

    /**
     * Handles the empty state by displaying a custom empty view or default empty
     * message if available.
     */
    private void handleEmptyState() {
        if (customEmptyView != null) {
            Utils.handleView(binding.emptyStateView, customEmptyView, true);
        } else {
            setEmptyStateVisibility(View.VISIBLE);
        }
    }

    /**
     * Sets the visibility of the empty state view.
     *
     * @param visibility Visibility constant (View.VISIBLE, View.GONE, etc.).
     */
    private void setEmptyStateVisibility(int visibility) {
        binding.emptyStateView.setVisibility(visibility);
    }

    /**
     * Hides all possible UI states like recycler view, empty state, error state,
     * shimmer, and custom loader.
     */
    private void hideAllStates() {
        setRecyclerViewVisibility(isConversationListEmpty ? View.GONE : View.VISIBLE);
        setEmptyStateVisibility(View.GONE);
        setErrorStateVisibility(View.GONE);
        setShimmerVisibility(View.GONE);
        setCustomLoaderVisibility(View.GONE);
    }

    /**
     * Sets the visibility of the recycler view.
     *
     * @param visibility Visibility constant (View.VISIBLE, View.GONE, etc.).
     */
    private void setRecyclerViewVisibility(int visibility) {
        binding.recyclerviewConversationsList.setVisibility(visibility);
    }

    /**
     * Sets the visibility of the custom loader view.
     *
     * @param visibility Visibility constant (View.VISIBLE, View.GONE, etc.).
     */
    private void setCustomLoaderVisibility(int visibility) {
        binding.customLayout.setVisibility(visibility);
    }

    /**
     * Sets the ConversationsRequestBuilder for fetching conversations.
     *
     * @param conversationsRequestBuilder The ConversationsRequestBuilder instance.
     */
    public void setConversationsRequestBuilder(ConversationsRequest.ConversationsRequestBuilder conversationsRequestBuilder) {
        conversationsViewModel.setConversationsRequestBuilder(conversationsRequestBuilder);
    }

    /**
     * Retrieves the custom title text for the error state.
     *
     * @return the custom error state title text.
     */
    public String getCustomErrorStateTitleText() {
        return customErrorStateTitleText;
    }

    /**
     * Sets the custom title text for the error state.
     *
     * @param customErrorStateTitleText the custom error state title text to set.
     */
    public void setCustomErrorStateTitleText(String customErrorStateTitleText) {
        this.customErrorStateTitleText = customErrorStateTitleText;
        binding.tvErrorTitle.setText(customErrorStateTitleText);
    }

    /**
     * Retrieves the custom subtitle text for the error state.
     *
     * @return the custom error state subtitle text.
     */
    public String getCustomErrorStateSubtitleText() {
        return customErrorStateSubtitleText;
    }

    /**
     * Sets the custom subtitle text for the error state.
     *
     * @param customErrorStateSubtitleText the custom error state subtitle text to set.
     */
    public void setCustomErrorStateSubtitleText(String customErrorStateSubtitleText) {
        this.customErrorStateSubtitleText = customErrorStateSubtitleText;
        binding.tvErrorSubtitle.setText(customErrorStateSubtitleText);
    }

    /**
     * Retrieves the custom title text for the empty state.
     *
     * @return the custom empty state title text.
     */
    public String getCustomEmptyStateTitleText() {
        return customEmptyStateTitleText;
    }

    /**
     * Sets the custom title text for the empty state.
     *
     * @param customEmptyStateTitleText the custom empty state title text to set.
     */
    public void setCustomEmptyStateTitleText(String customEmptyStateTitleText) {
        this.customEmptyStateTitleText = customEmptyStateTitleText;
        binding.tvEmptyConversationsTitle.setText(customEmptyStateTitleText);
    }

    /**
     * Retrieves the custom subtitle text for the empty state.
     *
     * @return the custom empty state subtitle text.
     */
    public String getCustomEmptyStateSubtitleText() {
        return customEmptyStateSubtitleText;
    }

    /**
     * Sets the custom subtitle text for the empty state.
     *
     * @param customEmptyStateSubtitleText the custom empty state subtitle text to set.
     */
    public void setCustomEmptyStateSubtitleText(String customEmptyStateSubtitleText) {
        this.customEmptyStateSubtitleText = customEmptyStateSubtitleText;
        binding.tvEmptyConversationsSubtitle.setText(customEmptyStateSubtitleText);
    }

    /**
     * Checks if the back icon should be hidden.
     *
     * @return true if the back icon is hidden, false otherwise.
     */
    public boolean hideBackIcon() {
        return hideBackIcon;
    }

    /**
     * Checks if the toolbar should be hidden.
     *
     * @return true if the toolbar is hidden, false otherwise.
     */
    public boolean hideToolBar() {
        return hideBackIcon;
    }

    /**
     * Checks if the overflow menu should be hidden.
     *
     * @return true if the overflow menu is hidden, false otherwise.
     */
    public boolean hideOverflowMenu() {
        return hideOverflowMenu;
    }

    /**
     * Gets the current OnItemClick listener.
     *
     * @return the OnItemClick listener instance.
     */
    public OnItemClick getOnItemClick() {
        return onItemClick;
    }

    /**
     * Sets the OnItemClick listener.
     *
     * @param onItemClick the OnItemClick listener to set.
     */
    public void setOnItemClick(OnItemClick onItemClick) {
        this.onItemClick = onItemClick;
    }

    /**
     * Gets the current OnItemLongClick listener.
     *
     * @return the OnItemLongClick listener instance.
     */
    public OnItemLongClick getOnItemLongClick() {
        return onItemLongClick;
    }

    /**
     * Sets the OnItemLongClick listener.
     *
     * @param onItemLongClick the OnItemLongClick listener to set.
     */
    public void setOnItemLongClick(OnItemLongClick onItemLongClick) {
        this.onItemLongClick = onItemLongClick;
    }

    public CometchatConversationsListViewBinding getBinding() {
        return binding;
    }

    /**
     * Interface for handling item click events.
     */
    public interface OnItemClick {
        /**
         * Called when an item is clicked.
         *
         * @param view         the view that was clicked.
         * @param position     the position of the clicked item in the adapter.
         * @param conversation the conversation associated with the clicked item.
         */
        void click(View view, int position, Conversation conversation);
    }

    /**
     * Interface for handling item long click events.
     */
    public interface OnItemLongClick {
        /**
         * Called when an item is long-clicked.
         *
         * @param view         the view that was long-clicked.
         * @param position     the position of the long-clicked item in the adapter.
         * @param conversation the conversation associated with the long-clicked item.
         */
        void longClick(View view, int position, Conversation conversation);
    }

    /**
     * Interface for handling selection events with a list of conversations.
     */
    public interface OnSelection {
        void onSelection(List<Conversation> conversations);
    }
}
