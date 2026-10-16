package com.tasklaunch.app.monitor

import android.content.Context
import android.content.Intent
import android.util.Log

object AppLauncher {
    private const val TAG = "TaskLaunch"

    fun launch(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            Log.w(TAG, "No launch intent for $packageName")
            return false
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return try {
            context.startActivity(intent)
            true
        } catch (error: RuntimeException) {
            Log.e(TAG, "Failed to launch $packageName", error)
            false
        }
    }
}
