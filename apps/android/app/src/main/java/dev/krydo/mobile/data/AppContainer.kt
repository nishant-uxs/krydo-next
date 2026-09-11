package dev.krydo.mobile.data

import android.app.Application
import dev.krydo.mobile.BuildConfig
import dev.krydo.mobile.network.ApiClientFactory
import dev.krydo.mobile.wallet.WalletManager
import dev.krydo.mobile.wallet.WalletSessionStore

class AppContainer(app: Application) {
    val settingsRepository = SettingsRepository(app)
    val walletSessionStore = WalletSessionStore(app)
    val walletManager = WalletManager(
        app = app,
        sessionStore = walletSessionStore,
        reownProjectId = BuildConfig.REOWN_PROJECT_ID,
    )
    private val apiFactory = ApiClientFactory()
    val credentialRepository = CredentialRepository(
        settingsRepository = settingsRepository,
        apiClientFactory = apiFactory,
    )
    val presentationRepository = PresentationRepository(
        settingsRepository = settingsRepository,
        apiClientFactory = apiFactory,
    )
}
