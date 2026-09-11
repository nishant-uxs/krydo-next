package dev.krydo.mobile.data

import dev.krydo.mobile.network.ApiClientFactory
import dev.krydo.mobile.network.ApiException
import dev.krydo.mobile.network.CreatePresentationBody
import dev.krydo.mobile.network.PresentationRequestDto
import dev.krydo.mobile.network.PresentationVerifyResultDto
import dev.krydo.mobile.network.VerifyPresentationBody
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement

class PresentationRepository(
    private val settingsRepository: SettingsRepository,
    private val apiClientFactory: ApiClientFactory,
) {
    suspend fun testConnection(): Result<String> {
        val settings = settingsRepository.settings.first()
        return runCatching {
            val api = apiClientFactory.create(settings.apiBaseUrl)
            val health = api.healthz()
            "OK — ${health.status ?: "up"} (version ${health.version ?: "?"})"
        }.recoverCatching { err ->
            throw ApiException(
                "Connection failed for ${settings.apiBaseUrl}: ${err.message ?: err.javaClass.simpleName}",
            )
        }
    }

    suspend fun getRequest(requestId: String): PresentationRequestDto {
        val settings = settingsRepository.settings.first()
        return try {
            apiClientFactory.create(settings.apiBaseUrl).getPresentationRequest(requestId)
        } catch (err: Exception) {
            throw ApiException(
                "Failed to fetch request from ${settings.apiBaseUrl}: ${err.message ?: "network error"}",
            )
        }
    }

    suspend fun createPresentation(requestId: String, credentialId: String): JsonElement {
        val settings = settingsRepository.settings.first()
        if (settings.authToken.isBlank()) {
            throw ApiException("Auth token required to create a presentation")
        }
        return try {
            apiClientFactory.create(settings.apiBaseUrl).createPresentation(
                authorization = "Bearer ${settings.authToken}",
                body = CreatePresentationBody(
                    requestId = requestId,
                    credentialId = credentialId,
                ),
            )
        } catch (err: Exception) {
            throw ApiException(
                "Create presentation failed: ${err.message ?: "network error"}",
            )
        }
    }

    suspend fun verifyPresentation(presentation: JsonElement): PresentationVerifyResultDto {
        val settings = settingsRepository.settings.first()
        return try {
            apiClientFactory.create(settings.apiBaseUrl)
                .verifyPresentation(VerifyPresentationBody(presentation))
        } catch (err: Exception) {
            throw ApiException(
                "Verify failed against ${settings.apiBaseUrl}: ${err.message ?: "network error"}",
            )
        }
    }
}
