# ZenMap Android - Network Scanner Application

A native Android network scanning application inspired by Zenmap (the official Nmap GUI). This app performs TCP port scanning to discover open ports and services on network hosts.

## Features

- **TCP Connect Scan**: Scans common ports using TCP connection attempts
- **Service Detection**: Identifies common services running on open ports
- **Hostname Resolution**: Resolves hostnames to IP addresses
- **Real-time Results**: Displays scan results as they are discovered
- **Common Port Presets**: Scans the most commonly used ports (21, 22, 23, 25, 53, 80, 110, 143, 443, 993, 995, 3306, 3389, 5432, 8080)

## Requirements

- Android 5.0 (API level 21) or higher
- Internet permission
- Network state access permission
- Location permission (required for WiFi information on Android 6.0+)

## Permissions

The app requires the following Android permissions:

- `INTERNET`: Required for network scanning
- `ACCESS_NETWORK_STATE`: To check network connectivity
- `ACCESS_WIFI_STATE`: To get WiFi network information
- `CHANGE_WIFI_STATE`: To manage WiFi connections
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`: Required for WiFi scanning on Android 6.0+

## Building

### Prerequisites

- Android Studio Arctic Fox or later
- JDK 8 or later
- Android SDK with API level 34

### Build Steps

1. Open the project in Android Studio
2. Sync Gradle files
3. Build the project: `./gradlew assembleDebug`
4. Install on device or emulator

## Usage

1. Launch the app on your Android device
2. Enter a target IP address or hostname (e.g., `192.168.1.1` or `scanme.nmap.org`)
3. Tap "Start Scan"
4. View real-time results showing open ports and detected services
5. Review the summary of open ports found

## Technical Details

### Scanning Method

This app uses **TCP Connect Scan** (also known as full open scan), which:
- Completes the full TCP three-way handshake (SYN, SYN-ACK, ACK)
- Does not require root privileges
- Is slower than SYN scan but more reliable on Android
- Uses a 3-second timeout per port

### Limitations vs Desktop Zenmap/Nmap

1. **No ICMP Ping**: Android doesn't allow raw socket access without root, so host discovery uses TCP connections instead
2. **Slower Scanning**: Single-threaded nature and TCP Connect method is slower than Nmap's optimized scans
3. **Limited Port Range**: Scans only common ports by default (configurable in code)
4. **No OS Detection**: Fingerprinting requires raw packet access
5. **No Scripting Engine**: Nmap Scripting Engine (NSE) not available
6. **No Visual Topology**: Network graph visualization not implemented

### Threading Model

- Uses `ExecutorService` with a thread pool of 10 threads
- Scans run on background threads to prevent UI blocking
- Results are posted to the UI thread using `runOnUiThread()`

## Project Structure

```
zenmap-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/zenmapclone/
│   │   │   └── MainActivity.java          # Main scanner logic
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_main.xml      # UI layout
│   │   │   ├── values/
│   │   │   │   ├── strings.xml            # String resources
│   │   │   │   ├── colors.xml             # Color definitions
│   │   │   │   └── themes.xml             # App theme
│   │   │   └── drawable/                  # Images and icons
│   │   └── AndroidManifest.xml            # App permissions and config
│   ├── build.gradle                       # App-level build config
│   └── proguard-rules.pro                 # ProGuard rules
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties      # Gradle version config
├── build.gradle                           # Project-level build config
├── settings.gradle                        # Project settings
├── gradle.properties                      # Gradle properties
├── gradlew                                # Unix Gradle wrapper
└── gradlew.bat                            # Windows Gradle wrapper
```

## Legal Disclaimer

This tool is intended for **educational purposes** and **authorized network administration** only. 

- Only scan networks you own or have explicit permission to scan
- Unauthorized scanning may violate laws and terms of service
- The developers are not responsible for misuse of this application

## Future Enhancements

Potential improvements for future versions:

1. **Custom Port Ranges**: Allow users to specify custom port ranges
2. **Scan Profiles**: Save different scanning configurations
3. **Export Results**: Export scan results to XML, JSON, or text files
4. **Host Discovery**: Implement ARP scanning for local networks
5. **Service Version Detection**: Banner grabbing for service identification
6. **Dark Mode**: Add dark theme support
7. **Network Topology Map**: Visual representation of scanned networks
8. **Scheduled Scans**: Automatic periodic scanning
9. **Multiple Targets**: Scan multiple hosts simultaneously
10. **Nmap Integration**: Bundle actual Nmap binary for advanced features

## License

This project is provided as-is for educational purposes.

## Acknowledgments

- Inspired by [Nmap](https://nmap.org/) and its GUI frontend Zenmap
- Built with Android SDK and Material Design components
