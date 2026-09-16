package com.mitraroute.ai.ui.screens.weather

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mitraroute.ai.data.model.LatLonLiteral
import com.mitraroute.ai.data.model.OpenMeteoResponse
import com.mitraroute.ai.data.repository.SetuMitraRepository
import com.mitraroute.ai.util.LocationTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WeatherUiState(
    val weatherData: OpenMeteoResponse? = null,
    val userLocation: LatLonLiteral? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SetuMitraRepository.getInstance(application)
    private val locationTracker = LocationTracker(application)
    
    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadWeather()
        startTracking()
    }

    private fun startTracking() {
        locationTracker.startLocationTracking { location ->
            _uiState.value = _uiState.value.copy(
                userLocation = LatLonLiteral(location.latitude, location.longitude)
            )
        }
    }

    fun loadWeather() {
        val cached = _uiState.value.userLocation
        if (cached != null) {
            fetchWeatherData(cached.lat, cached.lng)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val location = locationTracker.getCurrentLocation()
            
            if (location != null) {
                fetchWeatherData(location.latitude, location.longitude)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "GPS Location unavailable. Please enable Location."
                )
            }
        }
    }

    private fun fetchWeatherData(lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val data = repo.weather.getCurrentWeather(lat, lon)
                if (data != null) {
                    _uiState.value = _uiState.value.copy(
                        weatherData = data,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Weather data unavailable."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Unable to fetch weather. Check connection."
                )
            }
        }
    }
}
