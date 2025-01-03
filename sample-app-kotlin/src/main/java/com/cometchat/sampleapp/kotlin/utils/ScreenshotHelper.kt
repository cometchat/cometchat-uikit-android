package com.cometchat.sampleapp.kotlin.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Environment
import android.view.View
import com.cometchat.chatuikit.logger.CometChatLogger
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ScreenshotHelper {
    private val TAG: String = ScreenshotHelper::class.java.simpleName

    // Method to capture a screenshot
    fun captureScreenshot(view: View, context: Context): File? { // Create a Bitmap from the view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas) // Save the bitmap to a file
        val screenshotFile = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "screenshot.png")
        try {
            FileOutputStream(screenshotFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
                return screenshotFile // Return the file path of the screenshot
            }
        } catch (e: IOException) {
            CometChatLogger.e(TAG, e.toString())
            return null
        }
    }
}
