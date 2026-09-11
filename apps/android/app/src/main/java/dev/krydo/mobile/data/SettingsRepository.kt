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
    val useMockData: Boolean,
    val onboardingDone: Boolean,
)

class SettingsRepository(private val context: Context) {
    private val apiUrlKey = stringPreferencesKey("api_base_url")
    private val mockKey = booleanPreferencesKey("use_mock_data")
    private val onboardingKey = booleanPreferencesKey("onboarding_done")

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            apiBaseUrl = trimSlash(
                prefs[apiUrlKey] ?: BuildConfig.DEFAULT_API_BASE_URL,
            ),
            useMockData = prefs[mockKey] ?: true,
            onboardingDone = prefs[onboardingKey] ?: false,
        )
    }

    suspend fun setApiBaseUrl(url: String) {
        context.dataStore.edit { it[apiUrlKey] = trimSlash(url) }
    }

    suspend fun setUseMockData(enabled: Boolean) {
        context.dataStore.edit { it[mockKey] = enabled }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[onboardingKey] = done }
    }

    companion object {
        fun trimSlash(url: String): String = url.trim().trimEnd('/')
    }
}
