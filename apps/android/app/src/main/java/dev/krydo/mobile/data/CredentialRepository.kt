package dev.krydo.mobile.data

import dev.krydo.mobile.network.ApiClientFactory
import dev.krydo.mobile.network.ApiException
import dev.krydo.mobile.network.ClaimDataParser
import dev.krydo.mobile.network.TxHashLookup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class CredentialRepository(
    private val settingsRepository: SettingsRepository,
    private val apiClientFactory: ApiClientFactory,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _credentials = MutableStateFlow<List<StoredCredential>>(emptyList())
    val credentials: StateFlow<List<StoredCredential>> = _credentials.asStateFlow()

    fun get(id: String): StoredCredential? = _credentials.value.find { it.id == id }

    fun matchForClaim(claimType: String): List<StoredCredential> =
        _credentials.value.filter { it.claimType == claimType && it.status == "active" }

    suspend fun loadOfflineCache() {
        val raw = settingsRepository.loadOfflineCredentialsJson()
        if (raw.isBlank()) return
        runCatching {
            json.decodeFromString(ListSerializer(StoredCredential.serializer()), raw)
        }.onSuccess { cached ->
            if (_credentials.value.isEmpty() && cached.isNotEmpty()) {
                _credentials.value = cached
            }
        }
    }

    suspend fun refresh(): Result<List<StoredCredential>> {
        val settings = settingsRepository.settings.first()
        if (settings.holderAddress.isBlank()) {
            _credentials.value = emptyList()
            return Result.failure(ApiException("Set your Stellar holder address in Settings"))
        }
        if (settings.authToken.isBlank()) {
            loadOfflineCache()
            return if (_credentials.value.isNotEmpty()) {
                Result.success(_credentials.value)
            } else {
                Result.failure(ApiException("Set a JWT auth token in Settings"))
            }
        }
        return runCatching {
            val api = apiClientFactory.create(settings.apiBaseUrl)
            val auth = "Bearer ${settings.authToken}"
            val page = api.listCredentials(
                authorization = auth,
                address = settings.holderAddress,
            )
            val issuerNames = runCatching {
                api.listIssuers().associateBy({ it.walletAddress.uppercase() }, { it.name })
            }.getOrDefault(emptyMap())
            val transactions = runCatching {
                api.listTransactions(authorization = auth, address = settings.holderAddress)
            }.getOrDefault(emptyList())

            val mapped = page.map { dto ->
                val issuerLabel = issuerNames[dto.issuerAddress.uppercase()]
                    ?: (dto.issuerAddress.take(8) + "…")
                val claimValue = ClaimDataParser.extractValue(dto.claimData)
                // Only surface explorer-linkable (wallet-anchored) hashes.
                val txHash = TxHashLookup.forCredential(transactions, dto.credentialHash)
                val summaryBase = dto.claimSummary?.takeIf { it.isNotBlank() } ?: dto.claimType
                val display = if (claimValue != null) {
                    "$summaryBase · value $claimValue"
                } else {
                    summaryBase
                }
                StoredCredential(
                    id = dto.id,
                    title = dto.claimSummary?.takeIf { it.isNotBlank() } ?: dto.claimType,
                    claimType = dto.claimType,
                    issuerName = issuerLabel,
                    issuerAddress = dto.issuerAddress,
                    holderAddress = dto.holderAddress,
                    holderName = settings.holderAddress.take(8) + "…",
                    status = dto.status,
                    issuedAt = dto.issuedAt,
                    expiresAt = dto.expiresAt,
                    credentialHash = dto.credentialHash,
                    displaySummary = display,
                    claimValue = claimValue,
                    onChainTxHash = txHash,
                )
            }
            _credentials.value = mapped
            settingsRepository.saveOfflineCredentialsJson(
                json.encodeToString(ListSerializer(StoredCredential.serializer()), mapped),
            )
            mapped
        }.onFailure {
            loadOfflineCache()
        }
    }
}
