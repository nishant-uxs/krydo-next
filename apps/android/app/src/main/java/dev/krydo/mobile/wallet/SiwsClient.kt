package dev.krydo.mobile.wallet

import android.util.Base64
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

/**
 * Freighter WalletConnect login via SIWS (SEP-53).
 * Fetches a server nonce, builds a canonical message, signs with Freighter,
 * then exchanges the signature for a JWT. Unsigned address login is rejected.
 */
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

    data class AuthResult(
        val token: String,
        val address: String,
        val role: String?,
    )

    /**
     * After Freighter Approves the WC session — prove ownership with SEP-53
     * `stellar_signMessage`, then POST /api/auth/wc-session.
     */
    suspend fun authenticateFromWalletConnect(
        session: FreighterWcClient.SessionInfo,
        signMessage: suspend (String) -> String,
    ): AuthResult = withContext(Dispatchers.IO) {
        val base = resolveApiBase()
        val address = session.address

        val nonceUrl = "$base/api/auth/nonce?address=${java.net.URLEncoder.encode(address, "UTF-8")}"
        Log.i(TAG, "GET $nonceUrl")
        val nonceResp = http.newCall(
            Request.Builder().url(nonceUrl).header("Accept", "application/json").get().build(),
        ).execute()
        val nonceText = nonceResp.body?.string().orEmpty()
        if (!nonceResp.isSuccessful) {
            throw IllegalStateException("Nonce failed (${nonceResp.code}): ${nonceText.take(160)}")
        }
        val noncePayload = json.decodeFromString(NonceResponse.serializer(), nonceText)

        val message = buildSiwsMessage(
            address = address,
            nonce = noncePayload.nonce,
            network = networkFromChainId(session.chainId),
        )

        val rawSignature = signMessage(message)
        val signature = normalizeSignatureToBase64(rawSignature)

        val url = "$base/api/auth/wc-session"
        val body = buildJsonObject {
            put("address", address)
            put("message", message)
            put("signature", signature)
            put("chainId", session.chainId)
            put("topic", session.topic)
            put("provider", "freighter-wc")
        }.toString()

        Log.i(TAG, "POST $url (SIWS)")
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
                "Login failed (${resp.code}) at $url — ${text.take(200)}",
            )
        }
        if (!text.trimStart().startsWith("{")) {
            throw IllegalStateException(
                "API returned HTML instead of JSON at $url. Check API base URL (expected ${BuildConfig.DEFAULT_API_BASE_URL}). Got: ${text.take(80)}",
            )
        }

        val verified = json.decodeFromString(VerifyResponse.serializer(), text)
        AuthResult(
            token = verified.token,
            address = verified.wallet.address,
            role = verified.wallet.role,
        )
    }

    private fun buildSiwsMessage(address: String, nonce: String, network: String): String {
        val host = "krydo.app"
        val uri = BuildConfig.WEB_APP_URL.trimEnd('/')
        val issuedAt = java.time.Instant.now().toString()
        return listOf(
            "$host wants you to sign in with your Stellar account:",
            address,
            "",
            "Sign in to Krydo to prove ownership of this wallet.",
            "",
            "URI: $uri",
            "Version: 1",
            "Network: $network",
            "Nonce: $nonce",
            "Issued At: $issuedAt",
        ).joinToString("\n")
    }

    private fun networkFromChainId(chainId: String): String =
        when {
            chainId.contains("pubnet", ignoreCase = true) ||
                chainId.contains("public", ignoreCase = true) -> "mainnet"
            else -> "testnet"
        }

    /**
     * Freighter may return base64, hex, or a JSON blob — normalize to base64
     * for the server SEP-53 verifier.
     */
    private fun normalizeSignatureToBase64(raw: String): String {
        val trimmed = raw.trim().trim('"')
        if (trimmed.startsWith("{")) {
            return try {
                val obj = org.json.JSONObject(trimmed)
                normalizeSignatureToBase64(
                    obj.optString("signature").ifBlank {
                        obj.optString("signedMessage")
                    }.ifBlank { trimmed },
                )
            } catch (_: Exception) {
                trimmed
            }
        }
        if (trimmed.matches(Regex("^[0-9a-fA-F]{128}$"))) {
            val bytes = trimmed.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
        // Already base64 (or opaque — server also accepts hex)
        return trimmed
    }

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
    private data class NonceResponse(
        val nonce: String,
        val expiresAt: Long? = null,
    )

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
