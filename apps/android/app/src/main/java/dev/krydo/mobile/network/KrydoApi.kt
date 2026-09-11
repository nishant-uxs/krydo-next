package dev.krydo.mobile.network

import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface KrydoApi {
    @GET("healthz")
    suspend fun healthz(): HealthzResponse

    @GET("api/presentations/request/{requestId}")
    suspend fun getPresentationRequest(
        @Path("requestId") requestId: String,
    ): PresentationRequestDto

    @POST("api/presentations/verify")
    suspend fun verifyPresentation(
        @Body body: VerifyPresentationBody,
    ): PresentationVerifyResultDto

    @POST("api/verify")
    suspend fun verifyCredential(
        @Body body: VerifyCredentialBody,
    ): Response<JsonElement>
}
