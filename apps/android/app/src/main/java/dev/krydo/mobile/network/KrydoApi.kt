package dev.krydo.mobile.network

import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface KrydoApi {
    @GET("healthz")
    suspend fun healthz(): HealthzResponse

    @GET("api/presentations/request/{requestId}")
    suspend fun getPresentationRequest(
        @Path("requestId") requestId: String,
    ): PresentationRequestDto

    @POST("api/presentations/create")
    suspend fun createPresentation(
        @Header("Authorization") authorization: String,
        @Body body: CreatePresentationBody,
    ): JsonElement

    @POST("api/presentations/verify")
    suspend fun verifyPresentation(
        @Body body: VerifyPresentationBody,
    ): PresentationVerifyResultDto

    @GET("api/credentials/{address}")
    suspend fun listCredentials(
        @Header("Authorization") authorization: String,
        @Path("address") address: String,
    ): List<CredentialDto>

    @POST("api/verify")
    suspend fun verifyCredential(
        @Body body: VerifyCredentialBody,
    ): Response<JsonElement>
}
