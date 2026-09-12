package dev.krydo.mobile.network

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

object ClaimDataParser {
    fun extractValue(claimData: JsonElement?): String? {
        if (claimData == null || claimData is JsonNull) return null
        val obj = claimData as? JsonObject ?: return null
        readString(obj, "value")?.let { return it }
        readString(obj, "score")?.let { return it }
        val fields = obj["fields"] as? JsonObject ?: return null
        return readString(fields, "value") ?: readString(fields, "score")
    }

    private fun readString(obj: JsonObject, key: String): String? {
        val el = obj[key] ?: return null
        return when (el) {
            is JsonNull -> null
            is JsonPrimitive -> el.contentOrNull?.takeIf { it.isNotBlank() }
            else -> el.toString().trim('"').takeIf { it.isNotBlank() }
        }
    }
}

object TxHashLookup {
    /**
     * Only return hashes that were wallet-anchored on Stellar
     * (`data.onChain == true`). Synthetic server hashes look like real
     * 64-hex tx ids but 404 on stellar.expert.
     */
    fun forCredential(transactions: List<TransactionDto>, credentialHash: String): String? {
        val match = transactions.firstOrNull { tx ->
            if (tx.action != "credential_issued") return@firstOrNull false
            if (!isExplorerLinkable(tx.txHash, tx.data)) return@firstOrNull false
            val data = tx.data as? JsonObject ?: return@firstOrNull false
            val hash = data["credentialHash"]?.jsonPrimitive?.contentOrNull
            hash.equals(credentialHash, ignoreCase = true)
        }
        return match?.txHash
    }

    fun isOffChain(txHash: String?): Boolean {
        if (txHash.isNullOrBlank()) return true
        return txHash.all { it == '0' }
    }

    fun isExplorerLinkable(txHash: String?, data: JsonElement? = null): Boolean {
        if (txHash.isNullOrBlank() || isOffChain(txHash)) return false
        val obj = data as? JsonObject ?: return false
        return obj["onChain"]?.jsonPrimitive?.booleanOrNull == true
    }

    fun explorerUrl(txHash: String): String =
        "https://stellar.expert/explorer/testnet/tx/$txHash"
}
