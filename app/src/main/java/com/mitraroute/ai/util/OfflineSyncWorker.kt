package com.mitraroute.ai.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mitraroute.ai.data.repository.SetuMitraRepository

class OfflineSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repository = SetuMitraRepository.getInstance(applicationContext)
            val synced = repository.syncOfflineData()
            if (synced > 0) {
                Result.success()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
