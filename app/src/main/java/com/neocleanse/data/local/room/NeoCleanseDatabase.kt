package com.neocleanse.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ScanHistoryEntity::class], version = 1, exportSchema = false)
abstract class NeoCleanseDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao
}
