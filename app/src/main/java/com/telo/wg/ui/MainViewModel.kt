package com.telo.wg.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.telo.wg.data.AppPreferences
import com.telo.wg.data.TunnelConfig
import com.telo.wg.data.parseTunnelName
import com.telo.wg.service.TeloWgService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    val tunnelList = prefs.tunnelList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTunnel = prefs.activeTunnel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    fun toggleVpn() {
        val ctx = getApplication<Application>()
        val action = when (connectionState.value) {
            TeloWgService.ConnectionState.CONNECTED,
            TeloWgService.ConnectionState.CONNECTING -> TeloWgService.ACTION_STOP
            else -> TeloWgService.ACTION_START
        }
        ctx.startForegroundService(Intent(ctx, TeloWgService::class.java).apply { this.action = action })
    }

    fun importFromText(text: String, suggestedName: String? = null) {
        viewModelScope.launch {
            val trimmed = text.trim()
            if (!trimmed.contains("[Interface]", ignoreCase = true)) {
                _importError.value = "Geçersiz WireGuard config formatı"
                return@launch
            }
            val name = suggestedName ?: parseTunnelName(trimmed)
            val config = TunnelConfig(name = name, configText = trimmed)
            prefs.saveTunnel(config)
            _importError.value = null
        }
    }

    fun importFromUri(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readText() ?: return@launch
                val fileName = uri.lastPathSegment
                    ?.removeSuffix(".conf")
                    ?.removeSuffix(".zip")
                importFromText(text, fileName)
            } catch (e: Exception) {
                _importError.value = "Dosya okunamadı: ${e.message}"
            }
        }
    }

    fun deleteTunnel(id: String) = viewModelScope.launch { prefs.deleteTunnel(id) }

    fun setActiveTunnel(id: String) = viewModelScope.launch { prefs.setActiveTunnel(id) }

    fun clearImportError() { _importError.value = null }

    fun setKillSwitch(enabled: Boolean) = viewModelScope.launch { prefs.setKillSwitch(enabled) }
    fun setAutoConnect(enabled: Boolean) = viewModelScope.launch { prefs.setAutoConnect(enabled) }
    fun setCustomDns(dns: String) = viewModelScope.launch { prefs.setCustomDns(dns) }
}
