package dev.krydo.mobile.wallet

import android.app.Application
import android.util.Log
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.Modal
import com.reown.appkit.presets.AppKitChainsPresets
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient

/**
 * Shared WalletConnect core for EVM AppKit + Freighter (Stellar Sign).
 */
object WcCoreBridge {
    private const val TAG = "WcCoreBridge"

    @Volatile
    var coreReady: Boolean = false
        private set

    @Volatile
    var signReady: Boolean = false
        private set

    @Volatile
    var appKitReady: Boolean = false
        private set

    fun initialize(app: Application, projectId: String) {
        if (projectId.isBlank()) {
            Log.i(TAG, "REOWN_PROJECT_ID empty — WC disabled")
            return
        }
        if (coreReady) return

        val metaData = Core.Model.AppMetaData(
            name = "Krydo",
            description = "Krydo identity — Stellar + EVM",
            url = "https://krydo.onrender.com",
            icons = listOf("https://krydo.onrender.com/favicon.ico"),
            redirect = "krydo://wc",
        )

        CoreClient.initialize(
            application = app,
            projectId = projectId,
            metaData = metaData,
            connectionType = ConnectionType.AUTOMATIC,
        ) { error ->
            Log.e(TAG, "CoreClient init error", error.throwable)
        }
        coreReady = true

        SignClient.initialize(Sign.Params.Init(core = CoreClient)) { error ->
            Log.e(TAG, "SignClient init error", error.throwable)
        }
        signReady = true

        AppKit.initialize(
            init = Modal.Params.Init(core = CoreClient),
            onSuccess = {
                appKitReady = true
                AppKit.setChains(AppKitChainsPresets.ethChains.values.toList())
                Log.i(TAG, "AppKit ready")
            },
            onError = { error ->
                appKitReady = false
                Log.e(TAG, "AppKit init failed", error.throwable)
            },
        )

        // Keep legacy bridge flag in sync for EVM screens.
        EvmAppKitBridge.markReadyFromCore(appKitReady)
        Log.i(TAG, "WC core+sign initialized")
    }
}
