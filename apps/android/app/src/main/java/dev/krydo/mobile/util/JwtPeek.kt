package dev.krydo.mobile.util

import android.util.Base64
import org.json.JSONObject

object JwtPeek {
    fun role(token: String): String? = payload(token)?.optString("role")?.takeIf { it.isNotBlank() }

    fun sub(token: String): String? = payload(token)?.optString("sub")?.takeIf { it.isNotBlank() }

    private fun payload(token: String): JSONObject? = runCatching {
        val parts = token.split(".")
        if (parts.size < 2) return null
        val padded = parts[1] + "=".repeat((4 - parts[1].length % 4) % 4)
        val json = String(Base64.decode(padded, Base64.URL_SAFE or Base64.NO_WRAP))
        JSONObject(json)
    }.getOrNull()
}

object DidLinks {
    fun stellarDid(address: String, network: String = "testnet"): String =
        "did:pkh:stellar:$network:$address"
}
