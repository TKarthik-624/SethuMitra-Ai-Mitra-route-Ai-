package com.mitraroute.ai.data.repository

import android.util.Log
import com.mitraroute.ai.data.api.ApiService
import com.mitraroute.ai.data.model.*
import com.mitraroute.ai.util.PlacesHelper
import kotlinx.coroutines.flow.Flow

// ---------- Interfaces ----------

interface GeocodingProvider {
    suspend fun searchPlaces(query: String): List<PlacePrediction>
    suspend fun reverseGeocode(lat: Double, lon: Double): String
}

interface RoutingProvider {
    suspend fun calculateRoute(origin: LatLonLiteral, destination: LatLonLiteral): List<ModernRoute>
}

interface WeatherProvider {
    suspend fun getCurrentWeather(lat: Double, lon: Double): OpenMeteoResponse?
}

interface HospitalProvider {
    suspend fun getNearbyHospitals(lat: Double, lon: Double): List<NearbyPlaceResult>
}

// ---------- Implementations ----------

class FreeGeocodingProvider(
    private val api: ApiService,
    private val placesHelper: PlacesHelper
) : GeocodingProvider {
    override suspend fun searchPlaces(query: String): List<PlacePrediction> {
        return placesHelper.getAutocompletePredictions(query, true) // Defaulting to true for session tracking
    }

    override suspend fun reverseGeocode(lat: Double, lon: Double): String {
        return try {
            val response = api.reverseGeocodeNominatim(lat, lon)
            if (response.isSuccessful) {
                response.body()?.display_name ?: "Unknown Location"
            } else "Unknown Location"
        } catch (e: Exception) {
            "Unknown Location"
        }
    }
}

class FreeRoutingProvider(private val api: ApiService) : RoutingProvider {
    override suspend fun calculateRoute(origin: LatLonLiteral, destination: LatLonLiteral): List<ModernRoute> {
        val coords = "${origin.lng},${origin.lat};${destination.lng},${destination.lat}"
        Log.d("ROUTING_PROVIDER", "Requesting OSRM route for: $coords")
        return try {
            val response = api.getOsrmRoute(coords)
            if (response.isSuccessful) {
                Log.d("ROUTING_PROVIDER", "OSRM Response Success: ${response.body()?.code}")
                response.body()?.routes?.map {
                    ModernRoute(
                        distanceMeters = it.distance.toInt(),
                        duration = "${it.distance / (55.0 / 3.6)}s", 
                        polyline = ModernPolyline(it.geometry),
                        legs = it.legs.map { leg ->
                            ModernLeg(
                                distanceMeters = leg.distance.toInt(),
                                duration = "${leg.duration}s",
                                steps = leg.steps.map { step ->
                                    val maneuverLoc = step.maneuver?.location
                                    val startLoc = if (maneuverLoc != null && maneuverLoc.size >= 2) {
                                        WaypointLocation(LatLonLiteral(maneuverLoc[1], maneuverLoc[0]))
                                    } else null
                                    
                                    ModernStep(
                                        distanceMeters = step.distance.toInt(),
                                        travelMode = "DRIVE",
                                        navigationInstruction = NavigationInstruction(
                                            maneuver = step.maneuver?.type,
                                            instructions = step.instruction ?: step.name
                                        ),
                                        startLocation = startLoc
                                    )
                                }
                            )
                        },
                        warnings = emptyList(),
                        routeLabels = listOf("FASTEST")
                    )
                } ?: emptyList()
            } else {
                Log.e("ROUTING_PROVIDER", "OSRM Error: ${response.code()} ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ROUTING_PROVIDER", "OSRM Exception: ${e.message}")
            emptyList()
        }
    }
}

class OpenMeteoWeatherProvider(private val api: ApiService) : WeatherProvider {
    override suspend fun getCurrentWeather(lat: Double, lon: Double): OpenMeteoResponse? {
        return try {
            val response = api.getOpenMeteoWeather(lat, lon)
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }
}

class GoogleHospitalProvider(
    private val api: ApiService,
    private val placesHelper: PlacesHelper
) : HospitalProvider {
    override suspend fun getNearbyHospitals(lat: Double, lon: Double): List<NearbyPlaceResult> {
        return try {
            placesHelper.getNearbyHospitals(lat, lon)
        } catch (e: Exception) {
            Log.w("HOSPITAL_PROVIDER", "Google Search failed, falling back to OSM")
            val osmProvider = OsmHospitalProvider(api)
            osmProvider.getNearbyHospitals(lat, lon)
        }
    }
}

class OsmHospitalProvider(private val api: ApiService) : HospitalProvider {
    override suspend fun getNearbyHospitals(lat: Double, lon: Double): List<NearbyPlaceResult> {
        val query = "[out:json];(node[\"amenity\"~\"hospital|clinic|medical_centre\"](around:10000,$lat,$lon);way[\"amenity\"~\"hospital|clinic|medical_centre\"](around:10000,$lat,$lon);relation[\"amenity\"~\"hospital|clinic|medical_centre\"](around:10000,$lat,$lon););out center;"
        
        return try {
            Log.d("HOSPITAL_PROVIDER", "Requesting Overpass hospitals for: $lat,$lon")
            val response = api.getNearbyHospitalsOverpass(query)
            if (response.isSuccessful) {
                val elements = response.body()?.elements ?: emptyList()
                Log.d("HOSPITAL_PROVIDER", "Overpass returned ${elements.size} elements")
                elements.map {
                    val finalLat = it.lat ?: it.center?.lat ?: 0.0
                    val finalLon = it.lon ?: it.center?.lon ?: 0.0
                    NearbyPlaceResult(
                        name = it.tags?.get("name") ?: "Medical Facility",
                        vicinity = it.tags?.get("addr:full") ?: it.tags?.get("addr:street") ?: it.tags?.get("addr:city") ?: "Nearby",
                        place_id = "osm_${it.id}",
                        geometry = PlaceGeometry(LatLonLiteral(finalLat, finalLon))
                    )
                }
            } else {
                Log.e("HOSPITAL_PROVIDER", "Overpass Error: ${response.code()} ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("HOSPITAL_PROVIDER", "Overpass Exception: ${e.message}", e)
            emptyList()
        }
    }
}
