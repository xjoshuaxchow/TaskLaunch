package com.tasklaunch.app.monitor

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process

class ForegroundDetector(context: Context) {
    private val appContext = context.applicationContext
    private val usageStats = appContext.getSystemService(UsageStatsManager::class.java)
    private var cursorMs = System.currentTimeMillis() - LOOKBACK_MS
    private var currentPackage: String? = null
    private var seeded = false

    @Suppress("DEPRECATION")
    fun poll(nowMs: Long = System.currentTimeMillis()): String? {
        if (!hasUsageAccess(appContext)) {
            currentPackage = null
            seeded = false
            return null
        }
        if (!seeded) {
            seedFromDailyStats(nowMs)
            seeded = true
        }
        val events = usageStats.queryEvents(cursorMs, nowMs)
        val event = UsageEvents.Event()
        var latest = cursorMs
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.timeStamp > latest) latest = event.timeStamp
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                currentPackage = event.packageName
            }
        }
        cursorMs = latest
        return currentPackage
    }

    private fun seedFromDailyStats(nowMs: Long) {
        val stats = usageStats.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            nowMs - LOOKBACK_MS,
            nowMs,
        )
        currentPackage = stats.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    companion object {
        private const val LOOKBACK_MS = 24L * 60L * 60L * 1000L

        fun hasUsageAccess(context: Context): Boolean {
            val appOps = context.getSystemService(AppOpsManager::class.java)
            val mode = appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
            return mode == AppOpsManager.MODE_ALLOWED
        }
    }
}
