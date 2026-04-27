package com.zendroid.nmapgui.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.TypeConverters
import com.zendroid.nmapgui.data.database.Converters

@Entity(tableName = "scan_sessions")
data class ScanSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val target: String,
    val command: String,
    val startTime: Long,
    val endTime: Long? = null,
    val isCompleted: Boolean = false,
    val isRooted: Boolean = false,
    val deviceArch: String,
    val scanProfile: String = "Custom",
    val notes: String = ""
)

@Entity(
    tableName = "hosts",
    foreignKeys = [
        ForeignKey(
            entity = ScanSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class HostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val ipAddress: String,
    val macAddress: String? = null,
    val hostname: String? = null,
    val osName: String? = null,
    val osAccuracy: Int? = null,
    val hopCount: Int? = null,
    val status: String = "unknown",
    val latency: Long? = null
)

@Entity(
    tableName = "ports",
    foreignKeys = [
        ForeignKey(
            entity = HostEntity::class,
            parentColumns = ["id"],
            childColumns = ["hostId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["hostId"])]
)
@TypeConverters(Converters::class)
data class PortEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hostId: Long,
    val portNumber: Int,
    val protocol: String,
    val state: String,
    val serviceName: String? = null,
    val serviceVersion: String? = null,
    val serviceProduct: String? = null,
    val scriptOutput: Map<String, String>? = null,
    val cpe: List<String>? = null
)

@Entity(tableName = "scan_profiles")
data class ScanProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val flags: String,
    val isSystemProfile: Boolean = false,
    val requiresRoot: Boolean = false
)
