package com.ivor.ivormusic.ui.tv

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * True on Android TV / Google TV / Fire TV devices driven by a remote rather than touch.
 * Modeled after OpenStream's TvDetect.
 */
object TvDetect {
    fun isTv(context: Context): Boolean {
        val pm = context.packageManager
        if (pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) return true
        if (pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)) return true
        if (pm.hasSystemFeature("amazon.hardware.fire_tv")) return true
        val uiMode = (context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)
            ?.currentModeType ?: Configuration.UI_MODE_TYPE_UNDEFINED
        if (uiMode == Configuration.UI_MODE_TYPE_TELEVISION) return true
        // Fallback: non-touchscreen landscape display
        return !pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
    }

    /** Honor the system animator scale: no focus zoom when the user disabled animation. */
    fun animationsEnabled(context: Context): Boolean = runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) != 0f
    }.getOrDefault(true)
}

/** Remembers whether this device is a TV; TV mode never changes at runtime. */
@Composable
fun rememberTvMode(): Boolean {
    val context = LocalContext.current
    return remember { TvDetect.isTv(context) }
}

@Composable
fun rememberTvAnimated(): Boolean {
    val context = LocalContext.current
    return remember { TvDetect.animationsEnabled(context) }
}
