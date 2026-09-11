package dev.krydo.mobile.wallet

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.walletStore by preferencesDataStore(name = "krydo_wallet_accounts")

class WalletSessionStore(private val context: Context) {
    private val accountsKey = stringPreferencesKey("accounts_json")
    private val json = Json { ignoreUnknownKeys = true }

    val accounts: Flow<List<WalletAccount>> = context.walletStore.data.map { prefs ->
        val raw = prefs[accountsKey].orEmpty()
        if (raw.isBlank()) emptyList()
        else runCatching { json.decodeFromString<List<WalletAccount>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun setAccounts(list: List<WalletAccount>) {
        context.walletStore.edit { prefs ->
            prefs[accountsKey] = json.encodeToString(list)
        }
    }

    suspend fun upsert(account: WalletAccount) {
        context.walletStore.edit { prefs ->
            val current = prefs[accountsKey].orEmpty().let { raw ->
                if (raw.isBlank()) emptyList()
                else runCatching { json.decodeFromString<List<WalletAccount>>(raw) }.getOrDefault(emptyList())
            }
            val next = current.filterNot {
                it.chainId == account.chainId && it.address.equals(account.address, ignoreCase = true)
            } + account
            prefs[accountsKey] = json.encodeToString(next)
        }
    }

    suspend fun remove(chainId: String, address: String) {
        context.walletStore.edit { prefs ->
            val current = prefs[accountsKey].orEmpty().let { raw ->
                if (raw.isBlank()) emptyList()
                else runCatching { json.decodeFromString<List<WalletAccount>>(raw) }.getOrDefault(emptyList())
            }
            val next = current.filterNot {
                it.chainId == chainId && it.address.equals(address, ignoreCase = true)
            }
            prefs[accountsKey] = json.encodeToString(next)
        }
    }

    suspend fun clear() {
        context.walletStore.edit { it.remove(accountsKey) }
    }
}
