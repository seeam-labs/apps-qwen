package com.cyber.zenmappro.utils

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import com.cyber.zenmappro.model.HostObject
import com.cyber.zenmappro.model.LogLevel
import com.cyber.zenmappro.model.NetworkInterfaceInfo
import com.cyber.zenmappro.model.PortInfo
import com.cyber.zenmappro.model.ScanResult
import com.cyber.zenmappro.model.ScanType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.TimeUnit

/**
 * Core scanning engine for network operations
 */
object ScanEngine {
    
    private const val TAG = "ScanEngine"
    private const val DEFAULT_TIMEOUT_MS = 1000
    private const val PING_TIMEOUT_MS = 2000
    
    // Common ports to scan in quick mode
    private val COMMON_PORTS = listOf(
        21, 22, 23, 25, 53, 80, 110, 111, 135, 139, 143, 443, 445, 993, 995, 
        1723, 3306, 3389, 5900, 8080, 8443
    )
    
    // Well-known port to service mapping
    private val PORT_SERVICES = mapOf(
        21 to "FTP", 22 to "SSH", 23 to "Telnet", 25 to "SMTP", 53 to "DNS",
        80 to "HTTP", 110 to "POP3", 111 to "RPC", 135 to "MSRPC", 139 to "NetBIOS",
        143 to "IMAP", 443 to "HTTPS", 445 to "SMB", 993 to "IMAPS", 995 to "POP3S",
        1723 to "PPTP", 3306 to "MySQL", 3389 to "RDP", 5900 to "VNC", 
        8080 to "HTTP-Proxy", 8443 to "HTTPS-Alt"
    )
    
    /**
     * Perform a ping sweep on the subnet
     */
    suspend fun pingSweep(subnet: String): List<HostObject> = withContext(Dispatchers.IO) {
        AppState.addLog("Starting ping sweep on $subnet", LogLevel.INFO, TAG)
        val hosts = mutableListOf<HostObject>()
        
        try {
            val baseIp = subnet.substringBeforeLast('.')
            
            // Scan IPs from .1 to .254
            (1..254).forEach { i ->
                if (Thread.currentThread().isInterrupted) return@withContext hosts
                
                val ip = "$baseIp.$i"
                val isReachable = pingHost(ip, PING_TIMEOUT_MS)
                
                if (isReachable) {
                    val latency = measureLatency(ip)
                    val host = HostObject(
                        ip = ip,
                        isAlive = true,
                        latency = latency
                    )
                    hosts.add(host)
                    AppState.addLog("Host found: $ip (${latency}ms)", LogLevel.SUCCESS, TAG)
                }
            }
            
            AppState.addLog("Ping sweep complete. Found ${hosts.size} hosts", LogLevel.SUCCESS, TAG)
        } catch (e: Exception) {
            AppState.addLog("Ping sweep error: ${e.message}", LogLevel.ERROR, TAG)
        }
        
        hosts
    }
    
    /**
     * Ping a single host
     */
    private fun pingHost(ip: String, timeoutMs: Int): Boolean {
        return try {
            val address = InetAddress.getByName(ip)
            address.isReachable(timeoutMs)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Measure latency to a host
     */
    private suspend fun measureLatency(ip: String): Long = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            val address = InetAddress.getByName(ip)
            val reachable = address.isReachable(PING_TIMEOUT_MS)
            val endTime = System.currentTimeMillis()
            
            if (reachable) {
                endTime - startTime
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Perform TCP connect scan on a host
     */
    suspend fun tcpConnectScan(ip: String, ports: List<Int> = COMMON_PORTS): List<PortInfo> = 
        withContext(Dispatchers.IO) {
            AppState.addLog("Starting TCP connect scan on $ip", LogLevel.INFO, TAG)
            val openPorts = mutableListOf<PortInfo>()
            
            ports.forEach { port ->
                if (Thread.currentThread().isInterrupted) return@withContext openPorts
                
                try {
                    val socket = Socket()
                    socket.connect(
                        java.net.InetSocketAddress(ip, port),
                        DEFAULT_TIMEOUT_MS
                    )
                    socket.close()
                    
                    val serviceName = PORT_SERVICES[port] ?: "unknown"
                    val portInfo = PortInfo(
                        port = port,
                        protocol = "tcp",
                        state = "open",
                        serviceName = serviceName
                    )
                    openPorts.add(portInfo)
                    AppState.addLog("Port $port/$protocol open ($serviceName)", LogLevel.INFO, TAG)
                } catch (e: Exception) {
                    // Port is closed or filtered
                }
            }
            
            AppState.addLog("TCP scan complete. Found ${openPorts.size} open ports", LogLevel.SUCCESS, TAG)
            openPorts
        }
    
    /**
     * Perform a full network scan (ping + port scan)
     */
    suspend fun quickScan(subnet: String): ScanResult = withContext(Dispatchers.IO) {
        AppState.addLog("Starting quick scan on $subnet", LogLevel.INFO, TAG)
        val startTime = System.currentTimeMillis()
        val hosts = pingSweep(subnet)
        
        // Scan ports on discovered hosts
        val scannedHosts = hosts.map { host ->
            if (Thread.currentThread().isInterrupted) return@withContext hosts
            
            val openPorts = tcpConnectScan(host.ip)
            host.copy(openPorts = openPorts)
        }
        
        val totalOpenPorts = scannedHosts.sumOf { it.openPorts.size }
        
        ScanResult(
            startTime = startTime,
            endTime = System.currentTimeMillis(),
            hosts = scannedHosts,
            scanType = ScanType.QUICK,
            targetSubnet = subnet,
            totalHostsScanned = 254,
            hostsUp = scannedHosts.size,
            hostsDown = 254 - scannedHosts.size,
            openPortsFound = totalOpenPorts,
            commandUsed = "quick_scan $subnet"
        )
    }
    
    /**
     * Perform SYN scan (requires root)
     */
    suspend fun synScan(ip: String, ports: List<Int>): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!RootChecker.isDeviceRooted()) {
            AppState.addLog("SYN scan requires root access", LogLevel.ERROR, TAG)
            return@withContext Pair(false, "ROOT_REQUIRED")
        }
        
        try {
            // Attempt to use nmap if available
            if (RootChecker.isNmapAvailable()) {
                val portList = ports.joinToString(",")
                val command = "nmap -sS -p $portList $ip"
                val result = executeShellCommand(command)
                return@withContext Pair(true, result)
            } else {
                // Fallback: attempt raw socket (may not work on all devices)
                AppState.addLog("Nmap not found, attempting raw socket scan", LogLevel.WARNING, TAG)
                val result = attemptRawSocketScan(ip, ports)
                return@withContext Pair(true, result)
            }
        } catch (e: Exception) {
            AppState.addLog("SYN scan failed: ${e.message}", LogLevel.ERROR, TAG)
            return@withContext Pair(false, e.message ?: "Unknown error")
        }
    }
    
    /**
     * Execute shell command
     */
    private fun executeShellCommand(command: String): String {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            process.waitFor()
            output.toString().trim()
        } catch (e: Exception) {
            "Error: ${e.message}"
        } finally {
            process?.destroy()
        }
    }
    
    /**
     * Attempt raw socket scan (fallback for rooted devices without nmap)
     */
    private suspend fun attemptRawSocketScan(ip: String, ports: List<Int>): String = 
        withContext(Dispatchers.IO) {
            val results = StringBuilder()
            results.appendLine("Raw socket scan results for $ip:")
            results.appendLine("-".repeat(40))
            
            ports.forEach { port ->
                try {
                    // This is a simplified version - real SYN scan requires raw sockets
                    val socket = Socket()
                    socket.soTimeout = DEFAULT_TIMEOUT_MS
                    socket.connect(java.net.InetSocketAddress(ip, port), DEFAULT_TIMEOUT_MS)
                    socket.close()
                    results.appendLine("Port $port/tcp: OPEN")
                } catch (e: Exception) {
                    results.appendLine("Port $port/tcp: CLOSED/FILTERED")
                }
            }
            
            results.toString()
        }
    
    /**
     * Get device network information
     */
    fun getNetworkDeviceInfo(context: Context): NetworkInterfaceInfo {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            
            // Get IP address
            val ipInt = wifiInfo.ipAddress
            val ipAddress = if (ipInt == 0) {
                // WiFi not connected, try mobile network
                getMobileIpAddress()
            } else {
                String.format(
                    "%d.%d.%d.%d",
                    ipInt and 0xFF,
                    (ipInt shr 8) and 0xFF,
                    (ipInt shr 16) and 0xFF,
                    (ipInt shr 24) and 0xFF
                )
            }
            
            // Get MAC address
            val macAddress = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                wifiInfo.macAddress ?: "Unknown"
            } else {
                @Suppress("DEPRECATION")
                wifiInfo.macAddress ?: getMacFromHardware()
            }
            
            val info = NetworkInterfaceInfo(
                name = "wlan0",
                ipAddress = ipAddress,
                macAddress = macAddress,
                isUp = wifiInfo.networkId != -1
            )
            
            AppState.setDeviceInfo(info.ipAddress, info.macAddress, info.name)
            AppState.addLog("Device IP: $ipAddress, MAC: $macAddress", LogLevel.INFO, TAG)
            
            return info
        } catch (e: Exception) {
            AppState.addLog("Error getting network info: ${e.message}", LogLevel.ERROR, TAG)
            return NetworkInterfaceInfo(
                name = "unknown",
                ipAddress = "Unknown",
                macAddress = "Unknown"
            )
        }
    }
    
    /**
     * Get IP address from mobile network
     */
    private fun getMobileIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addresses = intf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress ?: "Unknown"
                    }
                }
            }
        } catch (e: Exception) {
            AppState.addLog("Error getting mobile IP: ${e.message}", LogLevel.ERROR, TAG)
        }
        return "Unknown"
    }
    
    /**
     * Get MAC address from hardware (fallback for older devices)
     */
    private fun getMacFromHardware(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (intf.name.equals("wlan0", ignoreCase = true)) {
                    val macBytes = intf.hardwareAddress ?: return "Unknown"
                    return macBytes.joinToString(":") { "%02X".format(it) }
                }
            }
        } catch (e: Exception) {
            AppState.addLog("Error getting MAC from hardware: ${e.message}", LogLevel.ERROR, TAG)
        }
        return "Unknown"
    }
    
    /**
     * Get subnet from current IP address
     */
    fun getCurrentSubnet(ipAddress: String): String {
        return if (ipAddress != "Unknown" && ipAddress.isNotEmpty()) {
            ipAddress.substringBeforeLast('.') + ".0/24"
        } else {
            "192.168.1.0/24" // Default fallback
        }
    }
    
    /**
     * Stop any ongoing scans
     */
    fun stopScan() {
        AppState.scanInProgress = false
        AppState.addLog("Scan stopped by user", LogLevel.WARNING, TAG)
    }
}
