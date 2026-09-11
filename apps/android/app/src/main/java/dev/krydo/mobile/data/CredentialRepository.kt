package dev.krydo.mobile.data

import dev.krydo.mobile.network.ApiClientFactory
import dev.krydo.mobile.network.ApiException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class CredentialRepository(
    private val settingsRepository: SettingsRepository,
    private val apiClientFactory: ApiClientFactory,
) {
    private val _credentials = MutableStateFlow<List<StoredCredential>>(emptyList())
    val credentials: StateFlow<List<StoredCredential>> = _credentials.asStateFlow()

    fun get(id: String): StoredCredential? = _credentials.value.find { it.id == id }

    fun matchForClaim(claimType: String): List<StoredCredential> =
        _credentials.value.filter { it.claimType == claimType && it.status == "active" }

    suspend fun refresh(): Result<List<StoredCredential>> {
        val settings = settingsRepository.settings.first()
        if (settings.holderAddress.isBlank()) {
            _credentials.value = emptyList()
            return Result.failure(ApiException("Set your Stellar holder address in Settings"))
        }
        if (settings.authToken.isBlank()) {
            _credentials.value = emptyList()
            return Result.failure(ApiException("Set a JWT auth token in Settings (from SIWS login on the web app)"))
        }
        return runCatching {
            val page = apiClientFactory.create(settings.apiBaseUrl).listCredentials(
                authorization = "Bearer ${settings.authToken}",
                address = settings.holderAddress,
            )
            val mapped = page.map { dto ->
                StoredCredential(
                    id = dto.id,
                    title = dto.claimSummary?.takeIf { it.isNotBlank() } ?: dto.claimType,
                    claimType = dto.claimType,
                    issuerName = dto.issuerAddress.take(8) + "…",
                    issuerAddress = dto.issuerAddress,
                    holderAddress = dto.holderAddress,
                    holderName = settings.holderAddress.take(8) + "…",
                    status = dto.status,
                    issuedAt = dto.issuedAt,
                    expiresAt = dto.expiresAt,
                    credentialHash = dto.credentialHash,
                    displaySummary = dto.claimSummary ?: dto.claimType,
                )
            }
            _credentials.value = mapped
            mapped
        }.onFailure {
            _credentials.value = emptyList()
        }
    }
}
