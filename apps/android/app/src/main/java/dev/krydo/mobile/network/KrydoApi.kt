package dev.krydo.mobile.network

import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
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

    @GET("api/issuers")
    suspend fun listIssuers(): List<IssuerDto>

    @POST("api/credential-requests")
    suspend fun createCredentialRequest(
        @Header("Authorization") authorization: String,
        @Body body: CreateCredentialRequestBody,
    ): CredentialRequestDto

    @GET("api/credential-requests/user/{address}")
    suspend fun listMyCredentialRequests(
        @Header("Authorization") authorization: String,
        @Path("address") address: String,
    ): List<CredentialRequestDto>

    @GET("api/credential-requests/issuer/{address}")
    suspend fun listIssuerCredentialRequests(
        @Header("Authorization") authorization: String,
        @Path("address") address: String,
    ): List<CredentialRequestDto>

    @POST("api/credential-requests/{id}/respond")
    suspend fun respondCredentialRequest(
        @Header("Authorization") authorization: String,
        @Path("id") id: String,
        @Body body: RespondCredentialRequestBody,
    ): JsonElement

    @DELETE("api/credential-requests/{id}")
    suspend fun deleteCredentialRequest(
        @Header("Authorization") authorization: String,
        @Path("id") id: String,
    ): Response<JsonElement>

    @GET("api/auth/me")
    suspend fun authMe(
        @Header("Authorization") authorization: String,
    ): AuthMeResponse

    @GET("api/zk/proofs/{address}")
    suspend fun listZkProofs(
        @Header("Authorization") authorization: String,
        @Path("address") address: String,
    ): List<ZkProofDto>

    @POST("api/zk/generate")
    suspend fun generateZkProof(
        @Header("Authorization") authorization: String,
        @Body body: GenerateZkProofBody,
    ): ZkProofDto

    @POST("api/zk/verify")
    suspend fun verifyZkProof(
        @Body body: VerifyZkProofBody,
    ): ZkVerifyResultDto

    @GET("api/transactions/{address}")
    suspend fun listTransactions(
        @Header("Authorization") authorization: String,
        @Path("address") address: String,
    ): List<TransactionDto>
}
