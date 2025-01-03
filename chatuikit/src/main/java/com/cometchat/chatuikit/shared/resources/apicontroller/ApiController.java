package com.cometchat.chatuikit.shared.resources.apicontroller;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.cometchat.chatuikit.logger.CometChatLogger;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiController {
    private static final String TAG = ApiController.class.getSimpleName();

    private static ApiController instance;
    private final OkHttpClient client;
    private final Handler handler;

    private ApiController() {
        this.client = new OkHttpClient();
        handler = new Handler(Looper.getMainLooper());
    }

    public static synchronized ApiController getInstance() {
        if (instance == null) {
            instance = new ApiController();
        }
        return instance;
    }

    public void call(String method, String url, JSONObject payload, JSONObject headers, final APICallback callback) {
        if (callback == null) return;

        if (url == null || url.isEmpty()) {
            callback.onError(new IllegalArgumentException("URL is null or empty"));
            return;
        }

        if (method == null || method.isEmpty()) {
            callback.onError(new IllegalArgumentException("method is null or empty"));
            return;
        }

        Request.Builder requestBuilder = new Request.Builder().url(url).method(method.toUpperCase(Locale.US), method.equalsIgnoreCase("GET") ? null : createRequestBody(payload));

        if (headers != null) {
            Iterator<String> keys = headers.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    String value = headers.getString(key);
                    requestBuilder.addHeader(key, value);
                } catch (Exception e) {
                    CometChatLogger.e(TAG, e.toString());
                }
            }
        }

        Request request = requestBuilder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                handler.post(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            callback.onSuccess(response.body().string());
                        } catch (Exception e) {
                            CometChatLogger.e(TAG, e.toString());
                        }
                    } else {
                        callback.onError(new IOException("Unexpected code " + response));
                    }
                });
            }
        });
    }

    private RequestBody createRequestBody(JSONObject payload) {
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        if (payload != null) {
            Iterator<String> keys = payload.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    String value = payload.getString(key);
                    formBodyBuilder.add(key, value);
                } catch (Exception e) {
                    CometChatLogger.e(TAG, e.toString());
                }
            }
        }
        return formBodyBuilder.build();
    }

    public interface APICallback {
        void onSuccess(String response);

        void onError(Exception e);
    }
}
