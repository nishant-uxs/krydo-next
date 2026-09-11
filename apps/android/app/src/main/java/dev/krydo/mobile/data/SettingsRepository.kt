package dev.krydo.mobile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.krydo.mobile.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "krydo_settings")

data class AppSettings(
    val apiBaseUrl: String,
    val holderAddress: String,
    val authToken: String,
    val onboardingDone: Boolean,
) {
    /** Verified wallet session required to leave the login gate. */
    val hasSession: Boolean
        get() = authToken.isNotBlank() && holderAddress.isNotBlank()
}

class SettingsRepository(private val context: Context) {
    private val apiUrlKey = stringPreferencesKey("api_base_url")
    private val holderKey = stringPreferencesKey("holder_address")
    private val tokenKey = stringPreferencesKey("auth_token")
    private val onboardingKey = booleanPreferencesKey("onboarding_done")

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            apiBaseUrl = trimSlash(prefs[apiUrlKey] ?: BuildConfig.DEFAULT_API_BASE_URL),
            holderAddress = prefs[holderKey].orEmpty().trim(),
            authToken = prefs[tokenKey].orEmpty().trim(),
            onboardingDone = prefs[onboardingKey] ?: false,
        )
    }

    suspend fun setApiBaseUrl(url: String) {
        context.dataStore.edit { it[apiUrlKey] = trimSlash(url) }
    }

    suspend fun setHolderAddress(address: String) {
        context.dataStore.edit { it[holderKey] = address.trim() }
    }

    suspend fun setAuthToken(token: String) {
        context.dataStore.edit { it[tokenKey] = token.trim() }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[onboardingKey] = done }
    }

    suspend fun clearSession() {
        context.dataStore.edit {
            it[tokenKey] = ""
            it[holderKey] = ""
            it[onboardingKey] = false
        }
    }

    companion object {
        fun trimSlash(url: String): String = url.trim().trimEnd('/')
    }
}
