package com.mitraroute.ai.data.repository

import android.content.Context
import com.mitraroute.ai.BuildConfig
import com.mitraroute.ai.data.api.RetrofitClient
import com.mitraroute.ai.data.local.*
import com.mitraroute.ai.data.model.*
import com.mitraroute.ai.util.PrefsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.firstOrNull

class MitraRouteRepository private constructor(context: Context) {

    private val api = RetrofitClient.api
    private val db = AppDatabase.getInstance(context)
    private val routeDao = db.routeDao()
    private val incidentDao = db.incidentDao()
    private val offlineQueueDao = db.offlineQueueDao()
    private val weatherDao = db.weatherDao()
    private val telemetryDao = db.telemetryDao()
    private val prefs = PrefsManager(context)
    private val googleKey = BuildConfig.GOOGLE_MAPS_KEY

    companion object {
        @Volatile
        private var instance: MitraRouteRepository? = null

        fun getInstance(context: Context): MitraRouteRepository {
            return instance ?: synchronized(this) {
                instance ?: MitraRouteRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    // Providers
    val geocoding: GeocodingProvider = FreeGeocodingProvider(api)
    val routing: RoutingProvider = FreeRoutingProvider(api)
    val weather: WeatherProvider = OpenMeteoWeatherProvider(api)
    val hospitals: HospitalProvider = OsmHospitalProvider(api)

    // Shared state for screens
    private var _latestPlannedRoute: ModernRoute? = null
    val latestPlannedRoute: ModernRoute? get() = _latestPlannedRoute
    
    var latestOriginName: String = ""
    var latestDestName: String = ""
    var latestWeatherSummary: String = ""
    var latestSafetyLevel: String = ""
    var latestSafetyScore: Int = 0
    var latestOriginLoc: LatLonLiteral? = null
    var latestDestLoc: LatLonLiteral? = null

    fun setLatestPlannedRoute(route: ModernRoute, origin: String, dest: String, weatherS: String, safetyLvl: String, safetyS: Int, originLoc: LatLonLiteral? = null, destLoc: LatLonLiteral? = null) {
        _latestPlannedRoute = route
        latestOriginName = origin
        latestDestName = dest
        latestWeatherSummary = weatherS
        latestSafetyLevel = safetyLvl
        latestSafetyScore = safetyS
        latestOriginLoc = originLoc
        latestDestLoc = destLoc
    }

    suspend fun getTransitRoutes(origin: LatLonLiteral, destination: LatLonLiteral): List<ModernRoute> {
        val request = ComputeRoutesRequest(
            origin = RouteWaypoint(location = WaypointLocation(origin)),
            destination = RouteWaypoint(location = WaypointLocation(destination)),
            travelMode = "TRANSIT",
            transitPreferences = TransitPreferences(
                allowedTravelModes = listOf("BUS", "SUBWAY", "TRAIN", "LIGHT_RAIL", "RAIL")
            )
        )
        val fieldMask = "routes.distanceMeters,routes.duration,routes.polyline,routes.legs.steps.transitDetails,routes.legs.steps.travelMode,routes.legs.steps.startLocation,routes.legs.steps.endLocation,routes.localizedValues,routes.travelAdvisory.transitFare"
        return try {
            val response = api.computeRoutes(request, googleKey, fieldMask)
            if (response.isSuccessful) response.body()?.routes ?: emptyList()
            else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ---------- Internal Backend / Offline Flow ----------

    suspend fun getRoutes(): Flow<List<Route>> = flow {
        try {
            val cached = routeDao.getAllRoutes().firstOrNull()
            if (!cached.isNullOrEmpty()) {
                emit(cached.map { it.toRoute() })
            }
        } catch (_: Exception) {}

        try {
            val response = api.getRoutes()
            if (response.isSuccessful) {
                val routes = response.body() ?: emptyList()
                if (routes.isNotEmpty()) {
                    routeDao.clearAll()
                    routeDao.insertRoutes(routes.map { it.toCachedRoute() })
                    emit(routes)
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun getIncidents(): Flow<List<Incident>> = flow {
        try {
            val cached = incidentDao.getAllIncidents().firstOrNull()
            if (!cached.isNullOrEmpty()) {
                emit(cached.map { it.toIncident() })
            }
        } catch (_: Exception) {}

        try {
            val response = api.getIncidents()
            if (response.isSuccessful) {
                val incidents = response.body() ?: emptyList()
                if (incidents.isNotEmpty()) {
                    incidentDao.clearAll()
                    incidentDao.insertIncidents(incidents.map { it.toCachedIncident() })
                    emit(incidents)
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun getDashboardStats(): DashboardStats {
        return try {
            val response = api.getDashboardStats()
            if (response.isSuccessful) response.body() ?: DashboardStats()
            else DashboardStats()
        } catch (e: Exception) {
            DashboardStats()
        }
    }

    suspend fun getPendingOfflineCount(): Int {
        return try {
            offlineQueueDao.getPendingList().size
        } catch (e: Exception) {
            0
        }
    }

    suspend fun reportIncident(incident: IncidentCreate): Result<Map<String, Any>> {
        return try {
            val response = api.reportIncident(incident)
            if (response.isSuccessful && response.body() != null) {
                // If online submission works, also save to local cache for immediate display
                try {
                    incidentDao.insertIncidents(listOf(CachedIncident(
                        routeId = incident.route_id,
                        incidentType = incident.incident_type,
                        severity = incident.severity,
                        description = incident.description,
                        lat = incident.lat,
                        lon = incident.lon,
                        status = "active",
                        createdAt = incident.client_timestamp
                    )))
                } catch (_: Exception) {}
                Result.success(response.body()!!)
            } else {
                queueOfflineIncident(incident)
                Result.success(mapOf("message" to "Saved locally for sync"))
            }
        } catch (e: Exception) {
            queueOfflineIncident(incident)
            Result.success(mapOf("message" to "Saved locally for sync"))
        }
    }

    private suspend fun queueOfflineIncident(incident: IncidentCreate) {
        // 1. Save to Offline Queue for later background sync
        offlineQueueDao.insertOfflineIncident(
            OfflineIncidentQueue(
                incidentType = incident.incident_type,
                severity = incident.severity,
                description = incident.description,
                lat = incident.lat,
                lon = incident.lon,
                routeId = incident.route_id,
                clientTimestamp = incident.client_timestamp
            )
        )
        // 2. ALSO save to Local Cache so it shows up in "Verified Incidents" list immediately
        try {
            incidentDao.insertIncidents(listOf(CachedIncident(
                routeId = incident.route_id,
                incidentType = incident.incident_type,
                severity = incident.severity,
                description = incident.description,
                lat = incident.lat,
                lon = incident.lon,
                status = "pending_sync",
                createdAt = incident.client_timestamp
            )))
        } catch (_: Exception) {}
    }

    suspend fun recordTelemetry(lat: Double, lon: Double, speed: Float, bearing: Float) {
        telemetryDao.insertTelemetry(
            CachedTelemetry(lat = lat, lon = lon, speed = speed, bearing = bearing)
        )
    }

    suspend fun syncOfflineData(): Int {
        val pendingIncidents = offlineQueueDao.getPendingList()
        val token = prefs.getToken() ?: ""
        
        var totalSynced = 0
        if (pendingIncidents.isNotEmpty()) {
            try {
                val payload = mapOf(
                    "incidents" to pendingIncidents.map { off ->
                        OfflineIncident(
                            incident_type = off.incidentType,
                            severity = off.severity,
                            description = off.description,
                            lat = off.lat,
                            lon = off.lon,
                            route_id = off.routeId,
                            client_timestamp = off.clientTimestamp
                        )
                    }
                )
                val response = api.syncIncidents(payload, "Bearer $token")
                if (response.isSuccessful) {
                    pendingIncidents.forEach { offlineQueueDao.markSynced(it.queueId) }
                    offlineQueueDao.clearSynced()
                    totalSynced += pendingIncidents.size
                }
            } catch (_: Exception) {}
        }
        return totalSynced
    }

    // ---------- Mapping helpers ----------

    private fun Route.toCachedRoute() = CachedRoute(
        id = id, name = name,
        startLat = start_lat, startLon = start_lon,
        endLat = end_lat, endLon = end_lon,
        distanceKm = distance_km, roadType = road_type,
        riskLevel = risk_level, riskScore = risk_score
    )

    private fun CachedRoute.toRoute() = Route(
        id = id, name = name,
        start_lat = startLat, start_lon = startLon,
        end_lat = endLat, end_lon = endLon,
        distance_km = distanceKm, road_type = roadType,
        risk_level = riskLevel, risk_score = riskScore
    )

    private fun Incident.toCachedIncident() = CachedIncident(
        routeId = route_id, incidentType = incident_type,
        severity = severity, description = description,
        lat = lat, lon = lon, status = status,
        createdAt = created_at ?: ""
    )

    private fun CachedIncident.toIncident() = Incident(
        id = id, route_id = routeId,
        incident_type = incidentType, severity = severity,
        description = description, lat = lat, lon = lon,
        status = status, created_at = createdAt
    )
}
