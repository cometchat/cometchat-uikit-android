package com.cometchat.sampleapp.java.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;
import android.view.View;

import com.cometchat.chatuikit.logger.CometChatLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ScreenshotHelper {
    private static final String TAG = ScreenshotHelper.class.getSimpleName();

    // Method to capture a screenshot
    public static File captureScreenshot(View view, Context context) {
        // Create a Bitmap from the view
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        // Save the bitmap to a file
        File screenshotFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "screenshot.png");
        try (FileOutputStream outputStream = new FileOutputStream(screenshotFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            return screenshotFile; // Return the file path of the screenshot
        } catch (IOException e) {
            CometChatLogger.e(TAG, e.toString());
            return null;
        }
    }
}
