package com.tasklaunch.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.tasklaunch.app.data.Preferences
import com.tasklaunch.app.monitor.ForegroundDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val settings = Preferences(context).current()
                val ready = settings.enabled &&
                    settings.targetPackage.isNotBlank() &&
                    ForegroundDetector.hasUsageAccess(context) &&
                    Settings.canDrawOverlays(context)
                if (ready) WatchdogService.start(context)
            } finally {
                pending.finish()
            }
        }
    }
}
