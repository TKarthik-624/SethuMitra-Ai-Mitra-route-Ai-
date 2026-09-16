package com.mitraroute.ai

import android.app.Application
import org.osmdroid.config.Configuration
import androidx.work.*
import com.mitraroute.ai.util.OfflineSyncWorker
import com.google.android.libraries.places.api.Places
import java.util.concurrent.TimeUnit

class MitraRouteApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Places SDK (New)
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(this, BuildConfig.GOOGLE_MAPS_KEY)
        }

        // Initialize osmdroid configuration
        Configuration.getInstance().apply {
            userAgentValue = "MitraRouteAI-NER-Logistics-Platform"
            osmdroidBasePath = getExternalFilesDir(null)
            osmdroidTileCache = getExternalFilesDir("tiles")
        }

        // Schedule periodic offline sync
        val syncRequest = PeriodicWorkRequestBuilder<OfflineSyncWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "offline_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
