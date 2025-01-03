package com.cometchat.sampleapp.kotlin.fcm.voip.interfaces

import com.cometchat.sampleapp.kotlin.fcm.voip.model.CometChatVoIPError

interface VoIPPermissionListener {
    fun onPermissionsGranted()

    fun onPermissionsDenied(error: CometChatVoIPError?)
}
