package com.zendroid.nmapgui.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.zendroid.nmapgui.R
import com.zendroid.nmapgui.databinding.FragmentScanBinding
import com.zendroid.nmapgui.domain.detector.RootDetector
import com.zendroid.nmapgui.ui.viewmodel.ScanCommand
import com.zendroid.nmapgui.ui.viewmodel.ScanUiState
import com.zendroid.nmapgui.ui.viewmodel.ScanViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ScanViewModel
    private lateinit var rootDetector: RootDetector

    private val scanProfiles = listOf(
        "Quick Scan" to listOf("-T4", "-F"),
        "Intense Scan" to listOf("-T4", "-A", "-v"),
        "Stealth Scan" to listOf("-sS", "-T4"),
        "Service Scan" to listOf("-sV", "-T4"),
        "Custom" to emptyList()
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[ScanViewModel::class.java]
        rootDetector = RootDetector(requireContext())

        setupProfileSpinner()
        setupCheckBoxes()
        setupStartButton()
        observeViewState()
        
        // Set default target
        binding.targetInput.setText("192.168.1.0/24")
    }

    private fun setupProfileSpinner() {
        val profileNames = scanProfiles.map { it.first }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            profileNames
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.profileSpinner.adapter = adapter

        binding.profileSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyProfile(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun applyProfile(position: Int) {
        val (_, flags) = scanProfiles[position]
        
        // Reset all checkboxes
        binding.chkServiceVersion.isChecked = false
        binding.chkOsDetection.isChecked = false
        binding.chkScriptScan.isChecked = false
        binding.chkAggressive.isChecked = false
        binding.chkNoPing.isChecked = false

        // Apply profile flags
        flags.forEach { flag ->
            when (flag) {
                "-sV" -> binding.chkServiceVersion.isChecked = true
                "-O" -> binding.chkOsDetection.isChecked = true
                "-sC" -> binding.chkScriptScan.isChecked = true
                "-A" -> binding.chkAggressive.isChecked = true
                "-Pn" -> binding.chkNoPing.isChecked = true
            }
        }

        updateCommandPreview()
    }

    private fun setupCheckBoxes() {
        binding.chkServiceVersion.setOnCheckedChangeListener { _, _ -> updateCommandPreview() }
        binding.chkOsDetection.setOnCheckedChangeListener { _, _ -> 
            checkRootForOsDetection()
            updateCommandPreview()
        }
        binding.chkScriptScan.setOnCheckedChangeListener { _, _ -> updateCommandPreview() }
        binding.chkAggressive.setOnCheckedChangeListener { _, _ -> updateCommandPreview() }
        binding.chkNoPing.setOnCheckedChangeListener { _, _ -> updateCommandPreview() }
    }

    private fun checkRootForOsDetection() {
        if (binding.chkOsDetection.isChecked) {
            val rootStatus = rootDetector.checkRoot()
            if (!rootStatus.isRooted) {
                Snackbar.make(
                    binding.root,
                    "OS Detection requires root access. Feature will be disabled.",
                    Snackbar.LENGTH_LONG
                ).show()
                binding.chkOsDetection.isChecked = false
            }
        }
    }

    private fun updateCommandPreview() {
        val flags = buildFlags()
        val target = binding.targetInput.text.toString().takeIf { it.isNotBlank() } ?: "<target>"
        binding.commandPreview.text = "nmap ${flags.joinToString(" ")} $target"
    }

    private fun buildFlags(): List<String> {
        val flags = mutableListOf<String>()
        
        if (binding.chkServiceVersion.isChecked) flags.add("-sV")
        if (binding.chkOsDetection.isChecked) flags.add("-O")
        if (binding.chkScriptScan.isChecked) flags.add("-sC")
        if (binding.chkAggressive.isChecked) {
            flags.add("-A")
            // -A includes -sV, -sC, -O, --traceroute
        }
        if (binding.chkNoPing.isChecked) flags.add("-Pn")
        
        // Default timing if nothing specified
        if (flags.isEmpty()) flags.add("-T4")
        
        return flags
    }

    private fun setupStartButton() {
        binding.startScanButton.setOnClickListener {
            val target = binding.targetInput.text.toString().trim()
            
            if (target.isBlank()) {
                Snackbar.make(binding.root, "Please enter a target", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val flags = buildFlags()
            val profileName = scanProfiles[binding.profileSpinner.selectedItemPosition].first

            viewModel.startScan(ScanCommand(target, flags, profileName))
        }
    }

    private fun observeViewState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is ScanUiState.Idle -> {
                        binding.startScanButton.isEnabled = true
                        binding.startScanButton.text = getString(R.string.start_scan)
                    }
                    is ScanUiState.Scanning -> {
                        binding.startScanButton.isEnabled = false
                        binding.startScanButton.text = "Scanning..."
                    }
                    is ScanUiState.Success -> {
                        binding.startScanButton.isEnabled = true
                        binding.startScanButton.text = getString(R.string.start_scan)
                        Snackbar.make(
                            binding.root,
                            "Scan complete! Found ${state.hostCount} hosts, ${state.portCount} ports",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    is ScanUiState.Error -> {
                        binding.startScanButton.isEnabled = true
                        binding.startScanButton.text = getString(R.string.start_scan)
                        Snackbar.make(binding.root, "Error: ${state.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.terminalOutput.collectLatest { output ->
                // Could show terminal in a dialog or navigate to results
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
