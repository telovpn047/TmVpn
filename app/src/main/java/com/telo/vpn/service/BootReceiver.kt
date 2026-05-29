package com.telo.vpn.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.telo.vpn.api.SubscriptionApi
import com.telo.vpn.data.VpnRepository
import com.telo.vpn.dataStore
import com.telo.vpn.subscription.ConfigParser
import com.telo.vpn.util.HwidManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        CoroutineScope(Dispatchers.IO).launch {
            val prefs = context.dataStore.data.first()
            val autoConnect = prefs[booleanPreferencesKey("auto_connect")] ?: false
            if (!autoConnect) return@launch

            runCatching {
                val api = SubscriptionApi()
                val result = api.fetchSubscription(VpnRepository.SUB_URL).getOrThrow()
                val hwidUuid = HwidManager.getHwidAsUuid(context)
                val servers = ConfigParser.parseSubscriptionWithHwid(result.rawLinks, hwidUuid)
                val first = servers.firstOrNull() ?: return@runCatching

                val killSwitch       = prefs[booleanPreferencesKey("kill_switch")] ?: false
                val customDns        = prefs[stringPreferencesKey("custom_dns")] ?: ""
                val showTrafficNotif = prefs[booleanPreferencesKey("show_traffic_notif")] ?: true
                val splitAppsRaw     = prefs[stringPreferencesKey("split_apps")] ?: ""
                val splitMode        = prefs[stringPreferencesKey("split_mode")] ?: "bypass"
                val splitApps        = ArrayList(
                    splitAppsRaw.split(",").filter { it.isNotBlank() }
                )

                val configJson = XrayConfigBuilder.build(first, customDns = customDns)
                XrayVpnService.start(
                    ctx              = context,
                    configJson       = configJson,
                    serverName       = first.remark,
                    killSwitch       = killSwitch,
                    splitApps        = splitApps,
                    splitMode        = splitMode,
                    customDns        = customDns,
                    showTrafficNotif = showTrafficNotif
                )
            }
        }
    }
}
