package dev.krydo.mobile.data

import dev.krydo.mobile.domain.DemoPresentationBuilder
import dev.krydo.mobile.network.ApiClientFactory
import dev.krydo.mobile.network.ApiException
import dev.krydo.mobile.network.PolicyDto
import dev.krydo.mobile.network.PresentationRequestDto
import dev.krydo.mobile.network.PresentationVerifyResultDto
import dev.krydo.mobile.network.RequestedCredentialDto
import dev.krydo.mobile.network.VerifyPresentationBody
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement
import java.time.Instant
import java.time.temporal.ChronoUnit

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
        if (settings.useMockData) {
            return mockRequest(requestId)
        }
        return try {
            apiClientFactory.create(settings.apiBaseUrl).getPresentationRequest(requestId)
        } catch (err: Exception) {
            throw ApiException(
                "Failed to fetch request from ${settings.apiBaseUrl}: ${err.message ?: "network error"}",
            )
        }
    }

    suspend fun buildDemoPresentation(
        request: PresentationRequestDto,
        credential: StoredCredential,
    ): Pair<JsonElement, String> = DemoPresentationBuilder.build(request, credential)

    suspend fun verifyPresentation(presentation: JsonElement): PresentationVerifyResultDto {
        val settings = settingsRepository.settings.first()
        if (settings.useMockData) {
            return PresentationVerifyResultDto(
                valid = true,
                message = "DEMO verification result — mock only",
                checks = mapOf(
                    "structure" to true,
                    "holderBinding" to true,
                    "challenge" to true,
                    "audience" to true,
                    "expiration" to true,
                    "credentialStatus" to true,
                    "issuerTrusted" to true,
                    "proof" to true,
                    "replay" to false,
                ),
            )
        }
        return try {
            apiClientFactory.create(settings.apiBaseUrl)
                .verifyPresentation(VerifyPresentationBody(presentation))
        } catch (err: Exception) {
            throw ApiException(
                "Verify failed against ${settings.apiBaseUrl}: ${err.message ?: "network error"}",
            )
        }
    }

    private fun mockRequest(requestId: String): PresentationRequestDto {
        val now = Instant.now()
        return PresentationRequestDto(
            id = requestId,
            version = "krydo-vp-request-v1",
            verifier = "GDEMOVERIFIER000000000000000000000000000000000000000",
            audience = "https://verifier.example.invalid",
            challenge = "c".repeat(64),
            reason = "Demo eligibility check (mock request)",
            requestedCredentials = listOf(
                RequestedCredentialDto(claimType = "identity_verification"),
            ),
            policy = PolicyDto(
                kind = "credential_status",
                claimType = "identity_verification",
            ),
            createdAt = now.toString(),
            expiresAt = now.plus(10, ChronoUnit.MINUTES).toString(),
            deepLink = "krydo://present?request=$requestId",
        )
    }
}
