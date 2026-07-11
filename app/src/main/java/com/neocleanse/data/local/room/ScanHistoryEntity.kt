package com.neocleanse.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val scanType: String, // "image", "video", "file"
    val timestamp: Long,
    val itemsFound: Int,
    val spaceSaved: Long
)
