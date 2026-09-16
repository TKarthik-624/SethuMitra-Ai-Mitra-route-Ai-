package com.mitraroute.ai.data.api

import com.mitraroute.ai.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ---------- Routes (Internal Backend) ----------

    @GET("api/routes")
    suspend fun getRoutes(
        @Query("limit") limit: Int = 50
    ): Response<List<Route>>

    @GET("api/routes/{id}")
    suspend fun getRoute(@Path("id") id: Int): Response<Route>

    @POST("api/routes")
    suspend fun createRoute(@Body route: RouteCreate): Response<Map<String, Any>>

    // ---------- Incidents (Internal Backend) ----------

    @GET("api/incidents")
    suspend fun getIncidents(
        @Query("route_id") routeId: Int? = null,
        @Query("status") status: String? = null
    ): Response<List<Incident>>

    @POST("api/incidents")
    suspend fun reportIncident(@Body incident: IncidentCreate): Response<Map<String, Any>>

    @POST("api/incidents/sync")
    suspend fun syncIncidents(
        @Body payload: Map<String, List<OfflineIncident>>,
        @Header("Authorization") token: String
    ): Response<Map<String, Any>>

    @POST("https://routes.googleapis.com/v1/computeRoutes")
    suspend fun computeRoutes(
        @Body request: ComputeRoutesRequest,
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String = "routes.distanceMeters,routes.duration,routes.polyline,routes.warnings,routes.routeLabels"
    ): Response<ComputeRoutesResponse>

    @GET("https://maps.googleapis.com/maps/api/directions/json")
    suspend fun getGoogleDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("key") apiKey: String,
        @Query("alternatives") alternatives: Boolean = true,
        @Query("mode") mode: String = "driving"
    ): Response<GoogleDirectionsResponse>

    // ---------- OpenWeatherMap API ----------
    
    @GET("https://api.openweathermap.org/data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): Response<OpenWeatherResponse>

    @GET("https://api.openweathermap.org/data/2.5/forecast")
    suspend fun getWeatherForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): Response<OpenWeatherForecastResponse>

    // ---------- Google Places API (Autocomplete) ----------

    @GET("https://maps.googleapis.com/maps/api/place/autocomplete/json")
    suspend fun getPlaceAutocomplete(
        @Query("input") input: String,
        @Query("key") apiKey: String,
        @Query("components") components: String = "country:in",
        @Query("types") types: String = "geocode"
    ): Response<GooglePlaceAutocompleteResponse>

    @GET("https://maps.googleapis.com/maps/api/place/details/json")
    suspend fun getPlaceDetails(
        @Query("place_id") placeId: String,
        @Query("key") apiKey: String,
        @Query("fields") fields: String = "geometry,formatted_address,name"
    ): Response<GooglePlaceDetailsResponse>

    @GET("https://maps.googleapis.com/maps/api/place/nearbysearch/json")
    suspend fun getNearbyPlaces(
        @Query("location") location: String, // "lat,lon"
        @Query("radius") radius: Int = 5000,
        @Query("type") type: String = "hospital",
        @Query("key") apiKey: String
    ): Response<GoogleNearbySearchResponse>

    // ---------- Dashboard ----------

    @GET("api/dashboard/stats")
    suspend fun getDashboardStats(): Response<DashboardStats>

    // ---------- FREE SERVICES (No Billing Required) ----------

    // 1. OSRM Routing
    @GET("https://router.project-osrm.org/route/v1/driving/{coords}")
    suspend fun getOsrmRoute(
        @Path("coords") coords: String, // "lon1,lat1;lon2,lat2"
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "polyline",
        @Query("steps") steps: Boolean = true,
        @Query("alternatives") alternatives: Boolean = true
    ): Response<OsrmResponse>

    // 2. Nominatim Geocoding (Autocomplete / Search)
    @GET("https://nominatim.openstreetmap.org/search")
    suspend fun searchNominatim(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 10,
        @Query("addressdetails") addressDetails: Int = 1
    ): Response<List<NominatimResult>>

    @GET("https://nominatim.openstreetmap.org/reverse")
    suspend fun reverseGeocodeNominatim(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "json"
    ): Response<NominatimResult>

    // 3. Open-Meteo Weather
    @GET("https://api.open-meteo.com/v1/forecast")
    suspend fun getOpenMeteoWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,rain,showers,snowfall,weather_code,wind_speed_10m",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max",
        @Query("timezone") timezone: String = "auto"
    ): Response<OpenMeteoResponse>

    // 4. Overpass API (Nearby Hospitals)
    @FormUrlEncoded
    @POST("https://overpass-api.de/api/interpreter")
    suspend fun getNearbyHospitalsOverpass(
        @Field("data") data: String
    ): Response<OverpassResponse>
}
