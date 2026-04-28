package com.cyber.zenmappro.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.cyber.zenmappro.R
import com.cyber.zenmappro.databinding.ActivityMainBinding
import com.cyber.zenmappro.model.LogLevel
import com.cyber.zenmappro.utils.AppState
import com.cyber.zenmappro.utils.PermissionManager
import com.cyber.zenmappro.utils.RootChecker
import com.cyber.zenmappro.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Main Activity - The Cockpit/Dashboard
 * Displays device info, scan cards, and terminal log
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        observeViewModel()
        initializeApp()
    }
    
    private fun setupUI() {
        // Setup card click listeners
        binding.cardQuickScan.setOnClickListener {
            onQuickScanClicked()
        }
        
        binding.cardNetworkMapper.setOnClickListener {
            onNetworkMapperClicked()
        }
        
        binding.cardVulnAudit.setOnClickListener {
            onVulnAuditClicked()
        }
        
        binding.cardPacketInjector.setOnClickListener {
            onPacketInjectorClicked()
        }
        
        binding.cardWifiAnalyzer.setOnClickListener {
            onWifiAnalyzerClicked()
        }
        
        binding.cardSavedProfiles.setOnClickListener {
            onSavedProfilesClicked()
        }
        
        // Clear log button
        binding.btnClearLog.setOnClickListener {
            viewModel.clearLogs()
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.isRooted.collectLatest { isRooted ->
                updateRootStatus(isRooted)
            }
        }
        
        lifecycleScope.launch {
            viewModel.deviceIp.collectLatest { ip ->
                binding.tvIpAddress.text = ip
            }
        }
        
        lifecycleScope.launch {
            viewModel.deviceMac.collectLatest { mac ->
                binding.tvMacAddress.text = mac
            }
        }
        
        lifecycleScope.launch {
            viewModel.logEntries.collectLatest { logs ->
                updateTerminalLog(logs)
            }
        }
        
        lifecycleScope.launch {
            viewModel.currentSubnet.collectLatest { subnet ->
                AppState.addLog("Current subnet: $subnet", LogLevel.INFO)
            }
        }
    }
    
    private fun initializeApp() {
        // Request permissions
        PermissionManager.initializePermissions(this)
        
        // Add initial log entry
        AppState.addLog("ZenMap Pro Ultra v1.0.0 initialized", LogLevel.SUCCESS, TAG)
        AppState.addLog("Loading system modules...", LogLevel.INFO, TAG)
    }
    
    private fun updateRootStatus(isRooted: Boolean) {
        if (isRooted) {
            binding.tvRootStatus.text = "ROOTED ✓"
            binding.tvRootStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_green))
            AppState.addLog("ROOT access detected - Advanced features enabled", LogLevel.SUCCESS, TAG)
        } else {
            binding.tvRootStatus.text = "NOT ROOTED ✗"
            binding.tvRootStatus.setTextColor(ContextCompat.getColor(this, R.color.hacker_red))
            AppState.addLog("ROOT access not available - Limited functionality", LogLevel.WARNING, TAG)
        }
    }
    
    private fun updateTerminalLog(logs: List<com.cyber.zenmappro.model.LogEntry>) {
        val logText = logs.joinToString("\n") { entry ->
            "${entry.getFormattedTime()} ${entry.getColoredPrefix()} ${entry.message}"
        }
        binding.terminalOutput.text = if (logs.isEmpty()) {
            "> ZenMap Pro Ultra v1.0.0\n> Initializing system...\n> Waiting for commands...\n"
        } else {
            "> ZenMap Pro Ultra v1.0.0\n$logText\n"
        }
        
        // Auto-scroll to bottom
        binding.terminalScroll.post {
            binding.terminalScroll.fullScroll(View.FOCUS_DOWN)
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (PermissionManager.handlePermissionResult(requestCode, permissions, grantResults)) {
            AppState.addLog("All permissions granted", LogLevel.SUCCESS, TAG)
        } else {
            AppState.addLog("Some permissions denied", LogLevel.ERROR, TAG)
            showPermissionDeniedDialog()
        }
    }
    
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this, R.style.HackerDialog)
            .setTitle("Permission Required")
            .setMessage(PermissionManager.getPermissionExplanation())
            .setPositiveButton("OK") { _, _ ->
                PermissionManager.requestPermissions(this)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    // Card click handlers
    private fun onQuickScanClicked() {
        AppState.addLog("Quick Scan initiated", LogLevel.INFO, TAG)
        val intent = Intent(this, ResultsActivity::class.java)
        intent.putExtra("scan_type", "QUICK")
        intent.putExtra("subnet", viewModel.currentSubnet.value)
        startActivity(intent)
    }
    
    private fun onNetworkMapperClicked() {
        AppState.addLog("Network Mapper initiated", LogLevel.INFO, TAG)
        val intent = Intent(this, ResultsActivity::class.java)
        intent.putExtra("scan_type", "PING")
        intent.putExtra("subnet", viewModel.currentSubnet.value)
        startActivity(intent)
    }
    
    private fun onVulnAuditClicked() {
        AppState.addLog("Vulnerability Audit initiated", LogLevel.INFO, TAG)
        val intent = Intent(this, ResultsActivity::class.java)
        intent.putExtra("scan_type", "VULN")
        intent.putExtra("subnet", viewModel.currentSubnet.value)
        startActivity(intent)
    }
    
    private fun onPacketInjectorClicked() {
        if (!AppState.isRooted.value) {
            showRootRequiredDialog("Packet Injector")
            return
        }
        AppState.addLog("Packet Injector - ROOT feature", LogLevel.WARNING, TAG)
        Toast.makeText(this, "Packet Injector requires additional setup", Toast.LENGTH_LONG).show()
    }
    
    private fun onWifiAnalyzerClicked() {
        if (!hasLocationPermission()) {
            showLocationRequiredDialog()
            return
        }
        AppState.addLog("Wi-Fi Analyzer initiated", LogLevel.INFO, TAG)
        val intent = Intent(this, ResultsActivity::class.java)
        intent.putExtra("scan_type", "WIFI")
        startActivity(intent)
    }
    
    private fun onSavedProfilesClicked() {
        AppState.addLog("Opening Profile Manager", LogLevel.INFO, TAG)
        val intent = Intent(this, ProfileManagerActivity::class.java)
        startActivity(intent)
    }
    
    private fun showRootRequiredDialog(feature: String) {
        AlertDialog.Builder(this, R.style.HackerDialog)
            .setTitle("⚠ ROOT ACCESS REQUIRED")
            .setMessage("""
                $feature requires ROOT access to function properly.
                
                Without root, this feature cannot perform low-level network operations.
                
                To enable ROOT access:
                1. Unlock your bootloader
                2. Install a custom recovery
                3. Flash Magisk for root management
                
                WARNING: Rooting may void your warranty!
            """.trimIndent())
            .setPositiveButton("I Understand", null)
            .setNeutralButton("Learn More") { _, _ ->
                // Could open a browser with Magisk info
                Toast.makeText(this, "Visit magisk.me for more info", Toast.LENGTH_LONG).show()
            }
            .show()
    }
    
    private fun showLocationRequiredDialog() {
        AlertDialog.Builder(this, R.style.HackerDialog)
            .setTitle("Location Permission Required")
            .setMessage("Android requires location permission for Wi-Fi scanning on Android 6.0+")
            .setPositiveButton("Grant Permission") { _, _ ->
                PermissionManager.requestPermissions(this)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
