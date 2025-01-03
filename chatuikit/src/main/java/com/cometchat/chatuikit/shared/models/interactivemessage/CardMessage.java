package com.cometchat.chatuikit.shared.models.interactivemessage;

import com.cometchat.chat.models.InteractiveMessage;
import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.models.interactiveelements.BaseInteractiveElement;
import com.cometchat.chatuikit.shared.models.interactiveelements.ButtonElement;
import com.cometchat.chatuikit.shared.models.interactiveelements.ElementEntity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CardMessage extends InteractiveMessage {
    private static final String TAG = CardMessage.class.getSimpleName();


    private String imageUrl;
    private String text;
    private String goalCompletionText;
    private List<BaseInteractiveElement> cardActions;

    private CardMessage() {
        super(null, null, UIKitConstants.MessageType.CARD, new JSONObject());
    }

    public CardMessage(String receiverId, String receiverType, String text, List<BaseInteractiveElement> cardActions) {
        super(receiverId, receiverType, UIKitConstants.MessageType.CARD, new JSONObject());
        setText(text);
        setCardActions(cardActions);
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setGoalCompletionText(String goalCompletionText) {
        this.goalCompletionText = goalCompletionText;
    }

    public String getGoalCompletionText() {
        return goalCompletionText;
    }

    public void setCardActions(List<BaseInteractiveElement> cardActions) {
        this.cardActions = cardActions;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getText() {
        return text;
    }

    public List<BaseInteractiveElement> getCardActions() {
        return cardActions;
    }

    public static CardMessage fromInteractive(InteractiveMessage interactiveMessage) {
        if (interactiveMessage == null) return null;
        CardMessage cardMessage = new CardMessage();
        cardMessage.setId(interactiveMessage.getId());
        cardMessage.setReceiverType(interactiveMessage.getReceiverType());
        cardMessage.setReceiver(interactiveMessage.getReceiver());
        cardMessage.setSender(interactiveMessage.getSender());
        cardMessage.setSentAt(interactiveMessage.getSentAt());
        cardMessage.setReadAt(interactiveMessage.getReadAt());
        cardMessage.setDeliveredAt(interactiveMessage.getDeliveredAt());
        cardMessage.setUpdatedAt(interactiveMessage.getUpdatedAt());
        cardMessage.setDeletedBy(interactiveMessage.getDeletedBy());
        cardMessage.setDeletedAt(interactiveMessage.getDeletedAt());
        cardMessage.setInteractionGoal(interactiveMessage.getInteractionGoal());
        cardMessage.setInteractions(interactiveMessage.getInteractions() == null ? new ArrayList<>() : interactiveMessage.getInteractions());
        cardMessage.setMetadata(interactiveMessage.getMetadata());
        cardMessage.setMuid(interactiveMessage.getMuid());
        cardMessage.setRawMessage(interactiveMessage.getRawMessage());
        cardMessage.setConversationId(interactiveMessage.getConversationId());
        cardMessage.setReadByMeAt(interactiveMessage.getReadByMeAt());
        cardMessage.setReceiverUid(interactiveMessage.getReceiverUid());
        cardMessage.setReplyCount(interactiveMessage.getReplyCount());
        cardMessage.setTags(interactiveMessage.getTags());
        cardMessage.setAllowSenderInteraction(interactiveMessage.isAllowSenderInteraction());

        try {
            if (interactiveMessage.getInteractiveData() != null) {
                JSONObject jsonObject = interactiveMessage.getInteractiveData();

                if (jsonObject.has(InteractiveConstants.IMAGE_URL)) {
                    cardMessage.setImageUrl(jsonObject.optString(InteractiveConstants.IMAGE_URL));
                }
                if (jsonObject.has(InteractiveConstants.TEXT)) {
                    cardMessage.setText(jsonObject.optString(InteractiveConstants.TEXT));
                }
                if (jsonObject.has(InteractiveConstants.GOAL_COMPLETION_TEXT)) {
                    cardMessage.setGoalCompletionText(jsonObject.optString(InteractiveConstants.GOAL_COMPLETION_TEXT));
                }
                if (jsonObject.has(InteractiveConstants.CARD_ACTIONS)) {
                    List<BaseInteractiveElement> elementEntityList = new ArrayList<>();
                    JSONArray jsonArray = jsonObject.getJSONArray(InteractiveConstants.CARD_ACTIONS);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject element = jsonArray.getJSONObject(i);
                        String elementType = element.optString(InteractiveConstants.ELEMENT_TYPE);
                        if (elementType.equals(InteractiveConstants.ElementsType.BUTTON)) {
                            elementEntityList.add(ButtonElement.fromJson(element));
                        }
                    }
                    cardMessage.setCardActions(elementEntityList);
                }
            }
        } catch (Exception e) {

        }
        return cardMessage;
    }

    public InteractiveMessage toInteractiveMessage() {
        InteractiveMessage interactiveMessage = new InteractiveMessage(getReceiverUid(), getReceiverType(), UIKitConstants.MessageType.CARD, new JSONObject());
        interactiveMessage.setId(getId());
        interactiveMessage.setReceiverType(getReceiverType());
        interactiveMessage.setReceiver(getReceiver());
        interactiveMessage.setSender(getSender());
        interactiveMessage.setSentAt(getSentAt());
        interactiveMessage.setReadAt(getReadAt());
        interactiveMessage.setDeletedAt(getDeletedAt());
        interactiveMessage.setDeliveredAt(getDeliveredAt());
        interactiveMessage.setUpdatedAt(getUpdatedAt());
        interactiveMessage.setDeletedBy(getDeletedBy());
        interactiveMessage.setInteractionGoal(getInteractionGoal());
        interactiveMessage.setInteractions(getInteractions() == null ? new ArrayList<>() : getInteractions());
        interactiveMessage.setMetadata(getMetadata());
        interactiveMessage.setMuid(getMuid());
        interactiveMessage.setRawMessage(getRawMessage());
        interactiveMessage.setConversationId(getConversationId());
        interactiveMessage.setReadByMeAt(getReadByMeAt());
        interactiveMessage.setReceiverUid(getReceiverUid());
        interactiveMessage.setReplyCount(getReplyCount());
        interactiveMessage.setTags(getTags());
        interactiveMessage.setAllowSenderInteraction(isAllowSenderInteraction());

        JSONObject jsonObject = new JSONObject();
        try {
            if (getImageUrl() != null && !getImageUrl().isEmpty()) {
                jsonObject.put(InteractiveConstants.IMAGE_URL, getImageUrl());
            }
            if (getText() != null && !getText().isEmpty()) {
                jsonObject.put(InteractiveConstants.TEXT, getText());
            }
            if (getGoalCompletionText() != null && !getGoalCompletionText().isEmpty()) {
                jsonObject.put(InteractiveConstants.GOAL_COMPLETION_TEXT, getGoalCompletionText());
            }
            if (getCardActions() != null && !getCardActions().isEmpty()) {
                JSONArray jsonArray = new JSONArray();
                for (ElementEntity elementEntity : getCardActions()) {
                    if (elementEntity.getElementType().equals(InteractiveConstants.ElementsType.BUTTON)) {
                        jsonArray.put(((ButtonElement) elementEntity).toJson());
                    }
                }
                jsonObject.put(InteractiveConstants.CARD_ACTIONS, jsonArray);
            }

        } catch (Exception e) {
            CometChatLogger.e(TAG, e.toString());
        }
        interactiveMessage.setInteractiveData(jsonObject);
        return interactiveMessage;
    }
}
