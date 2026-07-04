# StopClock

StopClock 是一個 Android 倒數計時 App，提供：

- 1 / 3 / 5 / 10 分鐘快速倒數
- 自訂分鐘數
- 倒數歸零時的鬧鐘提醒
- 重新開始與停止按鈕

## 開發環境

- Android Studio
- JDK 21
- Android SDK 36

## 開啟方式

1. 用 Android Studio 開啟此專案根目錄。
2. 等待 Gradle Sync 完成。
3. 執行 `app` 模組即可在模擬器或實機上測試。

## 建置

Debug：

```bash
./gradlew.bat assembleDebug
```

Release：

```bash
./gradlew.bat assembleRelease
```

Release APK 會同步輸出到：

`D:\github\stopClock\app\release`

## 版本

- `versionCode = 1`
- `versionName = 1`

