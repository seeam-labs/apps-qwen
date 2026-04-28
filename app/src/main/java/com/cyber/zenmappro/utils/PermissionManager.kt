package com.cyber.zenmappro.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cyber.zenmappro.model.LogLevel
import com.cyber.zenmappro.utils.AppState.addLog

/**
 * Permission manager for handling runtime permissions
 */
object PermissionManager {
    
    private const val TAG = "PermissionManager"
    const val PERMISSION_REQUEST_CODE = 1001
    
    // Required permissions for network scanning
    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE
    )
    
    // Location permissions required for WiFi scanning on Android 6+
    val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    /**
     * Check if all required permissions are granted
     */
    fun hasRequiredPermissions(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Check if location permissions are granted (required for WiFi scanning on Android 6+)
     */
    fun hasLocationPermissions(context: Context): Boolean {
        return LOCATION_PERMISSIONS.any { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Check if all permissions including location are granted
     */
    fun hasAllPermissions(context: Context): Boolean {
        return hasRequiredPermissions(context) && hasLocationPermissions(context)
    }
    
    /**
     * Request missing permissions from the activity
     */
    fun requestPermissions(activity: Activity) {
        val missingPermissions = getMissingPermissions(activity)
        
        if (missingPermissions.isNotEmpty()) {
            addLog("Requesting ${missingPermissions.size} permissions", LogLevel.INFO, TAG)
            ActivityCompat.requestPermissions(
                activity,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            addLog("All permissions already granted", LogLevel.SUCCESS, TAG)
            AppState.setPermissionsGranted(true)
        }
    }
    
    /**
     * Get list of permissions that are not yet granted
     */
    fun getMissingPermissions(context: Context): List<String> {
        val allPermissions = REQUIRED_PERMISSIONS + LOCATION_PERMISSIONS
        return allPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Handle permission request result
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.isNotEmpty() && 
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            
            if (allGranted) {
                addLog("All permissions granted successfully", LogLevel.SUCCESS, TAG)
                AppState.setPermissionsGranted(true)
                return true
            } else {
                val deniedPermissions = permissions.filterIndexed { index, _ ->
                    grantResults[index] == PackageManager.PERMISSION_DENIED
                }
                addLog("Permissions denied: ${deniedPermissions.joinToString()}", LogLevel.ERROR, TAG)
                AppState.setPermissionsGranted(false)
                return false
            }
        }
        return false
    }
    
    /**
     * Check if we should show rationale for permissions
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        val allPermissions = REQUIRED_PERMISSIONS + LOCATION_PERMISSIONS
        return allPermissions.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }
    
    /**
     * Get human-readable explanation for why permissions are needed
     */
    fun getPermissionExplanation(): String {
        return """
            ZenMap Pro Ultra requires the following permissions:
            
            • INTERNET - For network scanning and connectivity
            • ACCESS_NETWORK_STATE - To check network status
            • ACCESS_WIFI_STATE - To scan WiFi networks
            • CHANGE_WIFI_STATE - To enable/disable WiFi for scanning
            • ACCESS_FINE_LOCATION - Required by Android for WiFi scanning (Android 6+)
            
            No personal data is collected. All scanning is performed locally.
        """.trimIndent()
    }
    
    /**
     * Initialize permissions and device info on app start
     */
    fun initializePermissions(activity: Activity) {
        if (hasAllPermissions(activity)) {
            AppState.setPermissionsGranted(true)
            addLog("All permissions verified", LogLevel.SUCCESS, TAG)
        } else {
            requestPermissions(activity)
        }
    }
}
