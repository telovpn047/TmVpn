package com.seyit474.tmvpn.ui

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val vm: VpnViewModel by viewModels()

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // VPN izni alındı → servisi başlat
            // TODO: XrayVpnService.start(this, selectedConfig)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                        vm = vm,
                        onConnect = { requestVpnPermission() }
                    )
                }
            }
        }
        // İlk açılışta otomatik tara
        vm.refreshAndPickFastest()
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            // İzin zaten var
            // TODO: XrayVpnService.start(this, selectedConfig)
        }
    }
}

@Composable
fun HomeScreen(vm: VpnViewModel, onConnect: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            "TmVpn",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            statusLabel(state),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(48.dp))
        ConnectButton(state = state, onClick = onConnect)
        Spacer(Modifier.height(32.dp))

        when (val s = state) {
            is VpnViewModel.UiState.Ready -> {
                ServerList(
                    results = s.results,
                    selectedId = s.selected.id,
                    onSelect = vm::selectServer
                )
            }
            is VpnViewModel.UiState.Error -> {
                Text(s.message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { vm.refreshAndPickFastest() }) {
                    Text("Tekrar dene")
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun ConnectButton(state: VpnViewModel.UiState, onClick: () -> Unit) {
    val (label, color, enabled) = when (state) {
        is VpnViewModel.UiState.Loading -> Triple("Sunucular alınıyor...", Color(0xFF6B7280), false)
        is VpnViewModel.UiState.Testing -> Triple("Test ediliyor...", Color(0xFF6B7280), false)
        is VpnViewModel.UiState.Ready -> Triple("BAĞLAN", Color(0xFF10B981), true)
        is VpnViewModel.UiState.Connecting -> Triple("Bağlanıyor...", Color(0xFFF59E0B), false)
        is VpnViewModel.UiState.Connected -> Triple("BAĞLI", Color(0xFF10B981), true)
        else -> Triple("Hazırlanıyor", Color(0xFF6B7280), false)
    }
    Box(
        modifier = Modifier
            .size(180.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        TextButton(onClick = onClick, enabled = enabled) {
            Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ServerList(
    results: List<com.seyit474.tmvpn.ping.ServerPinger.Result>,
    selectedId: String,
    onSelect: (com.seyit474.tmvpn.model.ServerConfig) -> Unit
) {
    Text(
        "Sunucular (${results.size})",
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(results, key = { it.config.id }) { r ->
            ServerRow(r, selected = r.config.id == selectedId, onClick = { onSelect(r.config) })
        }
    }
}

@Composable
private fun ServerRow(
    r: com.seyit474.tmvpn.ping.ServerPinger.Result,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(r.config.remark, fontWeight = FontWeight.Medium)
                Text(
                    "${r.config.protocol.name} • ${r.config.address}:${r.config.port}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                if (r.isReachable) "${r.latencyMs} ms" else "—",
                color = latencyColor(r.latencyMs, r.isReachable),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun latencyColor(ms: Long, reachable: Boolean): Color = when {
    !reachable -> Color(0xFFEF4444)
    ms < 150 -> Color(0xFF10B981)
    ms < 400 -> Color(0xFFF59E0B)
    else -> Color(0xFFEF4444)
}

private fun statusLabel(s: VpnViewModel.UiState): String = when (s) {
    VpnViewModel.UiState.Idle -> "Hazır"
    VpnViewModel.UiState.Loading -> "Sunucular alınıyor"
    VpnViewModel.UiState.Testing -> "Hızlar ölçülüyor"
    is VpnViewModel.UiState.Ready -> "En hızlı: ${s.selected.remark}"
    VpnViewModel.UiState.Connecting -> "Bağlanıyor"
    is VpnViewModel.UiState.Connected -> "Bağlı: ${s.server.remark}"
    is VpnViewModel.UiState.Error -> "Hata"
}
