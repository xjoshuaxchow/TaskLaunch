package com.tasklaunch.app.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tasklaunch.app.data.InstalledApps
import com.tasklaunch.app.data.LaunchSettings
import com.tasklaunch.app.data.LaunchableApp
import com.tasklaunch.app.data.Preferences
import com.tasklaunch.app.service.WatchdogService
import com.tasklaunch.app.service.WatchdogStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val preferences = Preferences(app)
    private val installedApps = InstalledApps(app)

    val settings: StateFlow<LaunchSettings> = preferences.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        LaunchSettings(),
    )

    val status = WatchdogStatus.state

    var apps: StateFlow<List<LaunchableApp>>
        private set

    private val appsFlow = kotlinx.coroutines.flow.MutableStateFlow<List<LaunchableApp>>(emptyList())

    init {
        apps = appsFlow
        refreshApps()
    }

    fun refreshApps() {
        viewModelScope.launch(Dispatchers.Default) {
            appsFlow.value = installedApps.list()
        }
    }

    fun labelOf(packageName: String): String = installedApps.labelOf(packageName)

    fun selectApp(packageName: String) {
        viewModelScope.launch { preferences.setTargetPackage(packageName) }
    }

    fun setDelaySeconds(seconds: Int) {
        viewModelScope.launch { preferences.setDelaySeconds(seconds) }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setEnabled(enabled)
            val context = getApplication<Application>()
            if (enabled) {
                WatchdogService.start(context)
            } else {
                context.stopService(Intent(context, WatchdogService::class.java))
            }
        }
    }
}
