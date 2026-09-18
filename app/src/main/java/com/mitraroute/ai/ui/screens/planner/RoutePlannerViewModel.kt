package com.mitraroute.ai.ui.screens.planner

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.mitraroute.ai.data.model.*
import com.mitraroute.ai.data.repository.SetuMitraRepository
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
    val colorHex: String,
    val steps: List<ModernStep> = emptyList()
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
    val currentLatLng: LatLonLiteral? = null,
    val currentBearing: Float = 0f,
    val navigationState: NavigationState = NavigationState.IDLE,
    val currentStepIndex: Int = 0,
    val remainingDistance: String = "",
    val remainingDuration: String = "",
    val nextInstruction: String = ""
)

enum class NavigationState {
    IDLE, ROUTE_READY, NAVIGATING, ARRIVED, ERROR
}

class RoutePlannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SetuMitraRepository.getInstance(application)
    private val locationTracker = LocationTracker(application)
    
    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState = _uiState.asStateFlow()

    private var fromSearchJob: Job? = null
    private var toSearchJob: Job? = null

    private var lastRecalculationTime = 0L

    init {
        useCurrentLocationAsOrigin()
        observeLocationUpdates()
        locationTracker.startLocationTracking { /* init tracking */ }
    }

    private fun observeLocationUpdates() {
        viewModelScope.launch {
            locationTracker.locationFlow.collect { location ->
                if (location != null) {
                    val latLng = LatLonLiteral(location.latitude, location.longitude)
                    _uiState.value = _uiState.value.copy(
                        currentLatLng = latLng,
                        currentBearing = location.bearing
                    )
                    
                    if (_uiState.value.navigationState == NavigationState.NAVIGATING) {
                        processNavigationUpdate(location)
                    }
                }
            }
        }
    }

    private fun startTracking() {
        locationTracker.startLocationTracking { /* handled via flow */ }
    }

    fun updateFromQuery(query: String) {
        _uiState.value = _uiState.value.copy(fromQuery = query, selectedFrom = null, error = null)
        validateInputs()
        fromSearchJob?.cancel()
        if (query.isNotEmpty() && query != "Current Location" && query.length >= 2) {
            fromSearchJob = viewModelScope.launch {
                delay(300)
                try {
                    val results = repo.searchPlaces(query, isFrom = true)
                    _uiState.value = _uiState.value.copy(fromPredictions = results)
                } catch (e: Exception) {
                    Log.e("PLANNER_VM", "Search FROM failed: ${e.message}")
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(fromPredictions = emptyList())
        }
    }

    fun updateToQuery(query: String) {
        _uiState.value = _uiState.value.copy(toQuery = query, selectedTo = null, error = null)
        validateInputs()
        toSearchJob?.cancel()
        if (query.isNotEmpty() && query.length >= 2) {
            toSearchJob = viewModelScope.launch {
                delay(300)
                try {
                    val results = repo.searchPlaces(query, isFrom = false)
                    _uiState.value = _uiState.value.copy(toPredictions = results)
                } catch (e: Exception) {
                    Log.e("PLANNER_VM", "Search TO failed: ${e.message}")
                }
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
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, fromPredictions = emptyList(), error = null)
            try {
                val detail = repo.getPlaceDetails(prediction.place_id, isFrom = true)
                if (detail != null) {
                    _uiState.value = _uiState.value.copy(
                        selectedFrom = detail,
                        fromQuery = detail.name,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Could not resolve start location.")
                }
            } catch (e: Exception) {
                Log.e("PLANNER_VM", "Select FROM failed: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Location selection failed.")
            }
            validateInputs()
        }
    }

    fun selectTo(prediction: PlacePrediction) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, toPredictions = emptyList(), error = null)
            try {
                val detail = repo.getPlaceDetails(prediction.place_id, isFrom = false)
                if (detail != null) {
                    _uiState.value = _uiState.value.copy(
                        selectedTo = detail,
                        toQuery = detail.name,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Could not resolve destination.")
                }
            } catch (e: Exception) {
                Log.e("PLANNER_VM", "Select TO failed: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Location selection failed.")
            }
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
                            colorHex = if (index == 0) "#06B6D4" else "#10B981",
                            steps = mr.legs.flatMap { it.steps }
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        routes = enhanced,
                        selectedRouteIndex = 0,
                        isLoading = false,
                        navigationState = NavigationState.ROUTE_READY
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

    fun startNavigation() {
        val state = _uiState.value
        if (state.routes.isNotEmpty()) {
            _uiState.value = state.copy(
                navigationState = NavigationState.NAVIGATING,
                currentStepIndex = 0
            )
            locationTracker.startLocationTracking { /* flow takes over */ }
        }
    }

    fun stopNavigation() {
        _uiState.value = _uiState.value.copy(navigationState = NavigationState.ROUTE_READY)
        // Keep tracking if we want live map, but stop nav processing
    }

    private fun processNavigationUpdate(location: Location) {
        val state = _uiState.value
        val currentRoute = state.routes.getOrNull(state.selectedRouteIndex) ?: return
        
        // 1. Off-route detection
        val points = LocationTracker.decodePolyline(currentRoute.polylinePoints)
        val nearestDistance = findNearestDistance(location, points)
        
        val now = System.currentTimeMillis()
        if (nearestDistance > 200 && now - lastRecalculationTime > 15000) { 
            Log.d("PLANNER_VM", "Off-route detected ($nearestDistance m). Recalculating...")
            lastRecalculationTime = now
            checkRoute() 
            return
        }

        // 2. Progress and Instructions
        // Simple logic: find nearest step
        val steps = currentRoute.steps
        if (steps.isNotEmpty()) {
            // Find step user is currently on (simplification)
            val currentLatLng = LatLonLiteral(location.latitude, location.longitude)
            val nearestStepIndex = findNearestStepIndex(currentLatLng, steps)
            
            val nextStep = steps.getOrNull(nearestStepIndex + 1)
            val instruction = nextStep?.navigationInstruction?.instructions ?: "Continue to destination"
            
            // 3. Arrival check
            val destination = state.selectedTo?.geometry?.location
            if (destination != null) {
                val distToDest = calculatePhysicalDistance(location.latitude, location.longitude, destination.lat, destination.lng)
                if (distToDest < 50) { // 50 meters
                    _uiState.value = _uiState.value.copy(navigationState = NavigationState.ARRIVED)
                    return
                }
            }

            // 4. Update Estimates
            val remainingSteps = steps.drop(nearestStepIndex)
            val remDistMeters = remainingSteps.sumOf { it.distanceMeters ?: 0 }
            val remSeconds = remDistMeters / (55.0 / 3.6)

            _uiState.value = _uiState.value.copy(
                currentStepIndex = nearestStepIndex,
                nextInstruction = instruction,
                remainingDistance = "%.1f km".format(remDistMeters / 1000.0),
                remainingDuration = formatDuration(remSeconds)
            )
        }
    }

    private fun findNearestDistance(location: Location, points: List<LatLng>): Double {
        var minDistance = Double.MAX_VALUE
        for (point in points) {
            val dist = calculatePhysicalDistance(location.latitude, location.longitude, point.latitude, point.longitude)
            if (dist < minDistance) minDistance = dist
        }
        return minDistance
    }

    private fun findNearestStepIndex(current: LatLonLiteral, steps: List<ModernStep>): Int {
        var nearestIdx = 0
        var minDistance = Double.MAX_VALUE
        steps.forEachIndexed { index, step ->
            val start = step.startLocation?.latLng ?: return@forEachIndexed
            val dist = calculatePhysicalDistance(current.lat, current.lng, start.lat, start.lng)
            if (dist < minDistance) {
                minDistance = dist
                nearestIdx = index
            }
        }
        return nearestIdx
    }

    private fun calculatePhysicalDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3 // metres
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                Math.cos(phi1) * Math.cos(phi2) *
                Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return r * c
    }

    override fun onCleared() {
        super.onCleared()
        locationTracker.stopLocationTracking()
    }
}
