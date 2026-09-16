package com.mitraroute.ai.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.Locale

class LocationTracker(private val context: Context) {

    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _locationFlow = MutableStateFlow<Location?>(null)
    val locationFlow = _locationFlow.asStateFlow()

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return try {
            // Try to get fresh location first
            val freshLocation = client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
            if (freshLocation != null) {
                Log.d("LOCATION_TRACKER", "Fresh location acquired: ${freshLocation.latitude}, ${freshLocation.longitude}")
                freshLocation
            } else {
                // Fallback to last known location
                val lastLocation = client.lastLocation.await()
                Log.d("LOCATION_TRACKER", "Last location acquired: ${lastLocation?.latitude}, ${lastLocation?.longitude}")
                lastLocation
            }
        } catch (e: Exception) {
            Log.e("LOCATION_TRACKER", "Error acquiring location: ${e.message}")
            null
        }
    }

    private var currentOnLocationUpdate: ((Location) -> Unit)? = null

    @SuppressLint("MissingPermission")
    fun startLocationTracking(onLocationUpdate: (Location) -> Unit) {
        currentOnLocationUpdate = onLocationUpdate
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 10000
        ).setMinUpdateIntervalMillis(5000).build()

        client.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    fun stopLocationTracking() {
        client.removeLocationUpdates(locationCallback)
        currentOnLocationUpdate = null
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let {
                Log.d("LOCATION_TRACKER", "Tracking update: ${it.latitude}, ${it.longitude}")
                _locationFlow.value = it
                currentOnLocationUpdate?.invoke(it)
            }
        }
    }

    fun getAddressFromLocation(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val sb = StringBuilder()
                for (i in 0..addr.maxAddressLineIndex) {
                    sb.append(addr.getAddressLine(i)).append(" ")
                }
                sb.toString().trim()
            } else {
                "Unknown Address"
            }
        } catch (e: Exception) {
            Log.e("LOCATION_TRACKER", "Geocoding failed: ${e.message}")
            "Address Unavailable"
        }
    }

    companion object {
        fun decodePolyline(encoded: String): List<LatLng> {
            val poly = ArrayList<LatLng>()
            var index = 0
            val len = encoded.length
            var lat = 0
            var lng = 0
            while (index < len) {
                var b: Int
                var shift = 0
                var result = 0
                do {
                    if (index >= len) return poly
                    b = encoded[index++].code - 63
                    result = result or (b and 0x1f shl shift)
                    shift += 5
                } while (b >= 0x20)
                val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
                lat += dlat
                shift = 0
                result = 0
                do {
                    if (index >= len) return poly
                    b = encoded[index++].code - 63
                    result = result or (b and 0x1f shl shift)
                    shift += 5
                } while (b >= 0x20)
                val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
                lng += dlng
                val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
                poly.add(p)
            }
            return poly
        }
    }
}
