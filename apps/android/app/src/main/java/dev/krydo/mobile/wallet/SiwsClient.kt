package dev.krydo.mobile.wallet

import android.util.Log
import dev.krydo.mobile.BuildConfig
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
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    data class AuthResult(val token: String, val address: String)

    /** After Freighter Approves the WC session — single popup login. */
    suspend fun authenticateFromWalletConnect(
        address: String,
        chainId: String,
        topic: String,
    ): AuthResult = withContext(Dispatchers.IO) {
        val base = resolveApiBase()
        val url = "$base/api/auth/wc-session"
        val body = buildJsonObject {
            put("address", address)
            put("chainId", chainId)
            put("topic", topic)
            put("provider", "freighter-wc")
        }.toString()

        Log.i(TAG, "POST $url")
        val resp = http.newCall(
            Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build(),
        ).execute()

        val text = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
            throw IllegalStateException(
                "Login failed (${resp.code}) at $url — ${text.take(160)}",
            )
        }
        if (!text.trimStart().startsWith("{")) {
            throw IllegalStateException(
                "API returned HTML instead of JSON at $url. Check API base URL (expected ${BuildConfig.DEFAULT_API_BASE_URL}). Got: ${text.take(80)}",
            )
        }

        val verified = json.decodeFromString(VerifyResponse.serializer(), text)
        AuthResult(token = verified.token, address = verified.wallet.address)
    }

    /**
     * Prefer the baked-in Render API. Only keep a custom setting if it still
     * looks like our known backends (avoids stale localhost / wrong SPA URLs).
     */
    private suspend fun resolveApiBase(): String {
        val configured = settingsRepository.settings.first().apiBaseUrl.trim().trimEnd('/')
        val allowed = configured.isNotBlank() &&
            (configured.contains("krydo.onrender.com", ignoreCase = true) ||
                configured.contains("krydo-next.vercel.app", ignoreCase = true) ||
                configured.contains("localhost", ignoreCase = true) ||
                configured.contains("10.0.2.2"))
        return if (allowed) configured else BuildConfig.DEFAULT_API_BASE_URL.trimEnd('/')
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

    companion object {
        private const val TAG = "SiwsClient"
    }
}
