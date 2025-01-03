package com.cometchat.chatuikit.shared.resources.utils;

import android.content.Context;
import android.graphics.Typeface;

public class FontUtils {
    private static final String TAG = FontUtils.class.getSimpleName();


    private static FontUtils _instance;

    public static FontUtils getInstance(Context context) {
        return fontUtils();
    }

    public static synchronized FontUtils getInstance() {
        return fontUtils();
    }

    public static FontUtils fontUtils() {
        if (_instance == null) {
            _instance = new FontUtils();
        }
        return _instance;
    }

    private FontUtils() {
    }

    public Typeface getTypeFace(String fontName, Context context) {
        Typeface typeface = null;
        if (context != null) typeface = Typeface.createFromAsset(context.getAssets(), fontName);
        return typeface;
    }
}
