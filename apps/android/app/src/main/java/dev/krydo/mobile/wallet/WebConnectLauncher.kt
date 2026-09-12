package dev.krydo.mobile.wallet

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast

/**
 * Opens the Freighter Android app directly (no website / Custom Tab).
 *
 * Freighter registers `freighterwallet://` — it does NOT claim arbitrary https URLs,
 * so opening krydo-next.vercel.app always fell back to Chrome. That is fixed here.
 */
object WebConnectLauncher {
    private val FREIGHTER_PACKAGES = listOf(
        "org.stellar.freighterwallet",
        "org.stellar.freighterdev",
    )

    /** Open Freighter app only — never the Krydo website. */
    fun openStellarWalletConnect(context: Context, preferFreighter: Boolean = true): Boolean {
        if (preferFreighter) {
            for (pkg in FREIGHTER_PACKAGES) {
                if (openFreighterApp(context, pkg)) return true
            }
        }
        Toast.makeText(
            context,
            "Freighter not installed — install from Play Store",
            Toast.LENGTH_LONG,
        ).show()
        // Play Store only — do not open the website.
        openPlayStore(context, "org.stellar.freighterwallet")
        return false
    }

    fun openFreighterApp(context: Context, packageName: String = "org.stellar.freighterwallet"): Boolean {
        if (!isInstalled(context, packageName)) return false

        // 1) Preferred: Freighter deep link scheme (from its manifest).
        if (tryView(context, "freighterwallet://connect", packageName)) return true
        if (tryView(context, "freighterwallet://", packageName)) return true

        // 2) Launch main activity.
        val launch = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return try {
                context.startActivity(launch)
                true
            } catch (_: Exception) {
                false
            }
        }
        return false
    }

    private fun tryView(context: Context, uri: String, packageName: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                setPackage(packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun openPlayStore(context: Context, packageName: String) {
        val market = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(market)
        } catch (_: Exception) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private fun isInstalled(context: Context, packageName: String): Boolean {
        return try {
            val pm = context.packageManager
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
}
