package dev.krydo.mobile.data

import dev.krydo.mobile.network.ApiClientFactory
import dev.krydo.mobile.network.ApiException
import dev.krydo.mobile.network.GenerateZkProofBody
import dev.krydo.mobile.network.VerifyZkProofBody
import dev.krydo.mobile.network.ZkProofDto
import dev.krydo.mobile.network.ZkVerifyResultDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException

class ZkProofRepository(
    private val settingsRepository: SettingsRepository,
    private val apiClientFactory: ApiClientFactory,
) {
    private val _proofs = MutableStateFlow<List<ZkProofDto>>(emptyList())
    val proofs: StateFlow<List<ZkProofDto>> = _proofs.asStateFlow()

    suspend fun refresh(): Result<List<ZkProofDto>> {
        val settings = settingsRepository.settings.first()
        if (settings.holderAddress.isBlank() || settings.authToken.isBlank()) {
            _proofs.value = emptyList()
            return Result.failure(ApiException("Connect Freighter first"))
        }
        return runCatching {
            val list = apiClientFactory.create(settings.apiBaseUrl).listZkProofs(
                authorization = "Bearer ${settings.authToken}",
                address = settings.holderAddress,
            )
            _proofs.value = list
            list
        }.recoverCatching { err ->
            _proofs.value = emptyList()
            throw ApiException(friendlyError(err))
        }
    }

    suspend fun generate(
        credentialId: String,
        proofType: String,
        threshold: Double?,
        targetValue: String?,
    ): Result<ZkProofDto> {
        val settings = settingsRepository.settings.first()
        if (settings.authToken.isBlank()) {
            return Result.failure(ApiException("Connect Freighter first"))
        }
        return runCatching {
            val created = apiClientFactory.create(settings.apiBaseUrl).generateZkProof(
                authorization = "Bearer ${settings.authToken}",
                body = GenerateZkProofBody(
                    credentialId = credentialId,
                    proofType = proofType,
                    threshold = when (proofType) {
                        "range_above", "range_below" -> threshold
                        else -> null
                    },
                    targetValue = when (proofType) {
                        "equality" -> targetValue?.takeIf { it.isNotBlank() }
                        else -> null
                    },
                    clientWillAnchor = true,
                ),
            )
            refresh()
            created
        }.recoverCatching { err ->
            throw ApiException(friendlyError(err))
        }
    }

    suspend fun verifyPublic(proofId: String): Result<ZkVerifyResultDto> {
        val settings = settingsRepository.settings.first()
        return runCatching {
            apiClientFactory.create(settings.apiBaseUrl).verifyZkProof(
                body = VerifyZkProofBody(proofId = proofId),
            )
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
        val proofTypeLabels = mapOf(
            "range_above" to "Value Above Threshold",
            "range_below" to "Value Below Threshold",
            "equality" to "Exact Match",
            "non_zero" to "Non-Zero Proof",
        )

        fun labelFor(proofType: String): String =
            proofTypeLabels[proofType] ?: proofType.replace('_', ' ')
    }
}
