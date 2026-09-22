package com.msebenzi.app.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Runs periodically (and can be triggered immediately once connectivity
 * returns) to replay any queued offline actions — e.g. a job acceptance
 * that happened while the device had no signal.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val session = SessionManager(applicationContext)
        val token = session.getBearerToken() ?: return Result.success() // not logged in, nothing to do
        val api = RetrofitClient.api

        val pending = db.syncActionDao().getPending()
        if (pending.isEmpty()) return Result.success()

        var allSucceeded = true

        for (action in pending) {
            try {
                when (action.actionType) {
                    "ACCEPT_JOB" -> {
                        val response = api.acceptJob(token, action.targetJobId)
                        if (response.isSuccessful) {
                            db.syncActionDao().markSynced(action.id)
                        } else {
                            allSucceeded = false
                        }
                    }
                    else -> Log.w("SyncWorker", "Unknown action type: ${action.actionType}")
                }
            } catch (e: Exception) {
                Log.w("SyncWorker", "Sync attempt failed, will retry later: ${e.message}")
                allSucceeded = false
            }
        }

        return if (allSucceeded) Result.success() else Result.retry()
    }
}
