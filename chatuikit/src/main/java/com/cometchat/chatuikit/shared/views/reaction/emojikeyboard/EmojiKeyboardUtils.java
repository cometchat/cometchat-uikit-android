package com.cometchat.chatuikit.shared.views.reaction.emojikeyboard;

import android.content.Context;

import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.chatuikit.shared.views.reaction.emojikeyboard.model.EmojiCategory;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class EmojiKeyboardUtils {
    private static final String TAG = EmojiKeyboardUtils.class.getSimpleName();


    private static List<EmojiCategory> emojiCategories;

    public static void loadAndSaveEmojis(Context context) {
        if (emojiCategories == null || emojiCategories.isEmpty()) {
            new Thread(() -> {
                emojiCategories = new ArrayList<>();
                try {
                    JSONArray jsonArray = new JSONObject(loadJSONFromAsset(context)).getJSONArray("emojiCategory");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        EmojiCategory emojiCategory = new Gson().fromJson(String.valueOf(jsonArray.getJSONObject(i)), EmojiCategory.class);
                        emojiCategories.add(emojiCategory);
                    }
                } catch (Exception e) {
                    CometChatLogger.e(TAG, e.toString());
                }
            }).start();
        }
    }

    public static String loadJSONFromAsset(Context context) {
        String json = null;
        InputStream is = null;
        try {
            is = context.getAssets().open("emoji.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            int bytesRead = 0;
            // Read the file in chunks
            while (bytesRead < size) {
                int result = is.read(buffer, bytesRead, size - bytesRead);
                if (result == -1) break; // End of file
                bytesRead += result;
            }
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (Exception e) {
            CometChatLogger.e(TAG, e.getMessage());
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                CometChatLogger.e(TAG, e.getMessage());
            }
        }
        return json;
    }

    public static List<EmojiCategory> getEmojiCategories() {
        return emojiCategories;
    }
}
