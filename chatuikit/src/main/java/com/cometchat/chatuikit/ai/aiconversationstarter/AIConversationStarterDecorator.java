package com.cometchat.chatuikit.ai.aiconversationstarter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.StyleRes;

import com.cometchat.chat.constants.CometChatConstants;
import com.cometchat.chat.core.CometChat;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.MediaMessage;
import com.cometchat.chat.models.TextMessage;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.ai.AIExtensionDataSource;
import com.cometchat.chatuikit.ai.aiconversationstarter.view.CometChatAIConversationStarterView;
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

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

public class AIConversationStarterDecorator extends DataSourceDecorator {
    private static final String TAG = AIConversationStarterDecorator.class.getSimpleName();
    private HashMap<String, String> idMap;
    private User userTemp;
    private Group groupTemp;
    private @StyleRes int aiCardStyle;
    private AIConversationStarterConfiguration aiConversationStarterConfiguration;

    public AIConversationStarterDecorator(DataSource dataSource) {
        super(dataSource);
        addMessageListener();
    }

    private void addMessageListener() {
        String LISTENER_ID = System.currentTimeMillis() + "_" + AIConversationStarterDecorator.class.getSimpleName();
        CometChatUIEvents.addListener(LISTENER_ID, new CometChatUIEvents() {
            @Override
            public void ccActiveChatChanged(HashMap<String, String> id, BaseMessage message, User user, Group group) {
                idMap = id;
                userTemp = user;
                groupTemp = group;
                if (message == null && id != null && id.get(UIKitConstants.MapId.PARENT_MESSAGE_ID) == null) {
                    showRepliesPanel(id, UIKitConstants.CustomUIPosition.COMPOSER_TOP);
                }
            }
        });

        CometChatMessageEvents.addListener(LISTENER_ID, new CometChatMessageEvents() {
            @Override
            public void ccMessageSent(BaseMessage baseMessage, int status) {
                hideRepliesPanel();
            }

            @Override
            public void onTextMessageReceived(TextMessage textMessage) {
                hidePanelRealTime(textMessage);
            }

            @Override
            public void onMediaMessageReceived(MediaMessage mediaMessage) {
                hidePanelRealTime(mediaMessage);
            }

            @Override
            public void onFormMessageReceived(FormMessage formMessage) {
                hidePanelRealTime(formMessage);
            }

            @Override
            public void onSchedulerMessageReceived(SchedulerMessage schedulerMessage) {
                hidePanelRealTime(schedulerMessage);
            }

            @Override
            public void onCardMessageReceived(CardMessage cardMessage) {
                hidePanelRealTime(cardMessage);
            }

            @Override
            public void onCustomInteractiveMessageReceived(CustomInteractiveMessage customInteractiveMessage) {
                hidePanelRealTime(customInteractiveMessage);
            }
        });
    }

    public void showRepliesPanel(HashMap<String, String> id, UIKitConstants.CustomUIPosition alignment) {
        for (CometChatUIEvents events : CometChatUIEvents.uiEvents.values()) {
            events.showPanel(id, alignment, context -> getView(id, context));
        }
    }

    public void hideRepliesPanel() {
        for (CometChatUIEvents events : CometChatUIEvents.uiEvents.values()) {
            events.hidePanel(idMap, UIKitConstants.CustomUIPosition.COMPOSER_TOP);
        }
    }

    private void hidePanelRealTime(BaseMessage baseMessage) {
        if (baseMessage != null) {
            if (baseMessage.getReceiverType().equals(CometChatConstants.RECEIVER_TYPE_USER) && userTemp != null) {
                if (baseMessage.getSender() != null && baseMessage.getSender().getUid() != null && userTemp.getUid().equalsIgnoreCase(baseMessage
                                                                                                                                              .getSender()
                                                                                                                                              .getUid())) {
                    hideRepliesPanel();
                }
            } else if (baseMessage.getReceiverType().equals(UIKitConstants.ReceiverType.GROUP) && groupTemp != null) {
                if (groupTemp.getGuid() != null && groupTemp.getGuid().equalsIgnoreCase(baseMessage.getReceiverUid())) {
                    hideRepliesPanel();
                }
            }
        }
    }

    public View getView(HashMap<String, String> idMap, Context context) {
        CometChatAIConversationStarterView aiConversationsStarterView = new CometChatAIConversationStarterView(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                                                               ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(Utils.convertDpToPx(context, 10), 0, Utils.convertDpToPx(context, 10), 0);
        aiConversationsStarterView.setLayoutParams(layoutParams);

        if (idMap != null && idMap.containsKey(UIKitConstants.MapId.RECEIVER_ID)) {
            aiConversationsStarterView.setUid(idMap.get(UIKitConstants.MapId.RECEIVER_ID));
        }

        JSONObject customJson = null;
        if (aiConversationStarterConfiguration != null) {
            aiConversationsStarterView.setStyle(aiConversationStarterConfiguration.getStyle());

            aiConversationsStarterView.setErrorViewLayout(aiConversationStarterConfiguration.getErrorStateView());
            aiConversationsStarterView.setLoadingViewLayout(aiConversationStarterConfiguration.getLoadingStateView());
            aiConversationsStarterView.setErrorStateText(aiConversationStarterConfiguration.getErrorText());
            if (aiConversationStarterConfiguration.getAPIConfiguration() != null) {
                customJson = aiConversationStarterConfiguration.getAPIConfiguration().apply(idMap, userTemp, groupTemp);
            }
        }
        aiConversationsStarterView.showLoadingView();
        CometChat.getConversationStarter(userTemp != null ? userTemp.getUid() : groupTemp.getGuid(),
                                         userTemp != null ? UIKitConstants.ReceiverType.USER : UIKitConstants.ReceiverType.GROUP,
                                         customJson,
                                         new CometChat.CallbackListener<List<String>>() {
                                             @Override
                                             public void onSuccess(List<String> list) {
                                                 View view = null;
                                                 if (aiConversationStarterConfiguration != null) {
                                                     if (aiConversationStarterConfiguration.getCustomView() != null)
                                                         view = aiConversationStarterConfiguration.getCustomView().apply(context, list);
                                                 }
                                                 if (view != null) aiConversationsStarterView.setCustomView(view);
                                                 else {
                                                     aiConversationsStarterView.setReplyList(list);
                                                 }
                                             }

                                             @Override
                                             public void onError(CometChatException e) {
                                                 aiConversationsStarterView.showErrorView();
                                             }
                                         }
        );

        aiConversationsStarterView.setStyle(aiCardStyle);
        aiConversationsStarterView.setOnClick((id, text, pos) -> {
            for (CometChatUIEvents events : CometChatUIEvents.uiEvents.values()) {
                events.ccComposeMessage(id, text);
                events.hidePanel(idMap, UIKitConstants.CustomUIPosition.COMPOSER_TOP);
            }
        });
        return aiConversationsStarterView;
    }

    public AIConversationStarterDecorator(DataSource dataSource, AIConversationStarterConfiguration aiConversationStarterConfiguration) {
        super(dataSource);
        addMessageListener();
        if (aiConversationStarterConfiguration != null) {
            this.aiConversationStarterConfiguration = aiConversationStarterConfiguration;
            aiCardStyle = aiConversationStarterConfiguration.getStyle();
        }
    }

    @Override
    public String getId() {
        return AIExtensionDataSource.class.getSimpleName();
    }

}
