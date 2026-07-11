package com.neocleanse.domain.model

data class StorageStats(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long
)

enum class StorageCategory {
    IMAGES, VIDEOS, AUDIO, DOCUMENTS, DOWNLOADS, APKS, ARCHIVES, OTHERS, SYSTEM
}

data class CategoryStats(
    val category: StorageCategory,
    val sizeBytes: Long
)
