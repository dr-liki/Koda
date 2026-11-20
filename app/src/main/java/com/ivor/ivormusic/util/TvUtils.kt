package com.ivor.ivormusic.util

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

object TvUtils {
    /**
     * Determines whether the current device is an Android TV / Google TV / Fire TV device.
     */
    fun isTvDevice(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) return true
        val pm = context.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
               pm.hasSystemFeature("amazon.hardware.fire_tv") ||
               !pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
    }
}
