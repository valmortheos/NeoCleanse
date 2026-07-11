package com.neocleanse.domain.repository

import com.neocleanse.domain.model.CategoryStats
import com.neocleanse.domain.model.StorageStats
import kotlinx.coroutines.flow.Flow

interface StorageRepository {
    fun getOverallStorageStats(): Flow<StorageStats>
    fun getCategoryStats(): Flow<List<CategoryStats>>
}
