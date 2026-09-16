package com.mitraroute.ai.data.repository

import com.mitraroute.ai.data.model.Incident
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class IncidentType {
    ACCIDENT, LANDSLIDE, FLOOD, ROAD_CLOSURE, CONSTRUCTION, TRAFFIC, WEATHER_WARNING, OTHER
}

interface IncidentRepository {
    suspend fun getVerifiedIncidents(): Flow<List<Incident>>
    suspend fun getIncidentsNear(lat: Double, lng: Double, radiusKm: Double = 50.0): Flow<List<Incident>>
}

class IncidentRepositoryImpl(private val repo: SetuMitraRepository) : IncidentRepository {
    override suspend fun getVerifiedIncidents(): Flow<List<Incident>> {
        return repo.getIncidents()
    }

    override suspend fun getIncidentsNear(lat: Double, lng: Double, radiusKm: Double): Flow<List<Incident>> {
        return repo.getIncidents().map { list ->
            list.filter { incident ->
                calculateDistance(lat, lng, incident.lat, incident.lon) <= radiusKm
            }
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
