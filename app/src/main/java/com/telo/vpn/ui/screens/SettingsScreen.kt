package com.telo.vpn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telo.vpn.data.AppPreferences
import com.telo.vpn.ui.MainViewModel
import com.telo.vpn.ui.theme.TeloGreen

@Composable
fun SettingsScreen(
    vm: MainViewModel,
    prefs: AppPreferences,
    onNavigateSplitTunneling: () -> Unit
) {
    val killSwitch   by prefs.killSwitch.collectAsState(initial = false)
    val autoConnect  by prefs.autoConnect.collectAsState(initial = false)
    val autoReconnect by prefs.autoReconnect.collectAsState(initial = false)
    val showTrafficNotif by prefs.showTrafficNotif.collectAsState(initial = true)
    val splitApps    by prefs.splitTunnelingApps.collectAsState(initial = emptySet())
    val savedDns     by prefs.customDns.collectAsState(initial = "")

    val clipboard = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    var hwidCopied by remember { mutableStateOf(false) }
    var dnsInput by remember(savedDns) { mutableStateOf(savedDns) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            "Sazlamalar", fontSize = 20.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )

        // ── Enjam ──────────────────────────────────────────────
        SectionHeader("Enjam")

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null,
                    tint = TeloGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Enjam UUID (Server üçin)", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(vm.hwidUuid, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace)
                }
                IconButton(onClick = {
                    clipboard.setText(AnnotatedString(vm.hwidUuid))
                    hwidCopied = true
                }) {
                    Icon(
                        if (hwidCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Kopyala",
                        tint = if (hwidCopied) TeloGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        if (hwidCopied) {
            Text("Göçürildi", fontSize = 11.sp, color = TeloGreen,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
        }

        // ── VPN ────────────────────────────────────────────────
        Spacer(Modifier.height(8.dp))
        SectionHeader("VPN")

        ToggleRow(
            icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = TeloGreen) },
            title = "Kill Switch",
            subtitle = "VPN kesilende internet birikdirmesini kes",
            checked = killSwitch,
            onCheckedChange = { vm.setKillSwitch(it) }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
        ToggleRow(
            icon = { Icon(Icons.Default.PowerSettingsNew, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            title = "Açylanda birikdir",
            subtitle = "Enjam açylanda awtomatik VPN birikdirmesini gur",
            checked = autoConnect,
            onCheckedChange = { vm.setAutoConnect(it) }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
        ToggleRow(
            icon = { Icon(Icons.Default.Autorenew, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            title = "Awtomatik täzeden birikdir",
            subtitle = "VPN öçse awtomatik täzeden birikdir",
            checked = autoReconnect,
            onCheckedChange = { vm.setAutoReconnect(it) }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
        ToggleRow(
            icon = { Icon(Icons.Default.Notifications, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            title = "Bildirimde trafik",
            subtitle = "Bildiriş setirinde ýüklenme/ýükleme tizligini görkeziň",
            checked = showTrafficNotif,
            onCheckedChange = { vm.setShowTrafficNotif(it) }
        )

        // ── DNS ────────────────────────────────────────────────
        Spacer(Modifier.height(8.dp))
        SectionHeader("DNS")

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Dns, contentDescription = null,
                        tint = TeloGreen, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Özel DNS", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = dnsInput,
                    onValueChange = { dnsInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("1.1.1.1 (goýberilen)") },
                    label = { Text("DNS IP salgysy") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        vm.setCustomDns(dnsInput.trim())
                        focusManager.clearFocus()
                    }),
                    trailingIcon = {
                        if (dnsInput.isNotEmpty()) {
                            IconButton(onClick = {
                                dnsInput = ""
                                vm.setCustomDns("")
                                focusManager.clearFocus()
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Arassala")
                            }
                        }
                    }
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Boş goýsaňyz 1.1.1.1 ulanylýar. Üýtgeşme VPN täzeden bağlananda güýje girýär.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("1.1.1.1", "8.8.8.8", "9.9.9.9").forEach { dns ->
                        SuggestionChip(
                            onClick = { dnsInput = dns; vm.setCustomDns(dns); focusManager.clearFocus() },
                            label = { Text(dns, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }

        // ── Bölünmüş tünel ────────────────────────────────────
        Spacer(Modifier.height(8.dp))
        SectionHeader("Bölünmüş tünel")

        Card(
            onClick = onNavigateSplitTunneling,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CallSplit, contentDescription = null,
                    tint = TeloGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Uygulama saýlamak", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    val count = splitApps.size
                    Text(
                        if (count == 0) "Ähli uygulama VPN ulanýar"
                        else "$count uygulama saýlanan",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ── Goşundy ───────────────────────────────────────────
        Spacer(Modifier.height(8.dp))
        SectionHeader("Goşundy")

        InfoRow(
            icon = { Icon(Icons.Default.Info, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            title = "Wersiýa",
            value = "2.0.0"
        )
        InfoRow(
            icon = { Icon(Icons.Default.Security, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            title = "Protokol",
            value = "VLESS / VMess / Shadowsocks / Trojan"
        )
        InfoRow(
            icon = { Icon(Icons.Default.Code, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            title = "Binýat",
            value = "Xray-core"
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun ToggleRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TeloGreen,
                checkedTrackColor = TeloGreen.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun InfoRow(
    icon: @Composable () -> Unit,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}
