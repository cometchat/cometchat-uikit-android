package com.cometchat.chatuikit.ai.aismartreplies;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.cometchat.chat.constants.CometChatConstants;
import com.cometchat.chat.core.CometChat;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chat.models.CustomMessage;
import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.MediaMessage;
import com.cometchat.chat.models.TextMessage;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.CometChatTheme;
import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.ai.AIOptionsStyle;
import com.cometchat.chatuikit.ai.aismartreplies.view.CometChatAISmartRepliesView;
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKitHelper;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.events.CometChatMessageEvents;
import com.cometchat.chatuikit.shared.events.CometChatUIEvents;
import com.cometchat.chatuikit.shared.framework.DataSource;
import com.cometchat.chatuikit.shared.framework.DataSourceDecorator;
import com.cometchat.chatuikit.shared.models.AdditionParameter;
import com.cometchat.chatuikit.shared.models.CometChatMessageComposerAction;
import com.cometchat.chatuikit.shared.models.interactivemessage.CardMessage;
import com.cometchat.chatuikit.shared.models.interactivemessage.CustomInteractiveMessage;
import com.cometchat.chatuikit.shared.models.interactivemessage.FormMessage;
import com.cometchat.chatuikit.shared.models.interactivemessage.SchedulerMessage;
import com.cometchat.chatuikit.shared.resources.utils.Utils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AISmartRepliesDecorator extends DataSourceDecorator {
    private static final String TAG = AISmartRepliesDecorator.class.getSimpleName();

    private AISmartRepliesConfiguration configuration;
    private HashMap<String, String> id;
    private User userTemp;
    private Group groupTemp;

    public AISmartRepliesDecorator(DataSource dataSource) {
        super(dataSource);
        addListeners();
    }

    private void addListeners() {
        String LISTENER_ID = System.currentTimeMillis() + "_" + AISmartRepliesDecorator.class.getSimpleName();
        CometChatUIEvents.addListener(LISTENER_ID, new CometChatUIEvents() {
            @Override
            public void ccActiveChatChanged(HashMap<String, String> id, BaseMessage message, User user, Group group) {
                userTemp = user;
                groupTemp = group;
                AISmartRepliesDecorator.this.id = id;
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
            public void onCustomMessageReceived(CustomMessage customMessage) {
                hidePanelRealTime(customMessage);
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

    public void hideRepliesPanel() {
        hidePanel(id, UIKitConstants.CustomUIPosition.COMPOSER_TOP);
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

    public void hidePanel(HashMap<String, String> id, UIKitConstants.CustomUIPosition alignment) {
        CometChatUIKitHelper.hidePanel(id, alignment);
    }

    public AISmartRepliesDecorator(DataSource dataSource, AISmartRepliesConfiguration configuration) {
        super(dataSource);
        this.configuration = configuration;
        addListeners();
    }

    @Override
    public List<CometChatMessageComposerAction> getAIOptions(
        Context context, User user, Group group, HashMap<String, String> idMap, AIOptionsStyle aiOptionsStyle, AdditionParameter additionParameter
    ) {
        List<CometChatMessageComposerAction> actions = super.getAIOptions(context, user, group, idMap, aiOptionsStyle, additionParameter);
        id = idMap;
        if (!idMap.containsKey(UIKitConstants.MapId.PARENT_MESSAGE_ID)) {
            String title = context.getString(R.string.cometchat_suggest_a_reply);
            CometChatMessageComposerAction action = new CometChatMessageComposerAction()
                .setId(getId())
                .setTitle(title)
                .setTitleColor(CometChatTheme.getTextColorPrimary(context))
                .setBackground(CometChatTheme.getBackgroundColor1(context))
                .setTitleAppearance(CometChatTheme.getTextAppearanceBodyRegular(context))
                .setIconTintColor(CometChatTheme.getIconTintHighlight(context))
                .setIcon(configuration != null ? configuration.getButtonIcon() : R.drawable.cometchat_ic_ai_smart_reply)
                .setOnClick(() -> {
                    showRepliesPanel(idMap,
                                     UIKitConstants.CustomUIPosition.MESSAGE_LIST_BOTTOM,
                                     getSmartReplyPanel(idMap, context, 0, configuration, user, group)
                    );
                });

            if (aiOptionsStyle != null) {
                if (aiOptionsStyle.getListItemBackgroundColor() != 0) action.setBackground(aiOptionsStyle.getListItemBackgroundColor());
                if (aiOptionsStyle.getListItemTextAppearance() != 0) action.setTitleAppearance(aiOptionsStyle.getListItemTextAppearance());
                if (aiOptionsStyle.getCornerRadius() >= 0) action.setCornerRadius((int) aiOptionsStyle.getCornerRadius());
                if (aiOptionsStyle.getListItemTextColor() != 0) action.setTitleColor(aiOptionsStyle.getListItemTextColor());
            }
            actions.add(action);
        }
        return actions;
    }

    @Override
    public String getId() {
        return AISmartRepliesDecorator.class.getSimpleName();
    }

    public void showRepliesPanel(HashMap<String, String> id, UIKitConstants.CustomUIPosition alignment, View view) {
        for (CometChatUIEvents events : CometChatUIEvents.uiEvents.values()) {
            events.hidePanel(id, UIKitConstants.CustomUIPosition.MESSAGE_LIST_BOTTOM);
            events.showPanel(id, alignment, context -> view);
        }
    }

    public View getSmartReplyPanel(
        HashMap<String, String> idMap,
        @NonNull Context context,
        @StyleRes int aiCardStyle,
        AISmartRepliesConfiguration aiConversationStarterConfiguration,
        User userTemp,
        Group groupTemp
    ) {
        CometChatAISmartRepliesView aiSmartRepliesView = new CometChatAISmartRepliesView(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                                                               ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(Utils.convertDpToPx(context, 10),
                                context.getResources().getDimensionPixelSize(R.dimen.cometchat_margin_2),
                                Utils.convertDpToPx(context, 10),
                                Utils.convertDpToPx(context, 10)
        );
        aiSmartRepliesView.setLayoutParams(layoutParams);

        if (idMap != null && idMap.containsKey(UIKitConstants.MapId.RECEIVER_ID)) {
            aiSmartRepliesView.setUid(idMap.get(UIKitConstants.MapId.RECEIVER_ID));
        }

        JSONObject customJson = null;
        if (aiConversationStarterConfiguration != null) {
            aiSmartRepliesView.setErrorStateText(aiConversationStarterConfiguration.getErrorText());
            if (aiConversationStarterConfiguration.getApiConfiguration() != null) {
                customJson = aiConversationStarterConfiguration.getApiConfiguration().apply(idMap, userTemp, groupTemp);
            }
        }
        aiSmartRepliesView.setErrorViewLayout(aiConversationStarterConfiguration != null ? aiConversationStarterConfiguration.getErrorStateView() : 0);
        aiSmartRepliesView.setLoadingViewLayout(aiConversationStarterConfiguration != null ? aiConversationStarterConfiguration.getLoadingStateView() : 0);
        aiSmartRepliesView.showLoadingView();
        CometChat.getSmartReplies(userTemp != null ? userTemp.getUid() : groupTemp.getGuid(),
                                  userTemp != null ? UIKitConstants.ReceiverType.USER : UIKitConstants.ReceiverType.GROUP,
                                  customJson,
                                  new CometChat.CallbackListener<HashMap<String, String>>() {
                                      @Override
                                      public void onSuccess(HashMap<String, String> list) {
                                          View view = null;
                                          List<String> response = new ArrayList<>(list.values());
                                          if (aiConversationStarterConfiguration != null) {
                                              if (aiConversationStarterConfiguration.getCustomView() != null)
                                                  view = aiConversationStarterConfiguration.getCustomView().apply(context, response);
                                          }
                                          if (view != null) aiSmartRepliesView.setCustomView(view);
                                          else {
                                              aiSmartRepliesView.setReplyList(response);
                                          }
                                      }

                                      @Override
                                      public void onError(CometChatException e) {
                                          aiSmartRepliesView.showErrorView();
                                      }
                                  }
        );

        aiSmartRepliesView.setStyle(aiCardStyle);
        aiSmartRepliesView.setOnClick((view, id, text, pos) -> {
            CometChatUIKitHelper.onComposeMessage(id, text);
            hidePanel(idMap, UIKitConstants.CustomUIPosition.MESSAGE_LIST_BOTTOM);
        });

        aiSmartRepliesView.setOnCLoseIconClick(view -> hidePanel(idMap, UIKitConstants.CustomUIPosition.MESSAGE_LIST_BOTTOM));
        return aiSmartRepliesView;
    }
}
