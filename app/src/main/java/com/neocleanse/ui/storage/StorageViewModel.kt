package com.neocleanse.ui.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neocleanse.domain.model.CategoryStats
import com.neocleanse.domain.model.StorageStats
import com.neocleanse.domain.repository.StorageRepository
import com.neocleanse.ui.home.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StorageViewModel @Inject constructor(
    storageRepository: StorageRepository
) : ViewModel() {

    val storageStats: StateFlow<UiState<StorageStats>> = storageRepository.getOverallStorageStats()
        .map { stats -> UiState.Success(stats) as UiState<StorageStats> }
        .catch { e -> emit(UiState.Error(e.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState.Loading
        )

    val categoryStats: StateFlow<UiState<List<CategoryStats>>> = storageRepository.getCategoryStats()
        .map { stats -> UiState.Success(stats.sortedByDescending { it.sizeBytes }) as UiState<List<CategoryStats>> }
        .catch { e -> emit(UiState.Error(e.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState.Loading
        )
}
