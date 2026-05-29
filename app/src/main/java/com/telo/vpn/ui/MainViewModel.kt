package com.telo.vpn.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.telo.vpn.api.SubscriptionUserInfo
import com.telo.vpn.data.VpnRepository
import com.telo.vpn.model.ConnectionState
import com.telo.vpn.model.ServerConfig
import com.telo.vpn.model.TrafficStats
import com.telo.vpn.service.XrayConfigBuilder
import com.telo.vpn.service.XrayVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = VpnRepository(app)

    val hwid: String get() = repo.getHwid()
    val hwidUuid: String get() = com.telo.vpn.util.HwidManager.getHwidAsUuid(getApplication())

    private val _connState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connState: StateFlow<ConnectionState> = _connState.asStateFlow()

    private val _userInfo = MutableStateFlow(SubscriptionUserInfo())
    val userInfo: StateFlow<SubscriptionUserInfo> = _userInfo.asStateFlow()

    val trafficStats: StateFlow<TrafficStats> = XrayVpnService.trafficStats
    val isVpnConnected: StateFlow<Boolean> = XrayVpnService.isConnected

    // Exposed to Activity for use when launching the VPN service
    val customDns: StateFlow<String> = repo.prefs.customDns
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val showTrafficNotif: StateFlow<Boolean> = repo.prefs.showTrafficNotif
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val splitTunnelingApps: StateFlow<Set<String>> = repo.prefs.splitTunnelingApps
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val splitMode: StateFlow<String> = repo.prefs.splitMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, "bypass")

    private var _pendingConfig: ServerConfig? = null
    private var _lastConnectedConfig: ServerConfig? = null
    private var _killSwitch = false
    private var _autoReconnect = false
    val killSwitch get() = _killSwitch

    init {
        viewModelScope.launch {
            _killSwitch = repo.prefs.killSwitch.first()
            _autoReconnect = repo.prefs.autoReconnect.first()
            loadServers()
        }
        viewModelScope.launch { repo.prefs.killSwitch.collect { _killSwitch = it } }
        viewModelScope.launch { repo.prefs.autoReconnect.collect { _autoReconnect = it } }

        viewModelScope.launch {
            var wasPreviouslyConnected = false
            XrayVpnService.isConnected.collect { connected ->
                if (connected && _connState.value is ConnectionState.Connecting) {
                    _pendingConfig?.let {
                        _connState.value = ConnectionState.Connected(it)
                        _lastConnectedConfig = it
                        _pendingConfig = null
                    }
                } else if (wasPreviouslyConnected && !connected &&
                           _connState.value is ConnectionState.Connected) {
                    _connState.value = ConnectionState.Idle
                    _pendingConfig = null
                    if (_autoReconnect) {
                        viewModelScope.launch { delay(2000); doAutoReconnect() }
                    } else {
                        loadServers()
                    }
                }
                wasPreviouslyConnected = connected
            }
        }

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

    private fun doAutoReconnect() {
        val cfg = _lastConnectedConfig ?: run { loadServers(); return }
        val json = XrayConfigBuilder.build(cfg, customDns = customDns.value)
        _connState.value = ConnectionState.Connecting
        _pendingConfig = cfg
        XrayVpnService.start(
            ctx            = getApplication(),
            configJson     = json,
            serverName     = cfg.remark,
            killSwitch     = _killSwitch,
            splitApps      = ArrayList(splitTunnelingApps.value),
            splitMode      = splitMode.value,
            customDns      = customDns.value,
            showTrafficNotif = showTrafficNotif.value
        )
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

    fun setKillSwitch(enabled: Boolean)      { viewModelScope.launch { repo.prefs.setKillSwitch(enabled) } }
    fun setAutoConnect(enabled: Boolean)     { viewModelScope.launch { repo.prefs.setAutoConnect(enabled) } }
    fun setAutoReconnect(enabled: Boolean)   { viewModelScope.launch { repo.prefs.setAutoReconnect(enabled) } }
    fun setCustomDns(dns: String)            { viewModelScope.launch { repo.prefs.setCustomDns(dns) } }
    fun setShowTrafficNotif(enabled: Boolean){ viewModelScope.launch { repo.prefs.setShowTrafficNotif(enabled) } }
}
