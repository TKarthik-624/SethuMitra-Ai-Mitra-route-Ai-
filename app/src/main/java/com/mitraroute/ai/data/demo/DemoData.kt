package com.mitraroute.ai.data.demo

import com.mitraroute.ai.data.model.Incident
import com.mitraroute.ai.data.model.Route
import com.mitraroute.ai.data.model.WeatherResponse
import com.mitraroute.ai.data.model.CurrentWeather
import com.mitraroute.ai.data.model.DailyWeather
import java.util.UUID

/**
 * Deterministic local demo dataset for SIH live demo reliability.
 *
 * Use this as fallback when the backend/external APIs are unavailable.
 * This does NOT replace existing repository/Room/Retrofit behavior.
 */
object DemoData {

    private const val ID_PREFIX = "demo_"

    /** Guwahati -> Aizawl main corridor (primary route for demo) */
    val primaryRoute: Route = Route(
        id = 1,
        name = "Guwahati → Aizawl",
        start_lat = 26.14,
        start_lon = 91.73,
        end_lat = 23.73,
        end_lon = 92.72,
        distance_km = 540.0,
        road_type = "Highway",
        risk_level = "high",
        risk_score = 78.0
    )

    /** Alternate route suggested when primary corridor is disrupted */
    val alternateRoute: Route = Route(
        id = 2,
        name = "Guwahati → Aizawl (Alternate)",
        start_lat = 26.14,
        start_lon = 91.73,
        end_lat = 23.73,
        end_lon = 92.72,
        distance_km = 565.0,
        road_type = "Mixed Highway",
        risk_level = "medium",
        risk_score = 42.0
    )

    /** Initial demo weather for Aizawl (heavy rainfall scenario) */
    val aizawlWeather: WeatherResponse = WeatherResponse(
        latitude = 23.73,
        longitude = 92.72,
        current_weather = CurrentWeather(
            temperature = 18.2,
            windspeed = 32.0,
            winddirection = 210.0,
            weathercode = 80,
            time = "2026-09-11"
        ),
        daily = DailyWeather(
            time = listOf("2026-09-11"),
            rain_sum = listOf(48.5),
            windspeed_10m_max = listOf(41.0)
        )
    )

    /** TRK-01 demo vehicle */
    val vehicleId = "TRK-01"
    val deliveryId = "MED-001"
    val cargo = "Essential Medicines"
    val origin = "Guwahati"
    val destination = "Aizawl"
    val initialDeliveryStatus = "In Transit"
    val initialEtaMinutes = 260 // 4h 20m
    val initialDelayMinutes = 65 // +65 min

    /** Initial demo incident that explains the high-risk scenario */
    val initialLandslideIncident: Incident = Incident(
        id = 0,
        route_id = primaryRoute.id,
        incident_type = "landslide",
        severity = 5,
        description = "Road blocked due to landslide after heavy rainfall.",
        lat = 24.95,
        lon = 93.20,
        status = "active",
        client_timestamp = "2026-09-11T08:10:00Z"
    )

    /**
     * In-memory demo incident store.
     */
    object DemoIncidentStore {
        private val _incidents = mutableListOf<Incident>(initialLandslideIncident)
        val incidents: List<Incident> get() = _incidents

        fun addIncident(incident: Incident) {
            _incidents.add(incident)
        }

        fun activeCount(): Int = _incidents.count { it.status == "active" }

        fun clear() {
            _incidents.clear()
            _incidents.add(initialLandslideIncident)
        }
    }

    /** Fresh demo scenario initial state for repeated presentation runs */
    fun currentScenario() = ScenarioSnapshot(
        vehicleId = vehicleId,
        deliveryId = deliveryId,
        cargo = cargo,
        origin = origin,
        destination = destination,
        primaryRoute = primaryRoute,
        alternateRoute = alternateRoute,
        weather = aizawlWeather,
        deliveryStatus = initialDeliveryStatus,
        etaMinutes = initialEtaMinutes,
        delayMinutes = initialDelayMinutes,
        alternateSelected = false
    )

    /** Resets in-memory demo incident store to initial landslide scenario */
    fun resetDemoIncidents() {
        DemoIncidentStore.clear()
    }

    private fun randomUuidHashCode(): Int = UUID.randomUUID().hashCode()
}

/**
 * Transparent rule-based demo risk engine.
 */
object DemoRiskEngine {

    fun levelFromScore(score: Double): String = when {
        score >= 81 -> "critical"
        score >= 61 -> "high"
        score >= 31 -> "medium"
        else -> "low"
    }

    fun computePrimaryRisk(
        weather: WeatherResponse?,
        activeIncidents: List<Incident>
    ): RiskResult {
        var score = 0.0
        val reasons = mutableListOf<String>()

        val rainSum = weather?.daily?.rain_sum?.firstOrNull() ?: 0.0
        val weatherCode = weather?.current_weather?.weathercode ?: 0
        val windspeed = weather?.current_weather?.windspeed ?: 0.0

        if (weatherCode in 61..65 || weatherCode in 80..82 || weatherCode == 95 || rainSum >= 20.0) {
            score += 30.0
            reasons.add("Heavy rainfall detected")
        } else if (rainSum >= 5.0 || weatherCode in 51..55) {
            score += 12.0
            reasons.add("Moderate rainfall")
        }

        if (windspeed >= 40.0) {
            score += 10.0
            reasons.add("High wind conditions")
        }

        val severeIncidents = activeIncidents.filter { it.severity >= 4 }
        if (severeIncidents.isNotEmpty()) {
            score += 30.0
            reasons.add("Landslide / road blockage reported")
        } else if (activeIncidents.isNotEmpty()) {
            score += 15.0
            reasons.add("Active incident reported")
        }

        score += 13.0
        reasons.add("Vulnerable road segment")

        val clamped = score.coerceIn(0.0, 100.0)
        return RiskResult(clamped, levelFromScore(clamped), reasons)
    }

    fun computeAlternateRisk(
        weather: WeatherResponse?,
        activeIncidents: List<Incident>
    ): RiskResult {
        val primary = computePrimaryRisk(weather, activeIncidents)
        val score = (primary.score * 0.55).coerceIn(0.0, 100.0)
        return RiskResult(score, levelFromScore(score), listOf("Avoids blocked segment", "Still affected by weather"))
    }
}

data class RiskResult(
    val score: Double,
    val level: String,
    val reasons: List<String>
)

data class ScenarioSnapshot(
    val vehicleId: String,
    val deliveryId: String,
    val cargo: String,
    val origin: String,
    val destination: String,
    val primaryRoute: Route,
    val alternateRoute: Route,
    val weather: WeatherResponse,
    val deliveryStatus: String,
    val etaMinutes: Int,
    val delayMinutes: Int,
    val alternateSelected: Boolean
) {
    val currentRoute: Route get() = if (alternateSelected) alternateRoute else primaryRoute
    val currentRisk: RiskResult get() = if (alternateSelected) {
        DemoRiskEngine.computeAlternateRisk(weather, DemoData.DemoIncidentStore.incidents)
    } else {
        DemoRiskEngine.computePrimaryRisk(weather, DemoData.DemoIncidentStore.incidents)
    }

    val currentStatus: String get() = if (alternateSelected) {
        if (currentRisk.level == "high" || currentRisk.level == "critical") "At Risk" else "In Transit"
    } else {
        when (currentRisk.level) {
            "critical" -> "Delayed"
            "high" -> "At Risk"
            else -> deliveryStatus
        }
    }

    val currentEtaMinutes: Int get() = if (alternateSelected) {
        etaMinutes + 35
    } else {
        etaMinutes + delayMinutes
    }
}
