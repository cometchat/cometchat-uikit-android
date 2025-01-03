package com.cometchat.sampleapp.java.viewmodels;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cometchat.chat.core.CometChat;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit;
import com.cometchat.chatuikit.shared.cometchatuikit.UIKitSettings;
import com.cometchat.sampleapp.java.R;
import com.cometchat.sampleapp.java.ui.activity.LoginActivity;
import com.cometchat.sampleapp.java.utils.AppUtils;

/**
 * ViewModel for the SplashActivity to handle UI-related data and business
 * logic.
 */
public class AppCredentialsViewModel extends ViewModel {

    private final MutableLiveData<Boolean> loginStatus = new MutableLiveData<>();
    private final MutableLiveData<String> selectedRegion = new MutableLiveData<>();

    public MutableLiveData<String> getSelectedRegion() {
        return selectedRegion;
    }

    public void setSelectedRegion(String selectedRegion) {
        this.selectedRegion.setValue(selectedRegion);
    }

    public void initUIKit(Activity activity, String appId, String authKey) {
        AppUtils.clearSharePref(activity);
        AppUtils.saveDataInSharedPref(activity, activity.getString(R.string.app_cred_id), appId);
        AppUtils.saveDataInSharedPref(activity, activity.getString(R.string.app_cred_region), selectedRegion.getValue());
        AppUtils.saveDataInSharedPref(activity, activity.getString(R.string.app_cred_auth), authKey);

        UIKitSettings uiKitSettings = new UIKitSettings.UIKitSettingsBuilder()
                .setAutoEstablishSocketConnection(true)
                .setRegion(selectedRegion.getValue())
                .setAppId(appId)
                .setAuthKey(authKey)
                .subscribePresenceForAllUsers()
                .build();

        CometChatUIKit.init(activity, uiKitSettings, new CometChat.CallbackListener<String>() {
            @Override
            public void onSuccess(String s) {
                activity.startActivity(new Intent(activity, LoginActivity.class));
                activity.finish();
            }

            @Override
            public void onError(CometChatException e) {
                // Show error message
                Toast.makeText(activity, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Checks if the user is logged in and updates the login status.
     */
    private void checkUserIsNotLoggedIn() {
        if (CometChatUIKit.getLoggedInUser() != null) {
            loginStatus.setValue(true); // User is logged in
        } else {
            loginStatus.setValue(false); // User is not logged in
        }
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
