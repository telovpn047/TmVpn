package com.telo.wg.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.telo.wg.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val qrLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { qrText ->
            viewModel.importFromText(qrText, "QR Tunel")
            onDismiss()
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.importFromUri(it, context)
            onDismiss()
        }
    }

    var showPasteDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tunel goş",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            ImportOptionButton(
                icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                title = "QR kod tara",
                subtitle = "Kameranyň üsti bilen config skan et"
            ) {
                qrLauncher.launch(
                    ScanOptions().apply {
                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        setPrompt("TeloWG QR kod")
                        setBeepEnabled(false)
                        setCameraId(0)
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            ImportOptionButton(
                icon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                title = "Dosyadan al",
                subtitle = ".conf ýa-da .zip faýl saýla"
            ) {
                fileLauncher.launch("*/*")
            }

            Spacer(modifier = Modifier.height(12.dp))

            ImportOptionButton(
                icon = { Icon(Icons.Default.ContentPaste, contentDescription = null) },
                title = "Clipboard'dan yapıştır",
                subtitle = "Kopyalanan config tekstini yapıştır"
            ) {
                showPasteDialog = true
            }
        }
    }

    if (showPasteDialog) {
        PasteConfigDialog(
            context = context,
            onConfirm = { text ->
                viewModel.importFromText(text)
                showPasteDialog = false
                onDismiss()
            },
            onDismiss = { showPasteDialog = false }
        )
    }
}

@Composable
private fun ImportOptionButton(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun PasteConfigDialog(
    context: Context,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipText = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() ?: ""
    var text by remember { mutableStateOf(clipText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Config yapıştır") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                label = { Text("[Interface] bilen başlayan config") },
                placeholder = { Text("[Interface]\nPrivateKey = ...") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                enabled = text.isNotBlank()
            ) { Text("Goş") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Ýatyr") }
        }
    )
}
