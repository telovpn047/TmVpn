package com.telo.wg.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "telo_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        private val KEY_KILL_SWITCH = booleanPreferencesKey("kill_switch")
        private val KEY_AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        private val KEY_CUSTOM_DNS = stringPreferencesKey("custom_dns")
    }

    val killSwitch: Flow<Boolean> = context.dataStore.data.map { it[KEY_KILL_SWITCH] ?: false }
    val autoConnect: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_CONNECT] ?: false }
    val customDns: Flow<String> = context.dataStore.data.map { it[KEY_CUSTOM_DNS] ?: "1.1.1.1" }

    suspend fun setKillSwitch(enabled: Boolean) {
        context.dataStore.edit { it[KEY_KILL_SWITCH] = enabled }
    }

    suspend fun setAutoConnect(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_CONNECT] = enabled }
    }

    suspend fun setCustomDns(dns: String) {
        context.dataStore.edit { it[KEY_CUSTOM_DNS] = dns }
    }
}
