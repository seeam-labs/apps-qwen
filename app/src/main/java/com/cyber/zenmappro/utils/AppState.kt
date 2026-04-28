package com.cyber.zenmappro.utils

import android.util.Log
import com.cyber.zenmappro.model.LogEntry
import com.cyber.zenmappro.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton object to manage application state
 */
object AppState {
    
    private val _isRooted = MutableStateFlow(false)
    val isRooted: StateFlow<Boolean> = _isRooted.asStateFlow()
    
    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted.asStateFlow()
    
    private val _deviceIp = MutableStateFlow("Unknown")
    val deviceIp: StateFlow<String> = _deviceIp.asStateFlow()
    
    private val _deviceMac = MutableStateFlow("Unknown")
    val deviceMac: StateFlow<String> = _deviceMac.asStateFlow()
    
    private val _networkInterface = MutableStateFlow<String>("Unknown")
    val networkInterface: StateFlow<String> = _networkInterface.asStateFlow()
    
    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries: StateFlow<List<LogEntry>> = _logEntries.asStateFlow()
    
    var scanInProgress: Boolean = false
    
    fun setRooted(isRooted: Boolean) {
        _isRooted.value = isRooted
        addLog(if (isRooted) "ROOT access detected" else "ROOT access not available", 
            if (isRooted) LogLevel.SUCCESS else LogLevel.WARNING)
    }
    
    fun setPermissionsGranted(granted: Boolean) {
        _permissionsGranted.value = granted
    }
    
    fun setDeviceInfo(ip: String, mac: String, interfaceName: String = "") {
        _deviceIp.value = ip
        _deviceMac.value = mac
        if (interfaceName.isNotEmpty()) {
            _networkInterface.value = interfaceName
        }
    }
    
    fun addLog(message: String, level: LogLevel = LogLevel.INFO, tag: String = "ZenMap") {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            message = message,
            tag = tag
        )
        val currentLogs = _logEntries.value.toMutableList()
        currentLogs.add(entry)
        
        // Keep only last 500 entries to prevent memory issues
        if (currentLogs.size > 500) {
            currentLogs.removeAt(0)
        }
        
        _logEntries.value = currentLogs
        Log.d(tag, "[${level.name}] $message")
    }
    
    fun clearLogs() {
        _logEntries.value = emptyList()
        addLog("Terminal log cleared", LogLevel.INFO)
    }
    
    fun getRecentLogs(count: Int = 100): List<LogEntry> {
        return _logEntries.value.takeLast(count)
    }
}
