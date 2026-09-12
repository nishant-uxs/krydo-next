package dev.krydo.mobile.wallet

import dev.krydo.mobile.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** Freighter WC one-tap login (no SEP-53 sign popup). */
class SiwsClient(
    private val settingsRepository: SettingsRepository,
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    data class AuthResult(val token: String, val address: String)

    /** After Freighter Approves the WC session — single popup login. */
    suspend fun authenticateFromWalletConnect(
        address: String,
        chainId: String,
        topic: String,
    ): AuthResult = withContext(Dispatchers.IO) {
        val base = settingsRepository.settings.first().apiBaseUrl.trimEnd('/')
        val body = buildJsonObject {
            put("address", address)
            put("chainId", chainId)
            put("topic", topic)
            put("provider", "freighter-wc")
        }.toString()
        val resp = http.newCall(
            Request.Builder()
                .url("$base/api/auth/wc-session")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build(),
        ).execute()
        val text = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
            throw IllegalStateException("WC session login failed: $text")
        }
        val verified = json.decodeFromString(VerifyResponse.serializer(), text)
        AuthResult(token = verified.token, address = verified.wallet.address)
    }

    @Serializable
    private data class VerifyResponse(
        val token: String,
        val wallet: WalletPayload,
    )

    @Serializable
    private data class WalletPayload(
        val address: String,
        val role: String? = null,
        val label: String? = null,
    )
}
