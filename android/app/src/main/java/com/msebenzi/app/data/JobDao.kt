package com.msebenzi.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface JobDao {

    @Query("SELECT * FROM jobs ORDER BY id DESC")
    suspend fun getAllJobs(): List<Job>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(jobs: List<Job>)

    @Query("DELETE FROM jobs")
    suspend fun clearAll()

    /** Replaces the cached feed with a fresh one from the API. */
    suspend fun replaceAll(jobs: List<Job>) {
        clearAll()
        insertAll(jobs)
    }
}

/**
 * Represents a job-related action taken while offline (e.g. "accept this
 * job"), queued for WorkManager to replay once connectivity returns —
 * implements the "offline job actions" / sync outbox design from Part 1.
 */
@androidx.room.Entity(tableName = "sync_actions")
data class SyncAction(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Int = 0,
    val actionType: String, // e.g. "ACCEPT_JOB"
    val targetJobId: Int,
    val synced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface SyncActionDao {

    @Insert
    suspend fun enqueue(action: SyncAction): Long

    @Query("SELECT * FROM sync_actions WHERE synced = 0 ORDER BY createdAt ASC")
    suspend fun getPending(): List<SyncAction>

    @Query("UPDATE sync_actions SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Int)
}
