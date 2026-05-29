package com.telo.wg.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.telo.wg.data.AppPreferences
import com.telo.wg.service.TeloWgService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = AppPreferences(app)

    val connectionState: StateFlow<TeloWgService.ConnectionState> =
        TeloWgService.connectionState
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TeloWgService.ConnectionState.IDLE)

    val rxSpeed: StateFlow<Long> =
        TeloWgService.rxBytes
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val txSpeed: StateFlow<Long> =
        TeloWgService.txBytes
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val killSwitch = prefs.killSwitch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val autoConnect = prefs.autoConnect
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val customDns = prefs.customDns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "1.1.1.1")

    fun toggleVpn() {
        val ctx = getApplication<Application>()
        val action = when (connectionState.value) {
            TeloWgService.ConnectionState.CONNECTED,
            TeloWgService.ConnectionState.CONNECTING -> TeloWgService.ACTION_STOP
            else -> TeloWgService.ACTION_START
        }
        ctx.startForegroundService(Intent(ctx, TeloWgService::class.java).apply { this.action = action })
    }

    fun setKillSwitch(enabled: Boolean) = viewModelScope.launch { prefs.setKillSwitch(enabled) }
    fun setAutoConnect(enabled: Boolean) = viewModelScope.launch { prefs.setAutoConnect(enabled) }
    fun setCustomDns(dns: String) = viewModelScope.launch { prefs.setCustomDns(dns) }
}
