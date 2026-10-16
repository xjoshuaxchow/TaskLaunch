package com.tasklaunch.app.service

import com.tasklaunch.app.monitor.WatchPhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WatchStatus(
    val running: Boolean = false,
    val phase: WatchPhase = WatchPhase.Disabled,
    val foregroundPackage: String? = null,
    val remainingSeconds: Int? = null,
    val message: String? = null,
)

object WatchdogStatus {
    private val mutable = MutableStateFlow(WatchStatus())
    val state: StateFlow<WatchStatus> = mutable.asStateFlow()

    fun update(status: WatchStatus) {
        mutable.value = status
    }
}
