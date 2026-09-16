package com.mitraroute.ai.ui.screens.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mitraroute.ai.data.demo.DemoData
import com.mitraroute.ai.data.model.*
import com.mitraroute.ai.data.repository.MitraRouteRepository
import com.mitraroute.ai.util.LocationTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MapUiState(
    val routes: List<Route> = emptyList(),
    val incidents: List<Incident> = emptyList(),
    val selectedRoute: Route? = null,
    val activeModernRoute: ModernRoute? = null,
    val liveWeather: OpenMeteoResponse? = null,
    val riskScore: Int = 0,
    val riskLevel: String = "low",
    val riskFactors: List<String> = emptyList(),
    val userLocation: LatLonLiteral? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = MitraRouteRepository.getInstance(application)
    private val locationTracker = LocationTracker(application)
    
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadInternalData()
        startTracking()
    }

    private fun startTracking() {
        locationTracker.startLocationTracking { location ->
            _uiState.value = _uiState.value.copy(
                userLocation = LatLonLiteral(location.latitude, location.longitude)
            )
        }
    }

    private fun loadInternalData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repo.getRoutes().collect { routes ->
                    val finalRoutes = routes.ifEmpty {
                        listOf(DemoData.primaryRoute, DemoData.alternateRoute)
                    }
                    _uiState.value = _uiState.value.copy(routes = finalRoutes, isLoading = false)
                }
            } catch (e: Exception) {
                val fallback = listOf(DemoData.primaryRoute, DemoData.alternateRoute)
                _uiState.value = _uiState.value.copy(routes = fallback, isLoading = false)
            }
        }
        viewModelScope.launch {
            try {
                repo.getIncidents().collect { incidents ->
                    val finalIncidents = incidents.ifEmpty {
                        listOf(DemoData.initialLandslideIncident)
                    }
                    _uiState.value = _uiState.value.copy(incidents = finalIncidents)
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(incidents = listOf(DemoData.initialLandslideIncident))
            }
        }
    }

    fun selectRoute(route: Route) {
        _uiState.value = _uiState.value.copy(selectedRoute = route, isLoading = true)
        viewModelScope.launch {
            val from = LatLonLiteral(route.start_lat, route.start_lon)
            val to = LatLonLiteral(route.end_lat, route.end_lon)
            
            val routes = repo.routing.calculateRoute(from, to)
            val weather = repo.weather.getCurrentWeather(route.end_lat, route.end_lon)
            
            val factors = mutableListOf<String>()
            var score = 20
            
            if (weather?.current?.weather_code ?: 0 >= 61) {
                factors.add("Weather Hazard: Active Precipitation")
                score += 30
            }
            
            val level = when {
                score >= 70 -> "critical"
                score >= 50 -> "high"
                score >= 30 -> "medium"
                else -> "low"
            }

            _uiState.value = _uiState.value.copy(
                activeModernRoute = routes.firstOrNull(),
                liveWeather = weather,
                riskScore = score,
                riskLevel = level,
                riskFactors = factors,
                isLoading = false
            )
        }
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedRoute = null, 
            activeModernRoute = null,
            liveWeather = null,
            riskFactors = emptyList()
        )
    }

    override fun onCleared() {
        super.onCleared()
        locationTracker.stopLocationTracking()
    }
}
