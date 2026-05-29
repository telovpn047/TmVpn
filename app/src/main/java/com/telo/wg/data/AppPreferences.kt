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
        private val KEY_TUNNELS = stringPreferencesKey("tunnel_list")
        private val KEY_ACTIVE_TUNNEL = stringPreferencesKey("active_tunnel_id")
    }

    val killSwitch: Flow<Boolean> = context.dataStore.data.map { it[KEY_KILL_SWITCH] ?: false }
    val autoConnect: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_CONNECT] ?: false }
    val customDns: Flow<String> = context.dataStore.data.map { it[KEY_CUSTOM_DNS] ?: "1.1.1.1" }

    val tunnelList: Flow<List<TunnelConfig>> = context.dataStore.data.map {
        tunnelListFromJson(it[KEY_TUNNELS] ?: "")
    }

    val activeTunnelId: Flow<String?> = context.dataStore.data.map { it[KEY_ACTIVE_TUNNEL] }

    val activeTunnel: Flow<TunnelConfig?> = context.dataStore.data.map { prefs ->
        val list = tunnelListFromJson(prefs[KEY_TUNNELS] ?: "")
        val activeId = prefs[KEY_ACTIVE_TUNNEL]
        list.firstOrNull { it.id == activeId } ?: list.firstOrNull()
    }

    suspend fun saveTunnel(config: TunnelConfig) {
        context.dataStore.edit { prefs ->
            val list = tunnelListFromJson(prefs[KEY_TUNNELS] ?: "")
            val updated = list.filter { it.id != config.id } + config
            prefs[KEY_TUNNELS] = updated.toJsonString()
            if (prefs[KEY_ACTIVE_TUNNEL] == null) prefs[KEY_ACTIVE_TUNNEL] = config.id
        }
    }

    suspend fun deleteTunnel(id: String) {
        context.dataStore.edit { prefs ->
            val list = tunnelListFromJson(prefs[KEY_TUNNELS] ?: "")
            val updated = list.filter { it.id != id }
            prefs[KEY_TUNNELS] = updated.toJsonString()
            if (prefs[KEY_ACTIVE_TUNNEL] == id) {
                prefs[KEY_ACTIVE_TUNNEL] = updated.firstOrNull()?.id ?: ""
            }
        }
    }

    suspend fun setActiveTunnel(id: String) {
        context.dataStore.edit { it[KEY_ACTIVE_TUNNEL] = id }
    }

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
