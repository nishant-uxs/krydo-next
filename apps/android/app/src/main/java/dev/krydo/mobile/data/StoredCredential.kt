package dev.krydo.mobile.data

import kotlinx.serialization.Serializable

@Serializable
data class StoredCredential(
    val id: String,
    val title: String,
    val claimType: String,
    val issuerName: String,
    val issuerAddress: String,
    val holderAddress: String,
    val holderName: String,
    val status: String,
    val issuedAt: String,
    val expiresAt: String? = null,
    val credentialHash: String,
    val displaySummary: String,
    val claimValue: String? = null,
    val onChainTxHash: String? = null,
)
