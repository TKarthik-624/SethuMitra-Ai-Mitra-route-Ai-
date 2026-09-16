package com.mitraroute.ai.util

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.google.android.libraries.places.api.model.CircularBounds
import com.mitraroute.ai.data.model.*
import kotlinx.coroutines.tasks.await

class PlacesHelper(context: Context) {
    private val placesClient = Places.createClient(context)
    
    private var fromSessionToken: AutocompleteSessionToken? = null
    private var toSessionToken: AutocompleteSessionToken? = null

    private fun getFromToken(): AutocompleteSessionToken {
        if (fromSessionToken == null) fromSessionToken = AutocompleteSessionToken.newInstance()
        return fromSessionToken!!
    }

    private fun getToToken(): AutocompleteSessionToken {
        if (toSessionToken == null) toSessionToken = AutocompleteSessionToken.newInstance()
        return toSessionToken!!
    }

    fun resetFromToken() { fromSessionToken = null }
    fun resetToToken() { toSessionToken = null }

    suspend fun getAutocompletePredictions(query: String, isFrom: Boolean): List<PlacePrediction> {
        val token = if (isFrom) getFromToken() else getToToken()
        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(token)
            .setQuery(query)
            .build()

        return try {
            val response = placesClient.findAutocompletePredictions(request).await()
            Log.d("PLACES_HELPER", "Autocomplete success for: $query")
            response.autocompletePredictions.map {
                PlacePrediction(it.getFullText(null).toString(), it.placeId)
            }
        } catch (e: Exception) {
            Log.e("PLACES_HELPER", "Autocomplete error: ${e.message}")
            emptyList()
        }
    }

    suspend fun getPlaceDetails(placeId: String, isFrom: Boolean): PlaceDetailResult? {
        val token = if (isFrom) fromSessionToken else toSessionToken
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
        val request = FetchPlaceRequest.builder(placeId, placeFields)
            .setSessionToken(token)
            .build()

        return try {
            val response = placesClient.fetchPlace(request).await()
            val place = response.place
            val latLng = place.latLng ?: return null
            
            if (isFrom) resetFromToken() else resetToToken()
            
            Log.d("PLACES_HELPER", "Place details success for: ${place.name}")
            PlaceDetailResult(
                geometry = PlaceGeometry(LatLonLiteral(latLng.latitude, latLng.longitude)),
                formatted_address = place.address ?: "",
                name = place.name ?: ""
            )
        } catch (e: Exception) {
            Log.e("PLACES_HELPER", "Place details error: ${e.message}")
            null
        }
    }

    suspend fun getNearbyHospitals(lat: Double, lon: Double): List<NearbyPlaceResult> {
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
        
        // SearchByText is available in SDK 3.3.0+ but let's check if it's there
        // If not, we might need a different approach or REST if SDK version is too old for "New" API
        // Version 3.5.0 should have it.
        
        val center = LatLng(lat, lon)
        val circle = CircularBounds.newInstance(center, 5000.0)
        
        val request = SearchByTextRequest.builder("hospital", placeFields)
            .setLocationBias(circle)
            .setMaxResultCount(10)
            .build()

        return try {
            val response = placesClient.searchByText(request).await()
            Log.d("PLACES_HELPER", "Nearby hospitals success")
            response.places.map { place ->
                NearbyPlaceResult(
                    name = place.name ?: "Hospital",
                    vicinity = place.address,
                    place_id = place.id ?: "",
                    geometry = PlaceGeometry(LatLonLiteral(place.latLng?.latitude ?: 0.0, place.latLng?.longitude ?: 0.0))
                )
            }
        } catch (e: Exception) {
            Log.e("PLACES_HELPER", "Nearby hospitals error: ${e.message}")
            emptyList()
        }
    }
}
