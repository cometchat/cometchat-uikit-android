package com.cometchat.chatuikit.shared.models.interactivemessage;

import com.cometchat.chat.models.InteractiveMessage;
import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.models.interactiveelements.ButtonElement;
import com.cometchat.chatuikit.shared.models.interactiveelements.CheckboxElement;
import com.cometchat.chatuikit.shared.models.interactiveelements.DateTimeElement;
import com.cometchat.chatuikit.shared.models.interactiveelements.ElementEntity;
import com.cometchat.chatuikit.shared.models.interactiveelements.LabelElement;
import com.cometchat.chatuikit.shared.models.interactiveelements.RadioButtonElement;
import com.cometchat.chatuikit.shared.models.interactiveelements.SingleSelectElement;
import com.cometchat.chatuikit.shared.models.interactiveelements.SpinnerElement;
import com.cometchat.chatuikit.shared.models.interactiveelements.TextInputElement;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FormMessage extends InteractiveMessage {
    private static final String TAG = FormMessage.class.getSimpleName();


    private List<ElementEntity> formFields;
    private String title;
    private String goalCompletionText;
    private ButtonElement submitElement;

    private FormMessage() {
        super(null, null, UIKitConstants.MessageType.FORM, new JSONObject());
    }

    public FormMessage(String receiverId, String receiverType, List<ElementEntity> formFields, ButtonElement submitElement) {
        super(receiverId, receiverType, UIKitConstants.MessageType.FORM, new JSONObject());
        setFormFields(formFields);
        setSubmitElement(submitElement);
    }

    public void setGoalCompletionText(String goalCompletionText) {
        this.goalCompletionText = goalCompletionText;
    }

    public String getGoalCompletionText() {
        return goalCompletionText;
    }

    public List<ElementEntity> getFormFields() {
        return formFields;
    }

    public void setFormFields(List<ElementEntity> elementEntities) {
        this.formFields = elementEntities;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSubmitElement(ButtonElement submitElement) {
        this.submitElement = submitElement;
    }

    public ButtonElement getSubmitElement() {
        return submitElement;
    }

    public static FormMessage fromInteractive(InteractiveMessage interactiveMessage) {
        if (interactiveMessage == null) return null;
        FormMessage formMessage = new FormMessage();
        formMessage.setId(interactiveMessage.getId());
        formMessage.setReceiverType(interactiveMessage.getReceiverType());
        formMessage.setReceiver(interactiveMessage.getReceiver());
        formMessage.setSender(interactiveMessage.getSender());
        formMessage.setSentAt(interactiveMessage.getSentAt());
        formMessage.setReadAt(interactiveMessage.getReadAt());
        formMessage.setDeliveredAt(interactiveMessage.getDeliveredAt());
        formMessage.setUpdatedAt(interactiveMessage.getUpdatedAt());
        formMessage.setDeletedBy(interactiveMessage.getDeletedBy());
        formMessage.setDeletedAt(interactiveMessage.getDeletedAt());
        formMessage.setInteractionGoal(interactiveMessage.getInteractionGoal());
        formMessage.setInteractions(interactiveMessage.getInteractions() == null ? new ArrayList<>() : interactiveMessage.getInteractions());
        formMessage.setMetadata(interactiveMessage.getMetadata());
        formMessage.setMuid(interactiveMessage.getMuid());
        formMessage.setRawMessage(interactiveMessage.getRawMessage());
        formMessage.setConversationId(interactiveMessage.getConversationId());
        formMessage.setReadByMeAt(interactiveMessage.getReadByMeAt());
        formMessage.setReceiverUid(interactiveMessage.getReceiverUid());
        formMessage.setReplyCount(interactiveMessage.getReplyCount());
        formMessage.setTags(interactiveMessage.getTags());
        formMessage.setAllowSenderInteraction(interactiveMessage.isAllowSenderInteraction());

        try {
            if (interactiveMessage.getInteractiveData() != null) {
                JSONObject jsonObject = interactiveMessage.getInteractiveData();
                if (jsonObject.has(InteractiveConstants.TITLE)) {
                    formMessage.setTitle(jsonObject.optString(InteractiveConstants.TITLE));
                }
                if (jsonObject.has(InteractiveConstants.GOAL_COMPLETION_TEXT)) {
                    formMessage.setGoalCompletionText(jsonObject.optString(InteractiveConstants.GOAL_COMPLETION_TEXT));
                }
                if (jsonObject.has(InteractiveConstants.INTERACTIVE_MESSAGE_FORM_FIELD)) {
                    List<ElementEntity> elementEntityList = new ArrayList<>();
                    JSONArray jsonArray = jsonObject.getJSONArray(InteractiveConstants.INTERACTIVE_MESSAGE_FORM_FIELD);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject element = jsonArray.getJSONObject(i);
                        String elementType = element.optString(InteractiveConstants.ELEMENT_TYPE);
                        switch (elementType) {
                            case InteractiveConstants.ElementsType.LABEL:
                                elementEntityList.add(LabelElement.fromJson(element));
                                break;
                            case InteractiveConstants.ElementsType.BUTTON:
                                elementEntityList.add(ButtonElement.fromJson(element));
                                break;
                            case InteractiveConstants.ElementsType.CHECK_BOX:
                                elementEntityList.add(CheckboxElement.fromJson(element));
                                break;
                            case InteractiveConstants.ElementsType.DROP_DOWN:
                                elementEntityList.add(SpinnerElement.fromJson(element));
                                break;
                            case InteractiveConstants.ElementsType.RADIO_BUTTON:
                                elementEntityList.add(RadioButtonElement.fromJson(element));
                                break;
                            case InteractiveConstants.ElementsType.SINGLE_SELECT:
                                elementEntityList.add(SingleSelectElement.fromJson(element));
                                break;
                            case InteractiveConstants.ElementsType.TEXT_INPUT:
                                elementEntityList.add(TextInputElement.fromJson(element));
                                break;
                            case InteractiveConstants.ElementsType.DATE_TIME:
                                elementEntityList.add(DateTimeElement.fromJson(element));
                                break;
                        }
                    }
                    formMessage.setFormFields(elementEntityList);
                }
                if (jsonObject.has(InteractiveConstants.INTERACTIVE_MESSAGE_SUBMIT_ELEMENT)) {
                    JSONObject submitElement = jsonObject.getJSONObject(InteractiveConstants.INTERACTIVE_MESSAGE_SUBMIT_ELEMENT);
                    formMessage.setSubmitElement(ButtonElement.fromJson(submitElement));
                }
            }
        } catch (Exception e) {

        }
        return formMessage;
    }

    public InteractiveMessage toInteractiveMessage() {
        InteractiveMessage interactiveMessage = new InteractiveMessage(getReceiverUid(), getReceiverType(), UIKitConstants.MessageType.FORM, new JSONObject());
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
            if (getTitle() != null && !getTitle().isEmpty()) {
                jsonObject.put(InteractiveConstants.TITLE, getTitle());
            }
            if (getGoalCompletionText() != null && !getGoalCompletionText().isEmpty()) {
                jsonObject.put(InteractiveConstants.GOAL_COMPLETION_TEXT, getGoalCompletionText());
            }
            if (getFormFields() != null && !getFormFields().isEmpty()) {
                JSONArray jsonArray = new JSONArray();
                for (ElementEntity elementEntity : getFormFields()) {
                    switch (elementEntity.getElementType()) {
                        case InteractiveConstants.ElementsType.LABEL:
                            jsonArray.put(((LabelElement) elementEntity).toJson());
                            break;
                        case InteractiveConstants.ElementsType.BUTTON:
                            jsonArray.put(((ButtonElement) elementEntity).toJson());
                            break;
                        case InteractiveConstants.ElementsType.CHECK_BOX:
                            jsonArray.put(((CheckboxElement) elementEntity).toJson());
                            break;
                        case InteractiveConstants.ElementsType.DROP_DOWN:
                            jsonArray.put(((SpinnerElement) elementEntity).toJson());
                            break;
                        case InteractiveConstants.ElementsType.RADIO_BUTTON:
                            jsonArray.put(((RadioButtonElement) elementEntity).toJson());
                            break;
                        case InteractiveConstants.ElementsType.SINGLE_SELECT:
                            jsonArray.put(((SingleSelectElement) elementEntity).toJson());
                            break;
                        case InteractiveConstants.ElementsType.TEXT_INPUT:
                            jsonArray.put(((TextInputElement) elementEntity).toJson());
                            break;
                        case InteractiveConstants.ElementsType.DATE_TIME:
                            jsonArray.put(((DateTimeElement) elementEntity).toJson());
                            break;
                    }
                }
                jsonObject.put(InteractiveConstants.INTERACTIVE_MESSAGE_FORM_FIELD, jsonArray);
                jsonObject.put(InteractiveConstants.INTERACTIVE_MESSAGE_SUBMIT_ELEMENT, getSubmitElement().toJson());
            }

        } catch (Exception e) {
            CometChatLogger.e(TAG, e.toString());
        }
        interactiveMessage.setInteractiveData(jsonObject);
        return interactiveMessage;
    }
}
