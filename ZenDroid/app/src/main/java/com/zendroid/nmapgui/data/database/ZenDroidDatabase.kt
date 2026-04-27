package com.zendroid.nmapgui.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zendroid.nmapgui.data.model.ScanSessionEntity
import com.zendroid.nmapgui.data.model.HostEntity
import com.zendroid.nmapgui.data.model.PortEntity
import com.zendroid.nmapgui.data.model.ScanProfileEntity

@Database(
    entities = [
        ScanSessionEntity::class,
        HostEntity::class,
        PortEntity::class,
        ScanProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ZenDroidDatabase : RoomDatabase() {
    
    abstract fun scanSessionDao(): ScanSessionDao
    abstract fun hostDao(): HostDao
    abstract fun portDao(): PortDao
    abstract fun scanProfileDao(): ScanProfileDao
    
    companion object {
        const val DATABASE_NAME = "zendroid_database"
    }
}
