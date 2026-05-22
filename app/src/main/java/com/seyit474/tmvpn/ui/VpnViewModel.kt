package com.seyit474.tmvpn.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seyit474.tmvpn.BuildConfig
import com.seyit474.tmvpn.model.ServerConfig
import com.seyit474.tmvpn.ping.ServerPinger
import com.seyit474.tmvpn.subscription.SubscriptionFetcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VpnViewModel(
    private val fetcher: SubscriptionFetcher = SubscriptionFetcher(),
    private val pinger: ServerPinger = ServerPinger()
) : ViewModel() {

    sealed interface UiState {
        data object Idle : UiState
        data object Loading : UiState              // subscription çekiliyor
        data object Testing : UiState              // sunucular pingleniyor
        data class Ready(
            val results: List<ServerPinger.Result>,
            val selected: ServerConfig
        ) : UiState
        data object Connecting : UiState
        data class Connected(val server: ServerConfig) : UiState
        data class Error(val message: String) : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun refreshAndPickFastest() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val list = fetcher.fetch(BuildConfig.SUBSCRIPTION_URL).getOrElse {
                _state.value = UiState.Error("Abonelik alınamadı: ${it.message}")
                return@launch
            }
            if (list.isEmpty()) {
                _state.value = UiState.Error("Sunucu bulunamadı")
                return@launch
            }
            _state.value = UiState.Testing
            val results = pinger.pingAll(list)
            val fastest = results.firstOrNull { it.isReachable }?.config
            if (fastest == null) {
                _state.value = UiState.Error("Hiçbir sunucuya ulaşılamadı")
                return@launch
            }
            _state.value = UiState.Ready(results, fastest)
        }
    }

    fun selectServer(cfg: ServerConfig) {
        val cur = _state.value
        if (cur is UiState.Ready) {
            _state.value = cur.copy(selected = cfg)
        }
    }

    fun onConnectClicked() {
        val cur = _state.value as? UiState.Ready ?: return
        _state.value = UiState.Connecting
        // VpnService start çağrısı Activity tarafında yapılır (prepare için)
        // Bağlantı kurulduğunda dışarıdan markConnected çağrılır
    }

    fun markConnected(cfg: ServerConfig) {
        _state.value = UiState.Connected(cfg)
    }

    fun markDisconnected() {
        _state.value = UiState.Idle
    }
}
