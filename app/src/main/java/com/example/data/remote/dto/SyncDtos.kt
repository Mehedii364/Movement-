package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: T? = null
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "token") val token: String,
    @Json(name = "user_id") val userId: Long,
    @Json(name = "username") val username: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "role") val role: String
)

@JsonClass(generateAdapter = true)
data class RemotePersonDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "person_code") val personCode: String,
    @Json(name = "person_name") val personName: String,
    @Json(name = "designation") val designation: String,
    @Json(name = "department") val department: String,
    @Json(name = "mobile") val mobile: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "current_status") val currentStatus: String = "INSIDE"
)

@JsonClass(generateAdapter = true)
data class RemoteMovementDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "uuid") val uuid: String,
    @Json(name = "person_code") val personCode: String,
    @Json(name = "movement_type") val movementType: String,
    @Json(name = "purpose") val purpose: String,
    @Json(name = "destination") val destination: String,
    @Json(name = "remarks") val remarks: String = "",
    @Json(name = "movement_timestamp") val movementTimestamp: Long,
    @Json(name = "duration_minutes") val durationMinutes: Long? = null,
    @Json(name = "recorded_by") val recordedBy: String
)

@JsonClass(generateAdapter = true)
data class SyncUploadPayload(
    @Json(name = "persons") val persons: List<RemotePersonDto>,
    @Json(name = "movements") val movements: List<RemoteMovementDto>
)

@JsonClass(generateAdapter = true)
data class SyncDownloadResponse(
    @Json(name = "persons") val persons: List<RemotePersonDto>,
    @Json(name = "movements") val movements: List<RemoteMovementDto>,
    @Json(name = "server_time") val serverTime: Long
)
