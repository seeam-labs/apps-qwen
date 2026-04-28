package com.cyber.zenmappro.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cyber.zenmappro.adapter.HostAdapter
import com.cyber.zenmappro.databinding.ActivityResultsBinding
import com.cyber.zenmappro.model.LogLevel
import com.cyber.zenmappro.utils.AppState
import com.cyber.zenmappro.utils.ScanEngine
import com.cyber.zenmappro.viewmodel.ResultsViewModel
import com.cyber.zenmappro.viewmodel.ScanViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Results Activity - Displays scan results with charts and host list
 */
class ResultsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityResultsBinding
    private val scanViewModel: ScanViewModel by viewModels()
    private val resultsViewModel: ResultsViewModel by viewModels()
    private lateinit var hostAdapter: HostAdapter
    
    private var scanType: String = "QUICK"
    private var subnet: String = "192.168.1.0/24"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get intent extras
        scanType = intent.getStringExtra("scan_type") ?: "QUICK"
        subnet = intent.getStringExtra("subnet") ?: "192.168.1.0/24"
        
        setupUI()
        setupCharts()
        observeViewModel()
    }
    
    private fun setupUI() {
        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        // Start scan button
        binding.btnStartScan.setOnClickListener {
            startScan()
        }
        
        // Stop scan button
        binding.btnStopScan.setOnClickListener {
            scanViewModel.stopScan()
        }
        
        // Export button
        binding.btnExport.setOnClickListener {
            showExportDialog()
        }
        
        // Topology button
        binding.btnTopology.setOnClickListener {
            val intent = Intent(this, TopologyActivity::class.java)
            startActivity(intent)
        }
        
        // Setup RecyclerView
        hostAdapter = HostAdapter { host ->
            resultsViewModel.selectHost(host)
            AppState.addLog("Selected host: ${host.ip}", LogLevel.INFO)
        }
        
        binding.recyclerHosts.layoutManager = LinearLayoutManager(this)
        binding.recyclerHosts.adapter = hostAdapter
        
        // Update title based on scan type
        binding.tvTitle.text = when (scanType) {
            "QUICK" -> "QUICK SCAN"
            "PING" -> "NETWORK MAP"
            "VULN" -> "VULN AUDIT"
            "WIFI" -> "WI-FI ANALYZER"
            else -> "SCAN RESULTS"
        }
    }
    
    private fun setupCharts() {
        // Setup Pie Chart
        setupPieChart(binding.pieChart)
        
        // Setup Bar Chart
        setupBarChart(binding.barChart)
    }
    
    private fun setupPieChart(chart: PieChart) {
        chart.setDrawHoleEnabled(true)
        chart.holeColor = getColor(com.cyber.zenmappro.R.color.card_background)
        chart.transparentCircleRadius = 40f
        
        val description = Description()
        description.text = "Ports by Protocol"
        description.textSize = 10f
        description.textColor = getColor(com.cyber.zenmappro.R.color.neon_green)
        chart.description = description
        
        chart.legend.isEnabled = false
        chart.setUsePercentValues(false)
    }
    
    private fun setupBarChart(chart: BarChart) {
        chart.setDrawBarShadow(false)
        chart.setDrawValueAboveBar(true)
        
        val description = Description()
        description.text = "Latency (ms)"
        description.textSize = 10f
        description.textColor = getColor(com.cyber.zenmappro.R.color.neon_green)
        chart.description = description
        
        chart.legend.isEnabled = false
        chart.xAxis.axisMinimum = 0f
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            scanViewModel.isScanning.collectLatest { isScanning ->
                binding.btnStartScan.visibility = if (isScanning) View.GONE else View.VISIBLE
                binding.btnStopScan.visibility = if (isScanning) View.VISIBLE else View.GONE
                binding.tvScanStatus.text = if (isScanning) "Status: SCANNING..." else "Status: IDLE"
            }
        }
        
        lifecycleScope.launch {
            scanViewModel.progress.collectLatest { progress ->
                binding.progressBar.progress = (progress * 100).toInt()
            }
        }
        
        lifecycleScope.launch {
            scanViewModel.hostsFound.collectLatest { hosts ->
                hostAdapter.submitList(hosts)
                binding.tvHostsFound.text = "Hosts: ${hosts.size}"
                updateCharts(hosts)
            }
        }
        
        lifecycleScope.launch {
            scanViewModel.requiresRoot.collectLatest { requiresRoot ->
                if (requiresRoot) {
                    showRootRequiredMessage()
                }
            }
        }
    }
    
    private fun updateCharts(hosts: List<com.cyber.zenmappro.model.HostObject>) {
        // Update Pie Chart - Port distribution
        updatePieChart(hosts)
        
        // Update Bar Chart - Latency distribution
        updateBarChart(hosts)
    }
    
    private fun updatePieChart(hosts: List<com.cyber.zenmappro.model.HostObject>) {
        val portCounts = mutableMapOf<String, Int>()
        
        hosts.forEach { host ->
            host.openPorts.forEach { port ->
                val protocol = port.serviceName.ifEmpty { port.protocol }
                portCounts[protocol] = (portCounts[protocol] ?: 0) + 1
            }
        }
        
        val entries = portCounts.map { entry ->
            PieEntry(entry.value.toFloat(), entry.key)
        }
        
        if (entries.isEmpty()) {
            binding.pieChart.clear()
            return
        }
        
        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                getColor(com.cyber.zenmappro.R.color.neon_green),
                getColor(com.cyber.zenmappro.R.color.cyber_pink),
                getColor(com.cyber.zenmappro.R.color.hacker_yellow),
                getColor(com.cyber.zenmappro.R.color.text_secondary)
            )
            valueTextColor = getColor(com.cyber.zenmappro.R.color.white)
            valueTextSize = 10f
        }
        
        val data = PieData(dataSet)
        data.setValueFormatter(ValueFormatter())
        binding.pieChart.data = data
        binding.pieChart.invalidate()
    }
    
    private fun updateBarChart(hosts: List<com.cyber.zenmappro.model.HostObject>) {
        val entries = hosts.take(10).mapIndexed { index, host ->
            BarEntry(index.toFloat(), host.latency.toFloat())
        }
        
        if (entries.isEmpty()) {
            binding.barChart.clear()
            return
        }
        
        val dataSet = BarDataSet(entries, "").apply {
            color = getColor(com.cyber.zenmappro.R.color.neon_green)
            valueTextColor = getColor(com.cyber.zenmappro.R.color.white)
            valueTextSize = 9f
        }
        
        val data = BarData(dataSet).apply {
            barWidth = 0.5f
        }
        
        binding.barChart.data = data
        binding.barChart.invalidate()
    }
    
    private fun startScan() {
        AppState.addLog("Starting $scanType scan on $subnet", LogLevel.INFO)
        
        when (scanType) {
            "QUICK", "VULN" -> {
                scanViewModel.startQuickScan(subnet)
            }
            "PING" -> {
                scanViewModel.startQuickScan(subnet)
            }
            "WIFI" -> {
                // WiFi scan implementation
                AppState.addLog("WiFi scanning...", LogLevel.INFO)
                scanViewModel.startQuickScan(subnet)
            }
            else -> {
                scanViewModel.startQuickScan(subnet)
            }
        }
    }
    
    private fun showExportDialog() {
        val formats = arrayOf("Nmap XML", "Plain Text")
        androidx.appcompat.app.AlertDialog.Builder(this, com.cyber.zenmappro.R.style.HackerDialog)
            .setTitle("Export Format")
            .setItems(formats) { _, which ->
                val format = when (which) {
                    0 -> ResultsViewModel.ExportFormat.XML
                    1 -> ResultsViewModel.ExportFormat.TXT
                    else -> ResultsViewModel.ExportFormat.TXT
                }
                
                scanViewModel.scanResult.value?.let { result ->
                    resultsViewModel.exportResults(result, format)
                } ?: run {
                    AppState.addLog("No scan results to export", LogLevel.WARNING)
                }
            }
            .show()
    }
    
    private fun showRootRequiredMessage() {
        androidx.appcompat.app.AlertDialog.Builder(this, com.cyber.zenmappro.R.style.HackerDialog)
            .setTitle("⚠ ROOT REQUIRED")
            .setMessage("This scan type requires ROOT access. Please root your device or use a different scan type.")
            .setPositiveButton("OK", null)
            .show()
    }
}
