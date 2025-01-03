package com.cometchat.chatuikit.reactionlist;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cometchat.chat.constants.CometChatConstants;
import com.cometchat.chat.core.CometChat;
import com.cometchat.chat.core.ReactionsRequest;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chat.helpers.CometChatHelper;
import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chat.models.Reaction;
import com.cometchat.chat.models.ReactionCount;
import com.cometchat.chat.models.ReactionEvent;
import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit;
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKitHelper;
import com.cometchat.chatuikit.shared.constants.MessageStatus;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.events.CometChatMessageEvents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * ViewModel class for managing the reaction list in the CometChat UI Kit. This
 * class handles the logic for fetching, updating, and managing reactions and
 * reacted users.
 */
public class ReactionListViewModel extends ViewModel {
    private static final String TAG = ReactionListViewModel.class.getSimpleName();
    private final String listenerId = ReactionListViewModel.class.getSimpleName();

    private Context context;
    private boolean isRemovingReactionInProgress = false;

    private String currentRemovedReaction;
    private String currentSelectedReactionTab;
    private ReactionEvent reactionEvent;

    private final HashMap<String, ReactionsRequest> reactionRequestHashMap = new HashMap<>();
    private final HashMap<String, List<Reaction>> reactedUserCacheHashMap = new HashMap<>();

    @NonNull
    private final MutableLiveData<Integer> activeTabIndexLiveData;
    private final MutableLiveData<String> selectedReactionLiveData;
    private final MutableLiveData<BaseMessage> baseMessageLiveData;
    private final MutableLiveData<List<ReactionCount>> reactionHeaderLiveData;
    private final MutableLiveData<List<Reaction>> reactedUsersLiveData;
    private final MutableLiveData<UIKitConstants.States> loadingStateLiveData;

    /**
     * Constructor for ReactionListViewModel.
     */
    public ReactionListViewModel() {
        selectedReactionLiveData = new MutableLiveData<>();
        activeTabIndexLiveData = new MutableLiveData<>();
        baseMessageLiveData = new MutableLiveData<>();
        reactedUsersLiveData = new MutableLiveData<>();
        reactionHeaderLiveData = new MutableLiveData<>();
        loadingStateLiveData = new MutableLiveData<>();
    }

    /**
     * Gets the current context.
     *
     * @return The current context.
     */
    public Context getContext() {
        return context;
    }

    /**
     * Sets the current context.
     *
     * @param context The context to set.
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * Retrieves the current loading state as a MutableLiveData object.
     *
     * @return A MutableLiveData object representing the current loading state.
     */
    public MutableLiveData<UIKitConstants.States> getLoadingStateLiveData() {
        return loadingStateLiveData;
    }

    /**
     * Gets the LiveData for the base message.
     *
     * @return The LiveData for the base message.
     */
    public MutableLiveData<BaseMessage> getBaseMessageLiveData() {
        return baseMessageLiveData;
    }

    /**
     * Sets the LiveData for the base message.
     *
     * @param baseMessage The base message to set.
     */
    public void setBaseMessageLiveData(BaseMessage baseMessage) {
        baseMessageLiveData.setValue(baseMessage);
    }

    /**
     * Gets the LiveData for the reaction header list.
     *
     * @return The LiveData for the reaction header list.
     */
    public MutableLiveData<List<ReactionCount>> getReactionHeaderLiveData() {
        return reactionHeaderLiveData;
    }

    /**
     * Sets the LiveData for the reaction header list.
     *
     * @param reactionCounts The reaction header list to set.
     */
    public void setReactionHeaderLiveData(List<ReactionCount> reactionCounts) {
        List<ReactionCount> reactionHeaderListTemp = new ArrayList<>();
        if (!reactionCounts.isEmpty()) {
            ReactionCount reactionCount = new ReactionCount();
            int reactedCount = 0;
            for (ReactionCount r : reactionCounts) {
                reactedCount += r.getCount();
            }
            reactionCount.setCount(reactedCount);
            reactionCount.setReaction(getContext().getString(R.string.cometchat_all));
            reactionCount.setReactedByMe(false);
            reactionHeaderListTemp.add(reactionCount);
            reactionHeaderListTemp.addAll(reactionCounts);
        }
        reactionHeaderLiveData.setValue(reactionHeaderListTemp);
    }

    /**
     * Gets the LiveData for the reacted users list.
     *
     * @return The LiveData for the reacted users list.
     */
    public MutableLiveData<List<Reaction>> getReactedUsersLiveData() {
        return reactedUsersLiveData;
    }

    /**
     * Sets the LiveData for the reacted users list.
     *
     * @param reactions The reacted users list to set.
     */
    public void setMutableReactedUsersList(List<Reaction> reactions) {
        reactedUsersLiveData.setValue(reactions);
    }

    /**
     * Gets the LiveData for the selected reaction.
     *
     * @return The LiveData for the selected reaction.
     */
    public MutableLiveData<String> getSelectedReactionLiveData() {
        return selectedReactionLiveData;
    }

    /**
     * Sets the LiveData for the selected reaction.
     *
     * @param reactionFilter The selected reaction to set.
     */
    public void setSelectedReactionLiveData(String reactionFilter) {
        selectedReactionLiveData.setValue(reactionFilter);
    }

    /**
     * Gets the LiveData for the active tab.
     *
     * @return The LiveData for the active tab.
     */
    @NonNull
    public MutableLiveData<Integer> getActiveTabIndexLiveData() {
        return activeTabIndexLiveData;
    }

    /**
     * Sets the LiveData for the active tab.
     *
     * @param activeTab The active tab to set.
     */
    public void setActiveTabIndexLiveData(Integer activeTab) {
        if (reactionHeaderLiveData.getValue() != null && !reactionHeaderLiveData.getValue().isEmpty()) {
            int foundIndex = getReactionHeaderReactionIndex(selectedReactionLiveData.getValue(), reactionHeaderLiveData.getValue());
            if (foundIndex != -1) {
                activeTabIndexLiveData.setValue(foundIndex);
            }
        } else {
            activeTabIndexLiveData.setValue(activeTab);
        }
    }

    /**
     * Gets the reaction event.
     *
     * @return The reaction event.
     */
    public ReactionEvent getReactionEvent() {
        return reactionEvent;
    }

    /**
     * Sets the reaction event.
     *
     * @param reactionEvent The reaction event to set.
     */
    public void setReactionEvent(ReactionEvent reactionEvent) {
        this.reactionEvent = reactionEvent;
    }

    /**
     * Clears the reaction request hash map.
     */
    public void clearReactionRequestHashMap() {
        reactionRequestHashMap.clear();
    }

    /**
     * Clears the reacted user cache hash map.
     */
    public void clearReactedUserCacheHashMap() {
        reactedUserCacheHashMap.clear();
    }

    /**
     * Handles the removal of a reaction from a user.
     *
     * @param removedReaction The reaction that was removed.
     * @param uid             The unique identifier of the user who removed the reaction.
     */
    private void onReactionRemovedByMe(String removedReaction, String uid) {
        int newActiveTab = -2;
        int reactionHeaderIndex;
        if (reactionHeaderLiveData.getValue() != null && !reactionHeaderLiveData.getValue().isEmpty()) {
            reactionHeaderIndex = getReactionHeaderReactionIndex(removedReaction, reactionHeaderLiveData.getValue());
            if (reactionHeaderIndex != -1) {
                ReactionCount rc = reactionHeaderLiveData.getValue().get(reactionHeaderIndex);
                if (rc.getReaction().equals(removedReaction)) {
                    if (rc.getReactedByMe()) {
                        if (rc.getCount() == 1) {
                            newActiveTab = 0;
                            reactionHeaderLiveData.getValue().remove(reactionHeaderIndex);
                        } else {
                            if (!currentSelectedReactionTab.equals(context.getString(R.string.cometchat_all))) {
                                newActiveTab = reactionHeaderIndex;
                            }
                            reactionHeaderLiveData.getValue().get(reactionHeaderIndex).setCount(rc.getCount() - 1);
                            reactionHeaderLiveData.getValue().get(reactionHeaderIndex).setReactedByMe(false);
                        }
                    }
                }
                if (reactionHeaderLiveData.getValue().get(0).getCount() == 1) {
                    newActiveTab = -1;
                    reactionHeaderLiveData.getValue().remove(0);
                } else {
                    reactionHeaderLiveData.getValue().get(0).setCount(reactionHeaderLiveData.getValue().get(0).getCount() - 1);
                    reactionHeaderLiveData.getValue().get(0).setReactedByMe(false);
                }
            }
        }
        if (newActiveTab == -1) {
            reactedUserCacheHashMap.clear();
        } else {
            if (reactedUserCacheHashMap.containsKey(removedReaction)) {
                List<Reaction> reactions = reactedUserCacheHashMap.get(removedReaction);
                if (reactions != null && !reactions.isEmpty()) {
                    Iterator<Reaction> iterator = reactions.iterator();
                    while (iterator.hasNext()) {
                        Reaction reaction = iterator.next();
                        if (reaction.getReaction().equals(removedReaction) && reaction.getUid().equals(uid)) {
                            iterator.remove();
                            break;
                        }
                    }
                }
            }
            if (reactedUserCacheHashMap.containsKey(getContext().getString(R.string.cometchat_all))) {
                List<Reaction> reactions = reactedUserCacheHashMap.get(getContext().getString(R.string.cometchat_all));
                if (reactions != null && !reactions.isEmpty()) {
                    Iterator<Reaction> iterator = reactions.iterator();
                    while (iterator.hasNext()) {
                        Reaction reaction = iterator.next();
                        if (reaction.getReaction().equals(removedReaction) && reaction.getUid().equals(uid)) {
                            iterator.remove();
                            break;
                        }
                    }
                }
            }
        }
        reactionHeaderLiveData.setValue(reactionHeaderLiveData.getValue());
        if (newActiveTab == -1 || newActiveTab == 0) {
            activeTabIndexLiveData.setValue(newActiveTab);
            selectedReactionLiveData.setValue(getContext().getString(R.string.cometchat_all));
        } else if (newActiveTab > 0) {
            selectedReactionLiveData.setValue(removedReaction);
        }
    }

    /**
     * Removes a user's reaction from the cache and updates the reaction header list
     * accordingly.
     *
     * @param removedReaction The reaction to be removed (e.g., "like", "love").
     * @param uid             The unique identifier of the user whose reaction is being removed.
     */
    private void onReactionRemoved(String removedReaction, String uid) {
        int tabIndex = getReactionIndexFromHeader(currentSelectedReactionTab);

        // Update reaction header based on the removed reaction
        updateReactionHeaderList(tabIndex, getReactionIndexFromHeader(removedReaction));

        // Remove the user from the cache for the specific reaction and "All" reactions
        removeUserFromCache(removedReaction, uid, false);
        removeUserFromCache(removedReaction, uid, true);
    }

    /**
     * Finds the index of a specified reaction in the header list.
     *
     * @param removedReaction The reaction to search for in the header.
     * @return The index of the specified reaction, or -1 if not found.
     */
    private int getReactionIndexFromHeader(String removedReaction) {
        int index = -1;
        if (reactionHeaderLiveData.getValue() != null) {
            List<ReactionCount> reactions = reactionHeaderLiveData.getValue();
            for (int i = 0; i < reactions.size(); i++) {
                if (reactions.get(i).getReaction().equals(removedReaction)) {
                    index = i; // Found the index
                    break; // Exit loop once found
                }
            }
        }
        return index;
    }

    /**
     * Updates the reaction header list by adjusting reaction counts or removing
     * reactions as needed.
     *
     * @param tabIndex             The index of the currently selected reaction tab.
     * @param removedReactionIndex The index of the removed reaction in the header list.
     */
    private void updateReactionHeaderList(int tabIndex, int removedReactionIndex) {
        if (reactionHeaderLiveData.getValue() != null && removedReactionIndex != -1) {
            int newActiveTab = tabIndex;
            List<ReactionCount> currentReactionHeaderList = new ArrayList<>(reactionHeaderLiveData.getValue());

            // Decrease the count of the removed reaction
            ReactionCount removedReactionObj = currentReactionHeaderList.get(removedReactionIndex);
            removedReactionObj.setCount(removedReactionObj.getCount() - 1);

            // If the count drops below 1, remove the reaction from the list
            if (removedReactionObj.getCount() < 1) {
                newActiveTab = currentSelectedReactionTab.equals(removedReactionObj.getReaction()) ? 0 : newActiveTab;
                currentReactionHeaderList.remove(removedReactionIndex);
            } else {
                currentReactionHeaderList.set(removedReactionIndex, removedReactionObj);
            }

            // Handle the "All" tab adjustments
            if (!currentReactionHeaderList.isEmpty()) {
                ReactionCount reactionCountAll = currentReactionHeaderList.get(0);
                reactionCountAll.setCount(reactionCountAll.getCount() - 1);

                // Clear the entire list if the "All" count is less than 1
                if (reactionCountAll.getCount() < 1) {
                    currentReactionHeaderList.remove(0); // Remove the "All" tab
                } else {
                    currentRemovedReaction = context.getString(R.string.cometchat_all);
                    currentReactionHeaderList.set(0, reactionCountAll);
                }
            }

            // Update LiveData with the modified list and set the active tab index
            reactionHeaderLiveData.setValue(currentReactionHeaderList);
            activeTabIndexLiveData.setValue(newActiveTab);
        }
    }

    /**
     * Removes a user's reaction from the cache.
     *
     * @param removedReaction The reaction type being removed.
     * @param uid             The unique identifier of the user whose reaction is being removed.
     * @param isAll           Specifies whether the removal is for a specific reaction or for
     *                        "All" reactions.
     */
    private void removeUserFromCache(String removedReaction, String uid, boolean isAll) {
        String reactionKey = isAll ? context.getString(R.string.cometchat_all) : removedReaction;

        if (reactedUserCacheHashMap.containsKey(reactionKey)) {
            List<Reaction> reactions = reactedUserCacheHashMap.get(reactionKey);

            // Iterate and remove the matching reaction from the cache
            if (reactions != null && !reactions.isEmpty()) {
                Iterator<Reaction> iterator = reactions.iterator();
                while (iterator.hasNext()) {
                    Reaction reaction = iterator.next();
                    if (reaction.getReaction().equals(removedReaction) && reaction.getUid().equals(uid)) {
                        iterator.remove();
                        break;
                    }
                }
            }
        }
    }

    /**
     * Fetches the list of users who reacted to a message.
     *
     * @param reactionFilter                The filter to apply to the reactions (e.g., specific emoji).
     * @param customReactionsRequestBuilder A custom builder for the reactions request, if any.
     */
    public void getReactedUsersList(String reactionFilter, ReactionsRequest.ReactionsRequestBuilder customReactionsRequestBuilder) {
        ReactionsRequest reactionsRequest;
        if (reactionFilter != null) {
            currentSelectedReactionTab = reactionFilter;
            if (reactedUsersLiveData.getValue() != null) {
                reactedUsersLiveData.setValue(new ArrayList<>());
            }
        }
        if (reactedUserCacheHashMap.containsKey(currentSelectedReactionTab)) {
            reactedUsersLiveData.setValue(reactedUserCacheHashMap.get(currentSelectedReactionTab));
        }
        if (reactedUsersLiveData.getValue() == null || reactedUsersLiveData.getValue().isEmpty()) {
            loadingStateLiveData.setValue(UIKitConstants.States.LOADING);
        }
        if (reactionRequestHashMap.containsKey(currentSelectedReactionTab)) {
            reactionsRequest = reactionRequestHashMap.get(currentSelectedReactionTab);
        } else {
            ReactionsRequest.ReactionsRequestBuilder reactionRequestBuilder;
            if (customReactionsRequestBuilder != null) {
                reactionRequestBuilder = customReactionsRequestBuilder;
            } else {
                reactionRequestBuilder = new ReactionsRequest.ReactionsRequestBuilder().setLimit(10);
            }
            if (baseMessageLiveData.getValue() != null) {
                reactionRequestBuilder.setMessageId(baseMessageLiveData.getValue().getId());
            }
            if (currentSelectedReactionTab != null && !currentSelectedReactionTab.equals(getContext().getString(R.string.cometchat_all))) {
                reactionRequestBuilder.setReaction(currentSelectedReactionTab);
            }
            reactionsRequest = reactionRequestBuilder.build();
            reactionRequestHashMap.put(currentSelectedReactionTab, reactionsRequest);
        }
        if (reactionsRequest != null) {
            reactionsRequest.fetchNext(new CometChat.CallbackListener<List<Reaction>>() {
                @Override
                public void onSuccess(List<Reaction> messageReaction) {
                    try {
                        if (!messageReaction.isEmpty()) {
                            if (reactedUserCacheHashMap.containsKey(currentSelectedReactionTab)) {
                                if (reactedUserCacheHashMap.get(currentSelectedReactionTab) != null) {
                                    List<Reaction> reactions = reactedUserCacheHashMap.get(currentSelectedReactionTab);
                                    if (reactions != null) {
                                        reactions.addAll(messageReaction);
                                    }
                                }
                            } else {
                                reactedUserCacheHashMap.put(currentSelectedReactionTab, messageReaction);
                            }
                            reactedUsersLiveData.setValue(reactedUserCacheHashMap.get(currentSelectedReactionTab));
                        }
                        loadingStateLiveData.setValue(UIKitConstants.States.LOADED);
                    } catch (Exception e) {
                        loadingStateLiveData.setValue(UIKitConstants.States.ERROR);
                    }
                }

                @Override
                public void onError(CometChatException e) {
                    if (!e.getCode().equals(CometChatConstants.Errors.ERROR_REQUEST_IN_PROGRESS)) {
                        loadingStateLiveData.setValue(UIKitConstants.States.ERROR);
                    }
                }
            });
        }
    }

    /**
     * Finds the index of a reaction in the reaction header list.
     *
     * @param searchReaction     The reaction to search for.
     * @param reactionHeaderList The list of reaction headers.
     * @return The index of the reaction in the list, or -1 if not found.
     */
    private int getReactionHeaderReactionIndex(String searchReaction, List<ReactionCount> reactionHeaderList) {
        int foundIndex = -1;
        for (int i = 0; i < reactionHeaderList.size(); i++) {
            if (reactionHeaderList.get(i).getReaction().equals(searchReaction)) {
                foundIndex = i;
                break;
            }
        }
        return foundIndex;
    }

    /**
     * Adds a listener for CometChat message events.
     */
    public void addListener() {
        CometChatMessageEvents.addListener(listenerId, new CometChatMessageEvents() {
            @Override
            public void ccMessageEdited(BaseMessage baseMessage, int status) {
                if (baseMessageLiveData.getValue() != null && baseMessageLiveData.getValue().getId() == baseMessage.getId()) {
                    if (status == MessageStatus.SUCCESS) {
                        isRemovingReactionInProgress = false;
                        onReactionRemovedByMe(currentRemovedReaction, CometChatUIKit.getLoggedInUser().getUid());
                    }
                }
            }

            @Override
            public void onMessageReactionRemoved(ReactionEvent reactionEvent) {
                if (baseMessageLiveData.getValue() != null && baseMessageLiveData.getValue().getId() == reactionEvent.getReaction().getMessageId()) {
                    onReactionRemoved(reactionEvent.getReaction().getReaction(), reactionEvent.getReaction().getUid());
                }
            }
        });
    }

    /**
     * Removes the listener for CometChat message events.
     */
    public void removeListener() {
        CometChatMessageEvents.removeListener(listenerId);
    }

    /**
     * Removes a reaction from a message.
     *
     * @param baseMessage The message from which the reaction is to be removed.
     * @param emoji       The emoji representing the reaction to be removed.
     */
    public void removeReaction(BaseMessage baseMessage, String emoji) {
        if (!isRemovingReactionInProgress) {
            isRemovingReactionInProgress = true;
            currentRemovedReaction = emoji;
            Reaction reaction = new Reaction();
            reaction.setMessageId(baseMessage.getId());
            reaction.setReaction(emoji);
            reaction.setUid(CometChatUIKit.getLoggedInUser().getUid());
            reaction.setReactedBy(CometChatUIKit.getLoggedInUser());
            BaseMessage newBaseMessage = CometChatHelper.updateMessageWithReactionInfo(baseMessage, reaction, CometChatConstants.REACTION_REMOVED);
            CometChat.removeReaction(baseMessage.getId(), emoji, new CometChat.CallbackListener<BaseMessage>() {
                @Override
                public void onSuccess(BaseMessage baseMessage) {
                    CometChatUIKitHelper.onMessageEdited(baseMessage, MessageStatus.SUCCESS);
                }

                @Override
                public void onError(CometChatException e) {
                    isRemovingReactionInProgress = false;
                    BaseMessage modifiedBaseMessage = CometChatHelper.updateMessageWithReactionInfo(newBaseMessage, reaction, CometChatConstants.REACTION_ADDED);
                    CometChatUIKitHelper.onMessageEdited(modifiedBaseMessage, MessageStatus.SUCCESS);
                }
            });
        }
    }
}
