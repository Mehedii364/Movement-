package com.example.data.remote.api

import com.example.data.remote.dto.ApiResponse
import com.example.data.remote.dto.LoginRequest
import com.example.data.remote.dto.LoginResponse
import com.example.data.remote.dto.RemoteMovementDto
import com.example.data.remote.dto.RemotePersonDto
import com.example.data.remote.dto.SyncDownloadResponse
import com.example.data.remote.dto.SyncUploadPayload
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface MovementApiService {

    @POST("api/auth/login.php")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<LoginResponse>>

    @GET("api/person/list.php")
    suspend fun getPersons(): Response<ApiResponse<List<RemotePersonDto>>>

    @POST("api/person/create.php")
    suspend fun createPerson(
        @Body person: RemotePersonDto
    ): Response<ApiResponse<RemotePersonDto>>

    @POST("api/person/update.php")
    suspend fun updatePerson(
        @Body person: RemotePersonDto
    ): Response<ApiResponse<RemotePersonDto>>

    @POST("api/movement/create.php")
    suspend fun createMovement(
        @Body movement: RemoteMovementDto
    ): Response<ApiResponse<RemoteMovementDto>>

    @GET("api/movement/list.php")
    suspend fun getMovements(
        @Query("limit") limit: Int = 100
    ): Response<ApiResponse<List<RemoteMovementDto>>>

    @POST("api/sync/upload.php")
    suspend fun uploadSync(
        @Body payload: SyncUploadPayload
    ): Response<ApiResponse<Map<String, Int>>>

    @GET("api/sync/download.php")
    suspend fun downloadSync(
        @Query("since") sinceTimestamp: Long = 0
    ): Response<ApiResponse<SyncDownloadResponse>>

    companion object {
        fun create(baseUrl: String): MovementApiService {
            val validUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(validUrl)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(MovementApiService::class.java)
        }
    }
}
