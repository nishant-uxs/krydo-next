package dev.krydo.mobile.wallet

import android.app.Application

/**
 * EVM AppKit entry — delegates to [WcCoreBridge] (shared Core + Sign + AppKit).
 */
object EvmAppKitBridge {
    val ready: Boolean
        get() = WcCoreBridge.appKitReady

    fun initialize(app: Application, projectId: String) {
        WcCoreBridge.initialize(app, projectId)
    }

    internal fun markReadyFromCore(ready: Boolean) {
        // no-op mirror; [ready] reads WcCoreBridge.appKitReady
    }
}
