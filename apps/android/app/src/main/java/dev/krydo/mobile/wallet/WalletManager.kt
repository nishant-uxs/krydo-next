package dev.krydo.mobile.wallet

import android.app.Application
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WalletManager(
    private val app: Application,
    private val sessionStore: WalletSessionStore,
    private val reownProjectId: String,
) {
    private val _evmReady = MutableStateFlow(false)
    val evmReady: StateFlow<Boolean> = _evmReady.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun initialize() {
        if (reownProjectId.isBlank()) {
            Log.i(TAG, "REOWN_PROJECT_ID empty — EVM AppKit disabled")
            _evmReady.value = false
            return
        }
        try {
            EvmAppKitBridge.initialize(app, reownProjectId)
            _evmReady.value = EvmAppKitBridge.ready
        } catch (t: Throwable) {
            Log.e(TAG, "AppKit init failed", t)
            _lastError.value = t.message
            _evmReady.value = false
        }
    }

    fun openEvmConnect() {
        app.startActivity(
            Intent(app, KrydoEvmConnectActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    companion object {
        private const val TAG = "KrydoWallet"
    }
}
