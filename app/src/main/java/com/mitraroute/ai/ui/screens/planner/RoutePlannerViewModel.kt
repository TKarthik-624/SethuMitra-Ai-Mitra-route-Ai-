package com.mitraroute.ai.ui.screens.planner

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mitraroute.ai.data.model.*
import com.mitraroute.ai.data.repository.MitraRouteRepository
import com.mitraroute.ai.util.LocationTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

data class RouteDetail(
    val name: String,
    val distanceText: String,
    val durationText: String,
    val riskLevel: String,
    val riskScore: Int,
    val weatherSummary: String,
    val warnings: List<String>,
    val polylinePoints: String,
    val colorHex: String
)

data class PlannerUiState(
    val fromQuery: String = "",
    val toQuery: String = "",
    val fromPredictions: List<PlacePrediction> = emptyList(),
    val toPredictions: List<PlacePrediction> = emptyList(),
    val selectedFrom: PlaceDetailResult? = null,
    val selectedTo: PlaceDetailResult? = null,
    val routes: List<RouteDetail> = emptyList(),
    val selectedRouteIndex: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val canCheckRoute: Boolean = false,
    val currentLatLng: LatLonLiteral? = null
)

class RoutePlannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = MitraRouteRepository.getInstance(application)
    private val locationTracker = LocationTracker(application)
    
    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState = _uiState.asStateFlow()

    private var fromSearchJob: Job? = null
    private var toSearchJob: Job? = null

    init {
        useCurrentLocationAsOrigin()
        startTracking()
    }

    private fun startTracking() {
        locationTracker.startLocationTracking { location ->
            _uiState.value = _uiState.value.copy(
                currentLatLng = LatLonLiteral(location.latitude, location.longitude)
            )
        }
    }

    fun updateFromQuery(query: String) {
        _uiState.value = _uiState.value.copy(fromQuery = query, selectedFrom = null)
        validateInputs()
        fromSearchJob?.cancel()
        if (query.length >= 2) {
            fromSearchJob = viewModelScope.launch {
                delay(500)
                val results = repo.geocoding.searchPlaces(query)
                _uiState.value = _uiState.value.copy(fromPredictions = results)
            }
        } else {
            _uiState.value = _uiState.value.copy(fromPredictions = emptyList())
        }
    }

    fun updateToQuery(query: String) {
        _uiState.value = _uiState.value.copy(toQuery = query, selectedTo = null)
        validateInputs()
        toSearchJob?.cancel()
        if (query.length >= 2) {
            toSearchJob = viewModelScope.launch {
                delay(500)
                val results = repo.geocoding.searchPlaces(query)
                _uiState.value = _uiState.value.copy(toPredictions = results)
            }
        } else {
            _uiState.value = _uiState.value.copy(toPredictions = emptyList())
        }
    }

    fun swapLocations() {
        val oldFrom = _uiState.value.selectedFrom
        val oldTo = _uiState.value.selectedTo
        val oldFromQuery = _uiState.value.fromQuery
        val oldToQuery = _uiState.value.toQuery

        _uiState.value = _uiState.value.copy(
            selectedFrom = oldTo,
            selectedTo = oldFrom,
            fromQuery = oldToQuery,
            toQuery = oldFromQuery,
            fromPredictions = emptyList(),
            toPredictions = emptyList()
        )
        validateInputs()
    }

    private fun validateInputs() {
        val state = _uiState.value
        _uiState.value = state.copy(
            canCheckRoute = state.selectedFrom != null && state.selectedTo != null
        )
    }

    fun useCurrentLocationAsOrigin() {
        val cached = _uiState.value.currentLatLng
        if (cached != null) {
            viewModelScope.launch {
                val address = repo.geocoding.reverseGeocode(cached.lat, cached.lng)
                val detail = PlaceDetailResult(
                    geometry = PlaceGeometry(cached),
                    formatted_address = address,
                    name = "Current Location"
                )
                _uiState.value = _uiState.value.copy(
                    selectedFrom = detail,
                    fromQuery = "Current Location",
                    fromPredictions = emptyList()
                )
                validateInputs()
            }
            return
        }

        viewModelScope.launch {
            val location = locationTracker.getCurrentLocation()
            if (location != null) {
                val address = repo.geocoding.reverseGeocode(location.latitude, location.longitude)
                val latLng = LatLonLiteral(location.latitude, location.longitude)
                val detail = PlaceDetailResult(
                    geometry = PlaceGeometry(latLng),
                    formatted_address = address,
                    name = "Current Location"
                )
                _uiState.value = _uiState.value.copy(
                    selectedFrom = detail,
                    fromQuery = "Current Location",
                    fromPredictions = emptyList(),
                    currentLatLng = latLng
                )
                validateInputs()
            }
        }
    }

    fun selectFrom(prediction: PlacePrediction) {
        // Predictions from Nominatim have Lat/Lon encoded in ID as "osm_id_lat_lon"
        val parts = prediction.place_id.split("_")
        if (parts.size >= 4) {
            val lat = parts[2].toDoubleOrNull() ?: 0.0
            val lon = parts[3].toDoubleOrNull() ?: 0.0
            val detail = PlaceDetailResult(
                geometry = PlaceGeometry(LatLonLiteral(lat, lon)),
                formatted_address = prediction.description,
                name = prediction.description.split(",").first()
            )
            _uiState.value = _uiState.value.copy(
                selectedFrom = detail,
                fromQuery = detail.name,
                fromPredictions = emptyList()
            )
            validateInputs()
        }
    }

    fun selectTo(prediction: PlacePrediction) {
        val parts = prediction.place_id.split("_")
        if (parts.size >= 4) {
            val lat = parts[2].toDoubleOrNull() ?: 0.0
            val lon = parts[3].toDoubleOrNull() ?: 0.0
            val detail = PlaceDetailResult(
                geometry = PlaceGeometry(LatLonLiteral(lat, lon)),
                formatted_address = prediction.description,
                name = prediction.description.split(",").first()
            )
            _uiState.value = _uiState.value.copy(
                selectedTo = detail,
                toQuery = detail.name,
                toPredictions = emptyList()
            )
            validateInputs()
        }
    }

    fun checkRoute() {
        val from = _uiState.value.selectedFrom?.geometry?.location ?: return
        val to = _uiState.value.selectedTo?.geometry?.location ?: return

        Log.d("PLANNER_VM", "Checking route from ${from.lat},${from.lng} to ${to.lat},${to.lng}")

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val routes = repo.routing.calculateRoute(from, to)
                Log.d("PLANNER_VM", "Routing service returned ${routes.size} routes")
                
                if (routes.isNotEmpty()) {
                    val enhanced = routes.mapIndexed { index, mr ->
                        // Fetch weather for dest
                        val weather = repo.weather.getCurrentWeather(to.lat, to.lng)
                        val weatherSum = if (weather != null) "${weather.current?.temperature_2m?.toInt()}°C, ${getCondition(weather.current?.weather_code)}" else "Weather unavailable"
                        
                        val distKm = mr.distanceMeters / 1000.0
                        val realisticSeconds = (mr.distanceMeters / 1000.0) / (55.0 / 3600.0)
                        
                        val weatherRisk = if (weather?.current?.weather_code ?: 0 >= 61) 40 else 0
                        val distanceRisk = if (distKm > 300) 20 else 0
                        val totalRisk = (20 + weatherRisk + distanceRisk).coerceIn(10, 100)
                        val level = when {
                            totalRisk >= 70 -> "CRITICAL"
                            totalRisk >= 50 -> "HIGH"
                            totalRisk >= 30 -> "MODERATE"
                            else -> "LOW"
                        }
                        
                        RouteDetail(
                            name = if (index == 0) "Recommended Route" else "Alternative ${index}",
                            distanceText = "%.1f km".format(distKm),
                            durationText = formatDuration(realisticSeconds),
                            riskLevel = level, 
                            riskScore = totalRisk,
                            weatherSummary = weatherSum,
                            warnings = if (weatherRisk > 0) listOf("Caution: Rain reported at destination.") else listOf("Road conditions standard."),
                            polylinePoints = mr.polyline.encodedPolyline,
                            colorHex = if (index == 0) "#06B6D4" else "#10B981"
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        routes = enhanced,
                        selectedRouteIndex = 0,
                        isLoading = false
                    )

                    // Save the recommended route for AI analysis
                    routes.firstOrNull()?.let { 
                        val firstEnhanced = enhanced.first()
                        repo.setLatestPlannedRoute(
                            it, 
                            _uiState.value.selectedFrom?.name ?: "Origin",
                            _uiState.value.selectedTo?.name ?: "Destination",
                            firstEnhanced.weatherSummary, 
                            firstEnhanced.riskLevel, 
                            firstEnhanced.riskScore,
                            from,
                            to
                        ) 
                    }
                    Log.d("PLANNER_VM", "UI State updated with routes and saved to repo")
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "No road route found between these points.")
                }
            } catch (e: Exception) {
                Log.e("PLANNER_VM", "Routing process failed", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Route calculation failed. Check internet.")
            }
        }
    }

    private fun formatDuration(seconds: Double): String {
        val hrs = (seconds / 3600).toInt()
        val mins = ((seconds % 3600) / 60).toInt()
        return if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
    }

    private fun getCondition(code: Int?): String = when (code) {
        0 -> "Clear"
        1, 2, 3 -> "Partly Cloudy"
        61, 63, 65 -> "Rainy"
        95, 96, 99 -> "Stormy"
        else -> "Cloudy"
    }

    fun selectRoute(index: Int) {
        _uiState.value = _uiState.value.copy(selectedRouteIndex = index)
    }

    override fun onCleared() {
        super.onCleared()
        locationTracker.stopLocationTracking()
    }
}
