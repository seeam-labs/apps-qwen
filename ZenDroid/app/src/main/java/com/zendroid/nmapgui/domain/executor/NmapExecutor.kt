package com.zendroid.nmapgui.domain.executor

import android.util.Log
import com.zendroid.nmapgui.ZenDroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class NmapExecutor {

    companion object {
        private const val TAG = "NmapExecutor"
    }

    data class ExecutionResult(
        val exitCode: Int,
        val output: String,
        val errorOutput: String,
        val command: String
    )

    suspend fun execute(
        target: String,
        flags: List<String>,
        isRooted: Boolean,
        onProgressUpdate: ((String) -> Unit)? = null
    ): ExecutionResult = withContext(Dispatchers.IO) {
        
        val nmapPath = ZenDroidApp.instance.getNmapBinaryPath()
        val dataDir = ZenDroidApp.instance.getNmapDataDir()
        
        // Build command
        val commandBuilder = mutableListOf(nmapPath)
        commandBuilder.addAll(flags)
        commandBuilder.add("-oX")
        commandBuilder.add("-")  // Output XML to stdout
        commandBuilder.add(target)
        
        val command = commandBuilder.joinToString(" ")
        Log.d(TAG, "Executing: $command")
        
        // Adjust flags for non-root users
        val adjustedFlags = adjustFlagsForNonRoot(flags, isRooted)
        val adjustedCommandBuilder = mutableListOf(nmapPath)
        adjustedCommandBuilder.addAll(adjustedFlags)
        adjustedCommandBuilder.add("-oX")
        adjustedCommandBuilder.add("-")
        adjustedCommandBuilder.add(target)
        
        val processBuilder = ProcessBuilder(adjustedCommandBuilder)
        processBuilder.environment()["NMAP_DATADIR"] = dataDir
        
        if (isRooted) {
            try {
                val rootProcess = Runtime.getRuntime().exec("su")
                val outputStream = rootProcess.outputStream
                val commandString = adjustedCommandBuilder.joinToString(" ") + "\nexit\n"
                outputStream.write(commandString.toByteArray())
                outputStream.flush()
                outputStream.close()
                
                return@withContext readProcessOutput(rootProcess, command, onProgressUpdate)
            } catch (e: Exception) {
                Log.e(TAG, "Root execution failed, falling back to non-root", e)
                // Fall back to non-root execution
            }
        }
        
        val process = processBuilder.start()
        readProcessOutput(process, command, onProgressUpdate)
    }

    private fun adjustFlagsForNonRoot(flags: List<String>, isRooted: Boolean): List<String> {
        if (isRooted) return flags
        
        val adjusted = flags.toMutableList()
        var needsWarning = false
        
        // Replace SYN stealth scan with TCP connect
        if (adjusted.contains("-sS")) {
            adjusted.remove("-sS")
            if (!adjusted.contains("-sT")) {
                adjusted.add("-sT")
            }
            needsWarning = true
        }
        
        // Remove OS detection flag
        if (adjusted.contains("-O")) {
            adjusted.remove("-O")
            needsWarning = true
        }
        
        // Remove fragmentation
        if (adjusted.contains("-f")) {
            adjusted.remove("-f")
            needsWarning = true
        }
        
        // Remove aggressive mode's root-requiring components
        if (adjusted.contains("-A")) {
            // -A includes -O which won't work without root
            // Keep it but user should be warned
            needsWarning = true
        }
        
        if (needsWarning) {
            Log.w(TAG, "Flags adjusted for non-root execution")
        }
        
        return adjusted
    }

    private suspend fun readProcessOutput(
        process: Process,
        command: String,
        onProgressUpdate: ((String) -> Unit)?
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val outputBuilder = StringBuilder()
        val errorBuilder = StringBuilder()
        
        val outputReader = BufferedReader(InputStreamReader(process.inputStream))
        val errorReader = BufferedReader(InputStreamReader(process.errorStream))
        
        var line: String?
        while (outputReader.readLine().also { line = it } != null) {
            outputBuilder.append(line).append("\n")
            onProgressUpdate?.invoke(line!!)
        }
        
        while (errorReader.readLine().also { line = it } != null) {
            errorBuilder.append(line).append("\n")
            onProgressUpdate?.invoke("ERROR: ${line}")
        }
        
        process.waitFor()
        
        val exitCode = process.exitValue()
        Log.d(TAG, "Process completed with exit code: $exitCode")
        
        ExecutionResult(
            exitCode = exitCode,
            output = outputBuilder.toString(),
            errorOutput = errorBuilder.toString(),
            command = command
        )
    }

    fun validateTarget(target: String): Boolean {
        // Basic validation for IP addresses, hostnames, and CIDR notation
        val ipPattern = """^(\d{1,3}\.){3}\d{1,3}(/([0-9]|[1-2][0-9]|3[0-2]))?$""".toRegex()
        val hostnamePattern = """^[a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?)*$""".toRegex()
        val localhostPattern = """^localhost$""".toRegex()
        
        return ipPattern.matches(target) || 
               hostnamePattern.matches(target) || 
               localhostPattern.matches(target) ||
               target == "127.0.0.1"
    }
}
