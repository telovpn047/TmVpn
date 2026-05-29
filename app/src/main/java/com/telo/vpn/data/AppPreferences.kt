package com.telo.vpn.data

import android.content.Context
import androidx.datastore.preferences.core.*
import com.telo.vpn.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppPreferences(private val context: Context) {

    companion object {
        val KEY_KILL_SWITCH          = booleanPreferencesKey("kill_switch")
        val KEY_AUTO_CONNECT         = booleanPreferencesKey("auto_connect")
        val KEY_AUTO_RECONNECT       = booleanPreferencesKey("auto_reconnect")
        val KEY_CUSTOM_DNS           = stringPreferencesKey("custom_dns")
        val KEY_SHOW_TRAFFIC_NOTIF   = booleanPreferencesKey("show_traffic_notif")
        val KEY_SPLIT_APPS           = stringPreferencesKey("split_apps")   // comma-separated pkg names
        val KEY_SPLIT_MODE           = stringPreferencesKey("split_mode")   // "bypass" | "allow"
    }

    val killSwitch:         Flow<Boolean>     = context.dataStore.data.map { it[KEY_KILL_SWITCH]        ?: false }
    val autoConnect:        Flow<Boolean>     = context.dataStore.data.map { it[KEY_AUTO_CONNECT]       ?: false }
    val autoReconnect:      Flow<Boolean>     = context.dataStore.data.map { it[KEY_AUTO_RECONNECT]     ?: false }
    val customDns:          Flow<String>      = context.dataStore.data.map { it[KEY_CUSTOM_DNS]         ?: "" }
    val showTrafficNotif:   Flow<Boolean>     = context.dataStore.data.map { it[KEY_SHOW_TRAFFIC_NOTIF] ?: true }
    val splitTunnelingApps: Flow<Set<String>> = context.dataStore.data.map {
        it[KEY_SPLIT_APPS]?.split(",")?.filter { s -> s.isNotBlank() }?.toSet() ?: emptySet()
    }
    val splitMode:          Flow<String>      = context.dataStore.data.map { it[KEY_SPLIT_MODE]         ?: "bypass" }

    suspend fun setKillSwitch(v: Boolean)              { context.dataStore.edit { it[KEY_KILL_SWITCH]      = v } }
    suspend fun setAutoConnect(v: Boolean)             { context.dataStore.edit { it[KEY_AUTO_CONNECT]     = v } }
    suspend fun setAutoReconnect(v: Boolean)           { context.dataStore.edit { it[KEY_AUTO_RECONNECT]   = v } }
    suspend fun setCustomDns(v: String)                { context.dataStore.edit { it[KEY_CUSTOM_DNS]       = v } }
    suspend fun setShowTrafficNotif(v: Boolean)        { context.dataStore.edit { it[KEY_SHOW_TRAFFIC_NOTIF] = v } }
    suspend fun setSplitTunnelingApps(apps: Set<String>) {
        context.dataStore.edit { it[KEY_SPLIT_APPS] = apps.joinToString(",") }
    }
    suspend fun setSplitMode(mode: String)             { context.dataStore.edit { it[KEY_SPLIT_MODE]       = mode } }
}
