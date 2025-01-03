package com.cometchat.sampleapp.java.fcm.voip.interfaces;


import com.cometchat.sampleapp.java.fcm.voip.model.CometChatVoIPError;

public interface VoIPPermissionListener {
    void onPermissionsGranted();

    void onPermissionsDenied(CometChatVoIPError error);
}
