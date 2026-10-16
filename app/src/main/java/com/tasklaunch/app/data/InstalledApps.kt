package com.tasklaunch.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

data class LaunchableApp(
    val packageName: String,
    val label: String,
)

class InstalledApps(private val context: Context) {
    fun list(): List<LaunchableApp> {
        val pm = context.packageManager
        val probe = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(probe, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(probe, 0)
        }
        return resolved
            .mapNotNull { info ->
                val packageName = info.activityInfo?.packageName ?: return@mapNotNull null
                if (packageName == context.packageName) return@mapNotNull null
                val label = info.loadLabel(pm)?.toString()?.trim().orEmpty().ifBlank { packageName }
                LaunchableApp(packageName = packageName, label = label)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun labelOf(packageName: String): String {
        if (packageName.isBlank()) return ""
        return runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
    }
}
