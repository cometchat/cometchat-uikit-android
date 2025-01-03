package com.cometchat.chatuikit.groups;

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
import androidx.annotation.StyleRes;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chat.core.GroupsRequest;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chat.models.Group;
import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.databinding.CometchatGroupListBinding;
import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.interfaces.Function2;
import com.cometchat.chatuikit.shared.interfaces.OnBackPress;
import com.cometchat.chatuikit.shared.interfaces.OnError;
import com.cometchat.chatuikit.shared.models.CometChatOption;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.cometchat.chatuikit.shared.resources.utils.itemclicklistener.OnItemClickListener;
import com.cometchat.chatuikit.shared.resources.utils.recycler_touch.ClickListener;
import com.cometchat.chatuikit.shared.resources.utils.recycler_touch.RecyclerTouchListener;
import com.cometchat.chatuikit.shared.viewholders.GroupsViewHolderListener;
import com.cometchat.chatuikit.shared.views.searchbox.CometChatSearchBox;
import com.cometchat.chatuikit.shimmer.CometChatShimmerAdapter;
import com.cometchat.chatuikit.shimmer.CometChatShimmerUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The CometChatGroups class is a custom view in Android that extends the
 * MaterialCardView class. It is designed to represent and manage group-related
 * functionalities within the CometChat framework. This class can be utilized to
 * display group information, facilitate group interactions, and integrate
 * various group functionalities, providing a seamless user experience for group
 * chats. Created on: 22 October 2024
 */
public class CometChatGroups extends MaterialCardView {
    private static final String TAG = CometChatGroups.class.getSimpleName();

    private CometchatGroupListBinding binding;

    private boolean showShimmer = true;
    private boolean isGroupListEmpty = true;

    private GroupsViewModel groupsViewModel;
    private GroupsAdapter groupsAdapter;
    private LinearLayoutManager layoutManager;

    private boolean hideTitle = false;
    private boolean hideBackIcon = true;
    private boolean hideSearchBox = false;
    private boolean hideSelectionCount = true;
    private boolean hideDiscardSelectionIcon = true;
    private boolean hideSubmitSelectionIcon = true;
    private boolean hideErrorState = false;
    private boolean hideEmptyState = false;

    private boolean isFurtherSelectionEnabled = true;
    private String searchPlaceholderText;

    private View customEmptyView = null;
    private View customErrorView = null;
    private View customLoadingView = null;
    private View overflowMenu = null;

    private OnError onError;
    private OnSelection onSelection;
    private OnItemClickListener<Group> onItemClickListener;
    private OnBackPress onBackPress;

    private final HashMap<Group, Boolean> hashMap = new HashMap<>();
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

    private @StyleRes int subtitleTextAppearance;
    private @ColorInt int subtitleTextColor;

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
    private Drawable searchInputIcon;

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
    private int separatorVisibility;

    /**
     * The CometChatGroups class is a custom view in Android that extends the
     * MaterialCardView class. It is designed to represent and manage group-related
     * functionalities within the CometChat framework. This class can be utilized to
     * display group information, facilitate group interactions, and integrate
     * various group functionalities, providing a seamless user experience for group
     * chats.
     *
     * <p>
     * Created on: 22 October 2024
     */
    public CometChatGroups(Context context) {
        this(context, null);
    }

    /**
     * Constructs a CometChatGroups view with the specified context and attribute
     * set.
     *
     * @param context The context in which the view is running.
     * @param attrs   The attribute set containing the view's attributes.
     */
    public CometChatGroups(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.cometchatGroupsStyle);
    }

    /**
     * Constructs a CometChatGroups view with the specified context, attribute set,
     * and default style attribute.
     *
     * @param context      The context in which the view is running.
     * @param attrs        The attribute set containing the view's attributes.
     * @param defStyleAttr The default style attribute to apply.
     */
    public CometChatGroups(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode()) {
            inflateAndInitializeView(attrs, defStyleAttr);
        }
    }

    /**
     * Inflates the view layout and initializes its components based on the provided
     * attributes and default style.
     *
     * @param attrs        The attribute set containing the view's attributes.
     * @param defStyleAttr The default style attribute to apply.
     */
    private void inflateAndInitializeView(AttributeSet attrs, int defStyleAttr) {
        // Inflate the layout for this view
        binding = CometchatGroupListBinding.inflate(LayoutInflater.from(getContext()), this, true);

        // Reset the card view to default values
        Utils.initMaterialCard(this);

        // Set default values
        init();

        // Apply style attributes
        applyStyleAttributes(attrs, defStyleAttr);
    }

    /**
     * Initializes the view components such as the RecyclerView, ViewModels, and
     * click events.
     */
    private void init() {
        initRecyclerView();
        initViewModels();
        initClickEvents();
    }

    /**
     * Sets up the RecyclerView with an adapter and layout manager, and adds a
     * scroll listener.
     */
    private void initRecyclerView() {
        groupsAdapter = new GroupsAdapter(getContext());

        binding.recyclerViewList.setAdapter(groupsAdapter);
        layoutManager = new LinearLayoutManager(getContext());
        binding.recyclerViewList.setLayoutManager(layoutManager);

        binding.recyclerViewList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (!recyclerView.canScrollVertically(1)) {
                    groupsViewModel.fetchGroup();
                }
            }
        });
    }

    /**
     * Initializes the ViewModels and sets up observers for live data changes.
     */
    private void initViewModels() {
        groupsViewModel = new ViewModelProvider.NewInstanceFactory().create(GroupsViewModel.class);
        groupsViewModel.getMutableGroupsList().observe((LifecycleOwner) getContext(), listObserver);
        groupsViewModel.getStates().observe((LifecycleOwner) getContext(), stateChangeObserver);
        groupsViewModel.insertAtTop().observe((LifecycleOwner) getContext(), insertAtTop);
        groupsViewModel.moveToTop().observe((LifecycleOwner) getContext(), moveToTop);
        groupsViewModel.updateGroup().observe((LifecycleOwner) getContext(), update);
        groupsViewModel.removeGroup().observe((LifecycleOwner) getContext(), remove);
        groupsViewModel.getCometChatException().observe((LifecycleOwner) getContext(), exceptionObserver);
    }

    /**
     * Initializes click event listeners for the RecyclerView and search box.
     */
    private void initClickEvents() {
        binding.recyclerViewList.addOnItemTouchListener(new RecyclerTouchListener(getContext(), binding.recyclerViewList, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Group group = (Group) view.getTag(R.string.cometchat_group);
                if (!UIKitConstants.SelectionMode.NONE.equals(selectionMode)) {
                    selectGroup(group, selectionMode);
                }
                if (onItemClickListener != null) {
                    onItemClickListener.OnItemClick(group, position);
                }
            }

            @Override
            public void onLongClick(@NonNull View view, int position) {
                Group group = (Group) view.getTag(R.string.cometchat_group);
                if (onItemClickListener != null) {
                    onItemClickListener.OnItemLongClick(group, position);
                }
            }
        }));

        binding.searchBox.addOnSearchListener((state, text) -> {
            if (state.equals(CometChatSearchBox.SearchState.TextChange)) {
                if (text.isEmpty()) {
                    groupsViewModel.searchGroups(null);
                } else {
                    groupsViewModel.searchGroups(text);
                }
            }
        });

        binding.btnRetry.setOnClickListener(v -> {
            showShimmer = true;
            groupsViewModel.fetchGroup();
        });

        binding.ivDiscardSelection.setOnClickListener(v -> {
            clearSelection();
        });

        binding.ivSubmitSelection.setOnClickListener(v -> {
            if (onSelection != null) {
                onSelection.onSelection(getSelectedGroups());
            }
        });

        binding.ivBack.setOnClickListener(v -> {
            if (onBackPress != null) {
                onBackPress.onBack();
            }
        });
    }

    /**
     * Applies the style attributes defined in XML to this view.
     *
     * @param attrs        The attribute set containing the styling attributes.
     * @param defStyleAttr The default style attribute to apply.
     */
    private void applyStyleAttributes(AttributeSet attrs, int defStyleAttr) {
        TypedArray directAttributes = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CometChatGroups, defStyleAttr, 0);
        @StyleRes int styleResId = directAttributes.getResourceId(R.styleable.CometChatGroups_cometchatGroupsStyle, 0);
        directAttributes = styleResId != 0 ? getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CometChatGroups, defStyleAttr, styleResId) : null;
        extractAttributesAndApplyDefaults(directAttributes);
    }

    /**
     * Sets the style for this view from the provided style resource.
     *
     * @param style The style resource ID to apply.
     */
    public void setStyle(@StyleRes int style) {
        if (style != 0) {
            TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(style, R.styleable.CometChatGroups);
            extractAttributesAndApplyDefaults(typedArray);
        }
    }

    /**
     * Extracts attributes from the provided TypedArray and applies default values
     * to this view.
     *
     * @param typedArray The TypedArray containing the attributes to extract.
     */
    private void extractAttributesAndApplyDefaults(TypedArray typedArray) {
        if (typedArray == null) return;
        try {
            // Extract attributes or apply default values
            backgroundColor = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsBackgroundColor, 0);
            titleTextColor = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsTitleTextColor, 0);
            titleTextAppearance = typedArray.getResourceId(R.styleable.CometChatGroups_cometchatGroupsTitleTextAppearance, 0);
            strokeWidth = typedArray.getDimensionPixelSize(R.styleable.CometChatGroups_cometchatGroupsStrokeWidth, 0);
            strokeColor = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsStrokeColor, 0);
            cornerRadius = typedArray.getDimensionPixelSize(R.styleable.CometChatGroups_cometchatGroupsCornerRadius, 0);
            backIcon = typedArray.getDrawable(R.styleable.CometChatGroups_cometchatGroupsBackIcon);
            backIconTint = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsBackIconTint, 0);
            separatorColor = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsSeparatorColor, 0);
            discardSelectionIcon = typedArray.getDrawable(R.styleable.CometChatGroups_cometchatGroupsDiscardSelectionIcon);
            discardSelectionIconTint = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsDiscardSelectionIconTint, 0);
            submitSelectionIcon = typedArray.getDrawable(R.styleable.CometChatGroups_cometchatGroupsSubmitSelectionIcon);
            submitSelectionIconTint = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsSubmitSelectionIconTint, 0);
            searchInputStarIcon = typedArray.getDrawable(R.styleable.CometChatGroups_cometchatGroupsSearchInputStartIcon);
            searchInputStarIconTint = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsSearchInputStartIconTint, 0);
            searchInputEndIcon = typedArray.getDrawable(R.styleable.CometChatGroups_cometchatGroupsSearchInputEndIcon);
            searchInputEndIconTint = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsSearchInputEndIconTint, 0);
            searchInputStrokeWidth = typedArray.getDimensionPixelSize(R.styleable.CometChatGroups_cometchatGroupsSearchInputStrokeWidth, 0);
            searchInputStrokeColor = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsSearchInputStrokeColor, 0);
            searchInputCornerRadius = typedArray.getDimensionPixelSize(R.styleable.CometChatGroups_cometchatGroupsSearchInputCornerRadius, 0);
            searchInputBackgroundColor = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsSearchInputBackgroundColor, 0);
            searchInputTextAppearance = typedArray.getResourceId(R.styleable.CometChatGroups_cometchatGroupsSearchInputTextAppearance, 0);
            searchInputTextColor = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsSearchInputTextColor, 0);
            searchInputPlaceHolderTextAppearance = typedArray.getResourceId(R.styleable.CometChatGroups_cometchatGroupsSearchInputPlaceHolderTextAppearance, 0);
            searchInputPlaceHolderTextColor = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsSearchInputPlaceHolderTextColor, 0);
            searchInputIcon = typedArray.getDrawable(R.styleable.CometChatGroups_cometchatGroupsSearchInputIcon);
            searchInputIconTint = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsSearchInputIconTint, 0);
            avatar = typedArray.getResourceId(R.styleable.CometChatGroups_cometchatGroupsAvatarStyle, 0);
            itemTitleTextAppearance = typedArray.getResourceId(R.styleable.CometChatGroups_cometchatGroupsItemTitleTextAppearance, 0);
            itemTitleTextColor = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsItemTitleTextColor, 0);
            itemBackgroundColor = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsItemBackgroundColor, 0);
            statusIndicatorStyle = typedArray.getResourceId(R.styleable.CometChatGroups_cometchatGroupsStatusIndicator, 0);
            itemSelectedBackgroundColor = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsItemSelectedBackgroundColor, 0);
            checkBoxStrokeWidth = typedArray.getDimensionPixelSize(R.styleable.CometChatGroups_cometchatGroupsCheckBoxStrokeWidth, 0);
            checkBoxCornerRadius = typedArray.getDimensionPixelSize(R.styleable.CometChatGroups_cometchatGroupsCheckBoxCornerRadius, 0);
            checkBoxStrokeColor = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsCheckBoxStrokeColor, 0);
            checkBoxBackgroundColor = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsCheckBoxBackgroundColor, 0);
            checkBoxSelectIconTint = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsCheckBoxSelectIconTint, 0);
            checkBoxSelectIcon = typedArray.getDrawable(R.styleable.CometChatGroups_cometchatGroupsCheckBoxSelectIcon);
            checkBoxCheckedBackgroundColor = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsCheckBoxCheckedBackgroundColor, 0);
            emptyStateTextAppearance = typedArray.getResourceId(R.styleable.CometChatGroups_cometchatGroupsEmptyStateTextAppearance, 0);
            emptyStateTextColor = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsEmptyStateTextColor, 0);
            emptyStateSubTitleTextAppearance = typedArray.getResourceId(R.styleable.CometChatGroups_cometchatGroupsEmptyStateSubTitleTextAppearance, 0);
            emptyStateSubtitleTextColor = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsEmptyStateSubtitleTextColor, 0);
            errorStateTextAppearance = typedArray.getResourceId(R.styleable.CometChatGroups_cometchatGroupsErrorStateTextAppearance, 0);
            errorStateTextColor = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsErrorStateTextColor, 0);
            errorStateSubtitleTextAppearance = typedArray.getResourceId(R.styleable.CometChatGroups_cometchatGroupsErrorStateSubtitleTextAppearance, 0);
            errorStateSubtitleColor = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsErrorStateSubtitleColor, 0);
            retryButtonTextColor = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsRetryButtonTextColor, 0);
            retryButtonTextAppearance = typedArray.getResourceId(R.styleable.CometChatGroups_cometchatGroupsRetryButtonTextAppearance, 0);
            retryButtonBackgroundColor = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsRetryButtonBackgroundColor, 0);
            retryButtonStrokeColor = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsRetryButtonStrokeColor, 0);
            retryButtonStrokeWidth = typedArray.getDimensionPixelSize(R.styleable.CometChatGroups_cometchatGroupsRetryButtonStrokeWidth, 0);
            retryButtonCornerRadius = typedArray.getDimensionPixelSize(R.styleable.CometChatGroups_cometchatGroupsRetryButtonCornerRadius, 0);
            subtitleTextAppearance = typedArray.getResourceId(R.styleable.CometChatGroups_cometchatGroupsSubtitleTextAppearance, 0);
            subtitleTextColor = typedArray.getColor(R.styleable.CometChatGroups_cometchatGroupsSubtitleColor, 0);

            // Apply default styles
            updateUI();
        } finally {
            typedArray.recycle();
        }
    }

    /**
     * Updates the UI components of this view based on the extracted attributes.
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
        setSubtitleTextAppearance(subtitleTextAppearance);
        setSubtitleTextColor(subtitleTextColor);

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
     * Returns the visibility status of the back icon.
     *
     * @return true if the back icon is hidden, false otherwise.
     */
    public boolean hideBackIcon() {
        return hideBackIcon;
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
            binding.ivDiscardSelection.setVisibility(GONE);
        }
    }

    /**
     * Returns the drawable resource associated with the back icon.
     *
     * @return the {@code Drawable} for the back icon.
     */
    public Drawable getBackIcon() {
        return backIcon;
    }

    /**
     * Returns the {@code ImageView} instance for the back icon.
     *
     * @return the {@code ImageView} that represents the back icon.
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

    public @ColorInt int getSeparatorColor() {
        return separatorColor;
    }

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
        groupsAdapter.setAvatarStyle(avatar);
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
        groupsAdapter.setItemTitleTextAppearance(itemTitleTextAppearance);
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
        groupsAdapter.setItemTitleTextColor(itemTitleTextColor);
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
        groupsAdapter.setItemBackgroundColor(itemBackgroundColor);
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
        groupsAdapter.setStatusIndicatorStyle(statusIndicatorStyle);
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
        groupsAdapter.setItemSelectedBackgroundColor(itemSelectedBackgroundColor);
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
        groupsAdapter.setCheckBoxStrokeWidth(checkBoxStrokeWidth);
    }

    public @Dimension int getCheckBoxCornerRadius() {
        return checkBoxCornerRadius;
    }

    public void setCheckBoxCornerRadius(@Dimension int checkBoxCornerRadius) {
        this.checkBoxCornerRadius = checkBoxCornerRadius;
        groupsAdapter.setCheckBoxCornerRadius(checkBoxCornerRadius);
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
        groupsAdapter.setCheckBoxStrokeColor(checkBoxStrokeColor);
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
        groupsAdapter.setCheckBoxBackgroundColor(checkBoxBackgroundColor);
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
        groupsAdapter.setCheckBoxCheckedBackgroundColor(checkBoxCheckedBackgroundColor);
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
     * Retrieves the style resource ID for the error state subtitle text appearance.
     *
     * @return The style resource ID for the error state subtitle text appearance.
     */
    public @StyleRes int getErrorStateSubtitleTextAppearance() {
        return errorStateSubtitleTextAppearance;
    }

    /**
     * Sets the style resource ID for the error state subtitle text appearance and
     * applies it to the subtitle view.
     *
     * @param errorStateSubtitleTextAppearance The style resource ID to set for the error state subtitle text
     *                                         appearance.
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
     * Retrieves the color for the retry button's stroke.
     *
     * @return The color for the retry button's stroke.
     */
    public @ColorInt int getRetryButtonStrokeColor() {
        return retryButtonStrokeColor;
    }

    /**
     * Sets the color for the retry button's stroke and updates the button's
     * appearance.
     *
     * @param retryButtonStrokeColor The color to set for the retry button's stroke.
     */
    public void setRetryButtonStrokeColor(@ColorInt int retryButtonStrokeColor) {
        this.retryButtonStrokeColor = retryButtonStrokeColor;
        binding.btnRetry.setStrokeColor(ColorStateList.valueOf(retryButtonStrokeColor));
    }

    /**
     * Retrieves the stroke width for the retry button.
     *
     * @return The stroke width for the retry button.
     */
    public @Dimension int getRetryButtonStrokeWidth() {
        return retryButtonStrokeWidth;
    }

    /**
     * Sets the stroke width for the retry button and updates the button's
     * appearance.
     *
     * @param retryButtonStrokeWidth The stroke width to set for the retry button.
     */
    public void setRetryButtonStrokeWidth(@Dimension int retryButtonStrokeWidth) {
        this.retryButtonStrokeWidth = retryButtonStrokeWidth;
        binding.btnRetry.setStrokeWidth(retryButtonStrokeWidth);
    }

    /**
     * Retrieves the corner radius for the retry button.
     *
     * @return The corner radius for the retry button.
     */
    public @Dimension int getRetryButtonCornerRadius() {
        return retryButtonCornerRadius;
    }

    /**
     * Sets the corner radius for the retry button and updates the button's
     * appearance.
     *
     * @param retryButtonCornerRadius The corner radius to set for the retry button.
     */
    public void setRetryButtonCornerRadius(@Dimension int retryButtonCornerRadius) {
        this.retryButtonCornerRadius = retryButtonCornerRadius;
        binding.btnRetry.setCornerRadius(retryButtonCornerRadius);
    }

    /**
     * Sets the OnError listener for handling error events.
     *
     * @param onError The OnError listener to set.
     */
    public void setOnError(OnError onError) {
        this.onError = onError;
    }

    /**
     * Retrieves a list of selected groups.
     *
     * @return A list of selected groups.
     */
    public List<Group> getSelectedGroups() {
        List<Group> groupList = new ArrayList<>();
        for (HashMap.Entry<Group, Boolean> entry : hashMap.entrySet()) {
            groupList.add(entry.getKey());
        }
        return groupList;
    }

    /**
     * Called when the view is attached to a window. Adds listeners and fetches
     * groups.
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        groupsViewModel.addListeners();
        groupsViewModel.fetchGroup();
    }

    /**
     * Called when the view is detached from a window. Removes listeners.
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        groupsViewModel.removeListeners();
    }

    /**
     * Sets the options for displaying CometChat options in the getContext() menu
     * for a group.
     *
     * @param options The function that provides the list of CometChat options.
     */
    public void setOverflowMenuOptions(Function2<Context, Group, List<CometChatOption>> options) {
    }

    /**
     * Sets the title text for the empty state. If the provided message is null or
     * empty, a default empty state title is used.
     *
     * @param message The message to set as the empty state title. If null or empty, a
     *                default title is used.
     */
    public void setEmptyStateTitleText(String message) {
        if (message != null && !message.isEmpty()) {
            binding.tvErrorStateTitle.setText(message);
        } else {
            binding.tvEmptyStateTitle.setText(getResources().getString(R.string.cometchat_group_list_empty_state_title));
        }
    }

    /**
     * Sets the subtitle text for the empty state. If the provided message is null
     * or empty, a default empty state subtitle is used.
     *
     * @param message The message to set as the empty state subtitle. If null or empty,
     *                a default subtitle is used.
     */
    public void setEmptyStateSubtitleText(String message) {
        if (message != null && !message.isEmpty()) {
            binding.tvEmptyStateSubtitle.setText(message);
        } else {
            binding.tvEmptyStateSubtitle.setText(getResources().getString(R.string.cometchat_group_list_empty_state_subtitle));
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
            binding.tvErrorStateTitle.setText(getResources().getString(R.string.cometchat_group_list_error_state_title));
        }
    }

    /**
     * Sets the subtitle text for the error state. If the provided error subtitle
     * text is null, a default error state subtitle is used.
     *
     * @param errorSubtitleText The subtitle text to set for the error state. If null, a default
     *                          subtitle is used.
     */
    public void setErrorStateSubtitleText(String errorSubtitleText) {
        if (errorSubtitleText != null) {
            binding.tvErrorStateSubtitle.setText(errorSubtitleText);
        } else {
            binding.tvErrorStateSubtitle.setText(getResources().getString(R.string.cometchat_group_list_error_state_subtitle));
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
                customEmptyView = View.inflate(getContext(), id, null);
            } catch (Exception e) {
                customEmptyView = null;
                CometChatLogger.e(TAG, e.toString());
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
                customErrorView = View.inflate(getContext(), id, null);
            } catch (Exception e) {
                customErrorView = null;
                CometChatLogger.e(TAG, e.toString());
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
                CometChatLogger.e(TAG, e.toString());
            }
        }
    }

    /**
     * Sets the listener for the list item view.
     *
     * @param listItemView The listener to handle events for the list item view.
     */
    public void setListItemView(GroupsViewHolderListener listItemView) {
        groupsAdapter.setListItemView(listItemView);
    }

    /**
     * Sets the listener for the tail view.
     *
     * @param tailView The listener to handle events for the tail view.
     */
    public void setTailView(GroupsViewHolderListener tailView) {
        groupsAdapter.setTailView(tailView);
    }

    /**
     * Sets the listener for the subtitle view.
     *
     * @param subTitleView The listener to handle events for the subtitle view.
     */
    public void setSubtitleView(GroupsViewHolderListener subTitleView) {
        groupsAdapter.setSubtitleView(subTitleView);
    }

    /**
     * Retrieves the drawable resource used as the search input start icon.
     *
     * @return The drawable resource used as the search input start icon.
     */
    public Drawable getSearchInputStarIcon() {
        return searchInputStarIcon;
    }

    /**
     * Sets the drawable resource to be used as the search input start icon.
     *
     * @param searchInputStarIcon The drawable resource to set as the search input start icon.
     */
    public void setSearchInputStarIcon(Drawable searchInputStarIcon) {
        this.searchInputStarIcon = searchInputStarIcon;
        binding.searchBox.setSearchInputStartIcon(searchInputStarIcon);
    }

    /**
     * Retrieves the tint color for the search input start icon.
     *
     * @return The tint color for the search input start icon.
     */
    public @ColorInt int getSearchInputStarIconTint() {
        return searchInputStarIconTint;
    }

    /**
     * Sets the tint color for the search input start icon.
     *
     * @param searchInputStarIconTint The tint color to set for the search input start icon.
     */
    public void setSearchInputStarIconTint(@ColorInt int searchInputStarIconTint) {
        this.searchInputStarIconTint = searchInputStarIconTint;
        binding.searchBox.setSearchInputStartIconTint(searchInputStarIconTint);
    }

    /**
     * Retrieves the drawable resource used as the search input end icon.
     *
     * @return The drawable resource used as the search input end icon.
     */
    public Drawable getSearchInputEndIcon() {
        return searchInputEndIcon;
    }

    /**
     * Sets the drawable resource to be used as the search input end icon.
     *
     * @param searchInputEndIcon The drawable resource to set as the search input end icon.
     */
    public void setSearchInputEndIcon(Drawable searchInputEndIcon) {
        this.searchInputEndIcon = searchInputEndIcon;
        binding.searchBox.setSearchInputEndIcon(searchInputEndIcon);
    }

    /**
     * Retrieves the tint color for the search input end icon.
     *
     * @return The tint color for the search input end icon.
     */
    public @ColorInt int getSearchInputEndIconTint() {
        return searchInputEndIconTint;
    }

    /**
     * Sets the tint color for the search input end icon.
     *
     * @param searchInputEndIconTint The tint color to set for the search input end icon.
     */
    public void setSearchInputEndIconTint(@ColorInt int searchInputEndIconTint) {
        this.searchInputEndIconTint = searchInputEndIconTint;
        binding.searchBox.setSearchInputEndIconTint(searchInputEndIconTint);
    }

    /**
     * Sets the avatar style for the group adapter.
     *
     * @param style The style to apply to the avatars in the group adapter.
     */
    public void setAvatarStyle(@StyleRes int style) {
        groupsAdapter.setAvatarStyle(style);
    }

    /**
     * Sets the listener for group selection events.
     *
     * @param onSelection The listener to handle group selection events.
     */
    public void setOnSelection(OnSelection onSelection) {
        if (onSelection != null) {
            this.onSelection = onSelection;
        }
    }

    /**
     * Sets the GroupsRequestBuilder for fetching groups.
     *
     * @param groupsRequestBuilder The builder used to create requests for groups.
     */
    public void setGroupsRequestBuilder(GroupsRequest.GroupsRequestBuilder groupsRequestBuilder) {
        groupsViewModel.setGroupsRequestBuilder(groupsRequestBuilder);
    }

    /**
     * Sets the search request builder for the groups view model.
     *
     * @param groupsRequestBuilder The search request builder to set.
     */
    public void setSearchRequestBuilder(GroupsRequest.GroupsRequestBuilder groupsRequestBuilder) {
        groupsViewModel.setSearchRequestBuilder(groupsRequestBuilder);
    }

    /**
     * Sets the selection mode for the groups view.
     *
     * @param selectionMode The selection mode to set.
     */
    public void setSelectionMode(@NonNull UIKitConstants.SelectionMode selectionMode) {
        hashMap.clear();
        groupsAdapter.selectGroup(hashMap);
        this.selectionMode = selectionMode;
        if (UIKitConstants.SelectionMode.MULTIPLE.equals(selectionMode) || UIKitConstants.SelectionMode.SINGLE.equals(selectionMode)) {
            isFurtherSelectionEnabled = true;
            hideSelectionCount = false;
            hideDiscardSelectionIcon = false;
            hideSubmitSelectionIcon = false;
            groupsAdapter.isSelectionEnabled(true);
            setDiscardSelectionVisibility(VISIBLE);
            setSubmitSelectionIconVisibility(VISIBLE);
            setSelectionCountVisibility(VISIBLE);
        } else {
            isFurtherSelectionEnabled = false;
            groupsAdapter.isSelectionEnabled(false);
            setDiscardSelectionVisibility(GONE);
            setSubmitSelectionIconVisibility(GONE);
        }
    }

    /**
     * Checks if further selection is enabled.
     *
     * @return true if further selection is enabled, false otherwise.
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
     * Gets the binding object for the CometChatGroupList.
     *
     * @return The binding object.
     */
    public CometchatGroupListBinding getBinding() {
        return binding;
    }

    /**
     * Gets the ViewModel associated with the Groups.
     *
     * @return The GroupsViewModel.
     */
    public GroupsViewModel getViewModel() {
        return groupsViewModel;
    }

    /**
     * Gets the adapter for the group conversations.
     *
     * @return The GroupsAdapter instance.
     */
    public GroupsAdapter getConversationsAdapter() {
        return groupsAdapter;
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

    /**
     * Handles the loading state of the view based on the search box input and focus
     * status. If the search box is empty and not focused, this method either shows
     * a custom loading view or triggers the shimmer effect based on the current
     * settings.
     */
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

    /**
     * Handles the state when the content is non-empty. It sets the visibility of
     * the recycler view to visible.
     */
    private void handleNonEmptyState() {
        setRecyclerViewVisibility(View.VISIBLE);
    }

    /**
     * Handles the error state of the view. If a custom error view is set, it
     * displays that; otherwise, it makes the default error state visible.
     */
    private void handleErrorState() {
        if (customErrorView != null) {
            Utils.handleView(binding.errorStateView, customErrorView, true);
        } else {
            setErrorStateVisibility(View.VISIBLE);
        }
    }

    /**
     * Handles the empty state of the view. If a custom empty view is set, it
     * displays that; otherwise, it makes the default empty state visible.
     */
    private void handleEmptyState() {
        if (customEmptyView != null) {
            Utils.handleView(binding.emptyStateView, customEmptyView, true);
        } else {
            setEmptyStateVisibility(View.VISIBLE);
        }
    }

    /**
     * Hides all possible UI states like recycler view, empty state, error state,
     * shimmer, and custom loader.
     */
    private void hideAllStates() {
        setRecyclerViewVisibility(isGroupListEmpty ? View.GONE : View.VISIBLE);
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
            binding.emptyStateView.setVisibility(GONE);
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
     * Observer that inserts an item at the top of the list and scrolls to the top.
     */
    Observer<Integer> insertAtTop = new Observer<Integer>() {
        @Override
        public void onChanged(Integer integer) {
            groupsAdapter.notifyItemInserted(integer);
            scrollToTop();
        }
    };

    /**
     * Observer that moves an item to the top of the list and scrolls to the top.
     */
    Observer<Integer> moveToTop = new Observer<Integer>() {
        @Override
        public void onChanged(Integer integer) {
            groupsAdapter.notifyItemMoved(integer, 0);
            groupsAdapter.notifyItemChanged(0);
            scrollToTop();
        }
    };

    /**
     * Scrolls the recycler view to the top if the first visible item is less than
     * 5.
     */
    private void scrollToTop() {
        if (layoutManager.findFirstVisibleItemPosition() < 5) layoutManager.scrollToPosition(0);
    }

    /**
     * Observer that updates an item in the list.
     */
    Observer<Integer> update = new Observer<Integer>() {
        @Override
        public void onChanged(Integer integer) {
            groupsAdapter.notifyItemChanged(integer);
        }
    };

    /**
     * Observer that removes an item from the list.
     */
    Observer<Integer> remove = new Observer<Integer>() {
        @Override
        public void onChanged(Integer integer) {
            groupsAdapter.notifyItemRemoved(integer);
        }
    };

    /**
     * Observer that handles exceptions and triggers the OnError listener.
     */
    Observer<CometChatException> exceptionObserver = exception -> {
        if (onError != null) onError.onError(getContext(), exception);
    };

    /**
     * This method helps to get Click events of CometChatGroupList
     *
     * @param onItemClickListener object of the OnItemClickListener
     */
    public void setItemClickListener(OnItemClickListener<Group> onItemClickListener) {
        if (onItemClickListener != null) this.onItemClickListener = onItemClickListener;
    }

    /**
     * Selects a group in the groups view with the specified selection mode.
     *
     * @param group The group to select.
     * @param mode  The selection mode to apply.
     */
    public void selectGroup(Group group, UIKitConstants.SelectionMode mode) {
        if (mode != null && group != null) {
            this.selectionMode = mode;
            if (UIKitConstants.SelectionMode.SINGLE.equals(selectionMode)) {
                hashMap.clear();
                hashMap.put(group, true);
                groupsAdapter.selectGroup(hashMap);
            } else if (UIKitConstants.SelectionMode.MULTIPLE.equals(selectionMode)) {
                if (hashMap.containsKey(group)) {
                    hashMap.remove(group);
                } else {
                    if (isFurtherSelectionEnabled) {
                        hashMap.put(group, true);
                    }
                }
                if (hashMap.isEmpty()) {
                    setDiscardSelectionVisibility(View.GONE);
                    setSubmitSelectionIconVisibility(View.GONE);
                    setSelectionCountVisibility(GONE);
                    setTitleVisibility(VISIBLE);
                } else {
                    setSelectionCount(hashMap.size());
                    setDiscardSelectionVisibility(View.VISIBLE);
                    setSubmitSelectionIconVisibility(View.VISIBLE);
                    setSelectionCountVisibility(VISIBLE);
                    setTitleVisibility(GONE);
                }
                groupsAdapter.selectGroup(hashMap);
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
            binding.ivSubmitSelection.setVisibility(View.GONE);
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
     * @param count The number of selected items to display.
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
     * @param title The title text to display.
     */
    public void setTitleText(@NonNull String title) {
        binding.tvTitle.setText(title);
    }

    /**
     * Gets the placeholder text for the search input.
     *
     * @return The search placeholder text.
     */
    public String getSearchPlaceholderText() {
        return searchPlaceholderText;
    }

    /**
     * Sets the placeholder text for the search input.
     *
     * @param searchPlaceholderText The placeholder text to set.
     */
    public void setSearchPlaceholderText(String searchPlaceholderText) {
        this.searchPlaceholderText = searchPlaceholderText;
        binding.searchBox.setSearchPlaceholderText(searchPlaceholderText);
    }

    /**
     * Observer for the list of groups. Updates the groups adapter with the new
     * group list.
     */
    Observer<List<Group>> listObserver = groups -> {
        isGroupListEmpty = groups.isEmpty();
        groupsAdapter.setGroupList(groups);
    };

    /**
     * Gets the OnError listener for error handling.
     *
     * @return The OnError listener.
     */
    public OnError getOnError() {
        return onError;
    }

    /**
     * Interface for handling group selection events.
     */
    public interface OnSelection {
        void onSelection(List<Group> groupList);
    }

    /**
     * Gets the OnBackPress listener for back press events.
     *
     * @return The OnBackPress listener.
     */
    public OnBackPress getOnBackPress() {
        return onBackPress;
    }

    /**
     * Adds a listener for back press events.
     *
     * @param onBackPress The listener to be added.
     */
    public void addOnBackPressListener(OnBackPress onBackPress) {
        if (onBackPress != null) {
            this.onBackPress = onBackPress;
        }
    }

    /**
     * Hides or shows the toolbar based on the provided flag.
     *
     * @param hideToolbar true to hide the toolbar, false to show it.
     */
    public void hideToolbar(boolean hideToolbar) {
        binding.toolbar.setVisibility(hideToolbar ? GONE : VISIBLE);
    }

    /**
     * Gets the drawable for the checkbox select icon.
     *
     * @return The checkbox select icon drawable.
     */
    public Drawable getCheckBoxSelectIcon() {
        return checkBoxSelectIcon;
    }

    /**
     * Sets the drawable for the checkbox select icon.
     *
     * @param checkBoxSelectIcon The drawable to set as the checkbox select icon.
     */
    public void setCheckBoxSelectIcon(Drawable checkBoxSelectIcon) {
        this.checkBoxSelectIcon = checkBoxSelectIcon;
        groupsAdapter.setCheckBoxSelectIcon(checkBoxSelectIcon);
    }

    /**
     * Gets the tint color for the checkbox select icon.
     *
     * @return The checkbox select icon tint color.
     */
    public @ColorInt int getCheckBoxSelectIconTint() {
        return checkBoxSelectIconTint;
    }

    /**
     * Sets the tint color for the checkbox select icon.
     *
     * @param checkBoxSelectIconTint The tint color to set.
     */
    public void setCheckBoxSelectIconTint(@ColorInt int checkBoxSelectIconTint) {
        this.checkBoxSelectIconTint = checkBoxSelectIconTint;
        groupsAdapter.setCheckBoxSelectIconTint(checkBoxSelectIconTint);
    }

    /**
     * Gets the subtitle text color.
     *
     * @return The subtitle text color.
     */
    public @ColorInt int getSubtitleTextColor() {
        return subtitleTextColor;
    }

    /**
     * Sets the subtitle text color.
     *
     * @param subtitleTextColor The color to set for the subtitle text.
     */
    public void setSubtitleTextColor(@ColorInt int subtitleTextColor) {
        this.subtitleTextColor = subtitleTextColor;
        groupsAdapter.setSubtitleTextColor(subtitleTextColor);
    }

    /**
     * Gets the text appearance style resource for the subtitle.
     *
     * @return The subtitle text appearance style resource.
     */
    public @StyleRes int getSubtitleTextAppearance() {
        return subtitleTextAppearance;
    }

    /**
     * Sets the text appearance style resource for the subtitle.
     *
     * @param subtitleTextAppearance The style resource to set for the subtitle text appearance.
     */
    public void setSubtitleTextAppearance(@StyleRes int subtitleTextAppearance) {
        this.subtitleTextAppearance = subtitleTextAppearance;
        groupsAdapter.setSubtitleTextAppearance(subtitleTextAppearance);
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
        groupsAdapter.selectGroup(hashMap);
    }
}
