package com.cometchat.chatuikit.shared.models.interactivemessage;

import com.cometchat.chat.models.InteractiveMessage;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;

import org.json.JSONObject;

import java.util.ArrayList;

public class CustomInteractiveMessage extends InteractiveMessage {
    private static final String TAG = CustomInteractiveMessage.class.getSimpleName();


    private CustomInteractiveMessage() {
        super(null, null, UIKitConstants.MessageType.CUSTOM_INTERACTIVE, new JSONObject());
    }

    public CustomInteractiveMessage(String receiverId, String receiverType, JSONObject jsonObject) {
        super(receiverId, receiverType, UIKitConstants.MessageType.CUSTOM_INTERACTIVE, jsonObject);
    }

    public static CustomInteractiveMessage fromInteractive(InteractiveMessage interactiveMessage) {
        if (interactiveMessage == null) return null;
        CustomInteractiveMessage customInteractiveMessage = new CustomInteractiveMessage();
        customInteractiveMessage.setId(interactiveMessage.getId());
        customInteractiveMessage.setReceiverType(interactiveMessage.getReceiverType());
        customInteractiveMessage.setReceiver(interactiveMessage.getReceiver());
        customInteractiveMessage.setSender(interactiveMessage.getSender());
        customInteractiveMessage.setSentAt(interactiveMessage.getSentAt());
        customInteractiveMessage.setReadAt(interactiveMessage.getReadAt());
        customInteractiveMessage.setDeliveredAt(interactiveMessage.getDeliveredAt());
        customInteractiveMessage.setUpdatedAt(interactiveMessage.getUpdatedAt());
        customInteractiveMessage.setDeletedBy(interactiveMessage.getDeletedBy());
        customInteractiveMessage.setDeletedAt(interactiveMessage.getDeletedAt());
        customInteractiveMessage.setInteractionGoal(interactiveMessage.getInteractionGoal());
        customInteractiveMessage.setInteractions(interactiveMessage.getInteractions() == null ? new ArrayList<>() : interactiveMessage.getInteractions());
        customInteractiveMessage.setMetadata(interactiveMessage.getMetadata());
        customInteractiveMessage.setMuid(interactiveMessage.getMuid());
        customInteractiveMessage.setRawMessage(interactiveMessage.getRawMessage());
        customInteractiveMessage.setConversationId(interactiveMessage.getConversationId());
        customInteractiveMessage.setReadByMeAt(interactiveMessage.getReadByMeAt());
        customInteractiveMessage.setReceiverUid(interactiveMessage.getReceiverUid());
        customInteractiveMessage.setReplyCount(interactiveMessage.getReplyCount());
        customInteractiveMessage.setTags(interactiveMessage.getTags());
        customInteractiveMessage.setAllowSenderInteraction(interactiveMessage.isAllowSenderInteraction());
        customInteractiveMessage.setInteractiveData(interactiveMessage.getInteractiveData());
        return customInteractiveMessage;
    }

    public InteractiveMessage toInteractiveMessage() {
        InteractiveMessage interactiveMessage = new InteractiveMessage(getReceiverUid(), getReceiverType(), UIKitConstants.MessageType.CUSTOM_INTERACTIVE, new JSONObject());
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
        interactiveMessage.setInteractiveData(getInteractiveData());
        return interactiveMessage;
    }
}
