package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ChallengeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ChallengeViewModel) {
    val context = LocalContext.current
    val themeMode by viewModel.themeState.collectAsState()
    val scrollState = rememberScrollState()

    var showResetDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Application Data?") },
            text = { Text("This operation is irreversible! Accurate streak configurations, archived and active challenges, alongside logged timelines will be completely purged.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetApp()
                        showResetDialog = false
                        Toast.makeText(context, "All app metadata reset successfully", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Proceed Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import JSON Backup") },
            text = {
                Column {
                    Text("Paste your exported clipboard backup below:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("import_text_field"),
                        placeholder = { Text("Paste JSON here...") },
                        maxLines = 10
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (importText.isNotBlank()) {
                            val success = viewModel.importBackup(importText)
                            if (success) {
                                Toast.makeText(context, "Progress restored successfully!", Toast.LENGTH_SHORT).show()
                                showImportDialog = false
                                importText = ""
                            } else {
                                Toast.makeText(context, "Invalid JSON structure. Please check and retry.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Restore Progress")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Metadata & Rules",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Setting Section
            Text(
                text = "Styling Preferences",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            val isDark = MaterialTheme.colorScheme.background.red < 0.1f
            val baseCardBg = if (isDark) Color(0xFF161618) else MaterialTheme.colorScheme.surface
            val borderCol = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                    .testTag("theme_selection_panel"),
                colors = CardDefaults.cardColors(containerColor = baseCardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Visual Interface Theme",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Customize between midnight-skies or crisp daylight surfaces.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf(
                            Triple("system", "System", Icons.Default.Settings),
                            Triple("light", "Crisp Light", Icons.Default.LightMode),
                            Triple("dark", "Cosmic Dark", Icons.Default.DarkMode)
                        )
                        modes.forEach { mode ->
                            val isSelected = themeMode == mode.first
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setTheme(mode.first) },
                                label = { Text(mode.second) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = mode.third,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_chip_${mode.first}")
                            )
                        }
                    }
                }
            }

            // Sync Data & Coordinates
            Text(
                text = "Storage & Sync Backup",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderCol, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = baseCardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Export Progress Item
                    SettingsRow(
                        title = "Export Coordinates Data",
                        subtitle = "Copy all challenges, streaks and logs to clipboard in JSON formatting.",
                        icon = Icons.Default.ContentCopy,
                        onClick = {
                            val backup = viewModel.exportBackup()
                            if (backup.isNotBlank()) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Daily Challenge Backup", backup)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Backup copied to clipboard! Share it safely.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "No existing metadata logs to export.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        tag = "export_setting_row"
                    )

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    // Import Progress Item
                    SettingsRow(
                        title = "Restore Backup",
                        subtitle = "Paste coordinate configurations to instantly recover your achievements.",
                        icon = Icons.Default.RestorePage,
                        onClick = { showImportDialog = true },
                        tag = "import_setting_row"
                    )

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    // Wipe Reset Item
                    SettingsRow(
                        title = "Purge Metadata Reset",
                        subtitle = "Wipe and erase all sqlite configurations completely. Warning: irreversible.",
                        icon = Icons.Default.Warning,
                        iconColor = MaterialTheme.colorScheme.error,
                        onClick = { showResetDialog = true },
                        tag = "reset_setting_row"
                    )
                }
            }

            // About Section
            Text(
                text = "System Information",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderCol, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = baseCardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daily Challenge Tracker",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "v1.0.0 (Native Offline Build)\nReleased on: Android Jetpack Compose\nDeveloper ID: adityaudai1322@gmail.com",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Engage in incremental daily progression. By utilizing localized SQLite coordinates and push alarm schedules, we guarantee 100% privacy and offline capability with absolutely no tracking.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
