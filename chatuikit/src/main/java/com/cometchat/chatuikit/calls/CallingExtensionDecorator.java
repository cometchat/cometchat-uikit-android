package com.cometchat.chatuikit.calls;

import android.content.Context;
import android.text.SpannableString;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.calls.core.CometChatCalls;
import com.cometchat.chat.constants.CometChatConstants;
import com.cometchat.chat.core.Call;
import com.cometchat.chat.core.CometChat;
import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chat.models.Conversation;
import com.cometchat.chat.models.CustomMessage;
import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.calls.callbubble.CometChatMeetCallBubble;
import com.cometchat.chatuikit.calls.callbutton.CallButtonsConfiguration;
import com.cometchat.chatuikit.calls.callbutton.CometChatCallButtons;
import com.cometchat.chatuikit.calls.outgoingcall.OutgoingCallConfiguration;
import com.cometchat.chatuikit.calls.utils.CallUtils;
import com.cometchat.chatuikit.databinding.CometchatCallActionBubbleContainerBinding;
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.events.CometChatCallEvents;
import com.cometchat.chatuikit.shared.framework.ChatConfigurator;
import com.cometchat.chatuikit.shared.framework.DataSource;
import com.cometchat.chatuikit.shared.framework.DataSourceDecorator;
import com.cometchat.chatuikit.shared.models.AdditionParameter;
import com.cometchat.chatuikit.shared.models.CometChatMessageTemplate;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.cometchat.chatuikit.shared.viewholders.MessagesViewHolderListener;
import com.cometchat.chatuikit.shared.views.deletebubble.CometChatDeleteBubble;
import com.cometchat.chatuikit.shared.views.messagebubble.CometChatMessageBubble;

import java.util.List;

public class CallingExtensionDecorator extends DataSourceDecorator {
    private static final String TAG = CallingExtensionDecorator.class.getSimpleName();
    private OutgoingCallConfiguration outgoingCallConfiguration;
    private CallButtonsConfiguration callButtonsConfiguration;
    private CometChatCalls.CallSettingsBuilder groupCallSettingBuilder;

    public CallingExtensionDecorator(DataSource dataSource) {
        super(dataSource);
        addListener();
    }

    public void addListener() {
        String LISTENER_ID = System.currentTimeMillis() + "";
        CometChat.addCallListener(LISTENER_ID, new CometChat.CallListener() {
            @Override
            public void onIncomingCallReceived(Call call) {
                // will be handled at clients end or within our CometChatCallActivity
            }

            @Override
            public void onOutgoingCallAccepted(Call call) {
                CallingExtension.setActiveCall(null);
            }

            @Override
            public void onOutgoingCallRejected(Call call) {
                resetCall(call);
            }

            @Override
            public void onIncomingCallCancelled(Call call) {
                resetCall(call);
            }

            @Override
            public void onCallEndedMessageReceived(Call call) {
                resetCall(call);
            }
        });

        CometChatCallEvents.addListener(LISTENER_ID, new CometChatCallEvents() {
            @Override
            public void ccOutgoingCall(Call call) {
                CallingExtension.setActiveCall(call);
            }

            @Override
            public void ccCallAccepted(Call call) {
                CallingExtension.setActiveCall(null);
            }

            @Override
            public void ccCallRejected(Call call) {
                resetCall(call);
            }

            @Override
            public void ccCallEnded(Call call) {
                CallingExtension.setActiveCall(null);
            }
        });
    }

    private void resetCall(Call call) {
        if (CallingExtension.getActiveCall() != null && call != null && CallingExtension
            .getActiveCall()
            .getSessionId()
            .equalsIgnoreCase(call.getSessionId())) {
            CallingExtension.setActiveCall(null);
        }
    }

    public CallingExtensionDecorator(DataSource dataSource, CallingConfiguration callingConfiguration) {
        super(dataSource);
        if (callingConfiguration != null) {
            outgoingCallConfiguration = callingConfiguration.getOutgoingCallConfiguration();
            callButtonsConfiguration = callingConfiguration.getCallButtonsConfiguration();
            groupCallSettingBuilder = callingConfiguration.getGroupCallSettingsBuilder();
        }
        addListener();
    }

    @Override
    public List<CometChatMessageTemplate> getMessageTemplates(AdditionParameter additionParameter) {
        List<CometChatMessageTemplate> templates = super.getMessageTemplates(additionParameter);
        templates.add(getConferenceCallTemplate(additionParameter));
        templates.add(getDefaultVoiceCallActionTemplate(additionParameter));
        templates.add(getDefaultVideoCallActionTemplate(additionParameter));
        return templates;
    }

    @Override
    public List<String> getDefaultMessageTypes(AdditionParameter additionParameter) {
        List<String> messageTypes = super.getDefaultMessageTypes(additionParameter);
        messageTypes.add(CometChatConstants.CALL_TYPE_AUDIO);
        messageTypes.add(CometChatConstants.CALL_TYPE_VIDEO);
        messageTypes.add(UIKitConstants.MessageType.MEETING);
        return messageTypes;
    }

    @Override
    public List<String> getDefaultMessageCategories(AdditionParameter additionParameter) {
        List<String> categories = super.getDefaultMessageCategories(additionParameter);
        categories.add(UIKitConstants.MessageCategory.CALL);
        categories.add(UIKitConstants.MessageCategory.CUSTOM);
        return categories;
    }

    @Override
    public SpannableString getLastConversationMessage(@NonNull Context context, Conversation conversation, AdditionParameter additionParameter) {
        if (conversation != null && conversation.getLastMessage() != null && (UIKitConstants.MessageCategory.CALL.equalsIgnoreCase(conversation
                                                                                                                                       .getLastMessage()
                                                                                                                                       .getCategory()) || CometChatConstants.CATEGORY_CUSTOM.equals(
            conversation.getLastMessage().getCategory()) && UIKitConstants.MessageType.MEETING.equalsIgnoreCase(conversation
                                                                                                                    .getLastMessage()
                                                                                                                    .getType())))
            return SpannableString.valueOf(_getLastConversationMessage(context, conversation, additionParameter));
        else return super.getLastConversationMessage(context, conversation, additionParameter);
    }

    private String _getLastConversationMessage(Context context, Conversation conversation, AdditionParameter additionParameter) {
        String lastMessageText;
        BaseMessage baseMessage = conversation.getLastMessage();
        if (baseMessage != null) {
            String message = getLastMessage(context, baseMessage);
            if (message != null) {
                lastMessageText = getLastMessage(context, baseMessage);
            } else lastMessageText = String.valueOf(super.getLastConversationMessage(context, conversation, additionParameter));
            if (baseMessage.getDeletedAt() > 0) {
                lastMessageText = context.getString(R.string.cometchat_this_message_deleted);
            }
        } else {
            lastMessageText = context.getResources().getString(R.string.cometchat_start_conv_hint);
        }
        return lastMessageText;
    }

    @Nullable
    private String getLastMessage(Context context, @NonNull BaseMessage lastMessage) {
        String message = null;
        if (CometChatConstants.CATEGORY_CALL.equals(lastMessage.getCategory())) {
            if (lastMessage instanceof Call) {
                if (!CallUtils.isVideoCall((Call) lastMessage)) message = "📞 " + CallUtils.getCallStatus(context, (Call) lastMessage);
                else message = "📹 " + CallUtils.getCallStatus(context, (Call) lastMessage);
            }
        } else if (CometChatConstants.CATEGORY_CUSTOM.equals(lastMessage.getCategory()) && UIKitConstants.MessageType.MEETING.equalsIgnoreCase(
            lastMessage.getType())) {
            message = Utils.getMessagePrefix(lastMessage, context) + context.getString(R.string.cometchat_group_call);
        }
        return message;
    }

    @Override
    public View getAuxiliaryHeaderMenu(Context context, User user, Group group, AdditionParameter additionParameter) {
        LinearLayout layout = new LinearLayout(context);
        View presentView = super.getAuxiliaryHeaderMenu(context, user, group, additionParameter);
        CometChatCallButtons cometchatCallButtons = new CometChatCallButtons(context);
        cometchatCallButtons.setButtonTextVisibility(View.GONE);
        cometchatCallButtons.setUser(user);
        cometchatCallButtons.setGroup(group);
        if (additionParameter != null) {
            cometchatCallButtons.setStyle(additionParameter.getCallButtonStyle());
            cometchatCallButtons.setVideoCallButtonVisibility(additionParameter.getVideoCallButtonVisibility());
            cometchatCallButtons.setVoiceCallButtonVisibility(additionParameter.getVoiceCallButtonVisibility());
        }
        cometchatCallButtons.setOutgoingCallConfiguration(outgoingCallConfiguration);
        layout.addView(cometchatCallButtons);
        if (presentView != null) {
            Utils.removeParentFromView(presentView);
            layout.addView(presentView);
        }

        if (callButtonsConfiguration != null) {
            cometchatCallButtons.setOnVideoCallClick(callButtonsConfiguration.getOnVideoCallClick());
            cometchatCallButtons.setOnVoiceCallClick(callButtonsConfiguration.getOnVoiceCallClick());
            cometchatCallButtons.setOutgoingCallConfiguration(callButtonsConfiguration.getOutgoingCallConfiguration());
            cometchatCallButtons.setCallSettingsBuilder(callButtonsConfiguration.getCallSettingsBuilder());
        }

        return layout;
    }

    public CometChatMessageTemplate getConferenceCallTemplate(AdditionParameter additionParameter) {
        return new CometChatMessageTemplate()
            .setCategory(UIKitConstants.MessageCategory.CUSTOM)
            .setType(UIKitConstants.MessageType.MEETING)
            .setOptions((context, baseMessage, group) -> ChatConfigurator
                .getDataSource()
                .getImageMessageOptions(context, baseMessage, group, additionParameter))
            .setStatusInfoView(new MessagesViewHolderListener() {
                @Override
                public View createView(Context context, CometChatMessageBubble messageBubble, UIKitConstants.MessageBubbleAlignment alignment) {
                    return null;
                }

                @Override
                public void bindView(
                    Context context,
                    View createdView,
                    BaseMessage message,
                    UIKitConstants.MessageBubbleAlignment alignment,
                    RecyclerView.ViewHolder holder,
                    List<BaseMessage> messageList,
                    int position
                ) {
                }
            })
            .setContentView(new MessagesViewHolderListener() {
                @Override
                public View createView(Context context, CometChatMessageBubble messageBubble, UIKitConstants.MessageBubbleAlignment alignment) {
                    return View.inflate(context, R.layout.cometchat_meet_call_bubble_container, null);
                }

                @Override
                public void bindView(
                    Context context,
                    View createdView,
                    @NonNull BaseMessage message,
                    UIKitConstants.MessageBubbleAlignment alignment,
                    RecyclerView.ViewHolder holder,
                    List<BaseMessage> messageList,
                    int position
                ) {
                    CometChatMeetCallBubble bubble = createdView.findViewById(R.id.meet_bubble);
                    CometChatDeleteBubble deletedBubble = createdView.findViewById(R.id.cometchat_delete_text_bubble);
                    @StyleRes int meetCallStyle = message.getSender().getUid().equals(CometChatUIKit
                                                                                          .getLoggedInUser()
                                                                                          .getUid()) ? additionParameter.getOutgoingMeetCallBubbleStyle() : additionParameter.getIncomingMeetCallBubbleStyle();
                    if (message.getDeletedAt() == 0) {
                        bubble.setStyle(meetCallStyle);
                        deletedBubble.setVisibility(View.GONE);
                        bubble.setVisibility(View.VISIBLE);
                        bubble.setMessage((CustomMessage) message);
                        bubble.setOnClick(() -> {
                            CometChatCallActivity.launchConferenceCallScreen(context, message, groupCallSettingBuilder);
                        });
                    } else {
                        bubble.setVisibility(View.GONE);
                        deletedBubble.setVisibility(View.VISIBLE);
                        deletedBubble.setStyle(CometChatUIKit
                                                   .getLoggedInUser()
                                                   .getUid()
                                                   .equals(message
                                                               .getSender()
                                                               .getUid()) ? additionParameter.getOutgoingDeleteBubbleStyle() : additionParameter.getIncomingDeleteBubbleStyle());
                    }
                }
            })
            .setBottomView(new MessagesViewHolderListener() {
                @Override
                public View createView(Context context, CometChatMessageBubble messageBubble, UIKitConstants.MessageBubbleAlignment alignment) {
                    return CometChatUIKit.getDataSource().getBottomView(context, messageBubble, alignment);
                }

                @Override
                public void bindView(
                    Context context,
                    View createdView,
                    BaseMessage message,
                    UIKitConstants.MessageBubbleAlignment alignment,
                    RecyclerView.ViewHolder holder,
                    List<BaseMessage> messageList,
                    int position
                ) {
                    CometChatUIKit.getDataSource().bindBottomView(
                        context,
                        createdView,
                        message,
                        alignment,
                        holder,
                        messageList,
                        position,
                        additionParameter
                    );
                }
            });
    }

    private CometChatMessageTemplate getDefaultVoiceCallActionTemplate(AdditionParameter additionParameter) {
        return new CometChatMessageTemplate()
            .setCategory(CometChatConstants.CATEGORY_CALL)
            .setType(CometChatConstants.CALL_TYPE_AUDIO)
            .setContentView(new MessagesViewHolderListener() {
                @Override
                public View createView(Context context, CometChatMessageBubble messageBubble, UIKitConstants.MessageBubbleAlignment alignment) {
                    return View.inflate(context, R.layout.cometchat_call_action_bubble_container, null);
                }

                @Override
                public void bindView(
                    Context context,
                    View createdView,
                    BaseMessage message,
                    UIKitConstants.MessageBubbleAlignment alignment,
                    RecyclerView.ViewHolder holder,
                    List<BaseMessage> messageList,
                    int position
                ) {
                    CometchatCallActionBubbleContainerBinding binding = CometchatCallActionBubbleContainerBinding.bind(createdView);
                    binding.cometchatCallActionBubble.setStyle(additionParameter.getCallActionBubbleStyle());
                    binding.cometchatCallActionBubble.setMessage((Call) message);
                }
            });
    }

    private CometChatMessageTemplate getDefaultVideoCallActionTemplate(@NonNull AdditionParameter additionParameter) {
        return new CometChatMessageTemplate()
            .setCategory(CometChatConstants.CATEGORY_CALL)
            .setType(CometChatConstants.CALL_TYPE_VIDEO)
            .setContentView(new MessagesViewHolderListener() {
                @Override
                public View createView(Context context, CometChatMessageBubble messageBubble, UIKitConstants.MessageBubbleAlignment alignment) {
                    return View.inflate(context, R.layout.cometchat_call_action_bubble_container, null);
                }

                @Override
                public void bindView(
                    Context context,
                    View createdView,
                    BaseMessage message,
                    UIKitConstants.MessageBubbleAlignment alignment,
                    RecyclerView.ViewHolder holder,
                    List<BaseMessage> messageList,
                    int position
                ) {
                    CometchatCallActionBubbleContainerBinding binding = CometchatCallActionBubbleContainerBinding.bind(createdView);
                    binding.cometchatCallActionBubble.setStyle(additionParameter.getCallActionBubbleStyle());
                    binding.cometchatCallActionBubble.setMessage((Call) message);
                }
            });
    }

    @Override
    public String getId() {
        return CallingExtensionDecorator.class.getSimpleName();
    }
}
