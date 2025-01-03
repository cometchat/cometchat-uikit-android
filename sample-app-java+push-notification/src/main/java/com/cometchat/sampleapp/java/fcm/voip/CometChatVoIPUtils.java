package com.cometchat.sampleapp.java.fcm.voip;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cometchat.chatuikit.logger.CometChatLogger;

import java.util.List;

public class CometChatVoIPUtils {
    private static final String TAG = CometChatVoIPUtils.class.getSimpleName();
    public static boolean isCallOngoing = false;
    public static String currentSessionId = "";

    public static boolean hasManageOwnCallsPermission(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.MANAGE_OWN_CALLS) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestManageOwnCallsPermission(@NonNull Activity activity, int requestCode) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.MANAGE_OWN_CALLS}, requestCode);
    }

    public static boolean hasReadPhoneStatePermission(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestReadPhoneStatePermission(@NonNull Activity activity, int requestCode) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_PHONE_STATE}, requestCode);
    }

    public static boolean hasAnswerPhoneCallsPermission(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasEnabledPhoneAccountForVoIP(@NonNull Context context, @NonNull TelecomManager telecomManager) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        try {
            List<PhoneAccountHandle> enabledAccounts = telecomManager.getCallCapablePhoneAccounts();
            String expectedClassName = CometChatVoIPConnectionService.class.getName();
            for (PhoneAccountHandle account : enabledAccounts) {
                if (expectedClassName.equals(account.getComponentName().getClassName())) {
                    return true;
                }
            }
        } catch (Exception e) {
            CometChatLogger.e(TAG, e.getMessage());
        }
        return false;
    }

    public static void launchVoIPSetting(@NonNull Context context) {
        Intent intent = new Intent();
        intent.setAction(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS);
        ComponentName telecomComponent = new ComponentName(
            "com.android.server.telecom",
            "com.android.server.telecom.settings.EnableAccountPreferenceActivity"
        );
        intent.setComponent(telecomComponent);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    public static void requestAnswerPhoneCallsPermission(@NonNull Activity activity, int requestCode) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ANSWER_PHONE_CALLS}, requestCode);
    }


}
