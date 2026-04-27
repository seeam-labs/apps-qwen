package com.zendroid.nmapgui.data.database

import androidx.room.*
import com.zendroid.nmapgui.data.model.HostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HostDao {
    
    @Query("SELECT * FROM hosts WHERE sessionId = :sessionId ORDER BY ipAddress")
    fun getHostsBySessionId(sessionId: Long): Flow<List<HostEntity>>
    
    @Query("SELECT * FROM hosts WHERE id = :hostId")
    suspend fun getHostById(hostId: Long): HostEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHost(host: HostEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHosts(hosts: List<HostEntity>)
    
    @Update
    suspend fun updateHost(host: HostEntity)
    
    @Delete
    suspend fun deleteHost(host: HostEntity)
    
    @Query("DELETE FROM hosts WHERE sessionId = :sessionId")
    suspend fun deleteHostsBySessionId(sessionId: Long)
    
    @Query("SELECT * FROM hosts WHERE sessionId = :sessionId AND ipAddress LIKE :query ORDER BY ipAddress")
    fun searchHostsBySession(sessionId: Long, query: String): Flow<List<HostEntity>>
}
