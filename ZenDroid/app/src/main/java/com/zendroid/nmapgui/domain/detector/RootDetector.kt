package com.zendroid.nmapgui.domain.detector

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

class RootDetector(private val context: Context) {

    companion object {
        private const val TAG = "RootDetector"
    }

    data class RootStatus(
        val isRooted: Boolean,
        val rootMethod: String?,
        val riskLevel: RiskLevel
    )

    enum class RiskLevel {
        NONE,
        LOW,
        MEDIUM,
        HIGH
    }

    fun checkRoot(): RootStatus {
        var isRooted = false
        var rootMethod: String? = null
        var riskLevel = RiskLevel.NONE

        // Check 1: Test for su binary
        if (checkSuBinary()) {
            isRooted = true
            rootMethod = "su binary"
            riskLevel = RiskLevel.HIGH
        }

        // Check 2: Check build tags for test keys
        if (checkBuildTags()) {
            isRooted = true
            rootMethod = rootMethod?.plus(", test-keys") ?: "test-keys"
            riskLevel = RiskLevel.MEDIUM
        }

        // Check 3: Check for Magisk
        if (checkMagisk()) {
            isRooted = true
            rootMethod = rootMethod?.plus(", Magisk") ?: "Magisk"
            riskLevel = RiskLevel.HIGH
        }

        // Check 4: Check for SuperSU
        if (checkSuperSU()) {
            isRooted = true
            rootMethod = rootMethod?.plus(", SuperSU") ?: "SuperSU"
            riskLevel = RiskLevel.HIGH
        }

        // Check 5: Try executing su command
        if (checkSuExecution()) {
            isRooted = true
            rootMethod = rootMethod?.plus(", su execution") ?: "su execution"
            riskLevel = RiskLevel.HIGH
        }

        return RootStatus(isRooted, rootMethod, riskLevel)
    }

    private fun checkSuBinary(): Boolean {
        val paths = listOf(
            "/system/xbin/which",
            "/system/bin/which",
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su"
        )

        for (path in paths) {
            if (File(path).exists()) {
                return true
            }
        }

        // Also check PATH
        try {
            val process = Runtime.getRuntime().exec("which su")
            val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
            val result = reader.readLine()
            reader.close()
            return !result.isNullOrEmpty()
        } catch (e: Exception) {
            return false
        }
    }

    private fun checkBuildTags(): Boolean {
        return Build.TAGS?.contains("test-keys") == true ||
               Build.FINGERPRINT?.contains("test-keys") == true
    }

    private fun checkMagisk(): Boolean {
        // Check for Magisk binary
        if (File("/system/xbin/magisk").exists() || 
            File("/system/bin/magisk").exists() ||
            File("/data/adb/magisk").exists()) {
            return true
        }

        // Check for Magisk Manager package
        return try {
            context.packageManager.getPackageInfo("com.topjohnwu.magisk", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun checkSuperSU(): Boolean {
        return try {
            context.packageManager.getPackageInfo("eu.chainfire.supersu", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun checkSuExecution(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            process.waitFor()
            val exitCode = process.exitValue()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }
}
