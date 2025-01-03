package com.cometchat.chatuikit.shared.models.interactiveelements;

import com.cometchat.chatuikit.shared.models.interactiveactions.ActionEntity;

public class BaseInteractiveElement extends ElementEntity {
    private static final String TAG = BaseInteractiveElement.class.getSimpleName();
    private ActionEntity actionEntity;

    public BaseInteractiveElement(String elementType, String elementId, ActionEntity actionEntity) {
        super(elementType, elementId);
        this.actionEntity = actionEntity;
    }

    public ActionEntity getAction() {
        return actionEntity;
    }

    public void setAction(ActionEntity actionEntity) {
        this.actionEntity = actionEntity;
    }
}
