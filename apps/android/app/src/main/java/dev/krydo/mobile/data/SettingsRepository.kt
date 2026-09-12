package dev.krydo.mobile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.krydo.mobile.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "krydo_settings")

data class AppSettings(
    val apiBaseUrl: String,
    val holderAddress: String,
    val authToken: String,
    val onboardingDone: Boolean,
    val walletRole: String = "user",
    val knownCredentialCount: Int = 0,
) {
    val hasSession: Boolean
        get() = authToken.isNotBlank() && holderAddress.isNotBlank()

    val isIssuerOrRoot: Boolean
        get() = walletRole.equals("issuer", true) || walletRole.equals("root", true)

    val did: String
        get() = if (holderAddress.isBlank()) "" else "did:pkh:stellar:testnet:$holderAddress"
}

class SettingsRepository(private val context: Context) {
    private val apiUrlKey = stringPreferencesKey("api_base_url")
    private val holderKey = stringPreferencesKey("holder_address")
    private val tokenKey = stringPreferencesKey("auth_token")
    private val onboardingKey = booleanPreferencesKey("onboarding_done")
    private val roleKey = stringPreferencesKey("wallet_role")
    private val credCountKey = intPreferencesKey("known_credential_count")
    private val offlineCredsKey = stringPreferencesKey("offline_credentials_json")
    private val archivedCredsKey = stringPreferencesKey("archived_credential_ids")

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            apiBaseUrl = trimSlash(prefs[apiUrlKey] ?: BuildConfig.DEFAULT_API_BASE_URL),
            holderAddress = prefs[holderKey].orEmpty().trim(),
            authToken = prefs[tokenKey].orEmpty().trim(),
            onboardingDone = prefs[onboardingKey] ?: false,
            walletRole = prefs[roleKey].orEmpty().ifBlank { "user" },
            knownCredentialCount = prefs[credCountKey] ?: 0,
        )
    }

    /** Local archive set (credential ids). Not synced to server. */
    val archivedCredentialIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        decodeIdSet(prefs[archivedCredsKey])
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

    suspend fun setWalletRole(role: String) {
        context.dataStore.edit { it[roleKey] = role.trim().ifBlank { "user" } }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[onboardingKey] = done }
    }

    suspend fun setKnownCredentialCount(count: Int) {
        context.dataStore.edit { it[credCountKey] = count }
    }

    suspend fun saveOfflineCredentialsJson(json: String) {
        context.dataStore.edit { it[offlineCredsKey] = json }
    }

    suspend fun loadOfflineCredentialsJson(): String =
        context.dataStore.data.first()[offlineCredsKey].orEmpty()

    suspend fun setCredentialArchived(id: String, archived: Boolean) {
        context.dataStore.edit { prefs ->
            val next = decodeIdSet(prefs[archivedCredsKey]).toMutableSet()
            if (archived) next.add(id) else next.remove(id)
            prefs[archivedCredsKey] = encodeIdSet(next)
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit {
            it[tokenKey] = ""
            it[holderKey] = ""
            it[onboardingKey] = false
            it[roleKey] = "user"
        }
    }

    companion object {
        fun trimSlash(url: String): String = url.trim().trimEnd('/')

        private fun decodeIdSet(raw: String?): Set<String> =
            raw.orEmpty()
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()

        private fun encodeIdSet(ids: Set<String>): String =
            ids.filter { it.isNotBlank() }.sorted().joinToString(",")
    }
}
