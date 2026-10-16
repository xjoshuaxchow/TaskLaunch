package com.tasklaunch.app.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchdogPolicyTest {
    private val target = "com.example.target"
    private val self = "com.tasklaunch.app"

    @Test
    fun disabledClearsCountdown() {
        val decision = WatchdogPolicy.decide(
            enabled = false,
            targetPackage = target,
            delaySeconds = 30,
            foregroundPackage = "com.android.launcher",
            selfPackage = self,
            absentSinceMs = 1_000L,
            nowMs = 5_000L,
            cooldownUntilMs = 9_000L,
        )
        assertEquals(WatchPhase.Disabled, decision.phase)
        assertNull(decision.launchPackage)
        assertNull(decision.absentSinceMs)
    }

    @Test
    fun targetInForegroundResetsTimer() {
        val decision = WatchdogPolicy.decide(
            enabled = true,
            targetPackage = target,
            delaySeconds = 30,
            foregroundPackage = target,
            selfPackage = self,
            absentSinceMs = 1_000L,
            nowMs = 20_000L,
            cooldownUntilMs = 50_000L,
        )
        assertEquals(WatchPhase.Watching, decision.phase)
        assertNull(decision.absentSinceMs)
        assertEquals(0L, decision.cooldownUntilMs)
    }

    @Test
    fun ownAppPausesCountdown() {
        val decision = WatchdogPolicy.decide(
            enabled = true,
            targetPackage = target,
            delaySeconds = 30,
            foregroundPackage = self,
            selfPackage = self,
            absentSinceMs = 1_000L,
            nowMs = 40_000L,
            cooldownUntilMs = 0L,
        )
        assertEquals(WatchPhase.Watching, decision.phase)
        assertNull(decision.launchPackage)
    }

    @Test
    fun countdownStartsWhenTargetLeaves() {
        val decision = WatchdogPolicy.decide(
            enabled = true,
            targetPackage = target,
            delaySeconds = 30,
            foregroundPackage = "com.android.launcher",
            selfPackage = self,
            absentSinceMs = null,
            nowMs = 10_000L,
            cooldownUntilMs = 0L,
        )
        assertEquals(WatchPhase.Counting, decision.phase)
        assertEquals(10_000L, decision.absentSinceMs)
        assertEquals(30, decision.remainingSeconds)
        assertNull(decision.launchPackage)
    }

    @Test
    fun launchesAfterDelay() {
        val decision = WatchdogPolicy.decide(
            enabled = true,
            targetPackage = target,
            delaySeconds = 30,
            foregroundPackage = "com.android.launcher",
            selfPackage = self,
            absentSinceMs = 10_000L,
            nowMs = 40_000L,
            cooldownUntilMs = 0L,
        )
        assertEquals(target, decision.launchPackage)
        assertEquals(70_000L, decision.cooldownUntilMs)
    }

    @Test
    fun cooldownBlocksAnotherLaunch() {
        val decision = WatchdogPolicy.decide(
            enabled = true,
            targetPackage = target,
            delaySeconds = 30,
            foregroundPackage = "com.android.launcher",
            selfPackage = self,
            absentSinceMs = null,
            nowMs = 45_000L,
            cooldownUntilMs = 70_000L,
        )
        assertEquals(WatchPhase.Cooldown, decision.phase)
        assertNull(decision.launchPackage)
        assertEquals(25, decision.remainingSeconds)
    }

    @Test
    fun unknownForegroundDoesNotLaunch() {
        val decision = WatchdogPolicy.decide(
            enabled = true,
            targetPackage = target,
            delaySeconds = 5,
            foregroundPackage = null,
            selfPackage = self,
            absentSinceMs = 0L,
            nowMs = 60_000L,
            cooldownUntilMs = 0L,
        )
        assertEquals(WatchPhase.UnknownForeground, decision.phase)
        assertNull(decision.launchPackage)
    }
}
