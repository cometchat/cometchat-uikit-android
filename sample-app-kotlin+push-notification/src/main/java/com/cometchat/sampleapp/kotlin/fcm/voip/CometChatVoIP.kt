package com.cometchat.sampleapp.kotlin.fcm.voip

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.cometchat.chatuikit.logger.CometChatLogger
import com.cometchat.sampleapp.kotlin.fcm.voip.enums.CometChatVoIPErrorCode
import com.cometchat.sampleapp.kotlin.fcm.voip.interfaces.VoIPPermissionListener
import com.cometchat.sampleapp.kotlin.fcm.voip.model.CometChatVoIPError

object CometChatVoIP {
    private val TAG: String = CometChatVoIP::class.java.simpleName
    var phoneAccountHandle: PhoneAccountHandle? = null
        private set
    var telecomManager: TelecomManager? = null
        private set
    var phoneAccount: PhoneAccount? = null
        private set

    fun init(context: Context, label: String) {
        phoneAccountHandle = PhoneAccountHandle(
            ComponentName(
                context, CometChatVoIPConnectionService::class.java
            ), context.packageName
        )
        telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        phoneAccount = PhoneAccount
            .builder(phoneAccountHandle, label)
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
            .setHighlightColor(Color.BLUE) // Optional, to make calls identifiable
            .build()
        telecomManager!!.registerPhoneAccount(phoneAccount)
    }

    fun requestAllPermissions(activity: Activity, requestCode: Int): Boolean {
        val permissionsToRequest: MutableList<String> = ArrayList()
        if (!CometChatVoIPUtils.hasReadPhoneStatePermission(activity)) {
            permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (!CometChatVoIPUtils.hasManageOwnCallsPermission(activity)) {
            permissionsToRequest.add(Manifest.permission.MANAGE_OWN_CALLS)
        }
        if (!CometChatVoIPUtils.hasAnswerPhoneCallsPermission(activity)) {
            permissionsToRequest.add(Manifest.permission.ANSWER_PHONE_CALLS)
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                activity, permissionsToRequest.toTypedArray<String>(), requestCode
            )
            return false
        } else {
            CometChatLogger.i(TAG, "All permissions already granted.")
            return true
        }
    }

    fun hasEnabledPhoneAccountForVoIP(context: Context, listener: VoIPPermissionListener) {
        if (telecomManager == null)
            telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

        if (CometChatVoIPUtils.hasEnabledPhoneAccountForVoIP(context, telecomManager!!)) {
            listener.onPermissionsGranted()
        } else {
            listener.onPermissionsDenied(
                CometChatVoIPError(
                    CometChatVoIPErrorCode.ERROR_CODE_PHONE_ACCOUNT, "Phone account not enabled for VoIP"
                )
            )
        }
    }

    fun requestReadPhoneStatePermission(activity: Activity, requestCode: Int) {
        CometChatVoIPUtils.requestReadPhoneStatePermission(activity, requestCode)
    }

    fun requestManageOwnCallsPermission(activity: Activity, requestCode: Int) {
        CometChatVoIPUtils.requestManageOwnCallsPermission(activity, requestCode)
    }

    fun requestAnswerPhoneCallsPermission(activity: Activity, requestCode: Int) {
        CometChatVoIPUtils.requestAnswerPhoneCallsPermission(activity, requestCode)
    }

    fun hasAnswerPhoneCallsPermission(context: Context): Boolean {
        return CometChatVoIPUtils.hasAnswerPhoneCallsPermission(context)
    }

    fun hasManageOwnCallsPermission(context: Context): Boolean {
        return CometChatVoIPUtils.hasManageOwnCallsPermission(context)
    }

    fun hasReadPhoneStatePermission(context: Context): Boolean {
        return CometChatVoIPUtils.hasReadPhoneStatePermission(context)
    }

    fun hasEnabledPhoneAccountForVoIP(context: Context, telecomManager: TelecomManager?): Boolean {
        return CometChatVoIPUtils.hasEnabledPhoneAccountForVoIP(context, telecomManager!!)
    }

    fun placeCall(context: Context, extras: Bundle?, uri: Uri?) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            Log.e("TAG", "Permission CALL_PHONE not granted")
            return
        }
        telecomManager!!.placeCall(uri, extras)
    }

    fun addNewIncomingCall(context: Context?, extras: Bundle?) {
        if (CometChatVoIPUtils.hasManageOwnCallsPermission(context!!)) {
            telecomManager!!.addNewIncomingCall(phoneAccountHandle, extras)
        } else {
            Log.e(TAG, "Manage Own Calls Permission not granted")
        }
    }

    fun requestPhoneStatePermissions(activity: Activity, requestCode: Int) {
        if (!CometChatVoIPUtils.hasReadPhoneStatePermission(activity)) {
            ActivityCompat.requestPermissions(
                activity, arrayOf(Manifest.permission.READ_PHONE_STATE), requestCode
            )
        } else {
            CometChatLogger.e(TAG, "Read Phone State permission already granted.")
        }
    }

    fun requestManageOwnCallsPermissions(activity: Activity, requestCode: Int) {
        if (!CometChatVoIPUtils.hasManageOwnCallsPermission(activity)) {
            ActivityCompat.requestPermissions(
                activity, arrayOf(Manifest.permission.MANAGE_OWN_CALLS), requestCode
            )
        } else {
            CometChatLogger.e(TAG, "Manage Own Calls permission already granted.")
        }
    }

    fun requestAnswerPhoneCallsPermissions(activity: Activity, requestCode: Int) {
        if (!CometChatVoIPUtils.hasAnswerPhoneCallsPermission(activity)) {
            ActivityCompat.requestPermissions(
                activity, arrayOf(Manifest.permission.ANSWER_PHONE_CALLS), requestCode
            )
        } else {
            CometChatLogger.e(TAG, "Answer Phone Calls permission already granted.")
        }
    }

    fun requestCallAccountPermission(context: Context) {
        if (!CometChatVoIPUtils.hasEnabledPhoneAccountForVoIP(context, telecomManager!!)) {
            alertDialogForVoIP(context)
        } else {
            CometChatLogger.e(TAG, "Phone account already enabled for VoIP.")
        }
    }

    fun alertDialogForVoIP(context: Context) {
        val alertDialog = AlertDialog.Builder(context)
        alertDialog.setTitle("VoIP Permission")
        alertDialog.setMessage(
            "To make VoIP Calling work properly, you need to allow certain permission from your call account settings for this app."
        )
        alertDialog.setPositiveButton(
            "Open Settings"
        ) { dialog: DialogInterface?, which: Int -> launchVoIPSetting(context) }
        alertDialog.setNegativeButton("Cancel") { dialog: DialogInterface, which: Int -> dialog.dismiss() }
        alertDialog.create().show()
    }

    fun launchVoIPSetting(context: Context) {
        CometChatVoIPUtils.launchVoIPSetting(context)
    }
}

