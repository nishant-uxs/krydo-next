package dev.krydo.mobile.wallet

import kotlinx.serialization.Serializable

enum class ChainType {
    STELLAR,
    EVM,
}

@Serializable
data class WalletAccount(
    val chainType: String,
    /** CAIP-2 e.g. stellar:testnet or eip155:1 */
    val chainId: String,
    val address: String,
    val walletProvider: String? = null,
    val label: String? = null,
) {
    val type: ChainType
        get() = if (chainType.equals("EVM", ignoreCase = true)) ChainType.EVM else ChainType.STELLAR

    fun shortAddress(): String {
        val a = address
        return when {
            a.length <= 12 -> a
            a.startsWith("0x", ignoreCase = true) -> "${a.take(6)}…${a.takeLast(4)}"
            else -> "${a.take(4)}…${a.takeLast(4)}"
        }
    }
}

object WalletChains {
    const val STELLAR_TESTNET = "stellar:testnet"
    const val STELLAR_PUBNET = "stellar:pubnet"

    val EVM = listOf(
        1 to "Ethereum",
        137 to "Polygon",
        8453 to "Base",
        42161 to "Arbitrum",
        10 to "Optimism",
        56 to "BNB Chain",
        43114 to "Avalanche",
    )

    fun evmCaip2(chainId: Int): String = "eip155:$chainId"

    fun isSupportedEvm(chainId: Int): Boolean = EVM.any { it.first == chainId }
}
