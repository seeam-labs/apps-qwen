package com.zendroid.nmapgui.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zendroid.nmapgui.data.model.HostEntity
import com.zendroid.nmapgui.data.model.PortEntity
import com.zendroid.nmapgui.data.model.ScanProfileEntity
import com.zendroid.nmapgui.data.model.ScanSessionEntity
import com.zendroid.nmapgui.data.repository.ScanRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class ScanUiState {
    object Idle : ScanUiState()
    object Scanning : ScanUiState()
    data class Success(val sessionId: Long, val hostCount: Int, val portCount: Int) : ScanUiState()
    data class Error(val message: String) : ScanUiState()
}

data class ScanCommand(
    val target: String = "",
    val flags: List<String> = emptyList(),
    val profileName: String = "Custom"
)

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScanRepository.getInstance(application)

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _terminalOutput = MutableStateFlow("")
    val terminalOutput: StateFlow<String> = _terminalOutput.asStateFlow()

    private val _currentCommand = MutableStateFlow("")
    val currentCommand: StateFlow<String> = _currentCommand.asStateFlow()

    val allSessions: Flow<List<ScanSessionEntity>> = repository.getAllSessions()
    val allProfiles: Flow<List<ScanProfileEntity>> = repository.getAllProfiles()

    fun startScan(command: ScanCommand) {
        viewModelScope.launch {
            _uiState.value = ScanUiState.Scanning
            _terminalOutput.value = ""
            _currentCommand.value = "nmap ${command.flags.joinToString(" ")} ${command.target}"

            repository.executeScan(
                target = command.target,
                flags = command.flags,
                profileName = command.profileName,
                onProgressUpdate = { line ->
                    _terminalOutput.update { current ->
                        if (current.isEmpty()) line else "$current\n$line"
                    }
                }
            ).let { result ->
                when (result) {
                    is ScanRepository.ScanExecutionResult.Success -> {
                        _uiState.value = ScanUiState.Success(result.sessionId, result.hostCount, result.portCount)
                    }
                    is ScanRepository.ScanExecutionResult.Error -> {
                        _uiState.value = ScanUiState.Error(result.message)
                    }
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = ScanUiState.Idle
        _terminalOutput.value = ""
        _currentCommand.value = ""
    }

    fun getSessionById(sessionId: Long): Flow<ScanSessionEntity?> {
        return repository.getSessionById(sessionId)
    }

    fun getHostsBySessionId(sessionId: Long): Flow<List<HostEntity>> {
        return repository.getHostsBySessionId(sessionId)
    }

    fun getPortsByHostId(hostId: Long): Flow<List<PortEntity>> {
        return repository.getPortsByHostId(hostId)
    }

    fun searchSessions(query: String): Flow<List<ScanSessionEntity>> {
        return repository.searchSessions(query)
    }

    suspend fun deleteSession(session: ScanSessionEntity) {
        repository.deleteSession(session)
    }

    suspend fun insertProfile(profile: ScanProfileEntity) {
        repository.insertProfile(profile)
    }

    suspend fun deleteUserProfile(profileId: Long) {
        repository.deleteUserProfile(profileId)
    }
}
