package com.cyber.zenmappro.model

/**
 * Represents a scanned host on the network
 */
data class HostObject(
    val ip: String,
    val mac: String = "Unknown",
    val hostname: String = "",
    val openPorts: List<PortInfo> = emptyList(),
    val osGuess: String = "Unknown",
    val latency: Long = 0L,
    val isAlive: Boolean = true,
    val vendor: String = "Unknown",
    val services: List<ServiceInfo> = emptyList()
) {
    val portCount: Int get() = openPorts.size
    
    fun hasOpenPorts(): Boolean = openPorts.isNotEmpty()
}

/**
 * Represents an open port with service information
 */
data class PortInfo(
    val port: Int,
    val protocol: String = "tcp",
    val state: String = "open",
    val serviceName: String = "unknown",
    val version: String = "",
    val banner: String = ""
)

/**
 * Represents a service running on a port
 */
data class ServiceInfo(
    val name: String,
    val version: String = "",
    val product: String = "",
    val extraInfo: String = "",
    val method: String = "",
    val confidence: Int = 0
)

/**
 * Scan profile for saving/loading scan configurations
 */
data class ScanProfile(
    val id: String,
    val name: String,
    val description: String = "",
    val scanType: ScanType = ScanType.QUICK,
    val ports: String = "",
    val timing: String = "T3",
    val osDetection: Boolean = false,
    val versionDetection: Boolean = false,
    val scriptScan: Boolean = false,
    val aggressiveScan: Boolean = false,
    val firewallEvasion: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Types of scans available
 */
enum class ScanType {
    QUICK,
    INTENSE,
    PING,
    PORT,
    OS,
    VULN,
    CUSTOM
}

/**
 * Result of a network scan
 */
data class ScanResult(
    val scanId: String = java.util.UUID.randomUUID().toString(),
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = 0L,
    val hosts: List<HostObject> = emptyList(),
    val scanType: ScanType = ScanType.QUICK,
    val targetSubnet: String = "",
    val totalHostsScanned: Int = 0,
    val hostsUp: Int = 0,
    val hostsDown: Int = 0,
    val totalPortsScanned: Int = 0,
    val openPortsFound: Int = 0,
    val errors: List<String> = emptyList(),
    val commandUsed: String = ""
) {
    val duration: Long get() = if (endTime > 0) endTime - startTime else 0L
    val completionPercentage: Float get() = 
        if (totalHostsScanned > 0) hosts.size.toFloat() / totalHostsScanned else 0f
}

/**
 * Terminal log entry
 */
data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val message: String,
    val tag: String = "ZenMap"
) {
    fun getFormattedTime(): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
        return format.format(date)
    }
    
    fun getColoredPrefix(): String {
        return when (level) {
            LogLevel.ERROR -> "[ERR]"
            LogLevel.WARNING -> "[WRN]"
            LogLevel.SUCCESS -> "[OK]"
            LogLevel.DEBUG -> "[DBG]"
            LogLevel.INFO -> "[INF]"
        }
    }
}

/**
 * Log levels for terminal output
 */
enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    SUCCESS
}

/**
 * Network interface information
 */
data class NetworkInterfaceInfo(
    val name: String,
    val ipAddress: String,
    val macAddress: String,
    val broadcastAddress: String = "",
    val netmask: String = "",
    val isUp: Boolean = false,
    val isLoopback: Boolean = false,
    val mtu: Int = 1500
)
