package com.zendroid.nmapgui

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ZenDroidApp : Application() {

    companion object {
        private const val TAG = "ZenDroidApp"
        lateinit var instance: ZenDroidApp
            private set
        
        const val NATIVE_DIR = "nmap"
        const val NMAP_BINARY_NAME = "nmap"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Extract native binaries on app start
        CoroutineScope(Dispatchers.IO).launch {
            extractNmapBinaries()
        }
    }

    private fun extractNmapBinaries() {
        val nmapDir = File(filesDir, NATIVE_DIR)
        
        if (!nmapDir.exists()) {
            nmapDir.mkdirs()
            Log.d(TAG, "Created nmap directory: ${nmapDir.absolutePath}")
        }

        // Detect ABI and extract appropriate binary
        val supportedAbis = Build.SUPPORTED_ABIS
        Log.d(TAG, "Supported ABIs: ${supportedAbis.joinToString()}")
        
        for (abi in supportedAbis) {
            val assetPath = getAssetPathForAbi(abi)
            if (assetPath != null && assets.list(assetPath)?.contains(NMAP_BINARY_NAME) == true) {
                val targetFile = File(nmapDir, NMAP_BINARY_NAME)
                
                if (!targetFile.exists()) {
                    try {
                        assets.open("$assetPath/$NMAP_BINARY_NAME").use { input ->
                            targetFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        
                        // Set executable permissions
                        val success = targetFile.setExecutable(true, false)
                        Log.d(TAG, "Extracted $assetPath/$NMAP_BINARY_NAME to ${targetFile.absolutePath}, executable: $success")
                        
                        // Also extract nmap-services and nmap-os-db if available
                        extractAssetIfExists("nmap-services", nmapDir)
                        extractAssetIfExists("nmap-os-db", nmapDir)
                        
                        return@launch
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to extract binary for ABI $abi", e)
                    }
                } else {
                    Log.d(TAG, "Binary already exists at ${targetFile.absolutePath}")
                    return@launch
                }
            }
        }
        
        Log.w(TAG, "Could not find suitable Nmap binary for any supported ABI")
    }

    private fun getAssetPathForAbi(abi: String): String? {
        return when (abi) {
            "arm64-v8a" -> "nmap/arm64-v8a"
            "armeabi-v7a" -> "nmap/armeabi-v7a"
            "x86_64" -> "nmap/x86_64"
            "x86" -> "nmap/x86"
            else -> null
        }
    }

    private fun extractAssetIfExists(assetName: String, targetDir: File) {
        try {
            // Try each ABI path
            val supportedAbis = Build.SUPPORTED_ABIS
            for (abi in supportedAbis) {
                val assetPath = getAssetPathForAbi(abi) ?: continue
                val fullPath = "$assetPath/$assetName"
                
                try {
                    assets.open(fullPath).use { input ->
                        val targetFile = File(targetDir, assetName)
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                        Log.d(TAG, "Extracted $assetName to ${targetFile.absolutePath}")
                        return
                    }
                } catch (e: java.io.FileNotFoundException) {
                    // Try next ABI
                    continue
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract $assetName", e)
        }
    }

    fun getNmapBinaryPath(): String {
        return File(filesDir, NATIVE_DIR).absolutePath + "/" + NMAP_BINARY_NAME
    }

    fun getNmapDataDir(): String {
        return File(filesDir, NATIVE_DIR).absolutePath
    }
}
