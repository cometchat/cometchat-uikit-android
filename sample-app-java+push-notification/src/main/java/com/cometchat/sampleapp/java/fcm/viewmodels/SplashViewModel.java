package com.cometchat.sampleapp.java.fcm.viewmodels;

import android.content.Context;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cometchat.chat.core.CometChat;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit;
import com.cometchat.chatuikit.shared.cometchatuikit.UIKitSettings;
import com.cometchat.sampleapp.java.fcm.AppCredentials;
import com.cometchat.sampleapp.java.fcm.BuildConfig;
import com.cometchat.sampleapp.java.fcm.R;
import com.cometchat.sampleapp.java.fcm.utils.AppUtils;

import org.json.JSONObject;

/**
 * ViewModel for the SplashActivity to handle UI-related data and business
 * logic.
 */
public class SplashViewModel extends ViewModel {

    private final MutableLiveData<Boolean> loginStatus = new MutableLiveData<>();

    /**
     * Initializes the CometChat UIKit with necessary settings.
     *
     * @param context The activity context for initializing UIKit.
     */
    public void initUIKit(Context context) {
        initKit(context, null);
    }

    private void initKit(Context context, CometChat.CallbackListener<String> callbackListener) {

        String appId = AppUtils.getDataFromSharedPref(context, String.class, context.getString(R.string.app_cred_id), AppCredentials.APP_ID);
        String region = AppUtils.getDataFromSharedPref(context, String.class, context.getString(R.string.app_cred_region), AppCredentials.REGION);
        String authKey = AppUtils.getDataFromSharedPref(context, String.class, context.getString(R.string.app_cred_auth), AppCredentials.AUTH_KEY);

        UIKitSettings uiKitSettings = new UIKitSettings.UIKitSettingsBuilder()
            .setAutoEstablishSocketConnection(false)
            .setAppId(appId)
            .setRegion(region)
            .setAuthKey(authKey)
            .subscribePresenceForAllUsers()
            .build();

        CometChatUIKit.init(context, uiKitSettings, new CometChat.CallbackListener<String>() {
            @Override
            public void onSuccess(String s) {
                CometChat.setDemoMetaInfo(getAppMetadata(context));
                checkUserIsNotLoggedIn();
                if (callbackListener != null) {
                    callbackListener.onSuccess(s);
                }
            }

            @Override
            public void onError(CometChatException e) {
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                if (callbackListener != null) {
                    callbackListener.onError(e);
                }
            }
        });
    }

    private JSONObject getAppMetadata(Context context) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", context.getResources().getString(R.string.app_name));
            jsonObject.put("type", "sample");
            jsonObject.put("version", BuildConfig.VERSION_NAME);
            jsonObject.put("bundle", BuildConfig.APPLICATION_ID);
            jsonObject.put("platform", "android");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    /**
     * Checks if the user is logged in and updates the login status.
     */
    public void checkUserIsNotLoggedIn() {
        if (CometChatUIKit.getLoggedInUser() != null) {
            loginStatus.setValue(true); // User is logged in
        } else {
            User user = CometChatUIKit.getLoggedInUser();
            if (user != null) {
                loginStatus.setValue(true); // User is logged in
            } else {
                loginStatus.setValue(false); // User is not logged in
            }
        }
    }

    public void initUIKit(Context context, CometChat.CallbackListener<String> callbackListener) {
        initKit(context, callbackListener);
    }

    /**
     * Returns the login status LiveData.
     *
     * @return LiveData containing the user's login status.
     */
    public LiveData<Boolean> getLoginStatus() {
        return loginStatus;
    }
}
