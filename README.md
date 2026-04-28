## ZenMap Pro Ultra - Android Network Scanner

### Project Structure
```
app/
├── src/main/
│   ├── java/com/cyber/zenmappro/
│   │   ├── adapter/
│   │   │   └── HostAdapter.kt
│   │   ├── model/
│   │   │   └── Models.kt
│   │   ├── ui/
│   │   │   ├── MainActivity.kt
│   │   │   ├── ResultsActivity.kt
│   │   │   ├── TopologyActivity.kt
│   │   │   ├── ProfileManagerActivity.kt
│   │   │   └── TerminalTextView.kt
│   │   ├── utils/
│   │   │   ├── AppState.kt
│   │   │   ├── RootChecker.kt
│   │   │   ├── PermissionManager.kt
│   │   │   └── ScanEngine.kt
│   │   └── viewmodel/
│   │       └── ViewModels.kt
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml
│   │   │   ├── activity_results.xml
│   │   │   ├── activity_topology.xml
│   │   │   ├── activity_profile_manager.xml
│   │   │   └── item_host.xml
│   │   ├── values/
│   │   │   ├── colors.xml
│   │   │   ├── strings.xml
│   │   │   ├── styles.xml
│   │   │   └── attrs.xml
│   │   ├── drawable/
│   │   │   ├── scan_card_bg.xml
│   │   │   ├── hacker_card_bg.xml
│   │   │   ├── terminal_bg.xml
│   │   │   ├── header_bg.xml
│   │   │   ├── status_indicator_green.xml
│   │   │   └── ic_launcher_foreground.xml
│   │   ├── xml/
│   │   │   ├── backup_rules.xml
│   │   │   └── data_extraction_rules.xml
│   │   └── mipmap-*/
│   └── AndroidManifest.xml
├── build.gradle.kts
└── proguard-rules.pro

build.gradle.kts (project level)
settings.gradle.kts
```

### Key Features Implemented

1. **Cyberpunk/Hacker Theme**
   - Neon green (#39FF14) color scheme
   - Monospace fonts throughout
   - Terminal-style log output
   - Custom styled dialogs and buttons

2. **Root Detection**
   - Multi-method root checking
   - SU binary detection
   - Build tag analysis
   - Root command execution capability

3. **Network Scanning**
   - Ping sweep using InetAddress.isReachable()
   - TCP connect scan using java.net.Socket
   - SYN scan support (root required)
   - Port-to-service mapping

4. **UI Components**
   - Dashboard with scan cards
   - Real-time terminal log
   - Device info display (IP, MAC, Root status)
   - Results with MPAndroidChart visualization
   - Expandable host list with port details

5. **Permissions**
   - INTERNET, ACCESS_NETWORK_STATE
   - ACCESS_WIFI_STATE, CHANGE_WIFI_STATE
   - ACCESS_FINE_LOCATION (for WiFi scanning)

6. **MVVM Architecture**
   - ViewModels with StateFlow
   - Repository pattern for scan engine
   - LiveData for UI updates

### Build Instructions

1. Open project in Android Studio Arctic Fox or later
2. Sync Gradle files
3. Build → Build APK(s)

### Dependencies
- AndroidX Core KTX
- Lifecycle ViewModel & LiveData
- Kotlin Coroutines
- Material Design Components
- MPAndroidChart (via JitPack)
- Jsoup

### Notes
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- Requires JDK 17
- Some features require root access (clearly marked in UI)
