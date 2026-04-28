package com.cyber.zenmappro.utils

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Utility class for checking root access on Android devices
 */
object RootChecker {
    
    private const val TAG = "RootChecker"
    
    /**
     * Checks if the device has root access by attempting to execute 'su' command
     * @return true if root access is available, false otherwise
     */
    fun isDeviceRooted(): Boolean {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3()
    }
    
    /**
     * Method 1: Check for SU binary in common locations
     */
    private fun checkRootMethod1(): Boolean {
        val paths = arrayOf(
            "/system/xbin/su",
            "/system/bin/su",
            "/sbin/su",
            "/su/bin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su"
        )
        
        for (path in paths) {
            try {
                val suFile = java.io.File(path)
                if (suFile.exists()) {
                    Log.d(TAG, "Found SU at: $path")
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking path $path: ${e.message}")
            }
        }
        return false
    }
    
    /**
     * Method 2: Try executing 'su' command and check response
     */
    private fun checkRootMethod2(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val response = reader.readLine()
            process.waitFor()
            
            val isRoot = response != null && response.contains("uid=0")
            Log.d(TAG, "SU command response: $response, isRoot: $isRoot")
            isRoot
        } catch (e: Exception) {
            Log.e(TAG, "SU command failed: ${e.message}")
            false
        } finally {
            process?.destroy()
        }
    }
    
    /**
     * Method 3: Check build tags for test-keys (common in rooted/custom ROMs)
     */
    private fun checkRootMethod3(): Boolean {
        val buildTags = android.os.Build.TAGS
        Log.d(TAG, "Build tags: $buildTags")
        return buildTags != null && buildTags.contains("test-keys")
    }
    
    /**
     * Get detailed root status information
     */
    fun getRootStatusInfo(): RootStatusInfo {
        val isRooted = isDeviceRooted()
        val suPath = findSuPath()
        val hasTestKeys = android.os.Build.TAGS?.contains("test-keys") == true
        
        return RootStatusInfo(
            isRooted = isRooted,
            suPath = suPath,
            hasTestKeys = hasTestKeys,
            buildType = android.os.Build.TYPE,
            buildTags = android.os.Build.TAGS ?: "Unknown"
        )
    }
    
    /**
     * Find the path to SU binary if it exists
     */
    private fun findSuPath(): String {
        val paths = arrayOf(
            "/system/xbin/su",
            "/system/bin/su",
            "/sbin/su",
            "/su/bin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su"
        )
        
        for (path in paths) {
            try {
                val suFile = java.io.File(path)
                if (suFile.exists()) {
                    return path
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking path $path: ${e.message}")
            }
        }
        return "Not found"
    }
    
    /**
     * Execute a command with root privileges
     * @param command The command to execute
     * @return Pair of success status and output/error message
     */
    fun executeRootCommand(command: String): Pair<Boolean, String> {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            val output = StringBuilder()
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            val errorOutput = StringBuilder()
            while (errorReader.readLine().also { line = it } != null) {
                errorOutput.append(line).append("\n")
            }
            
            process.waitFor()
            
            if (process.exitValue() == 0) {
                Pair(true, output.toString().trim())
            } else {
                Pair(false, errorOutput.toString().trim())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Root command execution failed: ${e.message}")
            Pair(false, e.message ?: "Unknown error")
        } finally {
            process?.destroy()
        }
    }
    
    /**
     * Check if nmap binary is available on the device
     */
    fun isNmapAvailable(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("which", "nmap"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine()
            process.waitFor()
            result != null && result.contains("nmap")
        } catch (e: Exception) {
            Log.e(TAG, "Nmap check failed: ${e.message}")
            false
        } finally {
            process?.destroy()
        }
    }
}

/**
 * Data class containing detailed root status information
 */
data class RootStatusInfo(
    val isRooted: Boolean,
    val suPath: String,
    val hasTestKeys: Boolean,
    val buildType: String,
    val buildTags: String
)
