package dev.krydo.mobile.wallet

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.reown.android.CoreClient
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Freighter Mobile via WalletConnect Sign + deep link
 * `freighterwallet://wc-redirect?uri=…`
 */
object FreighterWcClient {
    private const val TAG = "FreighterWc"
    const val CHAIN_TESTNET = "stellar:testnet"
    const val CHAIN_PUBNET = "stellar:pubnet"
    private const val FREIGHTER_PACKAGE = "org.stellar.freighterwallet"
    private const val FREIGHTER_DEEPLINK = "freighterwallet://wc-redirect"

    data class SessionInfo(
        val topic: String,
        val address: String,
        val chainId: String,
    )

    private val pendingSession = AtomicReference<((Result<SessionInfo>) -> Unit)?>(null)
    private val pendingRequest = AtomicReference<((Result<String>) -> Unit)?>(null)

    private val dappDelegate = object : SignClient.DappDelegate {
        override fun onSessionApproved(approvedSession: Sign.Model.ApprovedSession) {
            val account = approvedSession.accounts.firstOrNull()
                ?: approvedSession.namespaces.values.flatMap { it.accounts }.firstOrNull()
            val parsed = parseAccount(account)
            if (parsed == null) {
                pendingSession.getAndSet(null)?.invoke(
                    Result.failure(IllegalStateException("No Stellar account in Freighter session")),
                )
                return
            }
            pendingSession.getAndSet(null)?.invoke(
                Result.success(
                    SessionInfo(
                        topic = approvedSession.topic,
                        address = parsed.first,
                        chainId = parsed.second,
                    ),
                ),
            )
        }

        override fun onSessionRejected(rejectedSession: Sign.Model.RejectedSession) {
            pendingSession.getAndSet(null)?.invoke(
                Result.failure(IllegalStateException(rejectedSession.reason.ifBlank { "Freighter rejected connection" })),
            )
        }

        override fun onSessionUpdate(updatedSession: Sign.Model.UpdatedSession) = Unit

        override fun onSessionExtend(session: Sign.Model.Session) = Unit

        override fun onSessionEvent(sessionEvent: Sign.Model.SessionEvent) = Unit

        override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) = Unit

        override fun onSessionRequestResponse(response: Sign.Model.SessionRequestResponse) {
            when (val result = response.result) {
                is Sign.Model.JsonRpcResponse.JsonRpcResult -> {
                    val sig = extractSignature(result.result)
                    if (sig != null) {
                        pendingRequest.getAndSet(null)?.invoke(Result.success(sig))
                    } else {
                        pendingRequest.getAndSet(null)?.invoke(
                            Result.failure(IllegalStateException("Empty signature from Freighter: ${result.result}")),
                        )
                    }
                }
                is Sign.Model.JsonRpcResponse.JsonRpcError -> {
                    pendingRequest.getAndSet(null)?.invoke(
                        Result.failure(IllegalStateException(result.message)),
                    )
                }
            }
        }

        override fun onProposalExpired(proposal: Sign.Model.ExpiredProposal) {
            pendingSession.getAndSet(null)?.invoke(
                Result.failure(IllegalStateException("Connection proposal expired — try again")),
            )
        }

        override fun onRequestExpired(request: Sign.Model.ExpiredRequest) {
            pendingRequest.getAndSet(null)?.invoke(
                Result.failure(IllegalStateException("Sign request expired — try again")),
            )
        }

        override fun onConnectionStateChange(state: Sign.Model.ConnectionState) = Unit

        override fun onError(error: Sign.Model.Error) {
            Log.e(TAG, "SignClient error", error.throwable)
        }
    }

    fun ensureDelegate() {
        SignClient.setDappDelegate(dappDelegate)
    }

    /**
     * Create WC pairing, open Freighter approve screen, wait for session.
     */
    suspend fun connectFreighter(context: Context): SessionInfo {
        if (!WcCoreBridge.signReady) {
            throw IllegalStateException("WalletConnect not ready — restart the app")
        }
        ensureDelegate()

        val pairing = CoreClient.Pairing.create { error ->
            Log.e(TAG, "Pairing.create failed", error.throwable)
        } ?: throw IllegalStateException("Could not create WalletConnect pairing")

        val methods = listOf(
            "stellar_signMessage",
            "stellar_signXDR",
            "stellar_signAndSubmitXDR",
            "stellar_signAuthEntry",
        )
        val events = listOf("accountsChanged")
        val required = mapOf(
            "stellar" to Sign.Model.Namespace.Proposal(
                chains = listOf(CHAIN_TESTNET),
                methods = methods,
                events = events,
            ),
        )
        val optional = mapOf(
            "stellar" to Sign.Model.Namespace.Proposal(
                chains = listOf(CHAIN_PUBNET, CHAIN_TESTNET),
                methods = methods,
                events = events,
            ),
        )

        return suspendCancellableCoroutine { cont ->
            pendingSession.set { result ->
                if (cont.isActive) {
                    result.fold(
                        onSuccess = { cont.resume(it) },
                        onFailure = { cont.resumeWithException(it) },
                    )
                }
            }
            cont.invokeOnCancellation { pendingSession.set(null) }

            val params = Sign.Params.Connect(
                namespaces = required,
                optionalNamespaces = optional,
                pairing = pairing,
            )
            SignClient.connect(
                connect = params,
                onSuccess = {
                    openFreighterWithUri(context, pairing.uri)
                },
                onError = { error ->
                    pendingSession.getAndSet(null)
                    if (cont.isActive) {
                        cont.resumeWithException(
                            error.throwable ?: IllegalStateException("Freighter connect failed"),
                        )
                    }
                },
            )
        }
    }

    suspend fun signMessage(session: SessionInfo, message: String): String {
        ensureDelegate()
        val paramsJson = JSONObject().put("message", message).toString()
        return suspendCancellableCoroutine { cont ->
            pendingRequest.set { result ->
                if (cont.isActive) {
                    result.fold(
                        onSuccess = { cont.resume(it) },
                        onFailure = { cont.resumeWithException(it) },
                    )
                }
            }
            cont.invokeOnCancellation { pendingRequest.set(null) }

            val request = Sign.Params.Request(
                sessionTopic = session.topic,
                method = "stellar_signMessage",
                params = paramsJson,
                chainId = session.chainId,
            )
            SignClient.request(
                request = request,
                onSuccess = {
                    // Freighter should already be open / come to foreground for approve.
                    Log.i(TAG, "stellar_signMessage sent")
                },
                onError = { error ->
                    pendingRequest.getAndSet(null)
                    if (cont.isActive) {
                        cont.resumeWithException(
                            error.throwable ?: IllegalStateException("Sign request failed"),
                        )
                    }
                },
            )
        }
    }

    fun openFreighterWithUri(context: Context, wcUri: String) {
        val encoded = Uri.encode(wcUri)
        val deep = Uri.parse("$FREIGHTER_DEEPLINK?uri=$encoded")
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, deep).apply {
                    setPackage(FREIGHTER_PACKAGE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
            return
        } catch (t: Throwable) {
            Log.w(TAG, "freighterwallet://wc-redirect failed, trying launcher", t)
        }
        // Fallback: open Freighter app (user may need to scan / approve from WC list)
        WebConnectLauncher.openFreighterApp(context, FREIGHTER_PACKAGE)
    }

    /** account = "stellar:testnet:G…" */
    private fun parseAccount(account: String?): Pair<String, String>? {
        if (account.isNullOrBlank()) return null
        val parts = account.split(":")
        if (parts.size < 3) return null
        val chainId = "${parts[0]}:${parts[1]}"
        val address = parts.last()
        if (!address.startsWith("G")) return null
        return address to chainId
    }

    private fun extractSignature(raw: Any?): String? {
        when (raw) {
            is String -> {
                val trimmed = raw.trim()
                if (trimmed.startsWith("{")) {
                    return try {
                        val obj = JSONObject(trimmed)
                        obj.optString("signature").ifBlank {
                            obj.optString("signedMessage")
                        }.ifBlank { null }
                    } catch (_: Exception) {
                        trimmed
                    }
                }
                return trimmed.ifBlank { null }
            }
            is Map<*, *> -> {
                val sig = raw["signature"] ?: raw["signedMessage"]
                return sig?.toString()?.ifBlank { null }
            }
            else -> return raw?.toString()?.ifBlank { null }
        }
    }
}
