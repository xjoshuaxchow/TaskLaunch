package com.tasklaunch.app.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

/**
 * Android only allows an app with a visible window to start another activity
 * from the background. This badge is that window: small, not focusable, and
 * it does not receive touches.
 */
class LaunchOverlay(context: Context) {
    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private var badge: TextView? = null

    fun show() {
        if (badge != null) return
        if (!Settings.canDrawOverlays(appContext)) return
        val view = TextView(appContext).apply {
            text = "TL"
            setTextColor(0xFF123C3A.toInt())
            textSize = 11f
            gravity = Gravity.CENTER
            setBackgroundColor(0xCCF4C95D.toInt())
            setPadding(dp(6), dp(2), dp(6), dp(2))
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(8)
            y = dp(28)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        try {
            windowManager.addView(view, params)
            badge = view
        } catch (error: RuntimeException) {
            Log.e(TAG, "Overlay was not added", error)
        }
    }

    fun hide() {
        val view = badge ?: return
        runCatching { windowManager.removeView(view) }
        badge = null
    }

    private fun dp(value: Int): Int {
        return (value * appContext.resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val TAG = "TaskLaunch"
    }
}
