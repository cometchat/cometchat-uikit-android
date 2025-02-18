package com.cometchat.chatuikit.shared.cometchatuikit;

import android.content.Context;
import android.view.View;

import androidx.annotation.DrawableRes;

import com.cometchat.chat.core.Call;
import com.cometchat.chat.models.Action;
import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chat.models.Conversation;
import com.cometchat.chat.models.CustomMessage;
import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.GroupMember;
import com.cometchat.chat.models.InteractionReceipt;
import com.cometchat.chat.models.MediaMessage;
import com.cometchat.chat.models.MessageReceipt;
import com.cometchat.chat.models.ReactionEvent;
import com.cometchat.chat.models.TextMessage;
import com.cometchat.chat.models.TransientMessage;
import com.cometchat.chat.models.TypingIndicator;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.shared.constants.MessageStatus;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.events.CometChatCallEvents;
import com.cometchat.chatuikit.shared.events.CometChatConversationEvents;
import com.cometchat.chatuikit.shared.events.CometChatGroupEvents;
import com.cometchat.chatuikit.shared.events.CometChatMessageEvents;
import com.cometchat.chatuikit.shared.events.CometChatUIEvents;
import com.cometchat.chatuikit.shared.events.CometChatUserEvents;
import com.cometchat.chatuikit.shared.interfaces.Function1;
import com.cometchat.chatuikit.shared.models.interactivemessage.CardMessage;
import com.cometchat.chatuikit.shared.models.interactivemessage.CustomInteractiveMessage;
import com.cometchat.chatuikit.shared.models.interactivemessage.FormMessage;
import com.cometchat.chatuikit.shared.models.interactivemessage.SchedulerMessage;

import java.util.HashMap;
import java.util.List;

public class CometChatUIKitHelper {
    private static final String TAG = CometChatUIKitHelper.class.getSimpleName();

    public static void onMessageSent(BaseMessage message, @MessageStatus int status) {
        for (CometChatMessageEvents event : CometChatMessageEvents.messageEvents.values()) {
            event.ccMessageSent(message, status);
        }
    }

    public static void onMessageEdited(BaseMessage message, @MessageStatus int status) {
        for (CometChatMessageEvents event : CometChatMessageEvents.messageEvents.values()) {
            event.ccMessageEdited(message, status);
        }
    }

    public static void onMessageDeleted(BaseMessage message) {
        for (CometChatMessageEvents event : CometChatMessageEvents.messageEvents.values()) {
            event.ccMessageDeleted(message);
        }
    }

    public static void onMessageRead(BaseMessage message) {
        for (CometChatMessageEvents event : CometChatMessageEvents.messageEvents.values()) {
            event.ccMessageRead(message);
        }
    }

    public static void onTextMessageReceived(TextMessage textMessage) {
        for (CometChatMessageEvents event : CometChatMessageEvents.messageEvents.values()) {
            event.onTextMessageReceived(textMessage);
        }
    }

    public static void onMediaMessageReceived(MediaMessage mediaMessage) {
        for (CometChatMessageEvents event : CometChatMessageEvents.messageEvents.values()) {
            event.onMediaMessageReceived(mediaMessage);
        }
    }

    public static void onCustomMessageReceived(CustomMessage customMessage) {
        for (CometChatMessageEvents event : CometChatMessageEvents.messageEvents.values()) {
            event.onCustomMessageReceived(customMessage);
        }
    }

    public static void onTypingStarted(TypingIndicator typingIndicator) {
        for (CometChatMessageEvents event : CometChatMessageEvents.messageEvents.values()) {
            event.onTypingStarted(typingIndicator);
        }
    }

    public static void onTypingEnded(TypingIndicator typingIndicator) {
        for (CometChatMessageEvents event : CometChatMessageEvents.messageEvents.values()) {
            event.onTypingEnded(typingIndicator);
        }
    }

    public static void onMessagesDelivered(MessageReceipt messageReceipt) {
        for (CometChatMessageEvents event : CometChatMessageEvents.messageEvents.values()) {
            event.onMessagesDelivered(messageReceipt);
        }
    }

    public static void onMessagesRead(MessageReceipt messageReceipt) {
        for (CometChatMessageEvents event : CometChatMessageEvents.messageEvents.values()) {
            event.onMessagesRead(messageReceipt);
        }
    }

    public static void onInteractionGoalCompleted(InteractionReceipt interactionReceipt) {
        for (CometChatMessageEvents event : CometChatMessageEvents.messageEvents.values()) {
            event.onInteractionGoalCompleted(interactionReceipt);
        }
    }

    public static void onMessageEdited(BaseMessage message) {
        for (CometChatMessageEvents event : CometChatMessageEvents.messageEvents.values()) {
            event.onMessageEdited(message);
        }
    }

    public static void onTransientMessageReceived(TransientMessage message) {
        for (CometChatMessageEvents event : CometChatMessageEvents.messageEvents.values()) {
            event.onTransientMessageReceived(message);
        }
    }

    public static void onFormMessageReceived(FormMessage formMessage) {
        for (CometChatMessageEvents event : CometChatMessageEvents.messageEvents.values()) {
            event.onFormMessageReceived(formMessage);
        }
    }

    public static void onSchedulerMessageReceived(SchedulerMessage schedulerMessage) {
        for (CometChatMessageEvents event : CometChatMessageEvents.messageEvents.values()) {
            event.onSchedulerMessageReceived(schedulerMessage);
        }
    }

    public static void onCardMessageReceived(CardMessage cardMessage) {
        for (CometChatMessageEvents event : CometChatMessageEvents.messageEvents.values()) {
            event.onCardMessageReceived(cardMessage);
        }
    }

    public static void onCustomInteractiveMessageReceived(CustomInteractiveMessage customInteractiveMessage) {
        for (CometChatMessageEvents event : CometChatMessageEvents.messageEvents.values()) {
            event.onCustomInteractiveMessageReceived(customInteractiveMessage);
        }
    }

    public static void onMessageReactionAdded(ReactionEvent reactionEvent) {
        for (CometChatMessageEvents event : CometChatMessageEvents.messageEvents.values()) {
            event.onMessageReactionAdded(reactionEvent);
        }
    }

    public static void onMessageReactionRemoved(ReactionEvent reactionEvent) {
        for (CometChatMessageEvents event : CometChatMessageEvents.messageEvents.values()) {
            event.onMessageReactionRemoved(reactionEvent);
        }
    }

    public static void onMessagesDeliveredToAll(MessageReceipt messageReceipt) {
        for (CometChatMessageEvents event : CometChatMessageEvents.messageEvents.values()) {
            event.onMessagesDeliveredToAll(messageReceipt);
        }
    }

    public static void onMessagesReadByAll(MessageReceipt messageReceipt) {
        for (CometChatMessageEvents event : CometChatMessageEvents.messageEvents.values()) {
            event.onMessagesReadByAll(messageReceipt);
        }
    }

    public static void onLiveReaction(@DrawableRes int icon) {
        for (CometChatMessageEvents event : CometChatMessageEvents.messageEvents.values()) {
            event.ccLiveReaction(icon);
        }
    }

    public static void onUserBlocked(User user) {
        for (CometChatUserEvents events : CometChatUserEvents.userEvents.values()) {
            events.ccUserBlocked(user);
        }
    }

    public static void onUserUnblocked(User user) {
        for (CometChatUserEvents events : CometChatUserEvents.userEvents.values()) {
            events.ccUserUnblocked(user);
        }
    }

    public static void onGroupCreated(Group group) {
        for (CometChatGroupEvents events : CometChatGroupEvents.groupEvents.values()) {
            events.ccGroupCreated(group);
        }
    }

    public static void onGroupDeleted(Group group) {
        for (CometChatGroupEvents events : CometChatGroupEvents.groupEvents.values()) {
            events.ccGroupDeleted(group);
        }
    }

    public static void onGroupLeft(Action message, User leftUser, Group leftGroup) {
        for (CometChatGroupEvents events : CometChatGroupEvents.groupEvents.values()) {
            events.ccGroupLeft(message, leftUser, leftGroup);
        }
    }

    public static void onGroupMemberScopeChanged(Action message, User updatedUser, String scopeChangedTo, String scopeChangedFrom, Group group) {
        for (CometChatGroupEvents events : CometChatGroupEvents.groupEvents.values()) {
            events.ccGroupMemberScopeChanged(message, updatedUser, scopeChangedTo, scopeChangedFrom, group);
        }
    }

    public static void onGroupMemberBanned(Action message, User bannedUser, User bannedBy, Group bannedFrom) {
        for (CometChatGroupEvents events : CometChatGroupEvents.groupEvents.values()) {
            events.ccGroupMemberBanned(message, bannedUser, bannedBy, bannedFrom);
        }
    }

    public static void onGroupMemberKicked(Action message, User kickedUser, User kickedBy, Group kickedFrom) {
        for (CometChatGroupEvents events : CometChatGroupEvents.groupEvents.values()) {
            events.ccGroupMemberKicked(message, kickedUser, kickedBy, kickedFrom);
        }
    }

    public static void onGroupMemberUnbanned(Action message, User unbannedUser, User unbannedBy, Group unbannedFrom) {
        for (CometChatGroupEvents events : CometChatGroupEvents.groupEvents.values()) {
            events.ccGroupMemberUnBanned(message, unbannedUser, unbannedBy, unbannedFrom);
        }
    }

    public static void onGroupMemberJoined(User joinedUser, Group joinedGroup) {
        for (CometChatGroupEvents events : CometChatGroupEvents.groupEvents.values()) {
            events.ccGroupMemberJoined(joinedUser, joinedGroup);
        }
    }

    public static void onGroupMemberAdded(List<Action> messages, List<User> usersAdded, Group groupAddedIn, User addedBy) {
        for (CometChatGroupEvents events : CometChatGroupEvents.groupEvents.values()) {
            events.ccGroupMemberAdded(messages, usersAdded, groupAddedIn, addedBy);
        }
    }

    public static void onOwnershipChanged(Group group, GroupMember newOwner) {
        for (CometChatGroupEvents events : CometChatGroupEvents.groupEvents.values()) {
            events.ccOwnershipChanged(group, newOwner);
        }
    }

    public static void showPanel(HashMap<String, String> id, UIKitConstants.CustomUIPosition alignment, Function1<Context, View> view) {
        for (CometChatUIEvents events : CometChatUIEvents.uiEvents.values()) {
            events.showPanel(id, alignment, view);
        }
    }

    public static void hidePanel(HashMap<String, String> id, UIKitConstants.CustomUIPosition alignment) {
        for (CometChatUIEvents events : CometChatUIEvents.uiEvents.values()) {
            events.hidePanel(id, alignment);
        }
    }

    public static void onActiveChatChanged(HashMap<String, String> id, BaseMessage message, User user, Group group) {
        for (CometChatUIEvents events : CometChatUIEvents.uiEvents.values()) {
            events.ccActiveChatChanged(id, message, user, group);
            events.ccActiveChatChanged(id, message, user, group, 0);
        }
    }

    public static void onActiveChatChanged(HashMap<String, String> id, BaseMessage message, User user, Group group, int unreadCount) {
        for (CometChatUIEvents events : CometChatUIEvents.uiEvents.values()) {
            events.ccActiveChatChanged(id, message, user, group, unreadCount);
            events.ccActiveChatChanged(id, message, user, group);
        }
    }

    public static void onComposeMessage(String id, String text) {
        for (CometChatUIEvents events : CometChatUIEvents.uiEvents.values()) {
            events.ccComposeMessage(id, text);
        }
    }

    public static void onOpenChat(User user, Group group) {
        for (CometChatUIEvents events : CometChatUIEvents.uiEvents.values()) {
            events.ccOpenChat(user, group);
        }
    }

    public static void onOutgoingCall(Call call) {
        for (CometChatCallEvents events : CometChatCallEvents.callingEvents.values()) {
            events.ccOutgoingCall(call);
        }
    }

    public static void onCallAccepted(Call call) {
        for (CometChatCallEvents events : CometChatCallEvents.callingEvents.values()) {
            events.ccCallAccepted(call);
        }
    }

    public static void onCallRejected(Call call) {
        for (CometChatCallEvents events : CometChatCallEvents.callingEvents.values()) {
            events.ccCallRejected(call);
        }
    }

    public static void onCallEnded(Call call) {
        for (CometChatCallEvents events : CometChatCallEvents.callingEvents.values()) {
            events.ccCallEnded(call);
        }
    }

    public static void onConversationDeleted(Conversation conversation) {
        for (CometChatConversationEvents events : CometChatConversationEvents.conversationEvents.values()) {
            events.ccConversationDeleted(conversation);
        }
    }
}
