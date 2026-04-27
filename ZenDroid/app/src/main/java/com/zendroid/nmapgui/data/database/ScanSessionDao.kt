package com.zendroid.nmapgui.data.database

import androidx.room.*
import com.zendroid.nmapgui.data.model.ScanSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanSessionDao {
    
    @Query("SELECT * FROM scan_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ScanSessionEntity>>
    
    @Query("SELECT * FROM scan_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): ScanSessionEntity?
    
    @Query("SELECT * FROM scan_sessions WHERE id = :sessionId")
    fun getSessionByIdFlow(sessionId: Long): Flow<ScanSessionEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ScanSessionEntity): Long
    
    @Update
    suspend fun updateSession(session: ScanSessionEntity)
    
    @Delete
    suspend fun deleteSession(session: ScanSessionEntity)
    
    @Query("DELETE FROM scan_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)
    
    @Query("SELECT * FROM scan_sessions WHERE target LIKE :query OR notes LIKE :query ORDER BY startTime DESC")
    fun searchSessions(query: String): Flow<List<ScanSessionEntity>>
    
    @Query("UPDATE scan_sessions SET endTime = :endTime, isCompleted = :isCompleted WHERE id = :sessionId")
    suspend fun completeSession(sessionId: Long, endTime: Long, isCompleted: Boolean = true)
}
