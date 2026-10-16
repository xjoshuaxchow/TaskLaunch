package com.tasklaunch.app.monitor

enum class WatchPhase {
    Disabled,
    UnknownForeground,
    Watching,
    Counting,
    Cooldown,
}

data class Decision(
    val absentSinceMs: Long?,
    val cooldownUntilMs: Long,
    val remainingSeconds: Int?,
    val launchPackage: String?,
    val phase: WatchPhase,
)

object WatchdogPolicy {
    const val MIN_DELAY_SECONDS = 5
    const val MAX_DELAY_SECONDS = 3600
    private const val MIN_COOLDOWN_MS = 10_000L

    fun decide(
        enabled: Boolean,
        targetPackage: String,
        delaySeconds: Int,
        foregroundPackage: String?,
        selfPackage: String,
        absentSinceMs: Long?,
        nowMs: Long,
        cooldownUntilMs: Long,
    ): Decision {
        if (!enabled || targetPackage.isBlank()) {
            return Decision(
                absentSinceMs = null,
                cooldownUntilMs = 0L,
                remainingSeconds = null,
                launchPackage = null,
                phase = WatchPhase.Disabled,
            )
        }
        if (foregroundPackage == null) {
            return Decision(
                absentSinceMs = null,
                cooldownUntilMs = cooldownUntilMs,
                remainingSeconds = null,
                launchPackage = null,
                phase = WatchPhase.UnknownForeground,
            )
        }
        if (foregroundPackage == targetPackage || foregroundPackage == selfPackage) {
            return Decision(
                absentSinceMs = null,
                cooldownUntilMs = 0L,
                remainingSeconds = null,
                launchPackage = null,
                phase = WatchPhase.Watching,
            )
        }
        if (nowMs < cooldownUntilMs) {
            val remaining = secondsUntil(cooldownUntilMs, nowMs)
            return Decision(
                absentSinceMs = absentSinceMs,
                cooldownUntilMs = cooldownUntilMs,
                remainingSeconds = remaining,
                launchPackage = null,
                phase = WatchPhase.Cooldown,
            )
        }
        val since = absentSinceMs ?: nowMs
        val delayMs = delaySeconds.coerceIn(MIN_DELAY_SECONDS, MAX_DELAY_SECONDS) * 1000L
        val elapsed = nowMs - since
        if (elapsed >= delayMs) {
            return Decision(
                absentSinceMs = null,
                cooldownUntilMs = nowMs + delayMs.coerceAtLeast(MIN_COOLDOWN_MS),
                remainingSeconds = 0,
                launchPackage = targetPackage,
                phase = WatchPhase.Counting,
            )
        }
        return Decision(
            absentSinceMs = since,
            cooldownUntilMs = cooldownUntilMs,
            remainingSeconds = secondsUntil(since + delayMs, nowMs),
            launchPackage = null,
            phase = WatchPhase.Counting,
        )
    }

    private fun secondsUntil(deadlineMs: Long, nowMs: Long): Int {
        val remainingMs = (deadlineMs - nowMs).coerceAtLeast(0L)
        return ((remainingMs + 999L) / 1000L).toInt()
    }
}
