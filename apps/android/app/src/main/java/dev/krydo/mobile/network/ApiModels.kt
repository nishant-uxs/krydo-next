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
    val claimData: JsonElement? = null,
    val status: String,
    val issuedAt: String,
    val expiresAt: String? = null,
    val txHash: String? = null,
)

@Serializable
data class IssuerDto(
    val id: String,
    val walletAddress: String,
    val name: String,
    val description: String? = null,
    val category: String = "general",
    val active: Boolean = true,
    val approvedBy: String? = null,
    val approvedAt: String? = null,
    val revokedAt: String? = null,
    val onChain: Boolean? = null,
)

@Serializable
data class CredentialRequestDto(
    val id: String,
    val requesterAddress: String,
    val issuerAddress: String? = null,
    val issuerCategory: String? = null,
    val claimType: String,
    val message: String? = null,
    val status: String,
    val responseMessage: String? = null,
    val credentialId: String? = null,
    val onChainTxHash: String? = null,
    val createdAt: String? = null,
    val respondedAt: String? = null,
)

@Serializable
data class CreateCredentialRequestBody(
    val claimType: String,
    val issuerAddress: String? = null,
    val issuerCategory: String? = null,
    val message: String? = null,
    val clientWillAnchor: Boolean = true,
)

@Serializable
data class TransactionDto(
    val id: String,
    val txHash: String,
    val action: String,
    val fromAddress: String? = null,
    val toAddress: String? = null,
    val data: JsonElement? = null,
    val blockNumber: String? = null,
    val timestamp: String? = null,
)

@Serializable
data class ZkProofDto(
    val id: String,
    val credentialId: String,
    val proverAddress: String? = null,
    val proofType: String,
    val commitment: String? = null,
    val verified: Boolean = false,
    val onChainTxHash: String? = null,
    val onChainStatus: String? = null,
    val createdAt: String? = null,
    val expiresAt: String? = null,
    val claimType: String? = null,
    val claimSummary: String? = null,
    val credentialHash: String? = null,
    val txHash: String? = null,
)

@Serializable
data class GenerateZkProofBody(
    val credentialId: String,
    val proofType: String,
    val threshold: Double? = null,
    val targetValue: String? = null,
    val clientWillAnchor: Boolean = true,
)

@Serializable
data class VerifyZkProofBody(
    val proofId: String,
)

@Serializable
data class RespondCredentialRequestBody(
    val status: String,
    val responseMessage: String? = null,
    val claimSummary: String? = null,
    val claimValue: String? = null,
    val offChainOk: Boolean = false,
)

@Serializable
data class AuthMeResponse(
    val wallet: WalletMeDto,
)

@Serializable
data class WalletMeDto(
    val address: String,
    val role: String = "user",
    val label: String? = null,
)

@Serializable
data class ZkVerifyResultDto(
    val valid: Boolean,
    val reason: String? = null,
    val cryptographicallyValid: Boolean? = null,
    val proof: ZkProofBriefDto? = null,
    val credential: ZkCredentialBriefDto? = null,
    val issuer: ZkIssuerBriefDto? = null,
)

@Serializable
data class ZkProofBriefDto(
    val id: String? = null,
    val proofType: String? = null,
    val commitment: String? = null,
    val onChainTxHash: String? = null,
)

@Serializable
data class ZkCredentialBriefDto(
    val claimType: String? = null,
    val status: String? = null,
)

@Serializable
data class ZkIssuerBriefDto(
    val name: String? = null,
    val active: Boolean? = null,
)

class ApiException(
    message: String,
    val status: Int = 0,
) : Exception(message)
