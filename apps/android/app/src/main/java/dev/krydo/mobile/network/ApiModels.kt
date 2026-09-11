package dev.krydo.mobile.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class HealthzResponse(
    val status: String? = null,
    val version: String? = null,
    val uptimeSeconds: Long? = null,
)

@Serializable
data class PolicyDto(
    val kind: String,
    val claimType: String,
    val threshold: Double? = null,
    val targetValue: String? = null,
    val memberSet: List<String>? = null,
    val fields: List<String>? = null,
)

@Serializable
data class RequestedCredentialDto(
    val claimType: String,
)

@Serializable
data class PresentationRequestDto(
    val id: String,
    val version: String = "krydo-vp-request-v1",
    val verifier: String,
    val audience: String,
    val challenge: String,
    val reason: String? = null,
    val requestedCredentials: List<RequestedCredentialDto> = emptyList(),
    val policy: PolicyDto,
    val createdAt: String,
    val expiresAt: String,
    val deepLink: String = "",
)

@Serializable
data class VerifyPresentationBody(
    val presentation: JsonElement,
)

@Serializable
data class CreatePresentationBody(
    val requestId: String,
    val credentialId: String,
    val proofId: String? = null,
)

@Serializable
data class CredentialStatusDto(
    val credentialHash: String? = null,
    val claimType: String? = null,
    val status: String? = null,
    val issuerAddress: String? = null,
    val holderAddress: String? = null,
    val expiresAt: String? = null,
)

@Serializable
data class ProofDto(
    val type: String? = null,
    val proofId: String? = null,
)

@Serializable
data class PresentationVerifyResultDto(
    val valid: Boolean,
    val message: String,
    val checks: Map<String, Boolean>? = null,
    val credential: CredentialStatusDto? = null,
    val issuerName: String? = null,
    val proof: ProofDto? = null,
)

@Serializable
data class VerifyCredentialBody(
    val credentialHash: String,
)

@Serializable
data class CredentialDto(
    val id: String,
    val credentialHash: String,
    val issuerAddress: String,
    val holderAddress: String,
    val claimType: String,
    val claimSummary: String? = null,
    val status: String,
    val issuedAt: String,
    val expiresAt: String? = null,
)

class ApiException(
    message: String,
    val status: Int = 0,
) : Exception(message)
