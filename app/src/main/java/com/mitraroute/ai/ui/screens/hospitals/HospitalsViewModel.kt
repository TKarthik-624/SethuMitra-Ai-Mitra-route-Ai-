package com.mitraroute.ai.ui.screens.hospitals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mitraroute.ai.data.model.LatLonLiteral
import com.mitraroute.ai.data.model.NearbyPlaceResult
import com.mitraroute.ai.data.repository.SetuMitraRepository
import com.mitraroute.ai.util.LocationTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HospitalsUiState(
    val hospitals: List<NearbyPlaceResult> = emptyList(),
    val userLocation: LatLonLiteral? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class HospitalsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SetuMitraRepository.getInstance(application)
    private val locationTracker = LocationTracker(application)
    
    private val _uiState = MutableStateFlow(HospitalsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadHospitals()
        startTracking()
    }

    private fun startTracking() {
        locationTracker.startLocationTracking { location ->
            val firstLocation = _uiState.value.userLocation == null
            _uiState.value = _uiState.value.copy(
                userLocation = LatLonLiteral(location.latitude, location.longitude)
            )
            if (firstLocation) {
                fetchHospitals(location.latitude, location.longitude)
            }
        }
    }

    fun loadHospitals() {
        val cached = _uiState.value.userLocation
        if (cached != null) {
            fetchHospitals(cached.lat, cached.lng)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val location = locationTracker.getCurrentLocation()
            if (location != null) {
                fetchHospitals(location.latitude, location.longitude)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Unable to get current location. Enable GPS.")
            }
        }
    }

    private fun fetchHospitals(lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val results = repo.hospitals.getNearbyHospitals(lat, lon)
                if (results.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        hospitals = results.sortedBy { calculateDistance(lat, lon, it.geometry.location.lat, it.geometry.location.lng) },
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "No nearby hospitals found.")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Hospital search unavailable. Check connection.")
            }
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
