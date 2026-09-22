package com.msebenzi.app.data

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val role: String, // "worker" | "household"
    val language: String,
    val skills: String? = null,
    val bio: String? = null,
    val verificationStatus: String = "unverified", // "unverified" | "pending" | "verified"
    val ratingAverage: Double = 0.0,
    val ratingCount: Int = 0
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String,
    val language: String = "en"
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val message: String,
    val token: String? = null,
    val user: User
)

data class UpdateProfileRequest(
    val name: String? = null,
    val language: String? = null,
    val skills: String? = null,
    val bio: String? = null
)
