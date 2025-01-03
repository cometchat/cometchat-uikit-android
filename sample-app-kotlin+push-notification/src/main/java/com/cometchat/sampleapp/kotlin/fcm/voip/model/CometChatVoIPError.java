package com.cometchat.sampleapp.kotlin.fcm.voip.model;

import com.cometchat.sampleapp.kotlin.fcm.voip.enums.CometChatVoIPErrorCode;

public class CometChatVoIPError {
    private CometChatVoIPErrorCode code;
    private String message;

    public CometChatVoIPError(CometChatVoIPErrorCode code, String message) {
        this.code = code;
        this.message = message;
    }

    public CometChatVoIPErrorCode getCode() {
        return code;
    }

    public void setCode(CometChatVoIPErrorCode code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
