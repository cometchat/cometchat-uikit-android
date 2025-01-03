package com.cometchat.sampleapp.java.fcm.voip;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.sampleapp.java.fcm.voip.enums.CometChatVoIPErrorCode;
import com.cometchat.sampleapp.java.fcm.voip.interfaces.VoIPPermissionListener;
import com.cometchat.sampleapp.java.fcm.voip.model.CometChatVoIPError;

import java.util.ArrayList;
import java.util.List;


public class CometChatVoIP {
    private static final String TAG = CometChatVoIP.class.getSimpleName();
    private static PhoneAccountHandle phoneAccountHandle;
    private static TelecomManager telecomManager;
    private static PhoneAccount phoneAccount;

    public static void init(@NonNull Context context, @NonNull String label) {
        phoneAccountHandle = new PhoneAccountHandle(new ComponentName(context, CometChatVoIPConnectionService.class), context.getPackageName());
        telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        phoneAccount = PhoneAccount.builder(phoneAccountHandle, label)
                                   .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
                                   .setHighlightColor(Color.BLUE) // Optional, to make calls identifiable
                                   .build();
        telecomManager.registerPhoneAccount(phoneAccount);
    }

    public static PhoneAccountHandle getPhoneAccountHandle() {
        return phoneAccountHandle;
    }

    public static TelecomManager getTelecomManager() {
        return telecomManager;
    }

    public static PhoneAccount getPhoneAccount() {
        return phoneAccount;
    }

    public static boolean requestAllPermissions(@NonNull Activity activity, int requestCode) {
        List<String> permissionsToRequest = new ArrayList<>();
        if (!CometChatVoIPUtils.hasReadPhoneStatePermission(activity)) {
            permissionsToRequest.add(android.Manifest.permission.READ_PHONE_STATE);
        }
        if (!CometChatVoIPUtils.hasManageOwnCallsPermission(activity)) {
            permissionsToRequest.add(android.Manifest.permission.MANAGE_OWN_CALLS);
        }
        if (!CometChatVoIPUtils.hasAnswerPhoneCallsPermission(activity)) {
            permissionsToRequest.add(android.Manifest.permission.ANSWER_PHONE_CALLS);
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toArray(new String[0]),
                requestCode
            );
            return false;
        } else {
            CometChatLogger.i(TAG, "All permissions already granted.");
            return true;
        }
    }

    public static void hasEnabledPhoneAccountForVoIP(@NonNull Context context, VoIPPermissionListener listener) {
        if (telecomManager == null)
            telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);

        if (CometChatVoIPUtils.hasEnabledPhoneAccountForVoIP(context, telecomManager)) {
            listener.onPermissionsGranted();
        } else {
            listener.onPermissionsDenied(new CometChatVoIPError(
                CometChatVoIPErrorCode.ERROR_CODE_PHONE_ACCOUNT,
                "Phone account not enabled for VoIP"
            ));
        }
    }

    public static void requestReadPhoneStatePermission(@NonNull Activity activity, int requestCode) {
        CometChatVoIPUtils.requestReadPhoneStatePermission(activity, requestCode);
    }

    public static void requestManageOwnCallsPermission(@NonNull Activity activity, int requestCode) {
        CometChatVoIPUtils.requestManageOwnCallsPermission(activity, requestCode);
    }

    public static void requestAnswerPhoneCallsPermission(@NonNull Activity activity, int requestCode) {
        CometChatVoIPUtils.requestAnswerPhoneCallsPermission(activity, requestCode);
    }

    public static boolean hasAnswerPhoneCallsPermission(@NonNull Context context) {
        return CometChatVoIPUtils.hasAnswerPhoneCallsPermission(context);
    }

    public static boolean hasManageOwnCallsPermission(@NonNull Context context) {
        return CometChatVoIPUtils.hasManageOwnCallsPermission(context);
    }

    public static boolean hasReadPhoneStatePermission(@NonNull Context context) {
        return CometChatVoIPUtils.hasReadPhoneStatePermission(context);
    }

    public static boolean hasEnabledPhoneAccountForVoIP(@NonNull Context context, TelecomManager telecomManager) {
        return CometChatVoIPUtils.hasEnabledPhoneAccountForVoIP(context, telecomManager);
    }

    public static void placeCall(Context context, Bundle extras, Uri uri) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            Log.e("TAG", "Permission CALL_PHONE not granted");
            return;
        }
        telecomManager.placeCall(uri, extras);
    }

    public static void addNewIncomingCall(Context context, Bundle extras) {
        if (CometChatVoIPUtils.hasManageOwnCallsPermission(context)) {
            telecomManager.addNewIncomingCall(phoneAccountHandle, extras);
        } else {
            Log.e(TAG, "Manage Own Calls Permission not granted");
        }
    }

    public static void requestPhoneStatePermissions(@NonNull Activity activity, int requestCode) {
        if (!CometChatVoIPUtils.hasReadPhoneStatePermission(activity)) {
            ActivityCompat.requestPermissions(
                activity,
                new String[]{android.Manifest.permission.READ_PHONE_STATE},
                requestCode
            );
        } else {
            CometChatLogger.e(TAG, "Read Phone State permission already granted.");
        }
    }

    public static void requestManageOwnCallsPermissions(@NonNull Activity activity, int requestCode) {
        if (!CometChatVoIPUtils.hasManageOwnCallsPermission(activity)) {
            ActivityCompat.requestPermissions(
                activity,
                new String[]{android.Manifest.permission.MANAGE_OWN_CALLS},
                requestCode
            );
        } else {
            CometChatLogger.e(TAG, "Manage Own Calls permission already granted.");
        }
    }

    public static void requestAnswerPhoneCallsPermissions(@NonNull Activity activity, int requestCode) {
        if (!CometChatVoIPUtils.hasAnswerPhoneCallsPermission(activity)) {
            ActivityCompat.requestPermissions(
                activity,
                new String[]{android.Manifest.permission.ANSWER_PHONE_CALLS},
                requestCode
            );
        } else {
            CometChatLogger.e(TAG, "Answer Phone Calls permission already granted.");
        }
    }

    public static void requestCallAccountPermission(@NonNull Context context) {
        if (!CometChatVoIPUtils.hasEnabledPhoneAccountForVoIP(context, telecomManager)) {
            alertDialogForVoIP(context);
        } else {
            CometChatLogger.e(TAG, "Phone account already enabled for VoIP.");
        }
    }

    public static void alertDialogForVoIP(Context context) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle("VoIP Permission");
        alertDialog.setMessage(
            "To make VoIP Calling work properly, you need to allow certain permission from your call account settings for this app.");
        alertDialog.setPositiveButton("Open Settings", (dialog, which) -> CometChatVoIP.launchVoIPSetting(context));
        alertDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        alertDialog.create().show();
    }

    public static void launchVoIPSetting(@NonNull Context context) {
        CometChatVoIPUtils.launchVoIPSetting(context);
    }

}

