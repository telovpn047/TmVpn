package com.telo.wg.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.telo.wg.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val killSwitch by viewModel.killSwitch.collectAsState()
    val autoConnect by viewModel.autoConnect.collectAsState()
    val customDns by viewModel.customDns.collectAsState()

    var dnsInput by remember(customDns) { mutableStateOf(customDns) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sazlamalar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Yza")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggleRow(
                title = "Kill Switch",
                subtitle = "Tunel öçende ähli traffigi bes et",
                checked = killSwitch,
                onCheckedChange = { viewModel.setKillSwitch(it) }
            )

            HorizontalDivider()

            SettingsToggleRow(
                title = "Açylanda birikdir",
                subtitle = "Enjam açylanda awtomatik birik",
                checked = autoConnect,
                onCheckedChange = { viewModel.setAutoConnect(it) }
            )

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DNS Serweri",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = dnsInput,
                onValueChange = { dnsInput = it },
                label = { Text("DNS (meselem: 1.1.1.1)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (dnsInput != customDns) {
                        TextButton(onClick = { viewModel.setCustomDns(dnsInput) }) {
                            Text("Sakla")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "TeloWG v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
