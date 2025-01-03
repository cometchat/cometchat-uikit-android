package com.cometchat.chatuikit.extensions.smartreplies;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chat.models.CustomMessage;
import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.MediaMessage;
import com.cometchat.chat.models.TextMessage;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.extensions.Extensions;
import com.cometchat.chatuikit.extensions.smartreplies.view.CometChatSmartReplies;
import com.cometchat.chatuikit.extensions.smartreplies.view.SmartRepliesConfiguration;
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit;
import com.cometchat.chatuikit.shared.constants.MessageStatus;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.events.CometChatMessageEvents;
import com.cometchat.chatuikit.shared.events.CometChatUIEvents;
import com.cometchat.chatuikit.shared.framework.DataSource;
import com.cometchat.chatuikit.shared.framework.DataSourceDecorator;
import com.cometchat.chatuikit.shared.models.interactivemessage.CardMessage;
import com.cometchat.chatuikit.shared.models.interactivemessage.CustomInteractiveMessage;
import com.cometchat.chatuikit.shared.models.interactivemessage.FormMessage;
import com.cometchat.chatuikit.shared.models.interactivemessage.SchedulerMessage;
import com.cometchat.chatuikit.shared.resources.utils.Utils;

import java.util.HashMap;
import java.util.List;

public class SmartRepliesExtensionDecorator extends DataSourceDecorator {
    private static final String TAG = SmartRepliesExtensionDecorator.class.getSimpleName();
    private SmartRepliesConfiguration smartRepliesConfiguration;
    private HashMap<String, String> idMap;

    public SmartRepliesExtensionDecorator(DataSource dataSource) {
        super(dataSource);
        addMessageListener();
    }

    public SmartRepliesExtensionDecorator(DataSource dataSource, SmartRepliesConfiguration configuration) {
        super(dataSource);
        addMessageListener();
        this.smartRepliesConfiguration = configuration;
    }

    private void addMessageListener() {
        String LISTENER_ID = SmartRepliesExtensionDecorator.class.getSimpleName() + System.currentTimeMillis();
        CometChatUIEvents.addListener(LISTENER_ID, new CometChatUIEvents() {
            @Override
            public void ccActiveChatChanged(HashMap<String, String> id, BaseMessage message, User user, Group group, int unreadCount) {
                idMap = id;
                if (message != null && message.getSender() != null && CometChatUIKit.getLoggedInUser() != null && message.getSender().getUid() != null && !message.getSender().getUid().equalsIgnoreCase(CometChatUIKit.getLoggedInUser().getUid())) {
                    showRepliesPanel(id, UIKitConstants.CustomUIPosition.MESSAGE_LIST_BOTTOM, message);
                }
            }
        });
        CometChatMessageEvents.addListener(LISTENER_ID, new CometChatMessageEvents() {
            @Override
            public void onTextMessageReceived(TextMessage textMessage) {
                setIdMap(textMessage);
                showRepliesPanel(idMap, UIKitConstants.CustomUIPosition.MESSAGE_LIST_BOTTOM, textMessage);
            }

            @Override
            public void ccMessageSent(BaseMessage baseMessage, int status) {
                if (MessageStatus.SUCCESS == status) {
                    setIdMap(baseMessage);
                    hideRepliesPanel();
                }
            }

            @Override
            public void onCardMessageReceived(CardMessage cardMessage) {
                setIdMap(cardMessage);
                hideRepliesPanel();
            }

            @Override
            public void onCustomInteractiveMessageReceived(CustomInteractiveMessage customInteractiveMessage) {
                setIdMap(customInteractiveMessage);
                hideRepliesPanel();
            }

            @Override
            public void onFormMessageReceived(FormMessage formMessage) {
                setIdMap(formMessage);
                hideRepliesPanel();
            }

            @Override
            public void onMediaMessageReceived(MediaMessage mediaMessage) {
                setIdMap(mediaMessage);
                hideRepliesPanel();
            }

            @Override
            public void onCustomMessageReceived(CustomMessage customMessage) {
                setIdMap(customMessage);
                hideRepliesPanel();
            }

            @Override
            public void onSchedulerMessageReceived(SchedulerMessage schedulerMessage) {
                setIdMap(schedulerMessage);
                hideRepliesPanel();
            }
        });
    }

    private void setIdMap(BaseMessage baseMessage) {
        this.idMap = Utils.getIdMap(baseMessage);
    }

    public void showRepliesPanel(HashMap<String, String> id, UIKitConstants.CustomUIPosition alignment, BaseMessage textMessage) {
        for (CometChatUIEvents events : CometChatUIEvents.uiEvents.values()) {
            events.showPanel(id, alignment, context -> getView(id, context, textMessage));
        }
    }

    public void hideRepliesPanel() {
        for (CometChatUIEvents events : CometChatUIEvents.uiEvents.values()) {
            events.hidePanel(idMap, UIKitConstants.CustomUIPosition.MESSAGE_LIST_BOTTOM);
        }
    }

    public View getView(HashMap<String, String> idMap, Context context, BaseMessage baseMessage) {
        if (baseMessage != null && baseMessage.getSender() != null && baseMessage.getSender().getUid() != null && !baseMessage.getSender().getUid().equals(CometChatUIKit.getLoggedInUser().getUid())) {
            if (baseMessage.getMetadata() != null) {
                List<String> smartRepliesList = Extensions.getSmartReplyList(baseMessage);
                if (!smartRepliesList.isEmpty()) {
                    CometChatSmartReplies smartReplies = new CometChatSmartReplies(context);
                    smartReplies.setSmartReplyList(smartRepliesList);

                    smartReplies.setOnClick(new CometChatSmartReplies.onClick() {
                        @Override
                        public void onClick(String text, int pos) {
                            for (CometChatUIEvents events : CometChatUIEvents.uiEvents.values()) {
                                events.hidePanel(idMap, UIKitConstants.CustomUIPosition.MESSAGE_LIST_BOTTOM);
                            }
                            sendMessage(idMap, text, baseMessage);
                        }

                        @Override
                        public void onLongClick(String text, int pos) {
                        }
                    });
                    smartReplies.setOnClose(view -> {
                        for (CometChatUIEvents events : CometChatUIEvents.uiEvents.values()) {
                            events.hidePanel(idMap, UIKitConstants.CustomUIPosition.MESSAGE_LIST_BOTTOM);
                        }
                    });
                    if (smartRepliesConfiguration != null) {
                        smartReplies.setCloseIcon(smartRepliesConfiguration.getCloseButtonIcon());
                        if (smartRepliesConfiguration.getOnClick() != null)
                            smartReplies.setOnClick(smartRepliesConfiguration.getOnClick());
                        if (smartRepliesConfiguration.getOnClose() != null)
                            smartReplies.setOnClose(smartRepliesConfiguration.getOnClose());
                    }
                    return smartReplies;
                }
            }
        }
        return new LinearLayout(context);
    }

    private void sendMessage(HashMap<String, String> idMap, String reply, BaseMessage baseMessage) {
        if (reply != null && !reply.isEmpty() && idMap != null && baseMessage != null) {
            TextMessage message;
            if (baseMessage.getReceiverType().equalsIgnoreCase(UIKitConstants.ReceiverType.USER)) {
                message = new TextMessage(baseMessage.getSender().getUid(), reply.trim(), idMap.get(UIKitConstants.MapId.RECEIVER_TYPE));
            } else {
                Group group = (Group) baseMessage.getReceiver();
                message = new TextMessage(group.getGuid(), reply.trim(), UIKitConstants.ReceiverType.GROUP);
            }
            if (baseMessage.getParentMessageId() > 0)
                message.setParentMessageId(baseMessage.getParentMessageId());
            message.setCategory(UIKitConstants.MessageCategory.MESSAGE);
            CometChatUIKit.sendTextMessage(message, null);
        }
    }

    @Override
    public String getId() {
        return SmartRepliesExtensionDecorator.class.getSimpleName();
    }
}
