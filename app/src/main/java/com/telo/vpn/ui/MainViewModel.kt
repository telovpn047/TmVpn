package com.telo.vpn.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.telo.vpn.api.SubscriptionUserInfo
import com.telo.vpn.data.VpnRepository
import com.telo.vpn.model.ConnectionState
import com.telo.vpn.model.ServerConfig
import com.telo.vpn.model.TrafficStats
import com.telo.vpn.service.XrayVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = VpnRepository(app)

    val hwid: String get() = repo.getHwid()

    private val _connState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connState: StateFlow<ConnectionState> = _connState.asStateFlow()

    private val _userInfo = MutableStateFlow(SubscriptionUserInfo())
    val userInfo: StateFlow<SubscriptionUserInfo> = _userInfo.asStateFlow()

    val trafficStats: StateFlow<TrafficStats> = XrayVpnService.trafficStats
    val isVpnConnected: StateFlow<Boolean> = XrayVpnService.isConnected

    private var _pendingConfig: ServerConfig? = null
    private var _killSwitch = false
    val killSwitch get() = _killSwitch

    init {
        viewModelScope.launch {
            _killSwitch = repo.prefs.killSwitch.first()
            loadServers()
        }
        viewModelScope.launch {
            repo.prefs.killSwitch.collect { _killSwitch = it }
        }
        // VPN bağlantısı kurulunca Connected'a geç
        viewModelScope.launch {
            var wasPreviouslyConnected = false
            XrayVpnService.isConnected.collect { connected ->
                if (connected && _connState.value is ConnectionState.Connecting) {
                    _pendingConfig?.let {
                        _connState.value = ConnectionState.Connected(it)
                        _pendingConfig = null
                    }
                } else if (wasPreviouslyConnected && !connected &&
                           _connState.value is ConnectionState.Connected) {
                    // VPN dışarıdan kesildi (sistem, pil vb.)
                    _connState.value = ConnectionState.Idle
                    _pendingConfig = null
                    loadServers()
                }
                wasPreviouslyConnected = connected
            }
        }
        // VPN başlatma hatası gelirse Error state'e geç
        viewModelScope.launch {
            XrayVpnService.startError.collect { error ->
                if (error != null && (_connState.value is ConnectionState.Connecting ||
                                      _connState.value is ConnectionState.Connected)) {
                    _connState.value = ConnectionState.Error(error)
                    _pendingConfig = null
                    XrayVpnService.startError.value = null
                }
            }
        }
    }

    fun loadServers() {
        viewModelScope.launch {
            _connState.value = ConnectionState.Loading
            val result = withContext(Dispatchers.IO) { repo.loadSubscription() }
            result
                .onSuccess { (info, servers) ->
                    _userInfo.value = info
                    val best = servers.firstOrNull { it.isReachable } ?: servers.first()
                    _connState.value = ConnectionState.Ready(servers, best.config)
                }
                .onFailure {
                    _connState.value = ConnectionState.Error(
                        it.message ?: "${it.javaClass.simpleName} (HWID: $hwid)"
                    )
                }
        }
    }

    fun selectServer(cfg: ServerConfig) {
        val cur = _connState.value as? ConnectionState.Ready ?: return
        _connState.value = cur.copy(selected = cfg)
    }

    // Connecting state'e geç ve seçili config'i sakla
    fun startConnecting(): ServerConfig? {
        val cur = _connState.value as? ConnectionState.Ready ?: return null
        _pendingConfig = cur.selected
        _connState.value = ConnectionState.Connecting
        return cur.selected
    }

    fun onDisconnect() {
        _connState.value = ConnectionState.Idle
        _pendingConfig = null
        loadServers()
    }

    fun setKillSwitch(enabled: Boolean) {
        viewModelScope.launch { repo.prefs.setKillSwitch(enabled) }
    }

    fun setAutoConnect(enabled: Boolean) {
        viewModelScope.launch { repo.prefs.setAutoConnect(enabled) }
    }
}
