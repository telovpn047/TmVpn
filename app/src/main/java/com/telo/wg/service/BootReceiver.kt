package com.telo.wg.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.telo.wg.data.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = AppPreferences(context)
            if (prefs.autoConnect.first()) {
                val vpnIntent = Intent(context, TeloWgService::class.java).apply {
                    action = TeloWgService.ACTION_START
                }
                context.startForegroundService(vpnIntent)
            }
        }
    }
}
