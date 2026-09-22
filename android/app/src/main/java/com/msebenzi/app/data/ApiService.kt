package com.msebenzi.app.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Defines every REST call the app makes against the Msebenzi API
 * (see /backend in the project root). Retrofit generates the actual
 * networking code from this interface at compile time.
 */
interface ApiService {

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @GET("api/users/me")
    suspend fun getProfile(@Header("Authorization") token: String): Response<User>

    @PUT("api/users/me")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body body: UpdateProfileRequest
    ): Response<Map<String, Any>>

    @GET("api/jobs")
    suspend fun getJobs(
        @Header("Authorization") token: String,
        @Query("status") status: String? = null
    ): Response<List<Job>>

    @GET("api/jobs/{id}")
    suspend fun getJob(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Job>

    @POST("api/jobs")
    suspend fun createJob(
        @Header("Authorization") token: String,
        @Body body: CreateJobRequest
    ): Response<Job>

    @PUT("api/jobs/{id}/accept")
    suspend fun acceptJob(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Map<String, Any>>

    @PUT("api/jobs/{id}/complete")
    suspend fun completeJob(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Map<String, Any>>

    // ---- Feature: Trusted ratings and reviews ----

    @POST("api/jobs/{id}/rate")
    suspend fun rateJob(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body body: SubmitRatingRequest
    ): Response<Map<String, Any>>

    @GET("api/users/{id}/ratings")
    suspend fun getRatingsForUser(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<List<Rating>>

    // ---- Feature: Worker profile and verification ----

    @GET("api/users/{id}")
    suspend fun getUserProfile(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<User>

    @PUT("api/users/me/request-verification")
    suspend fun requestVerification(
        @Header("Authorization") token: String
    ): Response<Map<String, Any>>

    // ---- Feature: Targeted job alerts (in-app) ----

    @GET("api/notifications")
    suspend fun getNotifications(
        @Header("Authorization") token: String
    ): Response<List<AppNotification>>

    @GET("api/notifications/unread-count")
    suspend fun getUnreadNotificationCount(
        @Header("Authorization") token: String
    ): Response<Map<String, Int>>

    @PUT("api/notifications/{id}/read")
    suspend fun markNotificationRead(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<AppNotification>
}
