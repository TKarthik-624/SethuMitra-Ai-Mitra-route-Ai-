package com.mitraroute.ai.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ComputeRoutesRequest(
    val origin: RouteWaypoint,
    val destination: RouteWaypoint,
    val intermediates: List<RouteWaypoint>? = null,
    val travelMode: String = "DRIVE",
    val routingPreference: String = "TRAFFIC_AWARE_OPTIMAL",
    val computeAlternativeRoutes: Boolean = true,
    val routeModifiers: RouteModifiers? = null,
    val transitPreferences: TransitPreferences? = null,
    val languageCode: String = "en-US",
    val units: String = "METRIC"
)

@Serializable
data class TransitPreferences(
    val allowedTravelModes: List<String> = emptyList(),
    val routingPreference: String? = null
)

@Serializable
data class RouteWaypoint(
    val location: WaypointLocation? = null,
    val placeId: String? = null
)

@Serializable
data class WaypointLocation(
    val latLng: LatLonLiteral
)

@Serializable
data class RouteModifiers(
    val avoidTolls: Boolean = false,
    val avoidHighways: Boolean = false,
    val avoidFerries: Boolean = false
)

@Serializable
data class ComputeRoutesResponse(
    val routes: List<ModernRoute> = emptyList()
)

@Serializable
data class ModernRoute(
    val distanceMeters: Int,
    val duration: String,
    val staticDuration: String? = null,
    val polyline: ModernPolyline,
    val legs: List<ModernLeg> = emptyList(),
    val warnings: List<String> = emptyList(),
    val viewport: Viewport? = null,
    val routeLabels: List<String> = emptyList(),
    val localizedValues: LocalizedValues? = null,
    val travelAdvisory: TravelAdvisory? = null
)

@Serializable
data class ModernLeg(
    val distanceMeters: Int? = null,
    val duration: String? = null,
    val staticDuration: String? = null,
    val polyline: ModernPolyline? = null,
    val startLocation: WaypointLocation? = null,
    val endLocation: WaypointLocation? = null,
    val steps: List<ModernStep> = emptyList()
)

@Serializable
data class ModernStep(
    val distanceMeters: Int? = null,
    val staticDuration: String? = null,
    val polyline: ModernPolyline? = null,
    val startLocation: WaypointLocation? = null,
    val endLocation: WaypointLocation? = null,
    val travelMode: String? = null,
    val transitDetails: TransitDetails? = null
)

@Serializable
data class TransitDetails(
    val stopDetails: TransitStopDetails? = null,
    val localizedValues: TransitLocalizedValues? = null,
    val transitLine: TransitLine? = null,
    val stopCount: Int? = null,
    val tripShortText: String? = null
)

@Serializable
data class TransitStopDetails(
    val arrivalStop: TransitStop? = null,
    val arrivalTime: String? = null,
    val departureStop: TransitStop? = null,
    val departureTime: String? = null
)

@Serializable
data class TransitStop(
    val name: String? = null,
    val location: WaypointLocation? = null
)

@Serializable
data class TransitLocalizedValues(
    val arrivalTime: LocalizedTime? = null,
    val departureTime: LocalizedTime? = null
)

@Serializable
data class LocalizedTime(
    val time: LocalizedText? = null,
    val timeZone: String? = null
)

@Serializable
data class TransitLine(
    val agencies: List<TransitAgency> = emptyList(),
    val name: String? = null,
    val color: String? = null,
    val iconUri: String? = null,
    val nameShort: String? = null,
    val textColor: String? = null,
    val vehicle: TransitVehicle? = null
)

@Serializable
data class TransitAgency(
    val name: String? = null,
    val phoneNumber: String? = null,
    val uri: String? = null
)

@Serializable
data class TransitVehicle(
    val name: LocalizedText? = null,
    val type: String? = null,
    val iconUri: String? = null
)

@Serializable
data class LocalizedValues(
    val distance: LocalizedText? = null,
    val duration: LocalizedText? = null,
    val staticDuration: LocalizedText? = null,
    val transitFare: LocalizedText? = null
)

@Serializable
data class LocalizedText(
    val text: String? = null,
    val languageCode: String? = null
)

@Serializable
data class TravelAdvisory(
    val transitFare: Money? = null
)

@Serializable
data class Money(
    val currencyCode: String? = null,
    val units: String? = null,
    val nanos: Int? = null
)

@Serializable
data class ModernPolyline(
    val encodedPolyline: String
)

@Serializable
data class Viewport(
    val low: LatLonLiteral,
    val high: LatLonLiteral
)
