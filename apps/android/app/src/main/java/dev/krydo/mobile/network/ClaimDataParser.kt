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
     * Prefer API-enriched issue hashes, then wallet-anchored credential_issued txs.
     */
    fun forCredential(
        transactions: List<TransactionDto>,
        credentialHash: String,
        apiTxHash: String? = null,
        apiOnChainTxHash: String? = null,
    ): String? {
        apiOnChainTxHash?.takeUnless { isOffChain(it) }?.let { return it }
        apiTxHash?.takeUnless { isOffChain(it) }?.let { return it }

        val match = transactions.firstOrNull { tx ->
            if (tx.action != "credential_issued") return@firstOrNull false
            if (!isExplorerLinkable(tx.txHash, tx.data, tx.blockNumber)) return@firstOrNull false
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

    fun readOnChainFlag(data: JsonElement?): Boolean? {
        val obj = data as? JsonObject ?: return null
        val el = obj["onChain"] ?: return null
        val prim = el as? JsonPrimitive ?: return null
        prim.booleanOrNull?.let { return it }
        return when (prim.contentOrNull?.lowercase()) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }

    fun isExplorerLinkable(
        txHash: String?,
        data: JsonElement? = null,
        blockNumber: String? = null,
    ): Boolean {
        if (txHash.isNullOrBlank() || isOffChain(txHash)) return false
        val onChain = readOnChainFlag(data)
        if (onChain == true) return true
        // Real ledger sequence means the tx was confirmed on Stellar.
        if (!blockNumber.isNullOrBlank() && blockNumber != "0") return true
        return false
    }

    fun explorerUrl(txHash: String): String =
        "https://stellar.expert/explorer/testnet/tx/$txHash"
}
