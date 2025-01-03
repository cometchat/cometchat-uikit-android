package com.cometchat.chatuikit.shared.models.interactiveelements;

import androidx.annotation.Nullable;

import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.models.interactiveactions.APIAction;
import com.cometchat.chatuikit.shared.models.interactiveactions.ActionEntity;
import com.cometchat.chatuikit.shared.models.interactiveactions.CustomAction;
import com.cometchat.chatuikit.shared.models.interactiveactions.DeepLinkingAction;
import com.cometchat.chatuikit.shared.models.interactiveactions.URLNavigationAction;
import com.cometchat.chatuikit.shared.models.interactivemessage.InteractiveConstants;

import org.json.JSONObject;

public class ButtonElement extends BaseInteractiveElement {
    private static final String TAG = ButtonElement.class.getSimpleName();

    private boolean disableAfterInteracted;
    private String text;

    private ButtonElement() {
        super(UIKitConstants.UIElementsType.UI_ELEMENT_BUTTON, null, null);
    }

    public ButtonElement(String elementId, String text, ActionEntity action) {
        super(UIKitConstants.UIElementsType.UI_ELEMENT_BUTTON, elementId, action);
        setText(text);
        disableAfterInteracted = true;
    }

    public static ButtonElement fromJson(@Nullable JSONObject element) {
        ButtonElement button = new ButtonElement();
        if (element != null) {
            try {
                if (element.has(InteractiveConstants.ELEMENT_ID))
                    button.setElementId(element.getString(InteractiveConstants.ELEMENT_ID));
                if (element.has(InteractiveConstants.ButtonUIConstants.TEXT))
                    button.setText(element.getString(InteractiveConstants.ButtonUIConstants.TEXT));
                if (element.has(InteractiveConstants.ButtonUIConstants.DISABLE_AFTER_INTERACTED))
                    button.setDisableAfterInteracted(element.getBoolean(InteractiveConstants.ButtonUIConstants.DISABLE_AFTER_INTERACTED));

                if (element.has(InteractiveConstants.ButtonUIConstants.ACTION)) {
                    JSONObject action = element.getJSONObject(InteractiveConstants.ButtonUIConstants.ACTION);
                    if (action.has(InteractiveConstants.ButtonUIConstants.ACTION_TYPE)) {
                        String actionType = action.getString(InteractiveConstants.ButtonUIConstants.ACTION_TYPE);
                        if (InteractiveConstants.ACTION_TYPE_API_ACTION.equalsIgnoreCase(actionType)) {
                            button.setAction(APIAction.fromJson(action));
                        } else if (InteractiveConstants.ACTION_TYPE_URL_NAVIGATION.equalsIgnoreCase(actionType)) {
                            button.setAction(URLNavigationAction.fromJson(action));
                        } else if (InteractiveConstants.ACTION_TYPE_CUSTOM.equalsIgnoreCase(actionType)) {
                            button.setAction(CustomAction.fromJson(action));
                        }
                    }
                }

            } catch (Exception e) {
                CometChatLogger.e(TAG, e.toString());
            }
        }
        return button;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(InteractiveConstants.ELEMENT_TYPE, getElementType());
            if (getElementId() != null)
                jsonObject.put(InteractiveConstants.ELEMENT_ID, getElementId());
            if (getText() != null)
                jsonObject.put(InteractiveConstants.ButtonUIConstants.TEXT, getText());
            jsonObject.put(InteractiveConstants.ButtonUIConstants.DISABLE_AFTER_INTERACTED, isDisableAfterInteracted());

            if (getAction() != null) {
                if (getAction() instanceof APIAction) {
                    APIAction apiAction = (APIAction) getAction();
                    jsonObject.put(InteractiveConstants.ButtonUIConstants.ACTION, apiAction.toJson());
                } else if (getAction() instanceof URLNavigationAction) {
                    URLNavigationAction urlNavigationAction = (URLNavigationAction) getAction();
                    jsonObject.put(InteractiveConstants.ButtonUIConstants.ACTION, urlNavigationAction.toJson());
                } else if (getAction() instanceof DeepLinkingAction) {
                    DeepLinkingAction deepLinkingAction = (DeepLinkingAction) getAction();
                    jsonObject.put(InteractiveConstants.ButtonUIConstants.ACTION, deepLinkingAction.toJson());
                } else if (getAction() instanceof CustomAction) {
                    CustomAction customAction = (CustomAction) getAction();
                    jsonObject.put(InteractiveConstants.ButtonUIConstants.ACTION, customAction.toJson());
                }
            }

        } catch (Exception e) {
            CometChatLogger.e(TAG, e.toString());
        }
        return jsonObject;
    }

    public void setDisableAfterInteracted(boolean disableAfterInteracted) {
        this.disableAfterInteracted = disableAfterInteracted;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isDisableAfterInteracted() {
        return disableAfterInteracted;
    }

    public String getText() {
        return text;
    }
}
