package com.telo.wg.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telo.wg.service.TeloWgService
import com.telo.wg.ui.MainActivity
import com.telo.wg.ui.MainViewModel
import com.telo.wg.ui.theme.TeloGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val state by viewModel.connectionState.collectAsState()
    val rxSpeed by viewModel.rxSpeed.collectAsState()
    val txSpeed by viewModel.txSpeed.collectAsState()
    val activeTunnel by viewModel.activeTunnel.collectAsState()
    val importError by viewModel.importError.collectAsState()

    var showTunnelSheet by remember { mutableStateOf(false) }
    var showImportSheet by remember { mutableStateOf(false) }

    val isConnected = state == TeloWgService.ConnectionState.CONNECTED
    val isConnecting = state == TeloWgService.ConnectionState.CONNECTING

    val buttonColor by animateColorAsState(
        targetValue = when (state) {
            TeloWgService.ConnectionState.CONNECTED -> TeloGreen
            TeloWgService.ConnectionState.CONNECTING -> Color(0xFFFFA726)
            TeloWgService.ConnectionState.ERROR -> Color(0xFFE53935)
            else -> Color(0xFF757575)
        },
        animationSpec = tween(400), label = "btnColor"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isConnecting) 0.95f else 1f,
        animationSpec = tween(400), label = "btnScale"
    )

    importError?.let { err ->
        LaunchedEffect(err) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearImportError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TeloWG", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Sazlamalar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showImportSheet = true },
                containerColor = TeloGreen
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tunel goş", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Tunnel selector card
            OutlinedCard(
                onClick = { showTunnelSheet = true },
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeTunnel?.name ?: "Tunel saýlanmady",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (activeTunnel != null)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = when (state) {
                    TeloWgService.ConnectionState.CONNECTED -> "Birikdirildi"
                    TeloWgService.ConnectionState.CONNECTING -> "Birikdirilýär..."
                    TeloWgService.ConnectionState.ERROR -> "Ýalňyşlyk"
                    else -> "Birikdirilmeýär"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = buttonColor
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    if (activeTunnel == null) {
                        showImportSheet = true
                        return@Button
                    }
                    if (isConnected || isConnecting) viewModel.toggleVpn()
                    else activity?.requestVpnAndConnect() ?: viewModel.toggleVpn()
                },
                modifier = Modifier
                    .size(160.dp)
                    .scale(buttonScale),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                enabled = state != TeloWgService.ConnectionState.CONNECTING
            ) {
                Text(
                    text = if (isConnected) "ÖÇÜR" else "BIRIKDIR",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            if (isConnected) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TrafficStat(label = "↑ Ugrat", bytes = txSpeed)
                        VerticalDivider(modifier = Modifier.height(40.dp))
                        TrafficStat(label = "↓ Al", bytes = rxSpeed)
                    }
                }
            }

            if (activeTunnel == null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Birikdirmek üçin tunel goşuň",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        importError?.let {
            Snackbar(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
            ) { Text(it) }
        }
    }

    if (showTunnelSheet) {
        TunnelSelectSheet(viewModel = viewModel, onDismiss = { showTunnelSheet = false })
    }

    if (showImportSheet) {
        ImportSheet(viewModel = viewModel, onDismiss = { showImportSheet = false })
    }
}

@Composable
private fun TrafficStat(label: String, bytes: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatSpeed(bytes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TeloGreen
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

private fun formatSpeed(bytesPerSec: Long): String = when {
    bytesPerSec >= 1_000_000 -> "%.1f MB/s".format(bytesPerSec / 1_000_000.0)
    bytesPerSec >= 1_000 -> "%.1f KB/s".format(bytesPerSec / 1_000.0)
    else -> "$bytesPerSec B/s"
}
