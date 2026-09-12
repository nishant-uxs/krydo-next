package dev.krydo.mobile.data

import dev.krydo.mobile.network.ApiClientFactory
import dev.krydo.mobile.network.ApiException
import dev.krydo.mobile.network.CreateCredentialRequestBody
import dev.krydo.mobile.network.CredentialRequestDto
import dev.krydo.mobile.network.IssuerDto
import dev.krydo.mobile.network.RespondCredentialRequestBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

import retrofit2.HttpException
import java.io.IOException

class IssuerRequestRepository(
    private val settingsRepository: SettingsRepository,
    private val apiClientFactory: ApiClientFactory,
) {
    private val _issuers = MutableStateFlow<List<IssuerDto>>(emptyList())
    val issuers: StateFlow<List<IssuerDto>> = _issuers.asStateFlow()

    private val _requests = MutableStateFlow<List<CredentialRequestDto>>(emptyList())
    val requests: StateFlow<List<CredentialRequestDto>> = _requests.asStateFlow()

    private val _inbox = MutableStateFlow<List<CredentialRequestDto>>(emptyList())
    val inbox: StateFlow<List<CredentialRequestDto>> = _inbox.asStateFlow()

    suspend fun refreshIssuers(): Result<List<IssuerDto>> {
        val settings = settingsRepository.settings.first()
        return runCatching {
            val list = apiClientFactory.create(settings.apiBaseUrl).listIssuers()
                .filter { it.active }
            _issuers.value = list
            list
        }.recoverCatching { err ->
            _issuers.value = emptyList()
            throw ApiException(friendlyError(err))
        }
    }

    suspend fun refreshMyRequests(): Result<List<CredentialRequestDto>> {
        val settings = settingsRepository.settings.first()
        if (settings.holderAddress.isBlank() || settings.authToken.isBlank()) {
            _requests.value = emptyList()
            return Result.failure(ApiException("Connect Freighter first"))
        }
        return runCatching {
            val list = apiClientFactory.create(settings.apiBaseUrl).listMyCredentialRequests(
                authorization = "Bearer ${settings.authToken}",
                address = settings.holderAddress,
            )
            _requests.value = list
            list
        }.recoverCatching { err ->
            _requests.value = emptyList()
            throw ApiException(friendlyError(err))
        }
    }

    suspend fun requestCredential(
        claimType: String,
        issuer: IssuerDto?,
        message: String?,
    ): Result<CredentialRequestDto> {
        val settings = settingsRepository.settings.first()
        if (settings.authToken.isBlank()) {
            return Result.failure(ApiException("Connect Freighter first"))
        }
        return runCatching {
            val created = apiClientFactory.create(settings.apiBaseUrl).createCredentialRequest(
                authorization = "Bearer ${settings.authToken}",
                body = CreateCredentialRequestBody(
                    claimType = claimType,
                    issuerAddress = issuer?.walletAddress,
                    issuerCategory = issuer?.category,
                    message = message?.takeIf { it.isNotBlank() },
                    clientWillAnchor = true,
                ),
            )
            refreshMyRequests()
            created
        }.recoverCatching { err ->
            throw ApiException(friendlyError(err))
        }
    }

    suspend fun cancelRequest(id: String): Result<Unit> {
        val settings = settingsRepository.settings.first()
        if (settings.authToken.isBlank()) {
            return Result.failure(ApiException("Not authenticated"))
        }
        return runCatching {
            val resp = apiClientFactory.create(settings.apiBaseUrl).deleteCredentialRequest(
                authorization = "Bearer ${settings.authToken}",
                id = id,
            )
            if (!resp.isSuccessful) {
                throw ApiException("Cancel failed (${resp.code()})", resp.code())
            }
            refreshMyRequests()
            Unit
        }.recoverCatching { err ->
            throw ApiException(friendlyError(err))
        }
    }

    suspend fun refreshIssuerInbox(): Result<List<CredentialRequestDto>> {
        val settings = settingsRepository.settings.first()
        if (settings.holderAddress.isBlank() || settings.authToken.isBlank()) {
            _inbox.value = emptyList()
            return Result.failure(ApiException("Connect as issuer first"))
        }
        return runCatching {
            val list = apiClientFactory.create(settings.apiBaseUrl).listIssuerCredentialRequests(
                authorization = "Bearer ${settings.authToken}",
                address = settings.holderAddress,
            )
            _inbox.value = list
            list
        }.recoverCatching { err ->
            _inbox.value = emptyList()
            throw ApiException(friendlyError(err))
        }
    }

    suspend fun rejectRequest(id: String, message: String?): Result<Unit> {
        val settings = settingsRepository.settings.first()
        if (settings.authToken.isBlank()) {
            return Result.failure(ApiException("Not authenticated"))
        }
        return runCatching {
            apiClientFactory.create(settings.apiBaseUrl).respondCredentialRequest(
                authorization = "Bearer ${settings.authToken}",
                id = id,
                body = RespondCredentialRequestBody(
                    status = "rejected",
                    responseMessage = message?.takeIf { it.isNotBlank() } ?: "Rejected",
                ),
            )
            refreshIssuerInbox()
            Unit
        }.recoverCatching { err ->
            throw ApiException(friendlyError(err))
        }
    }

    suspend fun approveAndIssue(
        id: String,
        claimSummary: String,
        claimValue: String,
        responseMessage: String?,
    ): Result<Unit> {
        val settings = settingsRepository.settings.first()
        if (settings.authToken.isBlank()) {
            return Result.failure(ApiException("Not authenticated"))
        }
        return runCatching {
            apiClientFactory.create(settings.apiBaseUrl).respondCredentialRequest(
                authorization = "Bearer ${settings.authToken}",
                id = id,
                body = RespondCredentialRequestBody(
                    status = "approved",
                    responseMessage = responseMessage?.takeIf { it.isNotBlank() } ?: "Credential issued",
                    claimSummary = claimSummary.trim(),
                    claimValue = claimValue.trim(),
                    offChainOk = true,
                ),
            )
            refreshIssuerInbox()
            Unit
        }.recoverCatching { err ->
            throw ApiException(friendlyError(err))
        }
    }

    private fun friendlyError(err: Throwable): String = when (err) {
        is ApiException -> err.message ?: "Request failed"
        is HttpException -> {
            val body = err.response()?.errorBody()?.string().orEmpty()
            Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.getOrNull(1)
                ?: "HTTP ${err.code()}"
        }
        is IOException -> "Network error — check API URL"
        else -> err.message ?: "Something went wrong"
    }

    companion object {
        val claimTypeLabels = mapOf(
            "credit_score" to "Credit Score Range",
            "income_verification" to "Income Verification",
            "asset_proof" to "Asset Proof",
            "debt_ratio" to "Debt-to-Income Ratio",
            "payment_history" to "Payment History",
            "identity_verification" to "Identity Verification",
        )

        fun claimTypesForCategory(category: String?): List<String> = when (category) {
            "credit_bureau" -> listOf("credit_score", "debt_ratio", "payment_history")
            "income_verifier" -> listOf("income_verification")
            "identity_provider" -> listOf("identity_verification")
            "asset_auditor" -> listOf("asset_proof")
            "employment_verifier" -> listOf("income_verification", "identity_verification")
            "tax_authority" -> listOf("income_verification", "debt_ratio", "asset_proof")
            "insurance_provider" -> listOf("asset_proof", "identity_verification")
            else -> claimTypeLabels.keys.toList()
        }

        fun labelForClaim(claimType: String): String =
            claimTypeLabels[claimType] ?: claimType.replace('_', ' ')
    }
}

