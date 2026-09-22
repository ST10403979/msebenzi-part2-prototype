package com.msebenzi.app

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.msebenzi.app.data.SyncWorker
import java.util.concurrent.TimeUnit

class MsebenziApp : Application() {

    override fun onCreate() {
        super.onCreate()
        schedulePeriodicSync()
    }

    /**
     * Schedules SyncWorker to run whenever the device has connectivity, so
     * any queued offline actions (e.g. a job acceptance made while offline)
     * get pushed to the server automatically.
     */
    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "msebenzi_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
