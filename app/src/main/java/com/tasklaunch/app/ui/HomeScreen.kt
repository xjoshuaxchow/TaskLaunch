package com.tasklaunch.app.ui

import android.widget.ImageView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.tasklaunch.app.data.LaunchableApp
import com.tasklaunch.app.data.Preferences
import com.tasklaunch.app.monitor.WatchPhase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    model: HomeViewModel,
    notificationsGranted: Boolean,
    usageGranted: Boolean,
    overlayGranted: Boolean,
    batteryIgnored: Boolean,
    onRequestNotifications: () -> Unit,
    onRequestUsage: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestBattery: () -> Unit,
) {
    val settings by model.settings.collectAsState()
    val status by model.status.collectAsState()
    val apps by model.apps.collectAsState()
    var picking by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    if (picking) {
        AppPicker(
            apps = apps,
            selected = settings.targetPackage,
            onBack = { picking = false },
            onSelect = { app ->
                model.selectApp(app.packageName)
                picking = false
            },
        )
        return
    }

    val ready = settings.targetPackage.isNotBlank() && notificationsGranted && usageGranted && overlayGranted
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TaskLaunch") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "目標 App 離開前景後開始倒數，時間到就重新開啟它。",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            item {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("自動重新開啟", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (settings.enabled) "功能已開啟" else "功能已關閉",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = settings.enabled,
                            onCheckedChange = { want ->
                                notice = when {
                                    want && settings.targetPackage.isBlank() -> "請先選擇要重新開啟的 App"
                                    want && !usageGranted -> "請先允許使用情況存取"
                                    want && !overlayGranted -> "請先允許顯示在其他應用程式上層"
                                    want && !notificationsGranted -> {
                                        onRequestNotifications()
                                        "請允許通知，前景服務才能持續執行"
                                    }
                                    else -> null
                                }
                                if (notice == null) model.setEnabled(want)
                            },
                        )
                    }
                }
            }
            item {
                SectionCard {
                    Text("目標 App", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (settings.targetPackage.isBlank()) {
                        Text(
                            "尚未選擇",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        AppRow(
                            packageName = settings.targetPackage,
                            label = model.labelOf(settings.targetPackage),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { picking = true }) { Text("選擇 App") }
                }
            }
            item {
                DelayField(
                    savedSeconds = settings.delaySeconds,
                    onValidSeconds = model::setDelaySeconds,
                )
            }
            item {
                SectionCard {
                    Text("目前狀態", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    val context = LocalContext.current
                    Text(
                        statusText(
                            phase = status.phase,
                            remainingSeconds = status.remainingSeconds,
                            message = status.message,
                            running = status.running,
                            foregroundPackage = status.foregroundPackage,
                            selfPackage = context.packageName,
                        ),
                    )
                    if (!status.foregroundPackage.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "前景：${model.labelOf(status.foregroundPackage!!)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            notice?.let { text ->
                item {
                    Text(text, color = MaterialTheme.colorScheme.error)
                }
            }
            item {
                Text("需要的權限", style = MaterialTheme.typography.titleMedium)
            }
            item {
                PermissionRow(
                    title = "使用情況存取",
                    detail = "讀取哪個 App 正在前景。這是系統的特殊權限。",
                    granted = usageGranted,
                    action = "前往設定",
                    onClick = onRequestUsage,
                )
            }
            item {
                PermissionRow(
                    title = "顯示在其他應用程式上層",
                    detail = "Android 不允許一般背景程式打開別的 App。TaskLaunch 會放一個小小的 TL 標記，讓系統把它視為可見視窗。",
                    granted = overlayGranted,
                    action = "前往設定",
                    onClick = onRequestOverlay,
                )
            }
            item {
                PermissionRow(
                    title = "通知",
                    detail = "長開的背景服務必須顯示一則持續通知。",
                    granted = notificationsGranted,
                    action = "允許",
                    onClick = onRequestNotifications,
                )
            }
            item {
                PermissionRow(
                    title = "不受電池最佳化限制",
                    detail = "建議開啟。否則螢幕關閉後，系統可能延後倒數。",
                    granted = batteryIgnored,
                    action = "允許",
                    onClick = onRequestBattery,
                )
            }
            item {
                Text(
                    if (ready) "權限已齊，可以開啟功能。" else "開啟功能前，請先選擇 App 並允許前三項權限。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 28.dp),
                )
            }
        }
    }
}

@Composable
private fun DelayField(savedSeconds: Int, onValidSeconds: (Int) -> Unit) {
    var text by remember(savedSeconds) { mutableStateOf(savedSeconds.toString()) }
    SectionCard {
        Text("倒數時間", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { raw ->
                val digits = raw.filter(Char::isDigit).take(4)
                text = digits
                val seconds = digits.toIntOrNull() ?: return@OutlinedTextField
                if (seconds in Preferences.MIN_DELAY..Preferences.MAX_DELAY) onValidSeconds(seconds)
            },
            label = { Text("秒") },
            supportingText = { Text("${Preferences.MIN_DELAY}–${Preferences.MAX_DELAY} 秒") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "你正在使用 TaskLaunch 時，倒數會暫停，避免設定被打斷。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPicker(
    apps: List<LaunchableApp>,
    selected: String,
    onBack: () -> Unit,
    onSelect: (LaunchableApp) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val shown = remember(apps, query) {
        val needle = query.trim()
        if (needle.isEmpty()) {
            apps
        } else {
            apps.filter {
                it.label.contains(needle, ignoreCase = true) ||
                    it.packageName.contains(needle, ignoreCase = true)
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("選擇 App") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("搜尋") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "清單來自已安裝、且有主畫面圖示的 App。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(shown, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(app) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(app.packageName)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                        ) {
                            Text(app.label, fontWeight = FontWeight.Medium)
                            Text(
                                app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (app.packageName == selected) {
                            Icon(Icons.Filled.Check, contentDescription = "已選擇")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(packageName: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIcon(packageName)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(label, fontWeight = FontWeight.Medium)
            Text(
                packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppIcon(packageName: String, size: Dp = 40.dp) {
    val context = LocalContext.current
    val drawable = remember(packageName) {
        runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
    }
    if (drawable == null) {
        Spacer(Modifier.size(size))
        return
    }
    AndroidView(
        modifier = Modifier.size(size),
        factory = { ImageView(it) },
        update = { it.setImageDrawable(drawable) },
    )
}

@Composable
private fun PermissionRow(
    title: String,
    detail: String,
    granted: Boolean,
    action: String,
    onClick: () -> Unit,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (granted) "已允許" else "尚未允許",
                    color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            if (!granted) {
                OutlinedButton(onClick = onClick) { Text(action) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

private fun statusText(
    phase: WatchPhase,
    remainingSeconds: Int?,
    message: String?,
    running: Boolean,
    foregroundPackage: String?,
    selfPackage: String,
): String {
    if (!running) return "服務未執行"
    return when (phase) {
        WatchPhase.Disabled -> "已關閉"
        WatchPhase.UnknownForeground -> message ?: "還不能判斷前景 App"
        WatchPhase.Watching -> if (foregroundPackage == selfPackage) {
            "你正在使用 TaskLaunch，倒數已暫停"
        } else {
            "目標正在前景"
        }
        WatchPhase.Counting -> "倒數 ${remainingSeconds ?: 0} 秒"
        WatchPhase.Cooldown -> message ?: "剛剛已嘗試開啟"
    }
}
