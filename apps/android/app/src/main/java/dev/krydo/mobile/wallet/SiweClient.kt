package dev.krydo.mobile.wallet

import com.reown.appkit.client.AppKit
import com.reown.appkit.client.Modal
import com.reown.appkit.client.models.request.Request
import dev.krydo.mobile.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request as HttpRequest
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SiweClient(
    private val settingsRepository: SettingsRepository,
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    data class AuthResult(val token: String, val address: String, val chain: String)

    suspend fun authenticate(
        address: String,
        chainId: Int,
        signMessage: suspend (String) -> String,
    ): AuthResult = withContext(Dispatchers.IO) {
        val base = settingsRepository.settings.first().apiBaseUrl
        val nonceUrl = "$base/api/auth/siwe/nonce?address=$address&chainId=$chainId"
        val nonceResp = http.newCall(HttpRequest.Builder().url(nonceUrl).get().build()).execute()
        val nonceBody = nonceResp.body?.string().orEmpty()
        if (!nonceResp.isSuccessful) {
            throw IllegalStateException("SIWE nonce failed: $nonceBody")
        }
        val noncePayload = json.decodeFromString(SiweNonceResponse.serializer(), nonceBody)

        val message = buildSiweMessage(
            domain = noncePayload.domain,
            address = address,
            uri = noncePayload.uri,
            nonce = noncePayload.nonce,
            chainId = chainId,
            statement = noncePayload.statement,
        )
        val signature = signMessage(message)

        val verifyBody = buildJsonObject {
            put("message", message)
            put("signature", signature)
        }.toString()
        val verifyResp = http.newCall(
            HttpRequest.Builder()
                .url("$base/api/auth/siwe/verify")
                .post(verifyBody.toRequestBody("application/json".toMediaType()))
                .build(),
        ).execute()
        val verifyText = verifyResp.body?.string().orEmpty()
        if (!verifyResp.isSuccessful) {
            throw IllegalStateException("SIWE verify failed: $verifyText")
        }
        val verified = json.decodeFromString(SiweVerifyResponse.serializer(), verifyText)
        AuthResult(token = verified.token, address = address, chain = verified.wallet.chain)
    }

    companion object {
        fun buildSiweMessage(
            domain: String,
            address: String,
            uri: String,
            nonce: String,
            chainId: Int,
            statement: String,
        ): String {
            val issuedAt = java.time.Instant.now().toString()
            return """
                |$domain wants you to sign in with your Ethereum account:
                |$address
                |
                |$statement
                |
                |URI: $uri
                |Version: 1
                |Chain ID: $chainId
                |Nonce: $nonce
                |Issued At: $issuedAt
            """.trimMargin()
        }

        /**
         * personal_sign via AppKit. Result arrives on ModalDelegate.onSessionRequestResponse
         * (request() onSuccess only means the request was sent to the wallet).
         */
        suspend fun signWithAppKit(message: String, address: String): String =
            suspendCancellableCoroutine { cont ->
                val pending = AtomicReference(cont)
                val delegate = object : AppKit.ModalDelegate {
                    override fun onSessionApproved(approvedSession: Modal.Model.ApprovedSession) {}
                    override fun onSessionRejected(rejectedSession: Modal.Model.RejectedSession) {}
                    override fun onSessionUpdate(updatedSession: Modal.Model.UpdatedSession) {}
                    override fun onSessionEvent(sessionEvent: Modal.Model.SessionEvent) {}
                    override fun onSessionExtend(session: Modal.Model.Session) {}
                    override fun onSessionDelete(deletedSession: Modal.Model.DeletedSession) {}
                    override fun onProposalExpired(proposal: Modal.Model.ExpiredProposal) {}
                    override fun onRequestExpired(request: Modal.Model.ExpiredRequest) {
                        pending.getAndSet(null)?.resumeWithException(
                            IllegalStateException("Wallet request expired"),
                        )
                    }
                    override fun onConnectionStateChange(state: Modal.Model.ConnectionState) {}
                    override fun onError(error: Modal.Model.Error) {
                        pending.getAndSet(null)?.resumeWithException(error.throwable)
                    }

                    override fun onSessionRequestResponse(response: Modal.Model.SessionRequestResponse) {
                        if (response.method != "personal_sign") return
                        val c = pending.getAndSet(null) ?: return
                        when (val result = response.result) {
                            is Modal.Model.JsonRpcResponse.JsonRpcResult -> {
                                c.resume(result.result.orEmpty().removeSurrounding("\""))
                            }
                            is Modal.Model.JsonRpcResponse.JsonRpcError -> {
                                c.resumeWithException(
                                    IllegalStateException(result.message ?: "personal_sign failed"),
                                )
                            }
                        }
                    }
                }

                try {
                    AppKit.setDelegate(delegate)
                    val params = JSONArray().put(message).put(address).toString()
                    AppKit.request(
                        request = Request(method = "personal_sign", params = params),
                        onSuccess = { _: com.reown.appkit.client.models.request.SentRequestResult -> },
                        onError = { err: Throwable ->
                            pending.getAndSet(null)?.resumeWithException(err)
                        },
                    )
                } catch (t: Throwable) {
                    pending.getAndSet(null)?.resumeWithException(t)
                }

                cont.invokeOnCancellation {
                    pending.set(null)
                }
            }
    }
}

@Serializable
private data class SiweNonceResponse(
    val nonce: String,
    val expiresAt: Long? = null,
    val chainId: Int,
    val chain: String,
    val domain: String,
    val uri: String,
    val statement: String = "Sign in to Krydo with your Ethereum wallet.",
)

@Serializable
private data class SiweVerifyResponse(
    val token: String,
    val wallet: SiweWalletDto,
)

@Serializable
private data class SiweWalletDto(
    val address: String,
    val chain: String,
    val role: String? = null,
)
