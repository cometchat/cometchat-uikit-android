package com.cometchat.chatuikit.shared.models.interactiveactions;

import com.cometchat.chatuikit.shared.models.interactivemessage.InteractiveConstants;

public class ActionEntity {
    private static final String TAG = ActionEntity.class.getSimpleName();
    private final String actionType;

    public ActionEntity(@InteractiveConstants.ActionType String actionType) {
        this.actionType = actionType;
    }

    public String getActionType() {
        return actionType;
    }
}
