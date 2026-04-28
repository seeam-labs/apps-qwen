package com.cyber.zenmappro.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyber.zenmappro.model.HostObject
import com.cyber.zenmappro.model.LogEntry
import com.cyber.zenmappro.model.ScanProfile
import com.cyber.zenmappro.model.ScanResult
import com.cyber.zenmappro.model.ScanType
import com.cyber.zenmappro.utils.AppState
import com.cyber.zenmappro.utils.RootChecker
import com.cyber.zenmappro.utils.ScanEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Main ViewModel for the dashboard
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _isRooted = MutableStateFlow(false)
    val isRooted: StateFlow<Boolean> = _isRooted.asStateFlow()
    
    private val _deviceIp = MutableStateFlow("Unknown")
    val deviceIp: StateFlow<String> = _deviceIp.asStateFlow()
    
    private val _deviceMac = MutableStateFlow("Unknown")
    val deviceMac: StateFlow<String> = _deviceMac.asStateFlow()
    
    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries: StateFlow<List<LogEntry>> = _logEntries.asStateFlow()
    
    private val _currentSubnet = MutableStateFlow("192.168.1.0/24")
    val currentSubnet: StateFlow<String> = _currentSubnet.asStateFlow()
    
    private val _scanProfiles = MutableStateFlow<List<ScanProfile>>(emptyList())
    val scanProfiles: StateFlow<List<ScanProfile>> = _scanProfiles.asStateFlow()
    
    init {
        observeAppState()
        initializeApp()
    }
    
    private fun observeAppState() {
        viewModelScope.launch {
            AppState.isRooted.collect { rooted ->
                _isRooted.value = rooted
            }
        }
        
        viewModelScope.launch {
            AppState.deviceIp.collect { ip ->
                _deviceIp.value = ip
            }
        }
        
        viewModelScope.launch {
            AppState.deviceMac.collect { mac ->
                _deviceMac.value = mac
            }
        }
        
        viewModelScope.launch {
            AppState.logEntries.collect { logs ->
                _logEntries.value = logs
            }
        }
    }
    
    private fun initializeApp() {
        viewModelScope.launch {
            // Check root status
            val rooted = RootChecker.isDeviceRooted()
            AppState.setRooted(rooted)
            
            // Get network device info
            val context = getApplication<Application>().applicationContext
            val deviceInfo = ScanEngine.getNetworkDeviceInfo(context)
            
            // Update subnet based on device IP
            if (deviceInfo.ipAddress != "Unknown") {
                _currentSubnet.value = ScanEngine.getCurrentSubnet(deviceInfo.ipAddress)
            }
            
            // Load saved profiles
            loadProfiles()
        }
    }
    
    fun clearLogs() {
        AppState.clearLogs()
    }
    
    fun saveProfile(profile: ScanProfile) {
        val currentProfiles = _scanProfiles.value.toMutableList()
        currentProfiles.add(profile)
        _scanProfiles.value = currentProfiles
        AppState.addLog("Profile saved: ${profile.name}", 
            com.cyber.zenmappro.model.LogLevel.SUCCESS)
    }
    
    private fun loadProfiles() {
        // Load from SharedPreferences in a real implementation
        val defaultProfiles = listOf(
            ScanProfile(
                id = "1",
                name = "Quick Scan",
                description = "Fast ping and port scan",
                scanType = ScanType.QUICK,
                timing = "T4"
            ),
            ScanProfile(
                id = "2",
                name = "Intense Scan",
                description = "Comprehensive scan with OS detection",
                scanType = ScanType.INTENSE,
                osDetection = true,
                versionDetection = true,
                timing = "T4"
            ),
            ScanProfile(
                id = "3",
                name = "Ping Sweep",
                description = "Discover live hosts only",
                scanType = ScanType.PING
            )
        )
        _scanProfiles.value = defaultProfiles
    }
}

/**
 * ViewModel for scan operations
 */
class ScanViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult: StateFlow<ScanResult?> = _scanResult.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    private val _hostsFound = MutableStateFlow<List<HostObject>>(emptyList())
    val hostsFound: StateFlow<List<HostObject>> = _hostsFound.asStateFlow()
    
    private var scanJob: Job? = null
    
    private val _requiresRoot = MutableStateFlow(false)
    val requiresRoot: StateFlow<Boolean> = _requiresRoot.asStateFlow()
    
    fun startQuickScan(subnet: String) {
        if (_isScanning.value) return
        
        _isScanning.value = true
        _progress.value = 0f
        _hostsFound.value = emptyList()
        _requiresRoot.value = false
        
        scanJob = viewModelScope.launch {
            try {
                val result = ScanEngine.quickScan(subnet)
                _scanResult.value = result
                _hostsFound.value = result.hosts
                _progress.value = 1f
            } catch (e: Exception) {
                AppState.addLog("Scan error: ${e.message}", 
                    com.cyber.zenmappro.model.LogLevel.ERROR)
            } finally {
                _isScanning.value = false
            }
        }
    }
    
    fun startSynScan(ip: String, ports: List<Int>) {
        if (_isScanning.value) return
        
        if (!AppState.isRooted.value) {
            _requiresRoot.value = true
            AppState.addLog("SYN scan requires ROOT access!", 
                com.cyber.zenmappro.model.LogLevel.ERROR)
            return
        }
        
        _isScanning.value = true
        _progress.value = 0f
        
        scanJob = viewModelScope.launch {
            try {
                val (success, output) = ScanEngine.synScan(ip, ports)
                if (success) {
                    AppState.addLog("SYN scan completed", 
                        com.cyber.zenmappro.model.LogLevel.SUCCESS)
                } else {
                    AppState.addLog("SYN scan failed: $output", 
                        com.cyber.zenmappro.model.LogLevel.ERROR)
                }
            } catch (e: Exception) {
                AppState.addLog("SYN scan exception: ${e.message}", 
                    com.cyber.zenmappro.model.LogLevel.ERROR)
            } finally {
                _isScanning.value = false
                _progress.value = 1f
            }
        }
    }
    
    fun stopScan() {
        scanJob?.cancel()
        ScanEngine.stopScan()
        _isScanning.value = false
    }
    
    fun resetScan() {
        _scanResult.value = null
        _hostsFound.value = emptyList()
        _progress.value = 0f
        _requiresRoot.value = false
    }
    
    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
    }
}

/**
 * ViewModel for results display
 */
class ResultsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _selectedHost = MutableStateFlow<HostObject?>(null)
    val selectedHost: StateFlow<HostObject?> = _selectedHost.asStateFlow()
    
    private val _exportSuccess = MutableStateFlow(false)
    val exportSuccess: StateFlow<Boolean> = _exportSuccess.asStateFlow()
    
    fun selectHost(host: HostObject) {
        _selectedHost.value = host
    }
    
    fun deselectHost() {
        _selectedHost.value = null
    }
    
    fun exportResults(scanResult: ScanResult, format: ExportFormat) {
        viewModelScope.launch {
            try {
                val content = when (format) {
                    ExportFormat.XML -> generateXmlExport(scanResult)
                    ExportFormat.TXT -> generateTextExport(scanResult)
                }
                
                // In a real implementation, save to file
                AppState.addLog("Results exported as ${format.name}", 
                    com.cyber.zenmappro.model.LogLevel.SUCCESS)
                _exportSuccess.value = true
            } catch (e: Exception) {
                AppState.addLog("Export failed: ${e.message}", 
                    com.cyber.zenmappro.model.LogLevel.ERROR)
                _exportSuccess.value = false
            }
        }
    }
    
    private fun generateXmlExport(result: ScanResult): String {
        val xml = StringBuilder()
        xml.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        xml.appendLine("<nmaprun scanner=\"zenmappro\" args=\"${result.commandUsed}\" start=\"${result.startTime}\">")
        xml.appendLine("  <scaninfo type=\"${result.scanType.name}\" protocol=\"tcp\"/>")
        xml.appendLine("  <taskbegin task=\"Ping Sweep\" time=\"${result.startTime}\"/>")
        
        result.hosts.forEach { host ->
            xml.appendLine("  <host starttime=\"${result.startTime}\" endtime=\"${result.endTime}\">")
            xml.appendLine("    <status state=\"up\" reason=\"user-set\"/>")
            xml.appendLine("    <address addr=\"${host.ip}\" addrtype=\"ipv4\"/>")
            if (host.mac != "Unknown") {
                xml.appendLine("    <address addr=\"${host.mac}\" addrtype=\"mac\"/>")
            }
            xml.appendLine("    <hostnames>")
            if (host.hostname.isNotEmpty()) {
                xml.appendLine("      <hostname name=\"${host.hostname}\" type=\"PTR\"/>")
            }
            xml.appendLine("    </hostnames>")
            xml.appendLine("    <ports>")
            host.openPorts.forEach { port ->
                xml.appendLine("      <port protocol=\"${port.protocol}\" portid=\"${port.port}\">")
                xml.appendLine("        <state state=\"${port.state}\"/>")
                xml.appendLine("        <service name=\"${port.serviceName}\"/>")
                xml.appendLine("      </port>")
            }
            xml.appendLine("    </ports>")
            xml.appendLine("    <os>")
            xml.appendLine("      <osmatch name=\"${host.osGuess}\"/>")
            xml.appendLine("    </os>")
            xml.appendLine("  </host>")
        }
        
        xml.appendLine("  <taskend task=\"Ping Sweep\" time=\"${result.endTime}\"/>")
        xml.appendLine("  <runstats>")
        xml.appendLine("    <finished time=\"${result.endTime}\" timestr=\"${java.util.Date(result.endTime)}\"/>")
        xml.appendLine("    <hosts up=\"${result.hostsUp}\" down=\"${result.hostsDown}\" total=\"${result.totalHostsScanned}\"/>")
        xml.appendLine("  </runstats>")
        xml.appendLine("</nmaprun>")
        
        return xml.toString()
    }
    
    private fun generateTextExport(result: ScanResult): String {
        val text = StringBuilder()
        text.appendLine("# ZenMap Pro Ultra Scan Results")
        text.appendLine("# Generated: ${java.util.Date()}")
        text.appendLine("# Target: ${result.targetSubnet}")
        text.appendLine("# Type: ${result.scanType.name}")
        text.appendLine("# Duration: ${result.duration / 1000}s")
        text.appendLine("")
        text.appendLine("-".repeat(60))
        text.appendLine("")
        
        result.hosts.forEach { host ->
            text.appendLine("Host: ${host.ip}")
            text.appendLine("  MAC: ${host.mac}")
            text.appendLine("  Latency: ${host.latency}ms")
            text.appendLine("  OS: ${host.osGuess}")
            if (host.openPorts.isNotEmpty()) {
                text.appendLine("  Open Ports:")
                host.openPorts.forEach { port ->
                    text.appendLine("    - ${port.port}/${port.protocol} (${port.serviceName})")
                }
            }
            text.appendLine("")
        }
        
        text.appendLine("-".repeat(60))
        text.appendLine("Summary:")
        text.appendLine("  Total Hosts: ${result.totalHostsScanned}")
        text.appendLine("  Hosts Up: ${result.hostsUp}")
        text.appendLine("  Hosts Down: ${result.hostsDown}")
        text.appendLine("  Open Ports Found: ${result.openPortsFound}")
        
        return text.toString()
    }
    
    enum class ExportFormat {
        XML, TXT
    }
}
