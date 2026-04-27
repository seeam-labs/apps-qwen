package com.zendroid.nmapgui.data.repository

import android.content.Context
import androidx.room.Room
import com.zendroid.nmapgui.ZenDroidApp
import com.zendroid.nmapgui.data.database.*
import com.zendroid.nmapgui.data.model.*
import com.zendroid.nmapgui.data.parser.NmapXmlParser
import com.zendroid.nmapgui.domain.executor.NmapExecutor
import com.zendroid.nmapgui.domain.detector.RootDetector
import kotlinx.coroutines.flow.Flow

class ScanRepository(private val context: Context) {

    private val database = Room.databaseBuilder(
        context.applicationContext,
        ZenDroidDatabase::class.java,
        ZenDroidDatabase.DATABASE_NAME
    ).build()

    private val scanSessionDao = database.scanSessionDao()
    private val hostDao = database.hostDao()
    private val portDao = database.portDao()
    private val scanProfileDao = database.scanProfileDao()

    private val nmapExecutor = NmapExecutor()
    private val rootDetector = RootDetector(context)
    private val xmlParser = NmapXmlParser()

    // Session operations
    fun getAllSessions(): Flow<List<ScanSessionEntity>> = scanSessionDao.getAllSessions()
    
    fun getSessionById(sessionId: Long): Flow<ScanSessionEntity?> = scanSessionDao.getSessionByIdFlow(sessionId)
    
    suspend fun insertSession(session: ScanSessionEntity): Long {
        return scanSessionDao.insertSession(session)
    }
    
    suspend fun updateSession(session: ScanSessionEntity) {
        scanSessionDao.updateSession(session)
    }
    
    suspend fun deleteSession(session: ScanSessionEntity) {
        scanSessionDao.deleteSession(session)
    }
    
    fun searchSessions(query: String): Flow<List<ScanSessionEntity>> = 
        scanSessionDao.searchSessions("%$query%")

    // Host operations
    fun getHostsBySessionId(sessionId: Long): Flow<List<HostEntity>> = 
        hostDao.getHostsBySessionId(sessionId)
    
    suspend fun insertHost(host: HostEntity): Long {
        return hostDao.insertHost(host)
    }
    
    suspend fun insertHosts(hosts: List<HostEntity>) {
        hostDao.insertHosts(hosts)
    }

    // Port operations
    fun getPortsByHostId(hostId: Long): Flow<List<PortEntity>> = 
        portDao.getPortsByHostId(hostId)
    
    suspend fun insertPorts(ports: List<PortEntity>) {
        portDao.insertPorts(ports)
    }

    // Profile operations
    fun getAllProfiles(): Flow<List<ScanProfileEntity>> = scanProfileDao.getAllProfiles()
    
    suspend fun insertProfile(profile: ScanProfileEntity): Long {
        return scanProfileDao.insertProfile(profile)
    }
    
    suspend fun deleteUserProfile(profileId: Long) {
        scanProfileDao.deleteUserProfile(profileId)
    }

    // Execute scan
    suspend fun executeScan(
        target: String,
        flags: List<String>,
        profileName: String,
        onProgressUpdate: ((String) -> Unit)? = null
    ): ScanExecutionResult {
        // Validate target
        if (!nmapExecutor.validateTarget(target)) {
            return ScanExecutionResult.Error("Invalid target address or hostname")
        }

        // Check root status
        val rootStatus = rootDetector.checkRoot()
        
        // Adjust flags based on root status
        val adjustedFlags = if (!rootStatus.isRooted) {
            flags.filterNot { it in listOf("-sS", "-O", "-f") }.let {
                if (it.contains("-sS")) it - "-sS" + "-sT" else it
            }
        } else {
            flags
        }

        // Create session
        val session = ScanSessionEntity(
            target = target,
            command = "nmap ${adjustedFlags.joinToString(" ")} $target",
            startTime = System.currentTimeMillis(),
            isRooted = rootStatus.isRooted,
            deviceArch = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
            scanProfile = profileName
        )

        val sessionId = insertSession(session)

        try {
            // Execute nmap
            val result = nmapExecutor.execute(target, adjustedFlags, rootStatus.isRooted, onProgressUpdate)

            if (result.exitCode == 0 && result.output.isNotEmpty()) {
                // Parse XML output
                val parseResult = xmlParser.parse(result.output, sessionId)

                // Save hosts and ports
                if (parseResult.hosts.isNotEmpty()) {
                    insertHosts(parseResult.hosts)
                    
                    parseResult.portsByHost.forEach { (hostId, ports) ->
                        if (ports.isNotEmpty()) {
                            insertPorts(ports)
                        }
                    }
                }

                // Complete session
                scanSessionDao.completeSession(
                    sessionId,
                    System.currentTimeMillis(),
                    true
                )

                return ScanExecutionResult.Success(sessionId, parseResult.hosts.size, parseResult.portsByHost.values.flatten().size)
            } else {
                return ScanExecutionResult.Error("Nmap execution failed: ${result.errorOutput}")
            }
        } catch (e: Exception) {
            return ScanExecutionResult.Error("Scan failed: ${e.message}")
        }
    }

    sealed class ScanExecutionResult {
        data class Success(val sessionId: Long, val hostCount: Int, val portCount: Int) : ScanExecutionResult()
        data class Error(val message: String) : ScanExecutionResult()
    }

    companion object {
        @Volatile
        private var instance: ScanRepository? = null

        fun getInstance(context: Context): ScanRepository {
            return instance ?: synchronized(this) {
                instance ?: ScanRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
