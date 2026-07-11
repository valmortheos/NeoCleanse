package com.neocleanse.data.repository

import android.app.usage.StorageStatsManager
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.neocleanse.domain.model.CategoryStats
import com.neocleanse.domain.model.StorageCategory
import com.neocleanse.domain.model.StorageStats
import com.neocleanse.domain.repository.StorageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class StorageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : StorageRepository {

    override fun getOverallStorageStats(): Flow<StorageStats> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }

        context.contentResolver.registerContentObserver(MediaStore.Files.getContentUri("external"), true, observer)
        trySend(Unit)

        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }.conflate()
     .map { fetchOverallStorageStats() }
     .flowOn(Dispatchers.IO)

    private fun fetchOverallStorageStats(): StorageStats {
        val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

        var totalBytes = 0L
        var freeBytes = 0L

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val storageVolumes = storageManager.storageVolumes
            for (volume in storageVolumes) {
                if (volume.isPrimary) {
                    val uuid = StorageManager.UUID_DEFAULT
                    try {
                        totalBytes = storageStatsManager.getTotalBytes(uuid)
                        freeBytes = storageStatsManager.getFreeBytes(uuid)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        val statFs = StatFs(Environment.getExternalStorageDirectory().path)
                        totalBytes = statFs.totalBytes
                        freeBytes = statFs.availableBytes
                    }
                    break
                }
            }
        } else {
             val statFs = StatFs(Environment.getExternalStorageDirectory().path)
             totalBytes = statFs.totalBytes
             freeBytes = statFs.availableBytes
        }

        if (totalBytes == 0L) {
             val statFs = StatFs(Environment.getExternalStorageDirectory().path)
             totalBytes = statFs.totalBytes
             freeBytes = statFs.availableBytes
        }

        val usedBytes = totalBytes - freeBytes
        return StorageStats(totalBytes, usedBytes, freeBytes)
    }

    override fun getCategoryStats(): Flow<List<CategoryStats>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }

        context.contentResolver.registerContentObserver(MediaStore.Files.getContentUri("external"), true, observer)
        trySend(Unit)

        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }.conflate()
     .map { fetchCategoryStats() }
     .flowOn(Dispatchers.IO)

    private fun fetchCategoryStats(): List<CategoryStats> {
        val categorySizes = mutableMapOf<StorageCategory, Long>()
        StorageCategory.values().forEach { categorySizes[it] = 0L }

        categorySizes[StorageCategory.IMAGES] = queryMediaStoreSize(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        categorySizes[StorageCategory.VIDEOS] = queryMediaStoreSize(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        categorySizes[StorageCategory.AUDIO] = queryMediaStoreSize(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)

        val filesUri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.MIME_TYPE, MediaStore.Files.FileColumns.DATA)

        try {
            context.contentResolver.query(filesUri, projection, null, null, null)?.use { cursor ->
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

                while (cursor.moveToNext()) {
                    val size = cursor.getLong(sizeColumn)
                    val mimeType = cursor.getString(mimeTypeColumn)
                    val data = cursor.getString(dataColumn) ?: ""

                    if (size <= 0) continue

                    val extension = MimeTypeMap.getFileExtensionFromUrl(data).lowercase()

                    when {
                        mimeType?.startsWith("image/") == true -> { /* already counted */ }
                        mimeType?.startsWith("video/") == true -> { /* already counted */ }
                        mimeType?.startsWith("audio/") == true -> { /* already counted */ }
                        mimeType?.startsWith("application/pdf") == true || mimeType?.startsWith("text/") == true || extension in listOf("doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt") -> {
                            categorySizes[StorageCategory.DOCUMENTS] = categorySizes[StorageCategory.DOCUMENTS]!! + size
                        }
                        extension == "apk" || mimeType == "application/vnd.android.package-archive" -> {
                            categorySizes[StorageCategory.APKS] = categorySizes[StorageCategory.APKS]!! + size
                        }
                        extension in listOf("zip", "rar", "7z", "tar", "gz") -> {
                            categorySizes[StorageCategory.ARCHIVES] = categorySizes[StorageCategory.ARCHIVES]!! + size
                        }
                        data.contains("/Download/") || data.contains("/Downloads/") -> {
                            categorySizes[StorageCategory.DOWNLOADS] = categorySizes[StorageCategory.DOWNLOADS]!! + size
                        }
                        else -> {
                            categorySizes[StorageCategory.OTHERS] = categorySizes[StorageCategory.OTHERS]!! + size
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return categorySizes.map { CategoryStats(it.key, it.value) }.filter { it.sizeBytes > 0 }
    }

    private fun queryMediaStoreSize(uri: Uri): Long {
        var totalSize = 0L
        val projection = arrayOf("sum(${MediaStore.MediaColumns.SIZE})")
        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    totalSize = cursor.getLong(0)
                }
            }
        } catch (e: Exception) {
             try {
                 val fallbackProjection = arrayOf(MediaStore.MediaColumns.SIZE)
                 context.contentResolver.query(uri, fallbackProjection, null, null, null)?.use { cursor ->
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    while (cursor.moveToNext()) {
                        totalSize += cursor.getLong(sizeColumn)
                    }
                 }
             } catch (fallbackEx: Exception) {
                 fallbackEx.printStackTrace()
             }
        }
        return totalSize
    }
}
