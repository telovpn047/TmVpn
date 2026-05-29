package com.telo.wg.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.telo.wg.data.TunnelConfig
import com.telo.wg.ui.MainViewModel
import com.telo.wg.ui.theme.TeloGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunnelSelectSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val tunnels by viewModel.tunnelList.collectAsState()
    val active by viewModel.activeTunnel.collectAsState()
    var showImport by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tuneller",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showImport = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Tunel goş")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (tunnels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Tunel tapylmady",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FilledTonalButton(onClick = { showImport = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tunel goş")
                        }
                    }
                }
            } else {
                LazyColumn {
                    items(tunnels, key = { it.id }) { tunnel ->
                        TunnelRow(
                            tunnel = tunnel,
                            isActive = tunnel.id == active?.id,
                            onSelect = {
                                viewModel.setActiveTunnel(tunnel.id)
                                onDismiss()
                            },
                            onDelete = { viewModel.deleteTunnel(tunnel.id) }
                        )
                    }
                }
            }
        }
    }

    if (showImport) {
        ImportSheet(
            viewModel = viewModel,
            onDismiss = { showImport = false }
        )
    }
}

@Composable
private fun TunnelRow(
    tunnel: TunnelConfig,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Text(
                text = tunnel.name,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
        },
        leadingContent = {
            Icon(
                imageVector = if (isActive) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isActive) TeloGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        },
        trailingContent = {
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Sil",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        },
        modifier = Modifier.clickable(onClick = onSelect)
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Tuneli sil?") },
            text = { Text("\"${tunnel.name}\" adly tunel silinsin mi?") },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Ýatyr") }
            }
        )
    }
}
