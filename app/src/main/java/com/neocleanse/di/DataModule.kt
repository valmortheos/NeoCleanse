package com.neocleanse.di

import android.content.Context
import androidx.room.Room
import com.neocleanse.data.local.datastore.SettingsManager
import com.neocleanse.data.local.room.NeoCleanseDatabase
import com.neocleanse.data.local.room.ScanHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NeoCleanseDatabase {
        return Room.databaseBuilder(
            context,
            NeoCleanseDatabase::class.java,
            "neocleanse.db"
        ).build()
    }

    @Provides
    fun provideScanHistoryDao(database: NeoCleanseDatabase): ScanHistoryDao {
        return database.scanHistoryDao()
    }

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }
}
