package com.mitraroute.ai.ui.screens.report

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mitraroute.ai.data.demo.DemoData
import com.mitraroute.ai.data.model.Incident
import com.mitraroute.ai.data.model.IncidentCreate
import com.mitraroute.ai.data.model.Route
import com.mitraroute.ai.data.repository.MitraRouteRepository
import com.mitraroute.ai.util.LocationTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

data class ReportUiState(
    val incidentType: String = "road_damage",
    val severity: Int = 3,
    val description: String = "",
    val lat: Double = 26.14,
    val lon: Double = 91.74,
    val routeId: Int? = null,
    val routes: List<Route> = emptyList(),
    val isSubmitting: Boolean = false,
    val resultMessage: String? = null,
    val resultSuccess: Boolean = false,
    val mapPinLat: Double? = null,
    val mapPinLon: Double? = null,
    val address: String = "",
    val submittedIncidents: List<Incident> = emptyList()
)

class ReportViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = MitraRouteRepository.getInstance(application)
    private val locationTracker = LocationTracker(application)
    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadRoutes()
        useCurrentLocation()
        loadSubmittedIncidents()
    }

    private fun loadSubmittedIncidents() {
        viewModelScope.launch {
            repo.getIncidents().collect { list ->
                _uiState.value = _uiState.value.copy(submittedIncidents = list)
            }
        }
    }

    fun useCurrentLocation() {
        viewModelScope.launch {
            val location = locationTracker.getCurrentLocation()
            if (location != null) {
                updateLocation(location.latitude, location.longitude)
            }
        }
    }

    private fun loadRoutes() {
        viewModelScope.launch {
            try {
                repo.getRoutes().collect { routes ->
                    _uiState.value = _uiState.value.copy(routes = routes)
                }
            } catch (_: Exception) {}
        }
    }

    fun updateIncidentType(type: String) {
        _uiState.value = _uiState.value.copy(incidentType = type, resultMessage = null)
    }

    fun updateSeverity(severity: Int) {
        _uiState.value = _uiState.value.copy(severity = severity, resultMessage = null)
    }

    fun updateDescription(desc: String) {
        _uiState.value = _uiState.value.copy(description = desc, resultMessage = null)
    }

    fun updateLocation(lat: Double, lon: Double) {
        viewModelScope.launch {
            val addr = repo.geocoding.reverseGeocode(lat, lon)
            _uiState.value = _uiState.value.copy(
                lat = lat, lon = lon,
                mapPinLat = lat, mapPinLon = lon,
                address = addr,
                resultMessage = null
            )
        }
    }

    fun updateRoute(routeId: Int?) {
        _uiState.value = _uiState.value.copy(routeId = routeId, resultMessage = null)
    }

    fun submitReport() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, resultMessage = null)
            val incident = IncidentCreate(
                route_id = state.routeId,
                incident_type = state.incidentType,
                severity = state.severity,
                description = state.description.ifBlank { null },
                lat = state.lat,
                lon = state.lon,
                client_timestamp = Instant.now().toString()
            )
            val result = repo.reportIncident(incident)
            result.fold(
                onSuccess = { response ->
                    val msg = response["message"] as? String
                        ?: response["error"] as? String
                        ?: "Report submitted"
                    val isOff = msg.contains("offline", ignoreCase = true)
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        resultMessage = msg,
                        resultSuccess = !isOff && response["error"] == null
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        resultMessage = "Error: ${e.localizedMessage}",
                        resultSuccess = false
                    )
                }
            )
        }
    }
}
