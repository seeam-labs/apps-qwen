package com.zendroid.nmapgui.data.database

import androidx.room.*
import com.zendroid.nmapgui.data.model.PortEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortDao {
    
    @Query("SELECT * FROM ports WHERE hostId = :hostId ORDER BY portNumber")
    fun getPortsByHostId(hostId: Long): Flow<List<PortEntity>>
    
    @Query("SELECT * FROM ports WHERE id = :portId")
    suspend fun getPortById(portId: Long): PortEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPort(port: PortEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPorts(ports: List<PortEntity>)
    
    @Update
    suspend fun updatePort(port: PortEntity)
    
    @Delete
    suspend fun deletePort(port: PortEntity)
    
    @Query("DELETE FROM ports WHERE hostId = :hostId")
    suspend fun deletePortsByHostId(hostId: Long)
    
    @Query("SELECT * FROM ports WHERE hostId = :hostId AND state = :state ORDER BY portNumber")
    fun getPortsByHostAndState(hostId: Long, state: String): Flow<List<PortEntity>>
    
    @Query("SELECT DISTINCT p.* FROM ports p INNER JOIN hosts h ON p.hostId = h.id WHERE h.sessionId = :sessionId AND p.portNumber = :portNumber")
    fun getPortsBySessionAndPortNumber(sessionId: Long, portNumber: Int): Flow<List<PortEntity>>
}
