package com.mitraroute.ai.ui.screens.command

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mitraroute.ai.data.model.Incident
import com.mitraroute.ai.data.model.OpenMeteoResponse
import com.mitraroute.ai.data.repository.SetuMitraRepository
import com.mitraroute.ai.util.LocationTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class CommandUiState(
    val locationName: String = "Getting location...",
    val area: String = "",
    val country: String = "",
    val lastUpdated: String = "",
    val activeIncidents: List<Incident> = emptyList(),
    val currentWeather: OpenMeteoResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class CommandDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SetuMitraRepository.getInstance(application)
    private val locationTracker = LocationTracker(application)
    
    private val _uiState = MutableStateFlow(CommandUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refreshLocation()
        loadIncidents()
        startTracking()
    }

    private fun startTracking() {
        locationTracker.startLocationTracking { location ->
            updateLocationData(location)
        }
    }

    fun refreshLocation() {
        Log.d("COMMAND_VM", "Refresh location triggered")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val location = locationTracker.getCurrentLocation()
            if (location != null) {
                updateLocationData(location)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Unable to determine current location. Turn on Location and try again."
                )
            }
        }
    }

    private fun updateLocationData(location: Location) {
        viewModelScope.launch {
            // Use free reverse geocoding
            val fullAddress = repo.geocoding.reverseGeocode(location.latitude, location.longitude)
            
            // Parse address components (simple comma split)
            val parts = fullAddress.split(",")
            val name = parts.firstOrNull()?.trim() ?: "Current Location"
            val area = if (parts.size > 1) parts[1].trim() else ""
            val country = parts.lastOrNull()?.trim() ?: ""
            
            val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

            // Fetch free weather
            val weather = repo.weather.getCurrentWeather(location.latitude, location.longitude)

            _uiState.value = _uiState.value.copy(
                locationName = name,
                area = area,
                country = country,
                lastUpdated = time,
                currentWeather = weather,
                isLoading = false
            )
            Log.d("COMMAND_VM", "Live tracking updated: $name")
        }
    }

    private fun loadIncidents() {
        viewModelScope.launch {
            repo.getIncidents().collect { incidents ->
                _uiState.value = _uiState.value.copy(activeIncidents = incidents)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationTracker.stopLocationTracking()
    }
}
