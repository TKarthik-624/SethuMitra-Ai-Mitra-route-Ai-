package com.mitraroute.ai.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mitraroute.ai.data.model.DashboardStats
import com.mitraroute.ai.data.model.Incident
import com.mitraroute.ai.data.repository.SetuMitraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val stats: DashboardStats = DashboardStats(),
    val incidents: List<Incident> = emptyList(),
    val isLoading: Boolean = false,
    val pendingSync: Int = 0
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SetuMitraRepository.getInstance(application)
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val stats = repo.getDashboardStats()
            val pending = repo.getPendingOfflineCount()
            _uiState.value = _uiState.value.copy(
                stats = stats, isLoading = false, pendingSync = pending
            )
        }
        viewModelScope.launch {
            try {
                repo.getIncidents().collect { incidents ->
                    _uiState.value = _uiState.value.copy(incidents = incidents)
                }
            } catch (_: Exception) {}
        }
    }
}
