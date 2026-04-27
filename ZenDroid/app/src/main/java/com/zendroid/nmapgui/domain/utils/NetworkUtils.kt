package com.zendroid.nmapgui.domain.utils

import android.content.Context
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.net.NetworkInterface

class NetworkUtils(private val context: Context) {

    data class NetworkInfo(
        val ipAddress: String?,
        val subnetMask: String?,
        val gateway: String?,
        val ssid: String?,
        val bssid: String?,
        val isWifiConnected: Boolean
    )

    fun getLocalNetworkInfo(): NetworkInfo {
        var ipAddress: String? = null
        var subnetMask: String? = null
        var gateway: String? = null
        var ssid: String? = null
        var bssid: String? = null
        var isWifiConnected = false

        try {
            // Get IP address from network interfaces
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address.hostAddress?.contains(":") != true) {
                        ipAddress = address.hostAddress
                        break
                    }
                }
                if (ipAddress != null) break
            }

            // Get WiFi info
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null && wifiManager.isWifiEnabled) {
                val wifiInfo = wifiManager.connectionInfo
                ssid = wifiInfo.ssid?.removeSurrounding("\"")
                bssid = wifiInfo.bssid
                
                if (wifiInfo.networkId != -1) {
                    isWifiConnected = true
                    
                    // Calculate subnet mask and gateway from IP
                    ipAddress?.let { ip ->
                        val ipInt = ip.split(".").map { it.toInt() }.fold(0) { acc, i -> (acc shl 8) + i }
                        val maskInt = wifiInfo.ipAddress and wifiInfo.suppliedIp?.subnetPrefixLength?.let { prefix ->
                            (-1 shl (32 - prefix))
                        } ?: wifiManager.dhcpInfo?.netmask ?: 0xFFFFFF00.toInt()
                        
                        subnetMask = "${(maskInt ushr 24) and 0xFF}.${(maskInt ushr 16) and 0xFF}.${(maskInt ushr 8) and 0xFF}.${maskInt and 0xFF}"
                        
                        val gatewayInt = wifiManager.dhcpInfo?.gateway ?: 0
                        if (gatewayInt != 0) {
                            gateway = "${(gatewayInt ushr 24) and 0xFF}.${(gatewayInt ushr 16) and 0xFF}.${(gatewayInt ushr 8) and 0xFF}.${gatewayInt and 0xFF}"
                        } else {
                            // Fallback: assume gateway is .1
                            val parts = ip.split(".")
                            gateway = "${parts[0]}.${parts[1]}.${parts[2]}.1"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return NetworkInfo(ipAddress, subnetMask, gateway, ssid, bssid, isWifiConnected)
    }

    fun calculateCidrFromSubnet(subnetMask: String): Int {
        try {
            val parts = subnetMask.split(".").map { it.toInt() }
            var cidr = 0
            for (part in parts) {
                var bit = part
                while (bit != 0) {
                    cidr += bit and 1
                    bit = bit ushr 1
                }
            }
            return cidr
        } catch (e: Exception) {
            return 24 // Default fallback
        }
    }

    fun getDefaultTarget(): String {
        val networkInfo = getLocalNetworkInfo()
        return networkInfo.gateway ?: networkInfo.ipAddress?.let { ip ->
            val parts = ip.split(".")
            "${parts[0]}.${parts[1]}.${parts[2]}.0/24"
        } ?: "192.168.1.0/24"
    }

    fun isValidIpAddress(ip: String): Boolean {
        val pattern = """^(\d{1,3}\.){3}\d{1,3}$""".toRegex()
        if (!pattern.matches(ip)) return false
        
        val parts = ip.split(".").map { it.toIntOrNull() ?: return false }
        return parts.all { it in 0..255 }
    }

    fun isValidCidr(cidr: String): Boolean {
        val pattern = """^(\d{1,3}\.){3}\d{1,3}/([0-9]|[1-2][0-9]|3[0-2])$""".toRegex()
        return pattern.matches(cidr)
    }

    fun ipToInt(ip: String): Long {
        val parts = ip.split(".").map { it.toLong() }
        return (parts[0] shl 24) + (parts[1] shl 16) + (parts[2] shl 8) + parts[3]
    }

    fun intToIp(int: Long): String {
        return "${(int ushr 24) and 0xFF}.${(int ushr 16) and 0xFF}.${(int ushr 8) and 0xFF}.${int and 0xFF}"
    }
}
