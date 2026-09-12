package dev.krydo.mobile.wallet

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

enum class WalletRail {
    STELLAR,
    EVM,
}

/**
 * Known mobile wallets we can detect via package name (Android 11+ needs [queries] in manifest).
 */
data class KnownWallet(
    val id: String,
    val displayName: String,
    val packageName: String,
    val rail: WalletRail,
    val playStoreUrl: String,
    val deepLinkScheme: String? = null,
    val subtitle: String,
)

object KnownWallets {
    val ALL: List<KnownWallet> = listOf(
        KnownWallet(
            id = "metamask",
            displayName = "MetaMask",
            packageName = "io.metamask",
            rail = WalletRail.EVM,
            playStoreUrl = "https://play.google.com/store/apps/details?id=io.metamask",
            deepLinkScheme = "metamask",
            subtitle = "EVM · WalletConnect / SIWE",
        ),
        KnownWallet(
            id = "rainbow",
            displayName = "Rainbow",
            packageName = "me.rainbow",
            rail = WalletRail.EVM,
            playStoreUrl = "https://play.google.com/store/apps/details?id=me.rainbow",
            deepLinkScheme = "rainbow",
            subtitle = "EVM · WalletConnect / SIWE",
        ),
        KnownWallet(
            id = "coinbase",
            displayName = "Coinbase Wallet",
            packageName = "org.toshi",
            rail = WalletRail.EVM,
            playStoreUrl = "https://play.google.com/store/apps/details?id=org.toshi",
            deepLinkScheme = "cbwallet",
            subtitle = "EVM · Base App / SIWE",
        ),
        KnownWallet(
            id = "trust",
            displayName = "Trust Wallet",
            packageName = "com.wallet.crypto.trustapp",
            rail = WalletRail.EVM,
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.wallet.crypto.trustapp",
            deepLinkScheme = "trust",
            subtitle = "EVM · WalletConnect / SIWE",
        ),
        KnownWallet(
            id = "freighter",
            displayName = "Freighter",
            packageName = "org.stellar.freighterwallet",
            rail = WalletRail.STELLAR,
            playStoreUrl = "https://play.google.com/store/apps/details?id=org.stellar.freighterwallet",
            deepLinkScheme = "freighterwallet",
        subtitle = "Stellar · one Approve in Freighter",
        ),
        KnownWallet(
            id = "freighter-dev",
            displayName = "Freighter (Dev)",
            packageName = "org.stellar.freighterdev",
            rail = WalletRail.STELLAR,
            playStoreUrl = "https://play.google.com/store/apps/details?id=org.stellar.freighterwallet",
            deepLinkScheme = "freighterwallet",
            subtitle = "Stellar · Freighter mobile dev build",
        ),
        KnownWallet(
            id = "lobstr",
            displayName = "LOBSTR",
            packageName = "com.lobstr.client",
            rail = WalletRail.STELLAR,
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.lobstr.client",
            deepLinkScheme = "lobstr",
            subtitle = "Stellar · web connect / JWT restore",
        ),
        KnownWallet(
            id = "solar",
            displayName = "Solar Wallet",
            packageName = "network.solar.android",
            rail = WalletRail.STELLAR,
            playStoreUrl = "https://play.google.com/store/apps/details?id=network.solar.android",
            subtitle = "Stellar · web connect / JWT restore",
        ),
    )
}

data class DetectedWallet(
    val wallet: KnownWallet,
    val installed: Boolean,
)

object WalletDetector {
    fun scan(context: Context): List<DetectedWallet> {
        val pm = context.packageManager
        return KnownWallets.ALL.map { wallet ->
            val installed = when {
                wallet.packageName.isBlank() -> false
                else -> isPackageInstalled(pm, wallet.packageName)
            }
            DetectedWallet(wallet = wallet, installed = installed)
        }.sortedWith(
            compareByDescending<DetectedWallet> { it.installed }
                .thenBy { it.wallet.rail.name }
                .thenBy { it.wallet.displayName },
        )
    }

    fun installedOnly(context: Context): List<DetectedWallet> =
        scan(context).filter { it.installed }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /** Launch the wallet app if installed; otherwise open Play Store / download page. */
    fun openWalletOrStore(context: Context, wallet: KnownWallet): Boolean {
        val pm = context.packageManager
        if (wallet.packageName.isNotBlank() && isPackageInstalled(pm, wallet.packageName)) {
            // Prefer wallet deep-link scheme when present (e.g. freighterwallet://).
            val scheme = wallet.deepLinkScheme
            if (!scheme.isNullOrBlank()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$scheme://")).apply {
                        setPackage(wallet.packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return true
                } catch (_: Exception) {
                    /* fall through to launcher */
                }
            }
            val launch = pm.getLaunchIntentForPackage(wallet.packageName)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                return true
            }
        }
        val uri = Uri.parse(wallet.playStoreUrl)
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            false
        } catch (_: Exception) {
            false
        }
    }
}
