package dev.krydo.mobile.data

import android.content.Context
import dev.krydo.mobile.network.ApiClientFactory

class AppContainer(context: Context) {
    val settingsRepository = SettingsRepository(context.applicationContext)
    val credentialRepository = CredentialRepository()
    private val apiFactory = ApiClientFactory()
    val presentationRepository = PresentationRepository(
        settingsRepository = settingsRepository,
        apiClientFactory = apiFactory,
    )
}
