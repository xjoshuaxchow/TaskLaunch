package com.tasklaunch.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tasklaunch.app.ui.HomeScreen
import com.tasklaunch.app.ui.HomeViewModel
import com.tasklaunch.app.ui.TaskLaunchTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val model by viewModels<HomeViewModel>()
    private var notificationsGranted by mutableStateOf(false)
    private var usageGranted by mutableStateOf(false)
    private var overlayGranted by mutableStateOf(false)
    private var batteryIgnored by mutableStateOf(false)

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsGranted = granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshPermissionFlags()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                refreshPermissionFlags()
            }
        }
        setContent {
            TaskLaunchTheme {
                HomeScreen(
                    model = model,
                    notificationsGranted = notificationsGranted,
                    usageGranted = usageGranted,
                    overlayGranted = overlayGranted,
                    batteryIgnored = batteryIgnored,
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onRequestUsage = {
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                    onRequestOverlay = {
                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName"),
                            ),
                        )
                    },
                    onRequestBattery = {
                        startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:$packageName"),
                            ),
                        )
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionFlags()
        model.refreshApps()
    }

    private fun refreshPermissionFlags() {
        notificationsGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        usageGranted = com.tasklaunch.app.monitor.ForegroundDetector.hasUsageAccess(this)
        overlayGranted = Settings.canDrawOverlays(this)
        val power = getSystemService(PowerManager::class.java)
        batteryIgnored = power.isIgnoringBatteryOptimizations(packageName)
    }
}
