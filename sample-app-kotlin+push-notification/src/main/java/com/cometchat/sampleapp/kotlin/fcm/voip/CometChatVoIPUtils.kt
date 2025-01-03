package com.cometchat.sampleapp.kotlin.fcm.voip

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telecom.TelecomManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cometchat.chatuikit.logger.CometChatLogger

object CometChatVoIPUtils {
    var isCallOngoing: Boolean = false
    var currentSessionId: String = ""
    val TAG: String = CometChatVoIPUtils::class.java.simpleName

    fun hasManageOwnCallsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.MANAGE_OWN_CALLS) == PackageManager.PERMISSION_GRANTED
    }

    fun requestManageOwnCallsPermission(activity: Activity, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.MANAGE_OWN_CALLS), requestCode)
    }

    fun hasReadPhoneStatePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    }

    fun requestReadPhoneStatePermission(activity: Activity, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_PHONE_STATE), requestCode)
    }

    fun hasAnswerPhoneCallsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED
    }

    fun hasEnabledPhoneAccountForVoIP(context: Context, telecomManager: TelecomManager): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        try {
            val enabledAccounts = telecomManager.callCapablePhoneAccounts
            val expectedClassName: String = CometChatVoIPConnectionService::class.java.name
            for (account in enabledAccounts) {
                if (expectedClassName == account.componentName.className) {
                    return true
                }
            }
        } catch (e: Exception) {
            CometChatLogger.e(TAG, e.message)
        }
        return false
    }

    fun launchVoIPSetting(context: Context) {
        val intent = Intent()
        intent.setAction(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS)
        val telecomComponent = ComponentName(
            "com.android.server.telecom", "com.android.server.telecom.settings.EnableAccountPreferenceActivity"
        )
        intent.setComponent(telecomComponent)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(intent)
    }

    fun requestAnswerPhoneCallsPermission(activity: Activity, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ANSWER_PHONE_CALLS), requestCode)
    }
}
