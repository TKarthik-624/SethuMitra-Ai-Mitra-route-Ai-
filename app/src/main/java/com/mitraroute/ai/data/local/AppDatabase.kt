package com.mitraroute.ai.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ---------- Entities ----------

@Entity(tableName = "cached_routes")
data class CachedRoute(
    @PrimaryKey val id: Int,
    val name: String,
    val startLat: Double,
    val startLon: Double,
    val endLat: Double,
    val endLon: Double,
    val distanceKm: Double?,
    val roadType: String,
    val riskLevel: String?,
    val riskScore: Double?,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_incidents")
data class CachedIncident(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val routeId: Int?,
    val incidentType: String,
    val severity: Int,
    val description: String?,
    val lat: Double,
    val lon: Double,
    val status: String = "active",
    val createdAt: String
)

@Entity(tableName = "offline_queue")
data class OfflineIncidentQueue(
    @PrimaryKey(autoGenerate = true) val queueId: Int = 0,
    val incidentType: String,
    val severity: Int,
    val description: String?,
    val lat: Double,
    val lon: Double,
    val routeId: Int?,
    val clientTimestamp: String,
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)

@Entity(tableName = "cached_weather")
data class CachedWeather(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lat: Double,
    val lon: Double,
    val temperature: Double?,
    val windspeed: Double?,
    val weathercode: Int?,
    val rainSum: Double?,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "telemetry")
data class CachedTelemetry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lat: Double,
    val lon: Double,
    val speed: Float,
    val bearing: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)

// ---------- DAOs ----------

@Dao
interface RouteDao {
    @Query("SELECT * FROM cached_routes ORDER BY cachedAt DESC")
    fun getAllRoutes(): Flow<List<CachedRoute>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutes(routes: List<CachedRoute>)

    @Query("DELETE FROM cached_routes")
    suspend fun clearAll()
}

@Dao
interface IncidentDao {
    @Query("SELECT * FROM cached_incidents ORDER BY createdAt DESC")
    fun getAllIncidents(): Flow<List<CachedIncident>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncidents(incidents: List<CachedIncident>)

    @Query("DELETE FROM cached_incidents")
    suspend fun clearAll()
}

@Dao
interface OfflineQueueDao {
    @Query("SELECT * FROM offline_queue WHERE synced = 0 ORDER BY createdAt DESC")
    fun getPendingIncidents(): Flow<List<OfflineIncidentQueue>>

    @Insert
    suspend fun insertOfflineIncident(incident: OfflineIncidentQueue)

    @Query("UPDATE offline_queue SET synced = 1 WHERE queueId = :queueId")
    suspend fun markSynced(queueId: Int)

    @Query("SELECT * FROM offline_queue WHERE synced = 0")
    suspend fun getPendingList(): List<OfflineIncidentQueue>

    @Query("DELETE FROM offline_queue WHERE synced = 1")
    suspend fun clearSynced()
}

@Dao
interface WeatherDao {
    @Query("SELECT * FROM cached_weather ORDER BY cachedAt DESC LIMIT 10")
    fun getRecentWeather(): Flow<List<CachedWeather>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weather: List<CachedWeather>)

    @Query("DELETE FROM cached_weather")
    suspend fun clearAll()
}

@Dao
interface TelemetryDao {
    @Insert
    suspend fun insertTelemetry(data: CachedTelemetry)

    @Query("SELECT * FROM telemetry WHERE synced = 0 ORDER BY timestamp ASC")
    suspend fun getPendingTelemetry(): List<CachedTelemetry>

    @Query("UPDATE telemetry SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Int>)

    @Query("DELETE FROM telemetry WHERE synced = 1")
    suspend fun clearSynced()
}

// ---------- Database ----------

@Database(
    entities = [
        CachedRoute::class,
        CachedIncident::class,
        OfflineIncidentQueue::class,
        CachedWeather::class,
        CachedTelemetry::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun incidentDao(): IncidentDao
    abstract fun offlineQueueDao(): OfflineQueueDao
    abstract fun weatherDao(): WeatherDao
    abstract fun telemetryDao(): TelemetryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "setumitra_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
