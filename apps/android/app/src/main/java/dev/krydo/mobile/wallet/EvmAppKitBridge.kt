package dev.krydo.mobile.wallet

import android.app.Application
import android.util.Log
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.Modal
import com.reown.appkit.presets.AppKitChainsPresets

object EvmAppKitBridge {
    private const val TAG = "EvmAppKitBridge"
    @Volatile
    var ready: Boolean = false
        private set

    fun initialize(app: Application, projectId: String) {
        if (projectId.isBlank()) {
            Log.i(TAG, "REOWN_PROJECT_ID empty — skipping AppKit init")
            return
        }
        val metaData = Core.Model.AppMetaData(
            name = "Krydo",
            description = "Krydo identity wallet",
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
        AppKit.initialize(
            init = Modal.Params.Init(core = CoreClient),
            onSuccess = {
                ready = true
                AppKit.setChains(AppKitChainsPresets.ethChains.values.toList())
                Log.i(TAG, "AppKit ready")
            },
            onError = { error ->
                ready = false
                Log.e(TAG, "AppKit init failed", error.throwable)
            },
        )
    }
}
