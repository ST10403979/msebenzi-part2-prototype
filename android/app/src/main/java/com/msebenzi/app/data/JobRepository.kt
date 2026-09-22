package com.msebenzi.app.data

import android.content.Context
import android.util.Log

/**
 * Combines the local Room cache and the remote REST API, following the
 * offline-first repository pattern described in the Part 1 design
 * (local database as the source of truth, network used to refresh it).
 */
class JobRepository(context: Context) {

    private val jobDao = AppDatabase.getInstance(context).jobDao()
    private val syncActionDao = AppDatabase.getInstance(context).syncActionDao()
    private val api = RetrofitClient.api

    companion object {
        private const val TAG = "JobRepository"
    }

    /**
     * Tries the network first; on success it refreshes the local cache and
     * returns fresh data. On any failure (no connectivity, server down,
     * timeout) it falls back to whatever is cached locally so the feed
     * screen never shows a hard error for a connectivity blip.
     */
    suspend fun getJobFeed(token: String): Result<List<Job>> {
        return try {
            val response = api.getJobs(token)
            if (response.isSuccessful && response.body() != null) {
                val jobs = response.body()!!
                jobDao.replaceAll(jobs)
                Result.success(jobs)
            } else {
                Log.w(TAG, "Server returned ${response.code()}, using local cache")
                Result.success(jobDao.getAllJobs())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network error, falling back to local cache: ${e.message}")
            Result.success(jobDao.getAllJobs())
        }
    }

    suspend fun createJob(token: String, request: CreateJobRequest): Result<Job> {
        return try {
            val response = api.createJob(token, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Accepts a job. If the device is offline, the action is queued in the
     * sync outbox instead of failing outright — WorkManager (SyncWorker)
     * replays it once connectivity returns.
     */
    suspend fun acceptJob(token: String, jobId: Int): Result<String> {
        return try {
            val response = api.acceptJob(token, jobId)
            if (response.isSuccessful) {
                Result.success("Job accepted")
            } else {
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Offline — queuing accept action for job $jobId")
            syncActionDao.enqueue(SyncAction(actionType = "ACCEPT_JOB", targetJobId = jobId))
            Result.success("You're offline — this will sync automatically once you're back online")
        }
    }
}
