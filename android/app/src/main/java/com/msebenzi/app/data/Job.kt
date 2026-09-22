package com.msebenzi.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * A single job card. This class does double duty:
 *  - It's the JSON model Retrofit deserializes from the REST API.
 *  - It's also the Room entity cached locally so the job feed still shows
 *    something useful when the device is offline (see AppDatabase / JobDao).
 */
@Entity(tableName = "jobs")
data class Job(
    @PrimaryKey
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val location: String,
    val budget: Double,
    val status: String, // "open" | "accepted" | "completed" | "cancelled"
    @SerializedName("postedById") val postedById: Int,
    @SerializedName("acceptedById") val acceptedById: Int? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

/** Request body for creating a job (household → POST /api/jobs). */
data class CreateJobRequest(
    val title: String,
    val description: String,
    val category: String,
    val location: String,
    val budget: Double
)
