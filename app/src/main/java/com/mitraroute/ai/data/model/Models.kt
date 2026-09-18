package com.mitraroute.ai.data.model

import kotlinx.serialization.Serializable

// ---------- Route ----------

@Serializable
data class Route(
    val id: Int = 0,
    val name: String,
    val start_lat: Double,
    val start_lon: Double,
    val end_lat: Double,
    val end_lon: Double,
    val waypoints: String? = null,
    val distance_km: Double? = null,
    val road_type: String = "highway",
    val risk_score: Double? = null,
    val risk_level: String? = null,
    val active_incidents: List<Incident>? = null
)

@Serializable
data class RouteCreate(
    val name: String,
    val start_lat: Double,
    val start_lon: Double,
    val end_lat: Double,
    val end_lon: Double,
    val waypoints: String? = null,
    val distance_km: Double? = null,
    val road_type: String = "highway"
)

// ---------- Incident ----------

@Serializable
data class Incident(
    val id: Int = 0,
    val route_id: Int? = null,
    val incident_type: String,
    val severity: Int,
    val description: String? = null,
    val lat: Double,
    val lon: Double,
    val photo_url: String? = null,
    val status: String = "active",
    val client_timestamp: String = "",
    val created_at: String? = null
)

@Serializable
data class IncidentCreate(
    val route_id: Int? = null,
    val incident_type: String,
    val severity: Int,
    val description: String? = null,
    val lat: Double,
    val lon: Double,
    val photo_url: String? = null,
    val client_timestamp: String
)

// ---------- Google Directions ----------

@Serializable
data class GoogleDirectionsResponse(
    val status: String,
    val routes: List<GoogleRoute> = emptyList(),
    val error_message: String? = null
)

@Serializable
data class GoogleRoute(
    val summary: String? = null,
    val legs: List<GoogleLeg> = emptyList(),
    val overview_polyline: GooglePolyline? = null,
    val warnings: List<String> = emptyList()
)

@Serializable
data class GoogleLeg(
    val distance: GoogleTextValue,
    val duration: GoogleTextValue,
    val start_address: String,
    val end_address: String,
    val start_location: LatLonLiteral,
    val end_location: LatLonLiteral
)

@Serializable
data class GoogleTextValue(
    val text: String,
    val value: Int
)

@Serializable
data class LatLonLiteral(
    val lat: Double,
    val lng: Double
)

@Serializable
data class GooglePolyline(
    val points: String
)

// ---------- OpenWeatherMap ----------

@Serializable
data class OpenWeatherResponse(
    val name: String = "", // City name
    val main: MainWeather,
    val weather: List<WeatherDescription>,
    val wind: WindData,
    val coord: LatLonLiteral? = null,
    val dt: Long? = 0
)

@Serializable
data class OpenWeatherForecastResponse(
    val list: List<ForecastItem>,
    val city: ForecastCity
)

@Serializable
data class ForecastItem(
    val dt: Long,
    val main: MainWeather,
    val weather: List<WeatherDescription>,
    val wind: WindData,
    val dt_txt: String
)

@Serializable
data class ForecastCity(
    val name: String,
    val country: String
)

@Serializable
data class MainWeather(
    val temp: Double,
    val feels_like: Double? = null,
    val humidity: Int,
    val pressure: Int? = null
)

@Serializable
data class WeatherDescription(
    val main: String,
    val description: String,
    val icon: String
)

@Serializable
data class WindData(
    val speed: Double,
    val deg: Double? = null
)

// ---------- Google Places ----------

@Serializable
data class GooglePlaceAutocompleteResponse(
    val status: String,
    val predictions: List<PlacePrediction> = emptyList()
)

@Serializable
data class PlacePrediction(
    val description: String,
    val place_id: String
)

@Serializable
data class GooglePlaceDetailsResponse(
    val status: String,
    val result: PlaceDetailResult? = null
)

@Serializable
data class PlaceDetailResult(
    val geometry: PlaceGeometry,
    val formatted_address: String,
    val name: String
)

@Serializable
data class PlaceGeometry(
    val location: LatLonLiteral
)

@Serializable
data class GoogleNearbySearchResponse(
    val status: String,
    val results: List<NearbyPlaceResult> = emptyList()
)

@Serializable
data class NearbyPlaceResult(
    val name: String,
    val vicinity: String? = null,
    val place_id: String,
    val geometry: PlaceGeometry
)

// ---------- Dashboard & Offline ----------

@Serializable
data class DashboardStats(
    val total_routes: Int = 0,
    val total_incidents: Int = 0,
    val active_incidents: Int = 0,
    val critical_routes: Int = 0,
    val total_users: Int = 0
)

@Serializable
data class OfflineIncident(
    val incident_type: String,
    val severity: Int,
    val description: String? = null,
    val lat: Double,
    val lon: Double,
    val route_id: Int? = null,
    val client_timestamp: String
)

// Backward compatibility for demo engine (if still used)
@Serializable
data class WeatherResponse(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val current_weather: CurrentWeather? = null,
    val daily: DailyWeather? = null
)

@Serializable
data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double,
    val winddirection: Double,
    val weathercode: Int,
    val time: String
)

@Serializable
data class DailyWeather(
    val time: List<String>,
    val rain_sum: List<Double>,
    val windspeed_10m_max: List<Double>
)

// ---------- OSRM Models ----------

@Serializable
data class OsrmResponse(
    val code: String,
    val routes: List<OsrmRoute> = emptyList(),
    val waypoints: List<OsrmWaypoint> = emptyList()
)

@Serializable
data class OsrmRoute(
    val distance: Double,
    val duration: Double,
    val geometry: String,
    val weight: Double? = null,
    val weight_name: String? = null,
    val legs: List<OsrmLeg> = emptyList()
)

@Serializable
data class OsrmWaypoint(
    val hint: String? = null,
    val distance: Double? = null,
    val name: String,
    val location: List<Double>
)

@Serializable
data class OsrmLeg(
    val distance: Double,
    val duration: Double,
    val summary: String = "",
    val steps: List<OsrmStep> = emptyList()
)

@Serializable
data class OsrmStep(
    val distance: Double,
    val duration: Double,
    val name: String = "",
    val instruction: String? = null,
    val maneuver: OsrmManeuver? = null
)

@Serializable
data class OsrmManeuver(
    val type: String? = null,
    val instruction: String? = null,
    val location: List<Double> = emptyList()
)

// ---------- Nominatim Models ----------

@Serializable
data class NominatimResult(
    val place_id: Long,
    val licence: String? = null,
    val osm_type: String? = null,
    val osm_id: Long? = null,
    val lat: String,
    val lon: String,
    val display_name: String,
    val address: NominatimAddress? = null
)

@Serializable
data class NominatimAddress(
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val country_code: String? = null
)

// ---------- Open-Meteo Models ----------

@Serializable
data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val current: OpenMeteoCurrent? = null,
    val daily: OpenMeteoDaily? = null
)

@Serializable
data class OpenMeteoCurrent(
    val time: String,
    val temperature_2m: Double,
    val relative_humidity_2m: Int? = null,
    val apparent_temperature: Double? = null,
    val is_day: Int? = null,
    val precipitation: Double? = null,
    val rain: Double? = null,
    val weather_code: Int,
    val wind_speed_10m: Double
)

@Serializable
data class OpenMeteoDaily(
    val time: List<String>,
    val weather_code: List<Int>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val precipitation_probability_max: List<Int?> = emptyList()
)

// ---------- Overpass Models ----------

@Serializable
data class OverpassResponse(
    val elements: List<OverpassElement> = emptyList()
)

@Serializable
data class OverpassElement(
    val type: String,
    val id: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenter? = null,
    val tags: Map<String, String>? = null
)

@Serializable
data class OverpassCenter(
    val lat: Double,
    val lon: Double
)
