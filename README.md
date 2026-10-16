# TaskLaunch

Android 原生 App。選定一個已安裝的 App 後，它離開前景就開始倒數，時間到就重新開啟。倒數秒數、目標 App 和功能開關都可以改。

最低 Android 8（API 26），目標 Android 15（API 35）。

## 行為

長開的是前景服務 `WatchdogService`。通知欄會留著一則持續通知，可從通知直接關閉功能。

每秒檢查一次目前的前景 App：

- 目標 App 在前景：不倒數。
- TaskLaunch 自己在前景：倒數暫停，方便改設定。
- 其他情況：開始倒數。目標 App 回到前景會取消倒數。
- 時間到：用該 App 的啟動入口重新開啟。若這次沒開成，會再等一個完整倒數，不會連續重試。

倒數範圍是 5–3600 秒，預設 30 秒。清單只包含有主畫面圖示、可以正常開啟的已安裝 App。

手機重新開機後，若功能是開著且權限還在，服務會自己起來。

## 權限

開啟功能前需要這三項：

| 權限 | 原因 |
| --- | --- |
| 使用情況存取 | 判斷哪個 App 正在前景 |
| 顯示在其他應用程式上層 | Android 不允許普通背景程式打開別的 App。TaskLaunch 會放一個不接收觸控的小 **TL** 標記，讓系統把它視為仍有可見視窗 |
| 通知 | 前景服務必須顯示持續通知 |

建議再允許「不受電池最佳化限制」。否則螢幕關閉後，倒數可能被延後。Honor、華為等機型還要在「電池 / 啟動管理」允許 TaskLaunch 自動啟動並在背景執行。

## 建置

需要 JDK 17 與 Android SDK。在專案根目錄的 `local.properties` 設定 SDK 路徑，例如：

```
sdk.dir=/Users/you/Library/Android/sdk
```

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Debug APK：`app/build/outputs/apk/debug/app-debug.apk`

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

用 Android Studio 打開專案根目錄也可以直接安裝。

## 專案

- `app/src/main/java/com/tasklaunch/app/ui`：設定畫面
- `app/src/main/java/com/tasklaunch/app/service`：前景服務、開機啟動、TL 標記
- `app/src/main/java/com/tasklaunch/app/monitor`：前景判斷、倒數決策、開啟 App
- `app/src/test`：倒數決策的單元測試
