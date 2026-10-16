package com.tasklaunch.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.tasklaunch.app.MainActivity
import com.tasklaunch.app.R
import com.tasklaunch.app.data.InstalledApps
import com.tasklaunch.app.data.LaunchSettings
import com.tasklaunch.app.data.Preferences
import com.tasklaunch.app.monitor.AppLauncher
import com.tasklaunch.app.monitor.ForegroundDetector
import com.tasklaunch.app.monitor.WatchPhase
import com.tasklaunch.app.monitor.WatchdogPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WatchdogService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val preferences by lazy { Preferences(this) }
    private val detector by lazy { ForegroundDetector(this) }
    private val overlay by lazy { LaunchOverlay(this) }
    private val apps by lazy { InstalledApps(this) }
    private var loop: Job? = null
    private var absentSinceMs: Long? = null
    private var cooldownUntilMs = 0L
    private var trackedPackage = ""
    private var lastOutcome: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            scope.launch {
                preferences.setEnabled(false)
                shutdown()
            }
            return START_NOT_STICKY
        }
        promoteToForeground(getString(R.string.app_name))
        WatchdogStatus.update(WatchStatus(running = true, message = "正在啟動"))
        overlay.show()
        ensureLoop()
        return START_STICKY
    }

    override fun onDestroy() {
        loop?.cancel()
        overlay.hide()
        WatchdogStatus.update(WatchStatus())
        scope.cancel()
        super.onDestroy()
    }

    private fun ensureLoop() {
        if (loop?.isActive == true) return
        loop = scope.launch {
            while (isActive) {
                tick()
                delay(POLL_MS)
            }
        }
    }

    private suspend fun tick() {
        val settings = preferences.current()
        if (!settings.enabled) {
            shutdown()
            return
        }
        if (settings.targetPackage != trackedPackage) {
            trackedPackage = settings.targetPackage
            absentSinceMs = null
            cooldownUntilMs = 0L
            lastOutcome = null
        }
        if (!ForegroundDetector.hasUsageAccess(this) || !Settings.canDrawOverlays(this)) {
            absentSinceMs = null
            val message = when {
                !ForegroundDetector.hasUsageAccess(this) -> "需要使用情況存取權限"
                else -> "需要顯示在其他應用程式上層"
            }
            publish(settings, null, WatchPhase.UnknownForeground, null, message)
            return
        }
        overlay.show()
        val now = System.currentTimeMillis()
        val foreground = withContext(Dispatchers.Default) { detector.poll(now) }
        val decision = WatchdogPolicy.decide(
            enabled = settings.enabled,
            targetPackage = settings.targetPackage,
            delaySeconds = settings.delaySeconds,
            foregroundPackage = foreground,
            selfPackage = packageName,
            absentSinceMs = absentSinceMs,
            nowMs = now,
            cooldownUntilMs = cooldownUntilMs,
        )
        absentSinceMs = decision.absentSinceMs
        cooldownUntilMs = decision.cooldownUntilMs
        var message: String? = null
        val launchPackage = decision.launchPackage
        if (launchPackage != null) {
            val started = AppLauncher.launch(this, launchPackage)
            message = if (started) {
                "已送出開啟要求"
            } else {
                "無法開啟，請確認該 App 仍已安裝"
            }
            lastOutcome = message
        } else if (decision.phase == WatchPhase.Watching) {
            lastOutcome = null
        } else if (message == null) {
            message = lastOutcome
        }
        publish(settings, foreground, decision.phase, decision.remainingSeconds, message)
    }

    private fun publish(
        settings: LaunchSettings,
        foreground: String?,
        phase: WatchPhase,
        remainingSeconds: Int?,
        message: String?,
    ) {
        val label = apps.labelOf(settings.targetPackage).ifBlank { "尚未選擇" }
        WatchdogStatus.update(
            WatchStatus(
                running = true,
                phase = phase,
                foregroundPackage = foreground,
                remainingSeconds = remainingSeconds,
                message = message,
            ),
        )
        val text = when (phase) {
            WatchPhase.Counting -> "$label 不在前景，${remainingSeconds ?: 0} 秒後重新開啟"
            WatchPhase.Cooldown -> message ?: "正在等待上一次開啟完成"
            WatchPhase.Watching -> if (foreground == packageName) {
                "TaskLaunch 使用中，倒數已暫停"
            } else {
                "$label 正在前景"
            }
            WatchPhase.UnknownForeground -> message ?: "無法判斷前景 App"
            WatchPhase.Disabled -> "已關閉"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun shutdown() {
        loop?.cancel()
        loop = null
        overlay.hide()
        WatchdogStatus.update(WatchStatus())
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun promoteToForeground(text: String) {
        ensureChannel()
        val notification = buildNotification(text)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, WatchdogService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_launch)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .addAction(0, getString(R.string.notification_stop), stop)
            .build()
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_STOP = "com.tasklaunch.app.STOP"
        private const val CHANNEL_ID = "watchdog"
        private const val NOTIFICATION_ID = 41
        private const val POLL_MS = 1_000L

        fun start(context: Context) {
            val intent = Intent(context, WatchdogService::class.java)
            context.startForegroundService(intent)
        }
    }
}
