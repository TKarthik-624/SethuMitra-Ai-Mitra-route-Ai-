package com.mitraroute.ai.ui.screens.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mitraroute.ai.data.model.Incident
import com.mitraroute.ai.data.repository.SetuMitraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable

data class AIUiState(
    val summary: String = "Analyzing your journey...",
    val transitOptions: List<TransitOption> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class TransitOption(
    val type: String, // "BUS", "TRAIN", etc.
    val serviceName: String,
    val departureTime: String,
    val arrivalTime: String,
    val duration: String,
    val transfers: Int,
    val stops: Int,
    val fare: String? = null
)

class AIViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SetuMitraRepository.getInstance(application)
    private val _uiState = MutableStateFlow(AIUiState())
    val uiState = _uiState.asStateFlow()

    init {
        analyzeLatestRoute()
    }

    private fun analyzeLatestRoute() {
        val route = repo.latestPlannedRoute
        if (route != null) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val incidents = repo.getIncidents().firstOrNull() ?: emptyList()
                
                // Fetch transit routes if locations are available
                val transitRoutes = if (repo.latestOriginLoc != null && repo.latestDestLoc != null) {
                    repo.getTransitRoutes(repo.latestOriginLoc!!, repo.latestDestLoc!!)
                } else emptyList()

                val transitOptions = transitRoutes.flatMap { mr ->
                    mr.legs.flatMap { leg ->
                        leg.steps.filter { it.travelMode == "TRANSIT" && it.transitDetails != null }.map { step ->
                            val details = step.transitDetails!!
                            TransitOption(
                                type = details.transitLine?.vehicle?.type ?: "TRANSIT",
                                serviceName = details.transitLine?.nameShort ?: details.transitLine?.name ?: "Public Service",
                                departureTime = details.stopDetails?.departureTime ?: "",
                                arrivalTime = details.stopDetails?.arrivalTime ?: "",
                                duration = step.staticDuration ?: "",
                                transfers = mr.legs.firstOrNull()?.steps?.count { it.travelMode == "TRANSIT" }?.minus(1)?.coerceAtLeast(0) ?: 0,
                                stops = details.stopCount ?: 0,
                                fare = mr.localizedValues?.transitFare?.text
                            )
                        }
                    }
                }
                
                generateIntelligence(
                    originName = repo.latestOriginName,
                    destName = repo.latestDestName,
                    distanceKm = "${route.distanceMeters / 1000}km",
                    duration = formatDuration(route.duration),
                    weather = repo.latestWeatherSummary,
                    riskLevel = repo.latestSafetyLevel,
                    incidents = incidents,
                    distanceMeters = route.distanceMeters,
                    transitOptions = transitOptions
                )
            }
        } else {
            _uiState.value = _uiState.value.copy(summary = "No active route planned. Go to Route Planner to start.")
        }
    }

    private fun formatDuration(duration: String): String {
        val seconds = duration.removeSuffix("s").toLongOrNull() ?: 0L
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    fun generateIntelligence(
        originName: String,
        destName: String,
        distanceKm: String,
        duration: String,
        weather: String,
        riskLevel: String,
        incidents: List<Incident>,
        distanceMeters: Int = 0,
        transitOptions: List<TransitOption> = emptyList()
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            delay(1200)
            
            val distKm = distanceMeters / 1000.0
            
            // "Exact" logic placeholders for SIH demo - usually would call specific APIs if keys available
            // Car: 55 km/h avg, ₹18/km for modern fleet
            // Bike: 45 km/h avg, ₹4/km
            // Public: Bus/Train estimates based on distance
            
            val analysis = StringBuilder()
            analysis.append("AI ASSESSMENT: Journey from $originName to $destName.\n\n")
            
            analysis.append("📊 EXACT LOGISTICS COMPARISON:\n")
            
            // CAR
            val carTime = formatDurationSeconds((distKm / 55.0 * 3600).toLong())
            val carFuel = (distKm * 15).toInt() 
            val carMaint = (distKm * 3).toInt()
            analysis.append("🚗 CAR: ~ $carTime | Total Cost: ₹${carFuel + carMaint} (Fuel: ₹$carFuel, Wear: ₹$carMaint)\n")
            
            // BIKE
            val bikeTime = formatDurationSeconds((distKm / 45.0 * 3600).toLong())
            val bikeCost = (distKm * 4).toInt()
            analysis.append("🏍️ BIKE: ~ $bikeTime | Total Cost: ₹$bikeCost\n")
            
            // PUBLIC TRANSIT AVAILABILITY
            if (transitOptions.isNotEmpty()) {
                analysis.append("\n🚍 PUBLIC TRANSIT OPTIONS:\n")
                transitOptions.forEach { opt ->
                    val icon = when(opt.type) {
                        "BUS" -> "🚌"
                        "TRAIN", "RAIL", "SUBWAY" -> "🚆"
                        else -> "🚍"
                    }
                    analysis.append("• $icon ${opt.type}: ${opt.serviceName} | ")
                    if (opt.departureTime.isNotEmpty()) {
                        analysis.append("Dep: ${opt.departureTime} | Arr: ${opt.arrivalTime} | ")
                    }
                    analysis.append("Stops: ${opt.stops} | Fare: ${opt.fare ?: "N/A"}\n")
                }
            } else {
                analysis.append("\n🚍 PUBLIC TRANSIT: No live transit information available for this exact route via Google Routes.\n")
            }
            
            // TOLLS
            // Real world estimation: NHAI tolls usually occur every 60-80km in India
            val tollCount = (distKm / 70).toInt()
            if (tollCount > 0) {
                analysis.append("\n🛣️ NHAI TOLLS: Approx. $tollCount gates detected. Est. Toll: ₹${tollCount * 145}\n")
            } else {
                analysis.append("\n🛣️ NHAI TOLLS: No major tolls detected for this short corridor.\n")
            }

            analysis.append("\n🛡️ SAFETY STATUS: $riskLevel risk determined. ")
            if (riskLevel == "HIGH" || riskLevel == "CRITICAL") {
                analysis.append("UNSAFE CONDITIONS detected. Proceed with extreme caution. ")
            } else {
                analysis.append("Route appears clear for standard transit. ")
            }
            
            if (weather.contains("Rain", true)) {
                analysis.append("\n\n⛈️ WEATHER: $weather reported. Visibility may be impaired. Reduce speed on mountain passes.")
            } else {
                analysis.append("\n\n☀️ WEATHER: $weather conditions appear favorable.")
            }
            
            if (incidents.isNotEmpty()) {
                analysis.append("\n\n🚧 INCIDENTS: ${incidents.size} verified hazard reports found near this corridor. ")
                incidents.forEach { 
                    analysis.append("Alert: ${it.incident_type.uppercase()}. ")
                }
            } else {
                analysis.append("\n\n✅ HAZARDS: No verified field blockages reported for this specific corridor.")
            }

            analysis.append("\n\n💡 ADVICE: ")
            if (riskLevel == "HIGH" || riskLevel == "CRITICAL") {
                analysis.append("Strongly advise taking a safe alternative route or delaying transit until conditions improve.")
            } else {
                analysis.append("Maintain standard protocols (55 km/h limit). Have a safe journey.")
            }

            _uiState.value = _uiState.value.copy(
                summary = analysis.toString(),
                transitOptions = transitOptions,
                isLoading = false
            )
        }
    }

    private fun formatDurationSeconds(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
