package com.zendroid.nmapgui.data.database

import androidx.room.*
import com.zendroid.nmapgui.data.model.ScanProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanProfileDao {
    
    @Query("SELECT * FROM scan_profiles ORDER BY name")
    fun getAllProfiles(): Flow<List<ScanProfileEntity>>
    
    @Query("SELECT * FROM scan_profiles WHERE id = :profileId")
    suspend fun getProfileById(profileId: Long): ScanProfileEntity?
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProfile(profile: ScanProfileEntity): Long
    
    @Update
    suspend fun updateProfile(profile: ScanProfileEntity)
    
    @Delete
    suspend fun deleteProfile(profile: ScanProfileEntity)
    
    @Query("DELETE FROM scan_profiles WHERE isSystemProfile = 0 AND id = :profileId")
    suspend fun deleteUserProfile(profileId: Long)
}
