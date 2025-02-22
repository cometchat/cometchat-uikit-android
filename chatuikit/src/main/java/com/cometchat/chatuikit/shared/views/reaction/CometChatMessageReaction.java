package com.cometchat.chatuikit.shared.views.reaction;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.StyleRes;

import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chat.models.ReactionCount;
import com.cometchat.chatuikit.databinding.CometchatMessageReactionsBinding;
import com.cometchat.chatuikit.shared.views.reaction.interfaces.OnAddMoreReactionsClick;
import com.cometchat.chatuikit.shared.views.reaction.interfaces.OnReactionClick;
import com.cometchat.chatuikit.shared.views.reaction.interfaces.OnReactionLongClick;

import java.util.List;

/**
 * CometChatMessageReaction class is a custom view in Android that represents
 * message reactions. It extends the RelativeLayout class and provides a way to
 * display reactions for a message. The view can be set with a BaseMessage
 * object, which contains the reactions for the message. The class uses the
 * CometChatReaction class to build the individual reaction chips and add them
 * to the view. It also provides customization options for the background color,
 * border color, border width, and border radius of the reaction chips. The
 * class supports handling click events on the reaction chips and provides
 * interfaces for setting reaction click listeners and reaction long click
 * listeners. Created on: 13 September 2024 Modified on:
 */
public class CometChatMessageReaction extends RelativeLayout {
    private static final String TAG = CometChatMessageReaction.class.getSimpleName();
    private CometchatMessageReactionsBinding binding;

    private OnAddMoreReactionsClick onAddMoreReactionsClick;
    private OnReactionClick onReactionClick;
    private OnReactionLongClick onReactionLongClick;

    private @StyleRes int reactionStyle;

    /**
     * Constructor for creating a CometChatMessageReaction instance.
     *
     * @param context The context of the current state of the application.
     */
    public CometChatMessageReaction(Context context) {
        this(context, null);
    }

    /**
     * Constructor for creating a CometChatMessageReaction instance.
     *
     * @param context The context of the current state of the application.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public CometChatMessageReaction(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructor for creating a CometChatMessageReaction instance.
     *
     * @param context      The context of the current state of the application.
     * @param attrs        The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr An attribute in the current theme that contains a reference to a
     *                     style resource that supplies default values for the view. Can be 0
     *                     to not look for defaults.
     */
    public CometChatMessageReaction(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateAndInitializeView();
    }

    /**
     * Inflates and initializes the view by setting up the layout, retrieving the
     * attributes, and applying styles.
     */
    private void inflateAndInitializeView() {
        binding = CometchatMessageReactionsBinding.inflate(LayoutInflater.from(getContext()), this, true);
    }

    /**
     * Binds and displays reactions for the given message. This method is
     * responsible for populating the parent view with reaction chips representing
     * the reactions to the given message. It limits the number of reactions
     * displayed to a defined limit (REACTION_LIMIT) and, if necessary, adds a "view
     * more" chip to indicate additional reactions.
     *
     * @param baseMessage The message object containing reaction information to be
     *                    displayed.
     */
    public void bindReactionsToMessage(BaseMessage baseMessage, int reactionLimit) {
        List<ReactionCount> reactionsList = baseMessage.getReactions();
        binding.parentView.removeAllViews(); // Always start by removing views
        if (reactionsList.isEmpty()) {
            binding.parentView.setVisibility(GONE);
            return;
        }
        binding.parentView.setVisibility(VISIBLE);
        // Limit the reactions to display based on the size
        int maxLimit = Math.min(reactionsList.size(), reactionLimit);
        // Add reactions to the view
        for (int i = 0; i < maxLimit; i++) {
            ReactionCount reactionCount = reactionsList.get(i);
            binding.parentView.addView(getReactionChip(reactionCount.getReaction(),
                                                       reactionCount.getCount(),
                                                       reactionCount.getReactedByMe(),
                                                       v -> handleReactionClick(baseMessage, reactionCount),
                                                       v -> handleReactionLongClick(baseMessage, reactionCount)));
        }
        // Handle the "view more" chip if reactions exceed the limit
        if (reactionsList.size() > reactionLimit) {
            int viewMoreCount = reactionsList.size() - reactionLimit;
            boolean flag = isReactedByMeBeyondLimit(reactionsList, reactionLimit);
            binding.parentView.addView(getReactionChip("+", viewMoreCount, flag, v -> handleViewMoreClick(baseMessage), null));
        }
    }

    /**
     * Creates and returns a view representing a reaction chip. This method
     * constructs a reaction chip using the given emoji, count, and state (whether
     * the user has reacted to it). It sets click and long click listeners for user
     * interaction.
     *
     * @param emoji          The emoji representing the reaction.
     * @param count          The number of users who have reacted with this emoji.
     * @param reactedByMe    Whether the user has reacted with this emoji.
     * @param clickEvent     The click event handler for the reaction chip.
     * @param longClickEvent The long click event handler for the reaction chip.
     * @return A view representing the reaction chip.
     */
    private View getReactionChip(String emoji, int count, boolean reactedByMe, OnClickListener clickEvent, OnLongClickListener longClickEvent) {
        CometChatReaction cometchatReaction = new CometChatReaction(getContext());
        cometchatReaction.setStyle(reactionStyle);
        return cometchatReaction.buildReactionView(emoji, count, reactedByMe, clickEvent, longClickEvent);
    }

    /**
     * Handles the click event on a reaction chip. This method triggers the
     * appropriate reaction action based on the current state: - If a reaction click
     * listener is set, it invokes that listener. - If the user has already reacted
     * to the message, it removes the reaction. - Otherwise, it adds the reaction.
     *
     * @param baseMessage   The message associated with the reaction.
     * @param reactionCount The reaction count object containing reaction details.
     */
    private void handleReactionClick(BaseMessage baseMessage, ReactionCount reactionCount) {
        if (onReactionClick != null) {
            onReactionClick.onClick(reactionCount.getReaction(), baseMessage);
        }
    }

    /**
     * Handles the long click event on a reaction chip. This method triggers the
     * appropriate long click action: - If a reaction long click listener is set, it
     * invokes that listener. - Otherwise, it triggers the default long click action
     * for the reaction.
     *
     * @param baseMessage   The message associated with the reaction.
     * @param reactionCount The reaction count object containing reaction details.
     * @return Always returns false, indicating that the event has not been fully
     * handled.
     */
    private boolean handleReactionLongClick(BaseMessage baseMessage, ReactionCount reactionCount) {
        if (onReactionLongClick != null) {
            onReactionLongClick.onReactionLongClick(reactionCount.getReaction(), baseMessage);
        }
        return false;
    }

    /**
     * Checks if the user has reacted to any reactions beyond the given limit. This
     * method scans through the reactions beyond the specified limit and returns
     * true if the user has reacted to any of those reactions.
     *
     * @param reactionsList The list of reactions for the message.
     * @param limit         The limit beyond which reactions should be checked.
     * @return True if the user has reacted to any reactions beyond the limit,
     * otherwise false.
     */
    private boolean isReactedByMeBeyondLimit(List<ReactionCount> reactionsList, int limit) {
        for (int i = limit - 1; i < reactionsList.size(); i++) {
            if (reactionsList.get(i).getReactedByMe()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handles the click event on the "view more reactions" chip. This method
     * triggers either the reaction click listener or the default action to open the
     * full list of reactions for the message.
     *
     * @param baseMessage The message for which additional reactions should be displayed.
     */
    private void handleViewMoreClick(BaseMessage baseMessage) {
        if (onAddMoreReactionsClick != null) {
            onAddMoreReactionsClick.onClick(baseMessage);
        }
    }

    /**
     * Sets the style for the reaction chips.
     */
    public void setStyle(@StyleRes int reactionStyle) {
        this.reactionStyle = reactionStyle;
    }

    public void setOnAddMoreReactionsClick(OnAddMoreReactionsClick onAddMoreReactionsClick) {
        this.onAddMoreReactionsClick = onAddMoreReactionsClick;
    }

    public void setOnReactionClick(OnReactionClick onReactionClick) {
        this.onReactionClick = onReactionClick;
    }

    public void setOnReactionLongClick(OnReactionLongClick onReactionLongClick) {
        this.onReactionLongClick = onReactionLongClick;
    }
}
