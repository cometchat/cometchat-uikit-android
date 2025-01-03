package com.cometchat.chatuikit.users;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chat.core.UsersRequest;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.databinding.CometchatUserListBinding;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.interfaces.Function2;
import com.cometchat.chatuikit.shared.interfaces.OnBackPress;
import com.cometchat.chatuikit.shared.interfaces.OnError;
import com.cometchat.chatuikit.shared.models.CometChatOption;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.cometchat.chatuikit.shared.resources.utils.itemclicklistener.OnItemClickListener;
import com.cometchat.chatuikit.shared.resources.utils.recycler_touch.ClickListener;
import com.cometchat.chatuikit.shared.resources.utils.recycler_touch.RecyclerTouchListener;
import com.cometchat.chatuikit.shared.resources.utils.sticker_header.StickyHeaderDecoration;
import com.cometchat.chatuikit.shared.viewholders.UsersViewHolderListener;
import com.cometchat.chatuikit.shared.views.searchbox.CometChatSearchBox;
import com.cometchat.chatuikit.shimmer.CometChatShimmerAdapter;
import com.cometchat.chatuikit.shimmer.CometChatShimmerUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The CometChatUsers class is a custom view in Android that extends the
 * MaterialCardView class. It is designed to represent and manage user-related
 * functionalities within the CometChat framework. This class can be utilized to
 * display user information, facilitate user interactions, and integrate various
 * user functionalities, providing a seamless user experience for one-on-one
 * chats. Created on: 22 October 2024
 */
public class CometChatUsers extends MaterialCardView {
    private static final String TAG = CometChatUsers.class.getSimpleName();
    private CometchatUserListBinding binding;

    private UsersViewModel usersViewModel;
    private UsersAdapter usersAdapter;
    private LinearLayoutManager layoutManager;

    private boolean showShimmer = true;
    private boolean isUserListEmpty = true;
    private boolean isFurtherSelectionEnabled = true;

    private boolean hideTitle = false;
    private boolean hideBackIcon = true;
    private boolean hideSearchBox = false;
    private boolean hideSelectionCount = true;
    private boolean hideDiscardSelectionIcon = true;
    private boolean hideSubmitSelectionIcon = true;
    private boolean hideErrorState = false;
    private boolean hideEmptyState = false;

    private String searchPlaceholderText;
    private int separatorVisibility;
    private View emptyView = null;
    private View errorView = null;
    private View customLoadingView = null;
    private View overflowMenu = null;

    private OnError onError;
    private OnSelection onSelection;
    private OnItemClickListener<User> onItemClickListener;
    private OnBackPress onBackPress;

    private final HashMap<User, Boolean> hashMap = new HashMap<>();
    private Function2<Context, User, List<CometChatOption>> options;
    private UIKitConstants.SelectionMode selectionMode = UIKitConstants.SelectionMode.NONE;

    private @ColorInt int backgroundColor;
    private @ColorInt int titleTextColor;
    private @StyleRes int titleTextAppearance;

    private @Dimension int strokeWidth;
    private @ColorInt int strokeColor;
    private @Dimension int cornerRadius;

    private Drawable backIcon;
    private @ColorInt int backIconTint;
    private @ColorInt int separatorColor;

    private Drawable discardSelectionIcon;
    private @ColorInt int discardSelectionIconTint;
    private Drawable submitSelectionIcon;
    private @ColorInt int submitSelectionIconTint;

    private Drawable searchInputStarIcon;
    private @ColorInt int searchInputStarIconTint;
    private Drawable searchInputEndIcon;
    private @ColorInt int searchInputEndIconTint;

    private @Dimension int searchInputStrokeWidth;
    private @ColorInt int searchInputStrokeColor;
    private @Dimension int searchInputCornerRadius;
    private @ColorInt int searchInputBackgroundColor;
    private @StyleRes int searchInputTextAppearance;
    private @ColorInt int searchInputTextColor;
    private @StyleRes int searchInputPlaceHolderTextAppearance;
    private @ColorInt int searchInputPlaceHolderTextColor;
    private @ColorInt int searchInputIconTint;
    @Nullable
    private Drawable searchInputIcon;

    private @ColorInt int stickyTitleColor;
    private @StyleRes int stickyTitleAppearance;
    private @ColorInt int stickyTitleBackgroundColor;

    private @StyleRes int avatar;
    private @StyleRes int itemTitleTextAppearance;
    private @ColorInt int itemTitleTextColor;
    private @ColorInt int itemBackgroundColor;
    private @StyleRes int statusIndicatorStyle;
    private @ColorInt int itemSelectedBackgroundColor;

    private @Dimension int checkBoxStrokeWidth;
    private @Dimension int checkBoxCornerRadius;
    private @ColorInt int checkBoxStrokeColor;
    private @ColorInt int checkBoxBackgroundColor;
    private @ColorInt int checkBoxCheckedBackgroundColor;
    private @ColorInt int checkBoxSelectIconTint;
    private Drawable checkBoxSelectIcon;

    private @StyleRes int emptyStateTextAppearance;
    private @ColorInt int emptyStateTextColor;
    private @StyleRes int emptyStateSubTitleTextAppearance;
    private @ColorInt int emptyStateSubtitleTextColor;

    private @StyleRes int errorStateTextAppearance;
    private @ColorInt int errorStateTextColor;
    private @StyleRes int errorStateSubtitleTextAppearance;
    private @ColorInt int errorStateSubtitleColor;

    private @ColorInt int retryButtonTextColor;
    private @StyleRes int retryButtonTextAppearance;
    private @ColorInt int retryButtonBackgroundColor;
    private @ColorInt int retryButtonStrokeColor;
    private @Dimension int retryButtonStrokeWidth;
    private @Dimension int retryButtonCornerRadius;

    /**
     * Constructs a new CometChatUsers instance with the specified context.
     *
     * @param context the context to use for this view
     */
    public CometChatUsers(Context context) {
        this(context, null);
    }

    /**
     * Constructs a new CometChatUsers instance with the specified context and
     * attributes.
     *
     * @param context the context to use for this view
     * @param attrs   the attribute set containing the view's attributes
     */
    public CometChatUsers(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.cometchatUsersStyle);
    }

    /**
     * Constructs a new CometChatUsers instance with the specified context,
     * attributes, and default style.
     *
     * @param context      the context to use for this view
     * @param attrs        the attribute set containing the view's attributes
     * @param defStyleAttr the default style to apply to this view
     */
    public CometChatUsers(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode()) {
            inflateAndInitializeView(attrs, defStyleAttr);
        }
    }

    /**
     * Inflates and initializes the view with the given attributes and style.
     *
     * @param attrs        the attribute set containing the view's attributes
     * @param defStyleAttr the default style to apply to this view
     */
    private void inflateAndInitializeView(AttributeSet attrs, int defStyleAttr) {
        // Inflate the layout for this view
        binding = CometchatUserListBinding.inflate(LayoutInflater.from(getContext()), this, true);

        // Reset the card view to default values
        Utils.initMaterialCard(this);

        // Set default values
        init();

        // Apply style attributes
        applyStyleAttributes(attrs, defStyleAttr);
    }

    /**
     * Initializes the view components and sets up listeners.
     */
    private void init() {
        initRecyclerView();
        initViewModels();
        initClickEvents();
    }

    /**
     * Initializes the RecyclerView and sets up its adapter and layout manager.
     */
    private void initRecyclerView() {
        usersAdapter = new UsersAdapter(getContext());
        StickyHeaderDecoration stickyHeaderDecoration = new StickyHeaderDecoration(usersAdapter);

        binding.recyclerViewList.setAdapter(usersAdapter);
        layoutManager = new LinearLayoutManager(getContext());
        binding.recyclerViewList.setLayoutManager(layoutManager);

        binding.recyclerViewList.addItemDecoration(stickyHeaderDecoration, 0);

        binding.recyclerViewList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (!recyclerView.canScrollVertically(1)) {
                    usersViewModel.fetchUser();
                }
            }
        });
    }

    /**
     * Initializes the ViewModel and observes its data for changes.
     */
    private void initViewModels() {
        usersViewModel = new ViewModelProvider.NewInstanceFactory().create(UsersViewModel.class);
        usersViewModel.getMutableUsersList().observe((LifecycleOwner) getContext(), listObserver);
        usersViewModel.getStates().observe((LifecycleOwner) getContext(), stateChangeObserver);
        usersViewModel.insertAtTop().observe((LifecycleOwner) getContext(), insertAtTop);
        usersViewModel.moveToTop().observe((LifecycleOwner) getContext(), moveToTop);
        usersViewModel.updateUser().observe((LifecycleOwner) getContext(), update);
        usersViewModel.removeUser().observe((LifecycleOwner) getContext(), remove);
        usersViewModel.getCometChatException().observe((LifecycleOwner) getContext(), exceptionObserver);
    }

    /**
     * Initializes click events for the view components.
     */
    private void initClickEvents() {
        binding.recyclerViewList.addOnItemTouchListener(new RecyclerTouchListener(getContext(), binding.recyclerViewList, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                User user = (User) view.getTag(R.string.cometchat_user);
                if (!UIKitConstants.SelectionMode.NONE.equals(selectionMode)) {
                    selectUser(user, selectionMode);
                }
                if (onItemClickListener != null) {
                    onItemClickListener.OnItemClick(user, position);
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                User user = (User) view.getTag(R.string.cometchat_user);
                if (onItemClickListener != null) {
                    onItemClickListener.OnItemLongClick(user, position);
                }
            }
        }));

        binding.searchBox.addOnSearchListener((state, text) -> {
            if (state.equals(CometChatSearchBox.SearchState.TextChange)) {
                if (text.isEmpty()) {
                    usersViewModel.searchUsers(null);
                } else {
                    usersViewModel.searchUsers(text);
                }
            }
        });

        binding.btnRetry.setOnClickListener(v -> {
            showShimmer = true;
            usersViewModel.fetchUser();
        });

        binding.ivDiscardSelection.setOnClickListener(v -> {
            clearSelection();
        });

        binding.ivSubmitSelection.setOnClickListener(v -> {
            if (onSelection != null) {
                onSelection.onSelection(getSelectedUsers());
            }
        });

        binding.ivBack.setOnClickListener(v -> {
            if (onBackPress != null) {
                onBackPress.onBack();
            }
        });
    }

    /**
     * Applies the style attributes defined in the XML layout to this view.
     *
     * @param attrs        the attribute set containing the view's attributes
     * @param defStyleAttr the default style to apply to this view
     */
    private void applyStyleAttributes(AttributeSet attrs, int defStyleAttr) {
        TypedArray directAttributes = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CometChatUsers, defStyleAttr, 0);
        @StyleRes int styleResId = directAttributes.getResourceId(R.styleable.CometChatUsers_cometchatUsersStyle, 0);
        directAttributes = styleResId != 0 ? getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CometChatUsers, defStyleAttr, styleResId) : null;
        extractAttributesAndApplyDefaults(directAttributes);
    }

    /**
     * Sets the style for this view using the provided style resource ID.
     *
     * @param style the resource ID of the style to apply
     */
    public void setStyle(@StyleRes int style) {
        if (style != 0) {
            TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(style, R.styleable.CometChatUsers);
            extractAttributesAndApplyDefaults(typedArray);
        }
    }

    /**
     * Extracts the attributes from the provided TypedArray and applies default
     * values if necessary.
     *
     * @param typedArray the TypedArray containing the attributes to extract
     */
    private void extractAttributesAndApplyDefaults(TypedArray typedArray) {
        if (typedArray == null) return;
        try {
            // Extract attributes or apply default values
            backgroundColor = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersBackgroundColor, 0);
            titleTextColor = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersTitleTextColor, 0);
            titleTextAppearance = typedArray.getResourceId(R.styleable.CometChatUsers_cometchatUsersTitleTextAppearance, 0);
            strokeWidth = typedArray.getDimensionPixelSize(R.styleable.CometChatUsers_cometchatUsersStrokeWidth, 0);
            strokeColor = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersStrokeColor, 0);
            cornerRadius = typedArray.getDimensionPixelSize(R.styleable.CometChatUsers_cometchatUsersCornerRadius, 0);
            backIcon = typedArray.getDrawable(R.styleable.CometChatUsers_cometchatUsersBackIcon);
            backIconTint = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersBackIconTint, 0);
            separatorColor = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersSeparatorColor, 0);
            discardSelectionIcon = typedArray.getDrawable(R.styleable.CometChatUsers_cometchatUsersDiscardSelectionIcon);
            discardSelectionIconTint = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersDiscardSelectionIconTint, 0);
            submitSelectionIcon = typedArray.getDrawable(R.styleable.CometChatUsers_cometchatUsersSubmitSelectionIcon);
            submitSelectionIconTint = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersSubmitSelectionIconTint, 0);
            searchInputStarIcon = typedArray.getDrawable(R.styleable.CometChatUsers_cometchatUsersSearchInputStartIcon);
            searchInputStarIconTint = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersSearchInputStartIconTint, 0);
            searchInputEndIcon = typedArray.getDrawable(R.styleable.CometChatUsers_cometchatUsersSearchInputEndIcon);
            searchInputEndIconTint = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersSearchInputEndIconTint, 0);
            searchInputStrokeWidth = typedArray.getDimensionPixelSize(R.styleable.CometChatUsers_cometchatUsersSearchInputStrokeWidth, 0);
            searchInputStrokeColor = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersSearchInputStrokeColor, 0);
            searchInputCornerRadius = typedArray.getDimensionPixelSize(R.styleable.CometChatUsers_cometchatUsersSearchInputCornerRadius, 0);
            searchInputBackgroundColor = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersSearchInputBackgroundColor, 0);
            searchInputTextAppearance = typedArray.getResourceId(R.styleable.CometChatUsers_cometchatUsersSearchInputTextAppearance, 0);
            searchInputTextColor = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersSearchInputTextColor, 0);
            searchInputPlaceHolderTextAppearance = typedArray.getResourceId(R.styleable.CometChatUsers_cometchatUsersSearchInputPlaceHolderTextAppearance, 0);
            searchInputPlaceHolderTextColor = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersSearchInputPlaceHolderTextColor, 0);
            searchInputIcon = typedArray.getDrawable(R.styleable.CometChatUsers_cometchatUsersSearchInputIcon);
            searchInputIconTint = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersSearchInputIconTint, 0);
            stickyTitleColor = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersStickyTitleColor, 0);
            stickyTitleAppearance = typedArray.getResourceId(R.styleable.CometChatUsers_cometchatUsersStickyTitleAppearance, 0);
            stickyTitleBackgroundColor = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersStickyTitleBackgroundColor, 0);
            avatar = typedArray.getResourceId(R.styleable.CometChatUsers_cometchatUsersAvatarStyle, 0);
            itemTitleTextAppearance = typedArray.getResourceId(R.styleable.CometChatUsers_cometchatUsersItemTitleTextAppearance, 0);
            itemTitleTextColor = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersItemTitleTextColor, 0);
            itemBackgroundColor = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersItemBackgroundColor, 0);
            statusIndicatorStyle = typedArray.getResourceId(R.styleable.CometChatUsers_cometchatUsersStatusIndicator, 0);
            itemSelectedBackgroundColor = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersItemSelectedBackgroundColor, 0);
            checkBoxStrokeWidth = typedArray.getDimensionPixelSize(R.styleable.CometChatUsers_cometchatUsersCheckBoxStrokeWidth, 0);
            checkBoxCornerRadius = typedArray.getDimensionPixelSize(R.styleable.CometChatUsers_cometchatUsersCheckBoxCornerRadius, 0);
            checkBoxStrokeColor = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersCheckBoxStrokeColor, 0);
            checkBoxBackgroundColor = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersCheckBoxBackgroundColor, 0);
            checkBoxSelectIconTint = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersCheckBoxSelectIconTint, 0);
            checkBoxSelectIcon = typedArray.getDrawable(R.styleable.CometChatUsers_cometchatUsersCheckBoxSelectIcon);
            checkBoxCheckedBackgroundColor = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersCheckBoxCheckedBackgroundColor, 0);
            emptyStateTextAppearance = typedArray.getResourceId(R.styleable.CometChatUsers_cometchatUsersEmptyStateTextAppearance, 0);
            emptyStateTextColor = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersEmptyStateTextColor, 0);
            emptyStateSubTitleTextAppearance = typedArray.getResourceId(R.styleable.CometChatUsers_cometchatUsersEmptyStateSubTitleTextAppearance, 0);
            emptyStateSubtitleTextColor = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersEmptyStateSubtitleTextColor, 0);
            errorStateTextAppearance = typedArray.getResourceId(R.styleable.CometChatUsers_cometchatUsersErrorStateTextAppearance, 0);
            errorStateTextColor = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersErrorStateTextColor, 0);
            errorStateSubtitleTextAppearance = typedArray.getResourceId(R.styleable.CometChatUsers_cometchatUsersErrorStateSubtitleTextAppearance, 0);
            errorStateSubtitleColor = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersErrorStateSubtitleColor, 0);
            retryButtonTextColor = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersRetryButtonTextColor, 0);
            retryButtonTextAppearance = typedArray.getResourceId(R.styleable.CometChatUsers_cometchatUsersRetryButtonTextAppearance, 0);
            retryButtonBackgroundColor = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersRetryButtonBackgroundColor, 0);
            retryButtonStrokeColor = typedArray.getColor(R.styleable.CometChatUsers_cometchatUsersRetryButtonStrokeColor, 0);
            retryButtonStrokeWidth = typedArray.getDimensionPixelSize(R.styleable.CometChatUsers_cometchatUsersRetryButtonStrokeWidth, 0);
            retryButtonCornerRadius = typedArray.getDimensionPixelSize(R.styleable.CometChatUsers_cometchatUsersRetryButtonCornerRadius, 0);

            // Apply default styles
            updateUI();
        } finally {
            typedArray.recycle();
        }
    }

    /**
     * Updates the user interface of this view based on current data and state.
     */
    private void updateUI() {
        setBackgroundColor(backgroundColor);
        setTitleTextColor(titleTextColor);
        setTitleTextAppearance(titleTextAppearance);
        setStrokeWidth(strokeWidth);
        setStrokeColor(strokeColor);
        setCornerRadius(cornerRadius);
        setBackIcon(backIcon);
        setBackIconTint(backIconTint);
        setSeparatorColor(separatorColor);
        setDiscardSelectionIcon(discardSelectionIcon);
        setDiscardSelectionIconTint(discardSelectionIconTint);
        setSubmitSelectionIcon(submitSelectionIcon);
        setSubmitSelectionIconTint(submitSelectionIconTint);
        setSearchInputStrokeWidth(searchInputStrokeWidth);
        setSearchInputStrokeColor(searchInputStrokeColor);
        setSearchInputCornerRadius(searchInputCornerRadius);
        setSearchInputBackgroundColor(searchInputBackgroundColor);
        setSearchInputTextAppearance(searchInputTextAppearance);
        setSearchInputTextColor(searchInputTextColor);
        setSearchInputPlaceHolderTextAppearance(searchInputPlaceHolderTextAppearance);
        setSearchInputPlaceHolderTextColor(searchInputPlaceHolderTextColor);
        setSearchInputIcon(searchInputIcon);
        setSearchInputIconTint(searchInputIconTint);
        setStickyTitleColor(stickyTitleColor);
        setStickyTitleAppearance(stickyTitleAppearance);
        setStickyTitleBackgroundColor(stickyTitleBackgroundColor);
        setAvatar(avatar);
        setItemTitleTextAppearance(itemTitleTextAppearance);
        setItemTitleTextColor(itemTitleTextColor);
        setItemBackgroundColor(itemBackgroundColor);
        setStatusIndicatorStyle(statusIndicatorStyle);
        setItemSelectedBackgroundColor(itemSelectedBackgroundColor);
        setCheckBoxStrokeWidth(checkBoxStrokeWidth);
        setCheckBoxCornerRadius(checkBoxCornerRadius);
        setCheckBoxStrokeColor(checkBoxStrokeColor);
        setCheckBoxBackgroundColor(checkBoxBackgroundColor);
        setCheckBoxCheckedBackgroundColor(checkBoxCheckedBackgroundColor);
        setEmptyStateTextAppearance(emptyStateTextAppearance);
        setEmptyStateTextColor(emptyStateTextColor);
        setEmptyStateSubTitleTextAppearance(emptyStateSubTitleTextAppearance);
        setEmptyStateSubtitleTextColor(emptyStateSubtitleTextColor);
        setErrorStateTextAppearance(errorStateTextAppearance);
        setErrorStateTextColor(errorStateTextColor);
        setErrorStateSubtitleTextAppearance(errorStateSubtitleTextAppearance);
        setErrorStateSubtitleColor(errorStateSubtitleColor);
        setRetryButtonTextColor(retryButtonTextColor);
        setRetryButtonTextAppearance(retryButtonTextAppearance);
        setRetryButtonBackgroundColor(retryButtonBackgroundColor);
        setRetryButtonStrokeColor(retryButtonStrokeColor);
        setRetryButtonStrokeWidth(retryButtonStrokeWidth);
        setRetryButtonCornerRadius(retryButtonCornerRadius);
        setSearchInputStarIcon(searchInputStarIcon);
        setSearchInputStarIconTint(searchInputStarIconTint);
        setSearchInputEndIcon(searchInputEndIcon);
        setSearchInputEndIconTint(searchInputEndIconTint);
        setCheckBoxSelectIcon(checkBoxSelectIcon);
        setCheckBoxSelectIconTint(checkBoxSelectIconTint);

        binding.tvSelectionCount.setTextAppearance(itemTitleTextAppearance);
        binding.tvSelectionCount.setTextColor(itemTitleTextColor);

        setSelectionMode(UIKitConstants.SelectionMode.NONE);
        setErrorStateTitleText(null);
        setErrorStateSubtitleText(null);
        setEmptyStateTitleText(null);
        setEmptyStateSubtitleText(null);
    }

    /**
     * Gets the background color.
     *
     * @return the background color.
     */
    public @ColorInt int getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Sets the background color.
     *
     * @param backgroundColor the background color to set.
     */
    public void setBackgroundColor(@ColorInt int backgroundColor) {
        this.backgroundColor = backgroundColor;
        super.setCardBackgroundColor(backgroundColor);
    }

    /**
     * Gets the title text color.
     *
     * @return the title text color.
     */
    public @ColorInt int getTitleTextColor() {
        return titleTextColor;
    }

    /**
     * Sets the title text color.
     *
     * @param titleTextColor the title text color to set.
     */
    public void setTitleTextColor(@ColorInt int titleTextColor) {
        this.titleTextColor = titleTextColor;
        binding.tvTitle.setTextColor(titleTextColor);
    }

    /**
     * Gets the title text appearance.
     *
     * @return the title text appearance resource.
     */
    public @StyleRes int getTitleTextAppearance() {
        return titleTextAppearance;
    }

    /**
     * Sets the title text appearance.
     *
     * @param titleTextAppearance the title text appearance resource to set.
     */
    public void setTitleTextAppearance(@StyleRes int titleTextAppearance) {
        this.titleTextAppearance = titleTextAppearance;
        binding.tvTitle.setTextAppearance(titleTextAppearance);
    }

    /**
     * Gets the stroke width.
     *
     * @return the stroke width.
     */
    public @Dimension int getStrokeWidth() {
        return strokeWidth;
    }

    /**
     * Sets the stroke width.
     *
     * @param strokeWidth the stroke width to set.
     */
    public void setStrokeWidth(@Dimension int strokeWidth) {
        this.strokeWidth = strokeWidth;
        super.setStrokeWidth(strokeWidth);
    }

    /**
     * Gets the stroke color.
     *
     * @return the stroke color.
     */
    public ColorStateList getStrokeColorStateList() {
        return ColorStateList.valueOf(strokeColor);
    }

    /**
     * Sets the stroke color.
     *
     * @param strokeColor the stroke color to set.
     */
    public void setStrokeColor(@ColorInt int strokeColor) {
        this.strokeColor = strokeColor;
        super.setStrokeColor(strokeColor);
    }

    /**
     * Gets the corner radius.
     *
     * @return the corner radius.
     */
    public @Dimension int getCornerRadius() {
        return cornerRadius;
    }

    /**
     * Sets the corner radius.
     *
     * @param cornerRadius the corner radius to set.
     */
    public void setCornerRadius(@Dimension int cornerRadius) {
        this.cornerRadius = cornerRadius;
        super.setRadius(cornerRadius);
    }

    /**
     * Returns the visibility status of the back icon.
     *
     * @return true if the back icon is hidden, false otherwise.
     */
    public boolean hideBackIcon() {
        return hideBackIcon;
    }

    /**
     * Sets the visibility of the separator based on the provided parameter. If
     * {@code visibility} is set to {@code GONE}, the separator will be hidden. Otherwise, it will be visible.
     */
    public void setSeparatorVisibility(int visibility) {
        this.separatorVisibility = visibility;
        binding.viewSeparator.setVisibility(visibility);
    }

    /**
     * Gets the visibility of the separator.
     *
     * @return the visibility of the separator.
     */
    public int getSeparatorVisibility() {
        return separatorVisibility;
    }

    /**
     * Sets the visibility of the back icon based on the provided parameter. If
     * {@code hideBackButton} is true, the back icon will be hidden. Otherwise, it
     * will be visible.
     *
     * @param hideBackButton true to hide the back icon, false to show it.
     */
    public void hideBackIcon(boolean hideBackButton) {
        this.hideBackIcon = hideBackButton;
        setBackIconVisibility(hideBackButton ? GONE : VISIBLE);
    }

    /**
     * Sets the visibility of the back icon based on the provided visibility
     * parameter. If {@code
     * hideBackIcon} is true or visibility is set to {@code GONE}, the back icon
     * will be hidden. Otherwise, the back icon will be visible, and the discard
     * selection icon will be hidden.
     *
     * @param visibility the desired visibility state of the back icon.
     */
    private void setBackIconVisibility(int visibility) {
        if (hideBackIcon || visibility == GONE) {
            binding.ivBack.setVisibility(GONE);
        } else {
            binding.ivBack.setVisibility(VISIBLE);
            binding.ivDiscardSelection.setVisibility(View.GONE);
        }
    }

    /**
     * Returns the drawable resource for the back icon.
     *
     * @return the back icon drawable
     */
    public Drawable getBackIcon() {
        return backIcon;
    }

    /**
     * Returns the ImageView that displays the back icon.
     *
     * @return the ImageView for the back icon
     */
    public ImageView getBackIconView() {
        return binding.ivBack;
    }

    /**
     * Sets the back icon drawable.
     *
     * @param backIcon the back icon drawable to set.
     */
    public void setBackIcon(Drawable backIcon) {
        this.backIcon = backIcon;
        binding.ivBack.setImageDrawable(backIcon);
    }

    /**
     * Gets the back icon tint color.
     *
     * @return the back icon tint color.
     */
    public @ColorInt int getBackIconTint() {
        return backIconTint;
    }

    /**
     * Sets the back icon tint color.
     *
     * @param backIconTint the back icon tint color to set.
     */
    public void setBackIconTint(@ColorInt int backIconTint) {
        this.backIconTint = backIconTint;
        binding.ivBack.setImageTintList(ColorStateList.valueOf(backIconTint));
    }

    /**
     * Returns the color of the separator.
     *
     * @return the separator color as an integer value
     */
    public @ColorInt int getSeparatorColor() {
        return separatorColor;
    }

    /**
     * Sets the color of the separator and updates the view accordingly.
     *
     * @param separatorColor the new color to set for the separator
     */
    public void setSeparatorColor(@ColorInt int separatorColor) {
        this.separatorColor = separatorColor;
        binding.viewSeparator.setBackgroundColor(separatorColor);
    }

    /**
     * Gets the discard selection icon drawable.
     *
     * @return the discard selection icon drawable.
     */
    public Drawable getDiscardSelectionIcon() {
        return discardSelectionIcon;
    }

    /**
     * Sets the discard selection icon drawable.
     *
     * @param discardSelectionIcon the discard selection icon drawable to set.
     */
    public void setDiscardSelectionIcon(Drawable discardSelectionIcon) {
        this.discardSelectionIcon = discardSelectionIcon;
        binding.ivDiscardSelection.setImageDrawable(discardSelectionIcon);
    }

    /**
     * Gets the discard selection icon tint color.
     *
     * @return the discard selection icon tint color.
     */
    public @ColorInt int getDiscardSelectionIconTint() {
        return discardSelectionIconTint;
    }

    /**
     * Sets the discard selection icon tint color.
     *
     * @param discardSelectionIconTint the discard selection icon tint color to set.
     */
    public void setDiscardSelectionIconTint(@ColorInt int discardSelectionIconTint) {
        this.discardSelectionIconTint = discardSelectionIconTint;
        binding.ivDiscardSelection.setImageTintList(ColorStateList.valueOf(discardSelectionIconTint));
    }

    /**
     * Gets the submit selection icon drawable.
     *
     * @return the submit selection icon drawable.
     */
    public Drawable getSubmitSelectionIcon() {
        return submitSelectionIcon;
    }

    /**
     * Sets the submit selection icon drawable.
     *
     * @param submitSelectionIcon the submit selection icon drawable to set.
     */
    public void setSubmitSelectionIcon(Drawable submitSelectionIcon) {
        this.submitSelectionIcon = submitSelectionIcon;
        binding.ivSubmitSelection.setImageDrawable(submitSelectionIcon);
    }

    /**
     * Gets the submit selection icon tint color.
     *
     * @return the submit selection icon tint color.
     */
    public @ColorInt int getSubmitSelectionIconTint() {
        return submitSelectionIconTint;
    }

    /**
     * Sets the submit selection icon tint color.
     *
     * @param submitSelectionIconTint the submit selection icon tint color to set.
     */
    public void setSubmitSelectionIconTint(@ColorInt int submitSelectionIconTint) {
        this.submitSelectionIconTint = submitSelectionIconTint;
        binding.ivSubmitSelection.setImageTintList(ColorStateList.valueOf(submitSelectionIconTint));
    }

    /**
     * Gets the stroke width for the search input.
     *
     * @return the stroke width for the search input.
     */
    public @Dimension int getSearchInputStrokeWidth() {
        return searchInputStrokeWidth;
    }

    /**
     * Sets the stroke width for the search input.
     *
     * @param searchInputStrokeWidth the stroke width for the search input to set.
     */
    public void setSearchInputStrokeWidth(@Dimension int searchInputStrokeWidth) {
        this.searchInputStrokeWidth = searchInputStrokeWidth;
        binding.searchBox.setStrokeWidth(searchInputStrokeWidth);
    }

    /**
     * Gets the stroke color for the search input.
     *
     * @return the stroke color for the search input.
     */
    public @ColorInt int getSearchInputStrokeColor() {
        return searchInputStrokeColor;
    }

    /**
     * Sets the stroke color for the search input.
     *
     * @param searchInputStrokeColor the stroke color for the search input to set.
     */
    public void setSearchInputStrokeColor(@ColorInt int searchInputStrokeColor) {
        this.searchInputStrokeColor = searchInputStrokeColor;
        binding.searchBox.setStrokeWidth(searchInputStrokeColor);
    }

    /**
     * Gets the corner radius for the search input.
     *
     * @return the corner radius for the search input.
     */
    public @Dimension int getSearchInputCornerRadius() {
        return searchInputCornerRadius;
    }

    /**
     * Sets the corner radius for the search input.
     *
     * @param searchInputCornerRadius the corner radius for the search input to set.
     */
    public void setSearchInputCornerRadius(@Dimension int searchInputCornerRadius) {
        this.searchInputCornerRadius = searchInputCornerRadius;
        binding.searchBox.setRadius(searchInputCornerRadius);
    }

    /**
     * Gets the background color for the search input.
     *
     * @return the background color for the search input.
     */
    public @ColorInt int getSearchInputBackgroundColor() {
        return searchInputBackgroundColor;
    }

    /**
     * Sets the background color for the search input.
     *
     * @param searchInputBackgroundColor the background color for the search input to set.
     */
    public void setSearchInputBackgroundColor(@ColorInt int searchInputBackgroundColor) {
        this.searchInputBackgroundColor = searchInputBackgroundColor;
        binding.searchBox.setCardBackgroundColor(searchInputBackgroundColor);
    }

    /**
     * Gets the text appearance for the search input.
     *
     * @return the text appearance for the search input.
     */
    public @StyleRes int getSearchInputTextAppearance() {
        return searchInputTextAppearance;
    }

    /**
     * Sets the text appearance for the search input.
     *
     * @param searchInputTextAppearance the text appearance for the search input to set.
     */
    public void setSearchInputTextAppearance(@StyleRes int searchInputTextAppearance) {
        this.searchInputTextAppearance = searchInputTextAppearance;
        binding.searchBox.setSearchInputTextAppearance(searchInputTextAppearance);
    }

    /**
     * Gets the text color for the search input.
     *
     * @return the text color for the search input.
     */
    public @ColorInt int getSearchInputTextColor() {
        return searchInputTextColor;
    }

    /**
     * Sets the text color for the search input.
     *
     * @param searchInputTextColor the text color for the search input to set.
     */
    public void setSearchInputTextColor(@ColorInt int searchInputTextColor) {
        this.searchInputTextColor = searchInputTextColor;
        binding.searchBox.setSearchInputTextColor(searchInputTextColor);
    }

    /**
     * Gets the placeholder text appearance for the search input.
     *
     * @return the placeholder text appearance for the search input.
     */
    public @StyleRes int getSearchInputPlaceHolderTextAppearance() {
        return searchInputPlaceHolderTextAppearance;
    }

    /**
     * Sets the placeholder text appearance for the search input.
     *
     * @param searchInputPlaceHolderTextAppearance the placeholder text appearance for the search input to set.
     */
    public void setSearchInputPlaceHolderTextAppearance(@StyleRes int searchInputPlaceHolderTextAppearance) {
        this.searchInputPlaceHolderTextAppearance = searchInputPlaceHolderTextAppearance;
        binding.searchBox.setSearchInputPlaceHolderTextAppearance(searchInputPlaceHolderTextAppearance);
    }

    /**
     * Gets the placeholder text color for the search input.
     *
     * @return the placeholder text color for the search input.
     */
    public @ColorInt int getSearchInputPlaceHolderTextColor() {
        return searchInputPlaceHolderTextColor;
    }

    /**
     * Sets the placeholder text color for the search input.
     *
     * @param searchInputPlaceHolderTextColor the placeholder text color for the search input to set.
     */
    public void setSearchInputPlaceHolderTextColor(@ColorInt int searchInputPlaceHolderTextColor) {
        this.searchInputPlaceHolderTextColor = searchInputPlaceHolderTextColor;
        binding.searchBox.setSearchInputPlaceHolderTextColor(searchInputPlaceHolderTextColor);
    }

    /**
     * Gets the search input icon drawable.
     *
     * @return the search input icon drawable.
     */
    @Nullable
    public Drawable getSearchInputIcon() {
        return searchInputIcon;
    }

    /**
     * Sets the search input icon drawable.
     *
     * @param searchInputIcon the search input icon drawable to set.
     */
    public void setSearchInputIcon(Drawable searchInputIcon) {
        this.searchInputIcon = searchInputIcon;
        binding.searchBox.setSearchInputStartIcon(searchInputIcon);
    }

    /**
     * Gets the tint color for the search input icon.
     *
     * @return the tint color for the search input icon.
     */
    public @ColorInt int getSearchInputIconTint() {
        return searchInputIconTint;
    }

    /**
     * Sets the tint color for the search input icon.
     *
     * @param searchInputIconTint the tint color for the search input icon to set.
     */
    public void setSearchInputIconTint(@ColorInt int searchInputIconTint) {
        this.searchInputIconTint = searchInputIconTint;
        binding.searchBox.setSearchInputStartIconTint(searchInputIconTint);
    }

    /**
     * Gets the color for the sticky title.
     *
     * @return the color for the sticky title.
     */
    public @ColorInt int getStickyTitleColor() {
        return stickyTitleColor;
    }

    /**
     * Sets the color for the sticky title.
     *
     * @param stickyTitleColor the color for the sticky title to set.
     */
    public void setStickyTitleColor(@ColorInt int stickyTitleColor) {
        this.stickyTitleColor = stickyTitleColor;
        usersAdapter.setStickyTitleColor(stickyTitleColor);
    }

    /**
     * Gets the text appearance for the sticky title.
     *
     * @return the text appearance for the sticky title.
     */
    public @StyleRes int getStickyTitleAppearance() {
        return stickyTitleAppearance;
    }

    /**
     * Sets the text appearance for the sticky title.
     *
     * @param stickyTitleAppearance the text appearance for the sticky title to set.
     */
    public void setStickyTitleAppearance(@StyleRes int stickyTitleAppearance) {
        this.stickyTitleAppearance = stickyTitleAppearance;
        usersAdapter.setStickyTitleAppearance(stickyTitleAppearance);
    }

    /**
     * Gets the background color for the sticky title.
     *
     * @return the background color for the sticky title.
     */
    public @ColorInt int getStickyTitleBackgroundColor() {
        return stickyTitleBackgroundColor;
    }

    /**
     * Sets the background color for the sticky title.
     *
     * @param stickyTitleBackgroundColor the background color for the sticky title to set.
     */
    public void setStickyTitleBackgroundColor(@ColorInt int stickyTitleBackgroundColor) {
        this.stickyTitleBackgroundColor = stickyTitleBackgroundColor;
        usersAdapter.setStickyTitleBackgroundColor(stickyTitleBackgroundColor);
    }

    /**
     * Gets the avatar style resource.
     *
     * @return the avatar style resource.
     */
    public @StyleRes int getAvatar() {
        return avatar;
    }

    /**
     * Sets the avatar style resource.
     *
     * @param avatar the avatar style resource to set.
     */
    public void setAvatar(@StyleRes int avatar) {
        this.avatar = avatar;
        usersAdapter.setAvatarStyle(avatar);
    }

    /**
     * Gets the text appearance for the item title.
     *
     * @return the text appearance for the item title.
     */
    public @StyleRes int getItemTitleTextAppearance() {
        return itemTitleTextAppearance;
    }

    /**
     * Sets the text appearance for the item title.
     *
     * @param itemTitleTextAppearance the text appearance for the item title to set.
     */
    public void setItemTitleTextAppearance(@StyleRes int itemTitleTextAppearance) {
        this.itemTitleTextAppearance = itemTitleTextAppearance;
        usersAdapter.setItemTitleTextAppearance(itemTitleTextAppearance);
    }

    /**
     * Gets the text color for the item title.
     *
     * @return the text color for the item title.
     */
    public @ColorInt int getItemTitleTextColor() {
        return itemTitleTextColor;
    }

    /**
     * Sets the text color for the item title.
     *
     * @param itemTitleTextColor the text color for the item title to set.
     */
    public void setItemTitleTextColor(@ColorInt int itemTitleTextColor) {
        this.itemTitleTextColor = itemTitleTextColor;
        usersAdapter.setItemTitleTextColor(itemTitleTextColor);
    }

    /**
     * Gets the background color for the item.
     *
     * @return the background color for the item.
     */
    public @ColorInt int getItemBackgroundColor() {
        return itemBackgroundColor;
    }

    /**
     * Sets the background color for the item.
     *
     * @param itemBackgroundColor the background color for the item to set.
     */
    public void setItemBackgroundColor(@ColorInt int itemBackgroundColor) {
        this.itemBackgroundColor = itemBackgroundColor;
        usersAdapter.setItemBackgroundColor(itemBackgroundColor);
    }

    /**
     * Gets the color for the status indicator.
     *
     * @return the color for the status indicator.
     */
    public @StyleRes int getStatusIndicatorStyle() {
        return statusIndicatorStyle;
    }

    /**
     * Sets the status indicator style for the user adapter.
     *
     * @param statusIndicatorStyle The style to apply to the status indicators in the user adapter.
     */
    public void setStatusIndicatorStyle(@StyleRes int statusIndicatorStyle) {
        this.statusIndicatorStyle = statusIndicatorStyle;
        usersAdapter.setStatusIndicatorStyle(statusIndicatorStyle);
    }

    /**
     * Gets the background color for the selected item.
     *
     * @return the background color for the selected item.
     */
    public @ColorInt int getItemSelectedBackgroundColor() {
        return itemSelectedBackgroundColor;
    }

    /**
     * Sets the background color for the selected item.
     *
     * @param itemSelectedBackgroundColor the background color for the selected item to set.
     */
    public void setItemSelectedBackgroundColor(@ColorInt int itemSelectedBackgroundColor) {
        this.itemSelectedBackgroundColor = itemSelectedBackgroundColor;
        usersAdapter.setItemSelectedBackgroundColor(itemSelectedBackgroundColor);
    }

    /**
     * Gets the stroke width for the checkbox.
     *
     * @return the stroke width for the checkbox.
     */
    public @Dimension int getCheckBoxStrokeWidth() {
        return checkBoxStrokeWidth;
    }

    /**
     * Sets the stroke width for the checkbox.
     *
     * @param checkBoxStrokeWidth the stroke width for the checkbox to set.
     */
    public void setCheckBoxStrokeWidth(@Dimension int checkBoxStrokeWidth) {
        this.checkBoxStrokeWidth = checkBoxStrokeWidth;
        usersAdapter.setCheckBoxStrokeWidth(checkBoxStrokeWidth);
    }

    /**
     * Returns the corner radius for the checkboxes.
     *
     * @return the checkbox corner radius as an integer value
     */
    public @Dimension int getCheckBoxCornerRadius() {
        return checkBoxCornerRadius;
    }

    /**
     * Sets the corner radius for the checkboxes and updates the adapter
     * accordingly.
     *
     * @param checkBoxCornerRadius the new corner radius to set for the checkboxes
     */
    public void setCheckBoxCornerRadius(@Dimension int checkBoxCornerRadius) {
        this.checkBoxCornerRadius = checkBoxCornerRadius;
        usersAdapter.setCheckBoxCornerRadius(checkBoxCornerRadius);
    }

    /**
     * Gets the stroke color for the checkbox.
     *
     * @return the stroke color for the checkbox.
     */
    public @ColorInt int getCheckBoxStrokeColor() {
        return checkBoxStrokeColor;
    }

    /**
     * Sets the stroke color for the checkbox.
     *
     * @param checkBoxStrokeColor the stroke color for the checkbox to set.
     */
    public void setCheckBoxStrokeColor(@ColorInt int checkBoxStrokeColor) {
        this.checkBoxStrokeColor = checkBoxStrokeColor;
        usersAdapter.setCheckBoxStrokeColor(checkBoxStrokeColor);
    }

    /**
     * Gets the background color for the checkbox.
     *
     * @return the background color for the checkbox.
     */
    public @ColorInt int getCheckBoxBackgroundColor() {
        return checkBoxBackgroundColor;
    }

    /**
     * Sets the background color for the checkbox.
     *
     * @param checkBoxBackgroundColor the background color for the checkbox to set.
     */
    public void setCheckBoxBackgroundColor(@ColorInt int checkBoxBackgroundColor) {
        this.checkBoxBackgroundColor = checkBoxBackgroundColor;
        usersAdapter.setCheckBoxBackgroundColor(checkBoxBackgroundColor);
    }

    /**
     * Gets the background color for the checked checkbox.
     *
     * @return the background color for the checked checkbox.
     */
    public @ColorInt int getCheckBoxCheckedBackgroundColor() {
        return checkBoxCheckedBackgroundColor;
    }

    /**
     * Sets the background color for the checked checkbox.
     *
     * @param checkBoxCheckedBackgroundColor the background color for the checked checkbox to set.
     */
    public void setCheckBoxCheckedBackgroundColor(@ColorInt int checkBoxCheckedBackgroundColor) {
        this.checkBoxCheckedBackgroundColor = checkBoxCheckedBackgroundColor;
        usersAdapter.setCheckBoxCheckedBackgroundColor(checkBoxCheckedBackgroundColor);
    }

    /**
     * Gets the text appearance for the empty state.
     *
     * @return the text appearance for the empty state.
     */
    public @StyleRes int getEmptyStateTextAppearance() {
        return emptyStateTextAppearance;
    }

    /**
     * Sets the text appearance for the empty state.
     *
     * @param emptyStateTextAppearance the text appearance for the empty state to set.
     */
    public void setEmptyStateTextAppearance(@StyleRes int emptyStateTextAppearance) {
        this.emptyStateTextAppearance = emptyStateTextAppearance;
        binding.tvEmptyStateTitle.setTextAppearance(emptyStateTextAppearance);
    }

    /**
     * Gets the text color for the empty state.
     *
     * @return the text color for the empty state.
     */
    public @ColorInt int getEmptyStateTextColor() {
        return emptyStateTextColor;
    }

    /**
     * Sets the text color for the empty state.
     *
     * @param emptyStateTextColor the text color for the empty state to set.
     */
    public void setEmptyStateTextColor(@ColorInt int emptyStateTextColor) {
        this.emptyStateTextColor = emptyStateTextColor;
        binding.tvEmptyStateTitle.setTextColor(emptyStateTextColor);
    }

    /**
     * Gets the text appearance for the empty state subtitle.
     *
     * @return the text appearance for the empty state subtitle.
     */
    public @StyleRes int getEmptyStateSubTitleTextAppearance() {
        return emptyStateSubTitleTextAppearance;
    }

    /**
     * Sets the text appearance for the empty state subtitle.
     *
     * @param emptyStateSubTitleTextAppearance the text appearance for the empty state subtitle to set.
     */
    public void setEmptyStateSubTitleTextAppearance(@StyleRes int emptyStateSubTitleTextAppearance) {
        this.emptyStateSubTitleTextAppearance = emptyStateSubTitleTextAppearance;
        binding.tvEmptyStateSubtitle.setTextAppearance(emptyStateSubTitleTextAppearance);
    }

    /**
     * Gets the text color for the empty state subtitle.
     *
     * @return the text color for the empty state subtitle.
     */
    public @ColorInt int getEmptyStateSubtitleTextColor() {
        return emptyStateSubtitleTextColor;
    }

    /**
     * Sets the text color for the empty state subtitle.
     *
     * @param emptyStateSubtitleTextColor the text color for the empty state subtitle to set.
     */
    public void setEmptyStateSubtitleTextColor(@ColorInt int emptyStateSubtitleTextColor) {
        this.emptyStateSubtitleTextColor = emptyStateSubtitleTextColor;
        binding.tvEmptyStateSubtitle.setTextColor(emptyStateSubtitleTextColor);
    }

    /**
     * Gets the text appearance for the error state.
     *
     * @return the text appearance for the error state.
     */
    public @StyleRes int getErrorStateTextAppearance() {
        return errorStateTextAppearance;
    }

    /**
     * Sets the text appearance for the error state.
     *
     * @param errorStateTextAppearance the text appearance for the error state to set.
     */
    public void setErrorStateTextAppearance(@StyleRes int errorStateTextAppearance) {
        this.errorStateTextAppearance = errorStateTextAppearance;
        binding.tvErrorStateTitle.setTextAppearance(errorStateTextAppearance);
    }

    /**
     * Gets the text color for the error state.
     *
     * @return the text color for the error state.
     */
    public @ColorInt int getErrorStateTextColor() {
        return errorStateTextColor;
    }

    /**
     * Sets the text color for the error state.
     *
     * @param errorStateTextColor the text color for the error state to set.
     */
    public void setErrorStateTextColor(@ColorInt int errorStateTextColor) {
        this.errorStateTextColor = errorStateTextColor;
        binding.tvErrorStateTitle.setTextColor(errorStateTextColor);
    }

    /**
     * Returns the text appearance resource ID for the error state subtitle.
     *
     * @return the error state subtitle text appearance as a resource ID
     */
    public @StyleRes int getErrorStateSubtitleTextAppearance() {
        return errorStateSubtitleTextAppearance;
    }

    /**
     * Sets the text appearance for the error state subtitle and updates the view
     * accordingly.
     *
     * @param errorStateSubtitleTextAppearance the new text appearance resource ID for the error state subtitle
     */
    public void setErrorStateSubtitleTextAppearance(@StyleRes int errorStateSubtitleTextAppearance) {
        this.errorStateSubtitleTextAppearance = errorStateSubtitleTextAppearance;
        binding.tvErrorStateSubtitle.setTextAppearance(errorStateSubtitleTextAppearance);
    }

    /**
     * Gets the text color for the error state subtitle.
     *
     * @return the text color for the error state subtitle.
     */
    public @ColorInt int getErrorStateSubtitleColor() {
        return errorStateSubtitleColor;
    }

    /**
     * Sets the text color for the error state subtitle.
     *
     * @param errorStateSubtitleColor the text color for the error state subtitle to set.
     */
    public void setErrorStateSubtitleColor(@ColorInt int errorStateSubtitleColor) {
        this.errorStateSubtitleColor = errorStateSubtitleColor;
        binding.tvErrorStateSubtitle.setTextColor(errorStateSubtitleColor);
    }

    /**
     * Gets the text color for the retry button.
     *
     * @return the text color for the retry button.
     */
    public @ColorInt int getRetryButtonTextColor() {
        return retryButtonTextColor;
    }

    /**
     * Sets the text color for the retry button.
     *
     * @param retryButtonTextColor the text color for the retry button to set.
     */
    public void setRetryButtonTextColor(@ColorInt int retryButtonTextColor) {
        this.retryButtonTextColor = retryButtonTextColor;
        binding.btnRetry.setTextColor(retryButtonTextColor);
    }

    /**
     * Gets the text appearance for the retry button.
     *
     * @return the text appearance for the retry button.
     */
    public @StyleRes int getRetryButtonTextAppearance() {
        return retryButtonTextAppearance;
    }

    /**
     * Sets the text appearance for the retry button.
     *
     * @param retryButtonTextAppearance the text appearance for the retry button to set.
     */
    public void setRetryButtonTextAppearance(@StyleRes int retryButtonTextAppearance) {
        this.retryButtonTextAppearance = retryButtonTextAppearance;
        binding.btnRetry.setTextAppearance(retryButtonTextAppearance);
    }

    /**
     * Gets the background color for the retry button.
     *
     * @return the background color for the retry button.
     */
    public @ColorInt int getRetryButtonBackgroundColor() {
        return retryButtonBackgroundColor;
    }

    /**
     * Sets the background color for the retry button.
     *
     * @param retryButtonBackgroundColor the background color for the retry button to set.
     */
    public void setRetryButtonBackgroundColor(@ColorInt int retryButtonBackgroundColor) {
        this.retryButtonBackgroundColor = retryButtonBackgroundColor;
        binding.btnRetry.setBackgroundColor(retryButtonBackgroundColor);
    }

    /**
     * Returns the stroke color for the retry button.
     *
     * @return the retry button stroke color as an integer value
     */
    public @ColorInt int getRetryButtonStrokeColor() {
        return retryButtonStrokeColor;
    }

    /**
     * Sets the stroke color for the retry button and updates the button
     * accordingly.
     *
     * @param retryButtonStrokeColor the new stroke color to set for the retry button
     */
    public void setRetryButtonStrokeColor(@ColorInt int retryButtonStrokeColor) {
        this.retryButtonStrokeColor = retryButtonStrokeColor;
        binding.btnRetry.setStrokeColor(ColorStateList.valueOf(retryButtonStrokeColor));
    }

    /**
     * Returns the stroke width for the retry button.
     *
     * @return the retry button stroke width as an integer value
     */
    public @Dimension int getRetryButtonStrokeWidth() {
        return retryButtonStrokeWidth;
    }

    /**
     * Sets the stroke width for the retry button and updates the button
     * accordingly.
     *
     * @param retryButtonStrokeWidth the new stroke width to set for the retry button
     */
    public void setRetryButtonStrokeWidth(@Dimension int retryButtonStrokeWidth) {
        this.retryButtonStrokeWidth = retryButtonStrokeWidth;
        binding.btnRetry.setStrokeWidth(retryButtonStrokeWidth);
    }

    /**
     * Returns the corner radius for the retry button.
     *
     * @return the retry button corner radius as an integer value
     */
    public @Dimension int getRetryButtonCornerRadius() {
        return retryButtonCornerRadius;
    }

    /**
     * Sets the corner radius for the retry button and updates the button
     * accordingly.
     *
     * @param retryButtonCornerRadius the new corner radius to set for the retry button
     */
    public void setRetryButtonCornerRadius(@Dimension int retryButtonCornerRadius) {
        this.retryButtonCornerRadius = retryButtonCornerRadius;
        binding.btnRetry.setCornerRadius(retryButtonCornerRadius);
    }

    /**
     * Sets the error listener for handling errors that occur in the CometChatUsers
     * class.
     *
     * @param onError The error listener to be set.
     */
    public void setOnError(OnError onError) {
        this.onError = onError;
    }

    /**
     * Retrieves a list of selected users from the HashMap of users.
     *
     * @return The list of selected users.
     */
    public List<User> getSelectedUsers() {
        List<User> userList = new ArrayList<>();
        for (HashMap.Entry<User, Boolean> entry : hashMap.entrySet()) {
            userList.add(entry.getKey());
        }
        return userList;
    }

    /**
     * Clears the current selection of users. This method resets the selection count
     * to zero, hides the selection count visibility, and informs the users adapter
     * to deselect any selected users.
     */
    public void clearSelection() {
        hashMap.clear();
        setSelectionCount(0);
        setSelectionCountVisibility(GONE);
        usersAdapter.selectUser(hashMap);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        usersViewModel.addListeners();
        usersViewModel.fetchUser();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        usersViewModel.removeListeners();
    }

    public Function2<Context, User, List<CometChatOption>> setOverflowMenuOptions() {
        return options;
    }

    /**
     * Sets the options for displaying CometChat options in the getContext() menu
     * for a user.
     *
     * @param options The function that provides the list of CometChat options.
     */
    public void setOverflowMenuOptions(Function2<Context, User, List<CometChatOption>> options) {
        this.options = options;
    }

    /**
     * Disables or enables the presence status of users .
     *
     * @param disable {@code true} to disable the presence status, {@code false} to
     *                enable it.
     */
    public void disableUsersPresence(boolean disable) {
        usersAdapter.setDisableUsersPresence(disable);
    }

    /**
     * Sets the empty state text for displaying a message when there are no users.
     *
     * @param message The message to be displayed in the empty state.
     */
    public void setEmptyStateTitleText(String message) {
        if (message != null && !message.isEmpty()) {
            binding.tvErrorStateTitle.setText(message);
        } else {
            binding.tvEmptyStateTitle.setText(getResources().getString(R.string.cometchat_user_list_empty_state_title));
        }
    }

    /**
     * Sets the subtitle text for the empty state. If the provided message is null
     * or empty, a default message is set instead.
     *
     * @param message the subtitle message to display in the empty state
     */
    public void setEmptyStateSubtitleText(String message) {
        if (message != null && !message.isEmpty()) {
            binding.tvEmptyStateSubtitle.setText(message);
        } else {
            binding.tvEmptyStateSubtitle.setText(getResources().getString(R.string.cometchat_user_list_empty_state_subtitle));
        }
    }

    /**
     * Sets the error state text.
     *
     * @param errorTitleText The error state text to be set.
     */
    public void setErrorStateTitleText(String errorTitleText) {
        if (errorTitleText != null) {
            binding.tvErrorStateTitle.setText(errorTitleText);
        } else {
            binding.tvErrorStateTitle.setText(getResources().getString(R.string.cometchat_user_list_error_state_title));
        }
    }

    /**
     * Sets the empty state view layout resource.
     *
     * @param id The layout resource ID for the empty state view.
     */
    public void setEmptyStateView(@LayoutRes int id) {
        if (id != 0) {
            try {
                emptyView = View.inflate(getContext(), id, null);
            } catch (Exception e) {
                emptyView = null;
            }
        }
    }

    /**
     * setErrorStateView is method allows you to set layout, show when there is a
     * error if user want to set Error layout other wise it will load default layout
     *
     * @param id The layout resource ID for the error state view.
     */
    public void setErrorStateView(@LayoutRes int id) {
        if (id != 0) {
            try {
                errorView = View.inflate(getContext(), id, null);
            } catch (Exception e) {
                errorView = null;
            }
        }
    }

    /**
     * Sets the loading state view layout resource.
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
     * Sets the custom view for each user item in the list.
     *
     * @param listItemView The listener interface that defines callbacks for interactions
     *                     with the user list item view. This method allows you to specify a
     *                     custom view to be used for each item in the user list. The
     *                     provided `UsersViewHolderListener` interface defines callbacks
     *                     that will be invoked when various interactions occur with the list
     *                     item view, such as: * Clicking on the user item * Long pressing on
     *                     the user item * Triggering other actions specific to the user item
     *                     view By implementing the `UsersViewHolderListener` interface and
     *                     passing an instance to this method, you can customize the
     *                     appearance and behavior of each user item in the list according to
     *                     your specific needs.
     */
    public void setListItemView(UsersViewHolderListener listItemView) {
        usersAdapter.setListItemView(listItemView);
    }

    /**
     * Sets the custom view for the tail element at the end of the user list.
     *
     * @param tailView The listener interface that defines callbacks for interactions
     *                 with the tail view. This method allows you to specify a custom
     *                 view to be displayed at the end of the user list item." The
     *                 provided `UsersViewHolderListener` interface defines callbacks
     *                 that will be invoked when various interactions occur with the tail
     *                 view, allowing you to customize its behavior based on user
     *                 actions.
     */
    public void setTailView(UsersViewHolderListener tailView) {
        usersAdapter.setTailView(tailView);
    }

    /**
     * Sets the custom view for the subtitle area below the user name in each list
     * item.
     *
     * @param subTitleView The listener interface that defines callbacks for interactions
     *                     with the subtitle view. This method allows you to specify a custom
     *                     view to be displayed below the main title or name of each user
     *                     item. This "subtitle" area can be used to display additional
     *                     information about the user, such as: * User status (online,
     *                     offline, busy) * User role (administrator, moderator, member) *
     *                     Any other relevant details you want to present alongside the user
     *                     name The provided `UsersViewHolderListener` interface defines
     *                     callbacks that will be invoked when various interactions occur
     *                     with the subtitle view, allowing you to customize its behavior
     *                     based on user actions.
     */
    public void setSubtitleView(UsersViewHolderListener subTitleView) {
        usersAdapter.setSubtitleView(subTitleView);
    }

    /**
     * Sets the subtitle text for the error state. If the provided error subtitle
     * text is null, a default error message is set instead.
     *
     * @param errorSubtitleText the subtitle message to display in the error state
     */
    private void setErrorStateSubtitleText(String errorSubtitleText) {
        if (errorSubtitleText != null) {
            binding.tvErrorStateSubtitle.setText(errorSubtitleText);
        } else {
            binding.tvErrorStateSubtitle.setText(getResources().getString(R.string.cometchat_user_list_error_state_subtitle));
        }
    }

    /**
     * Returns the drawable resource for the search input star icon.
     *
     * @return the search input star icon drawable
     */
    public Drawable getSearchInputStarIcon() {
        return searchInputStarIcon;
    }

    /**
     * Sets the drawable resource for the search input star icon and updates the
     * search box accordingly.
     *
     * @param searchInputStarIcon the new drawable to set as the search input star icon
     */
    public void setSearchInputStarIcon(Drawable searchInputStarIcon) {
        this.searchInputStarIcon = searchInputStarIcon;
        binding.searchBox.setSearchInputStartIcon(searchInputStarIcon);
    }

    /**
     * Returns the tint color for the search input star icon.
     *
     * @return the search input star icon tint as an integer value
     */
    public @ColorInt int getSearchInputStarIconTint() {
        return searchInputStarIconTint;
    }

    /**
     * Sets the tint color for the search input star icon and updates the search box
     * accordingly.
     *
     * @param searchInputStarIconTint the new tint color to set for the search input star icon
     */
    public void setSearchInputStarIconTint(@ColorInt int searchInputStarIconTint) {
        this.searchInputStarIconTint = searchInputStarIconTint;
        binding.searchBox.setSearchInputStartIconTint(searchInputStarIconTint);
    }

    /**
     * Returns the drawable resource for the search input end icon.
     *
     * @return the search input end icon drawable
     */
    public Drawable getSearchInputEndIcon() {
        return searchInputEndIcon;
    }

    /**
     * Sets the drawable resource for the search input end icon and updates the
     * search box accordingly.
     *
     * @param searchInputEndIcon the new drawable to set as the search input end icon
     */
    public void setSearchInputEndIcon(Drawable searchInputEndIcon) {
        this.searchInputEndIcon = searchInputEndIcon;
        binding.searchBox.setSearchInputEndIcon(searchInputEndIcon);
    }

    /**
     * Returns the tint color for the search input end icon.
     *
     * @return the search input end icon tint as an integer value
     */
    public @ColorInt int getSearchInputEndIconTint() {
        return searchInputEndIconTint;
    }

    /**
     * Sets the tint color for the search input end icon and updates the search box
     * accordingly.
     *
     * @param searchInputEndIconTint the new tint color to set for the search input end icon
     */
    public void setSearchInputEndIconTint(@ColorInt int searchInputEndIconTint) {
        this.searchInputEndIconTint = searchInputEndIconTint;
        binding.searchBox.setSearchInputEndIconTint(searchInputEndIconTint);
    }

    /**
     * Sets the avatar style for the user adapter.
     *
     * @param style The style to apply to the avatars in the user adapter.
     */
    public void setAvatarStyle(@StyleRes int style) {
        usersAdapter.setAvatarStyle(style);
    }

    /**
     * Sets the listener for user selection events.
     *
     * @param onSelection The listener to handle user selection events.
     */
    public void setOnSelection(OnSelection onSelection) {
        if (onSelection != null) {
            this.onSelection = onSelection;
        }
    }

    /**
     * Sets the users request builder for the users view model.
     *
     * @param usersRequestBuilder The users request builder to set.
     */
    public void setUsersRequestBuilder(UsersRequest.UsersRequestBuilder usersRequestBuilder) {
        usersViewModel.setUsersRequestBuilder(usersRequestBuilder);
    }

    /**
     * Sets the search request builder for the users view model.
     *
     * @param usersRequestBuilder The search request builder to set.
     */
    public void setSearchRequestBuilder(UsersRequest.UsersRequestBuilder usersRequestBuilder) {
        usersViewModel.setSearchRequestBuilder(usersRequestBuilder);
    }

    /**
     * Sets the selection mode for the users view.
     *
     * @param selectionMode The selection mode to set.
     */
    public void setSelectionMode(@NonNull UIKitConstants.SelectionMode selectionMode) {
        hashMap.clear();
        usersAdapter.selectUser(hashMap);
        this.selectionMode = selectionMode;
        if (UIKitConstants.SelectionMode.MULTIPLE.equals(selectionMode) || UIKitConstants.SelectionMode.SINGLE.equals(selectionMode)) {
            isFurtherSelectionEnabled = true;
            hideSelectionCount = false;
            hideDiscardSelectionIcon = false;
            hideSubmitSelectionIcon = false;
            usersAdapter.isSelectionEnabled(true);
            setDiscardSelectionVisibility(VISIBLE);
            setSubmitSelectionIconVisibility(VISIBLE);
            setSelectionCountVisibility(VISIBLE);
        } else {
            isFurtherSelectionEnabled = false;
            usersAdapter.isSelectionEnabled(false);
            setDiscardSelectionVisibility(GONE);
            setSubmitSelectionIconVisibility(GONE);
            setSelectionCountVisibility(GONE);
        }
    }

    /**
     * Checks if further selection is enabled.
     *
     * @return true if further selection is enabled, false otherwise
     */
    public boolean isFurtherSelectionEnabled() {
        return isFurtherSelectionEnabled;
    }

    /**
     * Returns the view associated with the overflow menu.
     *
     * @return the {@code View} representing the overflow menu.
     */
    public View getOverflowMenu() {
        return overflowMenu;
    }

    /**
     * Sets the overflow menu view.
     *
     * @param view The view to be set as the overflow menu.
     */
    public void setOverflowMenu(View view) {
        this.overflowMenu = view;
        if (view != null) {
            Utils.handleView(binding.overflowMenuLayout, view, true);
        }
    }

    /**
     * Returns the binding for the CometChat user list.
     *
     * @return the binding object for the user list
     */
    public CometchatUserListBinding getBinding() {
        return binding;
    }

    /**
     * Returns the UsersViewModel associated with this view.
     *
     * @return the UsersViewModel instance
     */
    public UsersViewModel getViewModel() {
        return usersViewModel;
    }

    /**
     * Returns the UsersAdapter associated with this view.
     *
     * @return the UsersAdapter instance
     */
    public UsersAdapter getConversationsAdapter() {
        return usersAdapter;
    }

    /**
     * Observer that handles state changes for the UI.
     */
    Observer<UIKitConstants.States> stateChangeObserver = states -> {
        hideAllStates();

        switch (states) {
            case LOADING:
                handleLoadingState();
                break;
            case NON_EMPTY:
                handleNonEmptyState();
                break;
            case ERROR:
                handleErrorState();
                break;
            case EMPTY:
                handleEmptyState();
                break;
        }
    };

    private void handleLoadingState() {
        if (binding.searchBox.getBinding().etSearch.getText() != null && binding.searchBox.getBinding().etSearch.getText().toString().trim().isEmpty() && !binding.searchBox.getBinding().etSearch.isFocused()) {
            if (customLoadingView != null) {
                Utils.handleView(binding.customLoadingLayout, customLoadingView, true);
            } else {
                if (showShimmer) {
                    showShimmer = false;
                    setShimmerVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void handleNonEmptyState() {
        setRecyclerViewVisibility(View.VISIBLE);
    }

    private void handleErrorState() {
        if (errorView != null) {
            Utils.handleView(binding.customLoadingLayout, errorView, true);
        } else {
            setErrorStateVisibility(View.VISIBLE);
        }
    }

    private void handleEmptyState() {
        if (emptyView != null) {
            Utils.handleView(binding.customLoadingLayout, emptyView, true);
        } else {
            setEmptyStateVisibility(View.VISIBLE);
        }
    }

    /**
     * Hides all possible UI states like recycler view, empty state, error state,
     * shimmer, and custom loader.
     */
    private void hideAllStates() {
        setRecyclerViewVisibility(isUserListEmpty ? View.GONE : View.VISIBLE);
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
        binding.recyclerViewList.setVisibility(visibility);
    }

    /**
     * Returns the visibility status of the error state.
     *
     * @return true if the error state is hidden, false otherwise.
     */
    public boolean hideErrorState() {
        return hideErrorState;
    }

    /**
     * Sets the error state visibility based on the provided parameter.
     *
     * @param hideErrorState true to hide the error state, false to show it.
     */
    public void hideErrorState(boolean hideErrorState) {
        this.hideErrorState = hideErrorState;
        setErrorStateVisibility(hideErrorState ? View.GONE : View.VISIBLE);
    }

    /**
     * Sets the visibility of the error state view.
     *
     * @param visibility Visibility constant (View.VISIBLE, View.GONE, etc.).
     */
    private void setErrorStateVisibility(int visibility) {
        if (hideErrorState || visibility == GONE) {
            binding.errorStateView.setVisibility(GONE);
        } else {
            binding.errorStateView.setVisibility(VISIBLE);
        }
    }

    /**
     * Returns the visibility status of the empty state.
     *
     * @return true if the empty state is hidden, false otherwise.
     */
    public boolean hideEmptyState() {
        return hideEmptyState;
    }

    /**
     * Sets the empty state visibility based on the provided parameter.
     *
     * @param hideEmptyState true to hide the empty state, false to show it.
     */
    public void hideEmptyState(boolean hideEmptyState) {
        this.hideEmptyState = hideEmptyState;
        setEmptyStateVisibility(hideEmptyState ? View.GONE : View.VISIBLE);
    }

    /**
     * Sets the visibility of the empty state view.
     *
     * @param visibility Visibility constant (View.VISIBLE, View.GONE, etc.).
     */
    private void setEmptyStateVisibility(int visibility) {
        if (hideEmptyState || visibility == GONE) {
            binding.emptyStateView.setVisibility(View.GONE);
        } else {
            binding.emptyStateView.setVisibility(VISIBLE);
        }
    }

    /**
     * Sets the visibility of the custom loader view.
     *
     * @param visibility Visibility constant (View.VISIBLE, View.GONE, etc.).
     */
    private void setCustomLoaderVisibility(int visibility) {
        binding.customLoadingLayout.setVisibility(visibility);
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
            binding.shimmerRecyclerviewList.setAdapter(adapter);
            binding.shimmerEffectFrame.setShimmer(CometChatShimmerUtils.getCometChatShimmerConfig(getContext()));
            binding.shimmerEffectFrame.startShimmer();
        }
        binding.shimmerParentLayout.setVisibility(visibility);
    }

    /**
     * Observer to handle insertion of items at the top of the list.
     */
    Observer<Integer> insertAtTop = new Observer<Integer>() {
        @Override
        public void onChanged(Integer integer) {
            usersAdapter.notifyItemInserted(integer);
            scrollToTop();
        }
    };

    /**
     * Observer to handle moving items to the top of the list.
     */
    Observer<Integer> moveToTop = new Observer<Integer>() {
        @Override
        public void onChanged(Integer integer) {
            usersAdapter.notifyItemMoved(integer, 0);
            usersAdapter.notifyItemChanged(0);
            scrollToTop();
        }
    };

    /**
     * Scrolls the recycler view to the top if the first visible item position is
     * less than 5.
     */
    private void scrollToTop() {
        if (layoutManager.findFirstVisibleItemPosition() < 5) layoutManager.scrollToPosition(0);
    }

    /**
     * Observer to handle updates to items in the list.
     */
    Observer<Integer> update = new Observer<Integer>() {
        @Override
        public void onChanged(Integer integer) {
            usersAdapter.notifyItemChanged(integer);
        }
    };

    /**
     * Observer to handle removal of items from the list.
     */
    Observer<Integer> remove = new Observer<Integer>() {
        @Override
        public void onChanged(Integer integer) {
            usersAdapter.notifyItemRemoved(integer);
        }
    };

    /**
     * Observer to handle exceptions that occur in the CometChat framework.
     */
    Observer<CometChatException> exceptionObserver = exception -> {
        if (onError != null) onError.onError(getContext(), exception);
    };

    /**
     * This method helps to get Click events of CometChatUserList
     *
     * @param onItemClickListener object of the OnItemClickListener
     */
    public void setItemClickListener(OnItemClickListener<User> onItemClickListener) {
        if (onItemClickListener != null) this.onItemClickListener = onItemClickListener;
    }

    /**
     * Selects a user in the users view with the specified selection mode.
     *
     * @param user The user to select.
     * @param mode The selection mode to apply.
     */
    public void selectUser(User user, @Nullable UIKitConstants.SelectionMode mode) {
        if (mode != null && user != null) {
            this.selectionMode = mode;
            if (UIKitConstants.SelectionMode.SINGLE.equals(selectionMode)) {
                hashMap.clear();
                hashMap.put(user, true);
                usersAdapter.selectUser(hashMap);
            } else if (UIKitConstants.SelectionMode.MULTIPLE.equals(selectionMode)) {
                if (hashMap.containsKey(user)) {
                    hashMap.remove(user);
                } else {
                    if (isFurtherSelectionEnabled) {
                        hashMap.put(user, true);
                    }
                }
                if (hashMap.isEmpty()) {
                    setDiscardSelectionVisibility(GONE);
                    setSubmitSelectionIconVisibility(GONE);
                    setSelectionCountVisibility(GONE);
                    setTitleVisibility(VISIBLE);
                } else {
                    setSelectionCount(hashMap.size());
                    setDiscardSelectionVisibility(VISIBLE);
                    setSubmitSelectionIconVisibility(VISIBLE);
                    setSelectionCountVisibility(VISIBLE);
                }
                usersAdapter.selectUser(hashMap);
            }
        }
    }

    /**
     * Returns the visibility status of the discard selection icon.
     *
     * @return true if the discard selection icon is hidden, false otherwise.
     */
    public boolean hideDiscardSelectionIcon() {
        return hideDiscardSelectionIcon;
    }

    /**
     * Hides or shows the discard selection icon based on the provided flag.
     *
     * @param hideDiscardSelectionIcon true to hide the discard selection icon, false to show it.
     */
    public void hideDiscardSelectionIcon(boolean hideDiscardSelectionIcon) {
        this.hideDiscardSelectionIcon = hideDiscardSelectionIcon;
        setDiscardSelectionVisibility(hideDiscardSelectionIcon ? GONE : VISIBLE);
    }

    /**
     * Sets the visibility of the discard selection icon based on the provided
     * visibility parameter. If {@code hideDiscardSelectionIcon} is true or the
     * visibility is set to {@code View.GONE}, the discard selection icon will be
     * hidden. Otherwise, the discard selection icon will be visible, and the back
     * icon will be hidden.
     *
     * @param visibility the desired visibility state of the discard selection icon.
     */
    private void setDiscardSelectionVisibility(int visibility) {
        if (hideDiscardSelectionIcon || visibility == View.GONE) {
            binding.ivDiscardSelection.setVisibility(View.GONE);
        } else {
            binding.ivDiscardSelection.setVisibility(View.VISIBLE);
            binding.ivBack.setVisibility(View.GONE);
        }
    }

    /**
     * Hides or shows the search box container.
     *
     * @param hideSearchBox true to hide the search box container, false to show it.
     */
    public void hideSearchBox(boolean hideSearchBox) {
        this.hideSearchBox = hideSearchBox;
        setSearchBoxVisibility(hideSearchBox ? GONE : VISIBLE);
    }

    /**
     * Returns the visibility status of the search box.
     *
     * @return true if the search box is hidden, false otherwise.
     */
    public boolean hideSearchBox() {
        return hideSearchBox;
    }

    /**
     * Sets the visibility of the search box based on the provided visibility
     * parameter. If {@code
     * hideSearchBox} is true, the search box will be hidden (set to
     * {@code View.GONE}). Otherwise, the visibility will be set according to the
     * provided {@code visibility} parameter.
     *
     * @param visibility the desired visibility state of the search box.
     */
    private void setSearchBoxVisibility(int visibility) {
        if (hideSearchBox || visibility == GONE) {
            binding.searchBox.setVisibility(GONE);
        } else {
            binding.searchBox.setVisibility(VISIBLE);
        }
    }

    /**
     * Returns the visibility status of the submit selection icon.
     *
     * @return true if the submit selection icon is hidden, false otherwise.
     */
    public boolean hideSubmitSelectionIcon() {
        return hideSubmitSelectionIcon;
    }

    /**
     * Hides or shows the submit selection icon.
     *
     * @param hideSubmitSelectionIcon true to hide the submit selection icon, false to show it.
     */
    public void hideSubmitSelectionIcon(boolean hideSubmitSelectionIcon) {
        this.hideSubmitSelectionIcon = hideSubmitSelectionIcon;
        setSubmitSelectionIconVisibility(hideSubmitSelectionIcon ? GONE : VISIBLE);
    }

    /**
     * Sets the visibility of the submit selection icon based on the provided
     * visibility parameter. If {@code hideSubmitSelectionIcon} is true, the icon
     * will be hidden (set to {@code View.GONE}). Otherwise, the visibility will be
     * set according to the provided {@code visibility} parameter.
     *
     * @param visibility the desired visibility state of the submit selection icon.
     */
    private void setSubmitSelectionIconVisibility(int visibility) {
        if (hideSubmitSelectionIcon || visibility == GONE) {
            binding.ivSubmitSelection.setVisibility(GONE);
        } else {
            binding.ivSubmitSelection.setVisibility(VISIBLE);
        }
    }

    /**
     * Returns the visibility status of the selection count.
     *
     * @return true if the selection count is hidden, false otherwise.
     */
    public boolean hideSelectionCount() {
        return hideSelectionCount;
    }

    /**
     * Hides or shows the selection count display.
     *
     * @param hideSelectionCount true to hide the selection count, false to show it.
     */
    public void hideSelectionCount(boolean hideSelectionCount) {
        this.hideSelectionCount = hideSelectionCount;
        setSelectionCountVisibility(hideSelectionCount ? GONE : VISIBLE);
    }

    /**
     * Sets the visibility of the selection count based on the provided visibility
     * parameter. If {@code hideSelectionCount} is true, the selection count will be
     * hidden (set to {@code GONE}). Otherwise, the visibility will be set according
     * to the provided {@code visibility} parameter.
     *
     * @param visibility the desired visibility state of the selection count.
     */
    private void setSelectionCountVisibility(int visibility) {
        if (hideSelectionCount || visibility == GONE) {
            binding.tvSelectionCount.setVisibility(GONE);
        } else {
            binding.tvSelectionCount.setVisibility(VISIBLE);
            setTitleVisibility(GONE);
        }
    }

    /**
     * Sets the selection count text.
     *
     * @param count the number of selected items
     */
    public void setSelectionCount(int count) {
        binding.tvSelectionCount.setText(String.valueOf(count));
    }

    /**
     * Hides or shows the title view based on the provided flag.
     *
     * @param hideTitle true to hide the title, false to show it
     */
    public void hideTitle(boolean hideTitle) {
        this.hideTitle = hideTitle;
        setTitleVisibility(hideTitle ? GONE : VISIBLE);
    }

    /**
     * Sets the visibility of the title based on the provided visibility parameter.
     * If {@code
     * hideTitle} is true, the title will be hidden (set to {@code GONE}).
     * Otherwise, the visibility of the title will be set according to the provided
     * {@code visibility} parameter.
     *
     * @param visibility the desired visibility state of the title.
     */
    private void setTitleVisibility(int visibility) {
        if (hideTitle || visibility == GONE) {
            binding.tvTitle.setVisibility(GONE);
        } else {
            binding.tvTitle.setVisibility(VISIBLE);
        }
    }

    /**
     * Sets the title text.
     *
     * @param title the title text to set
     * @throws NullPointerException if the title is null
     */
    public void setTitleText(@NonNull String title) {
        binding.tvTitle.setText(title);
    }

    /**
     * Returns the search placeholder text.
     *
     * @return the search placeholder text
     */
    public String getSearchPlaceholderText() {
        return searchPlaceholderText;
    }

    /**
     * Sets the search placeholder text.
     *
     * @param searchPlaceholderText the placeholder text to set
     */
    public void setSearchPlaceholderText(String searchPlaceholderText) {
        this.searchPlaceholderText = searchPlaceholderText;
        binding.searchBox.setSearchPlaceholderText(searchPlaceholderText);
    }

    /**
     * Observer to handle updates to the user list.
     */
    Observer<List<User>> listObserver = users -> {
        isUserListEmpty = users.isEmpty();
        usersAdapter.setUserList(users);
    };

    /**
     * Returns the current OnError listener.
     *
     * @return the OnError listener
     */
    public OnError getOnError() {
        return onError;
    }

    /**
     * Interface for handling selection events.
     */
    public interface OnSelection {
        void onSelection(List<User> userList);
    }

    /**
     * Returns the current OnBackPress listener.
     *
     * @return the OnBackPress listener
     */
    public OnBackPress getOnBackPress() {
        return onBackPress;
    }

    /**
     * Adds a listener for back press events.
     *
     * @param onBackPress the OnBackPress listener to add
     */
    public void addOnBackPressListener(OnBackPress onBackPress) {
        if (onBackPress != null) {
            this.onBackPress = onBackPress;
        }
    }

    /**
     * Hides or shows the toolbar based on the provided flag.
     *
     * @param hideToolbar true to hide the toolbar, false to show it
     */
    public void hideToolbar(boolean hideToolbar) {
        binding.toolbar.setVisibility(hideToolbar ? GONE : VISIBLE);
    }

    /**
     * Returns the checkbox select icon drawable.
     *
     * @return the checkbox select icon
     */
    public Drawable getCheckBoxSelectIcon() {
        return checkBoxSelectIcon;
    }

    /**
     * Sets the checkbox select icon drawable.
     *
     * @param checkBoxSelectIcon the drawable to set as the checkbox select icon
     */
    public void setCheckBoxSelectIcon(Drawable checkBoxSelectIcon) {
        this.checkBoxSelectIcon = checkBoxSelectIcon;
        usersAdapter.setCheckBoxSelectIcon(checkBoxSelectIcon);
    }

    /**
     * Returns the tint color for the checkbox select icon.
     *
     * @return the checkbox select icon tint color
     */
    public @ColorInt int getCheckBoxSelectIconTint() {
        return checkBoxSelectIconTint;
    }

    /**
     * Sets the tint color for the checkbox select icon.
     *
     * @param checkBoxSelectIconTint the tint color to set
     */
    public void setCheckBoxSelectIconTint(@ColorInt int checkBoxSelectIconTint) {
        this.checkBoxSelectIconTint = checkBoxSelectIconTint;
        usersAdapter.setCheckBoxSelectIconTint(checkBoxSelectIconTint);
    }
}
