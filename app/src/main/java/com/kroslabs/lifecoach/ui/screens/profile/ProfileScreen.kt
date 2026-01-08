package com.kroslabs.lifecoach.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kroslabs.lifecoach.data.model.ThemeMode
import com.kroslabs.lifecoach.data.model.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    themeMode: ThemeMode,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    totalTokensUsed: Int,
    totalCostCents: Float,
    apiKeySet: Boolean,
    userProfile: UserProfile?,
    onThemeChange: (ThemeMode) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onApiKeySave: (String) -> Unit,
    onExportData: () -> Unit,
    onClearData: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onDeepDiveClick: () -> Unit,
    onDebugLogsClick: () -> Unit,
    onNotificationToggle: (String, Boolean) -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Quick Actions
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilledTonalButton(
                        onClick = onAnalyticsClick,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analytics")
                    }
                    FilledTonalButton(
                        onClick = onDeepDiveClick,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Explore, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Deep Dive")
                    }
                }
            }

            // API Usage Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Token,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "API Usage",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Total Tokens",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "%,d".format(totalTokensUsed),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Estimated Cost",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$%.2f".format(totalCostCents / 100),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notifications Section
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text("Enable Notifications") },
                supportingContent = { Text("Receive reminders and alerts") },
                leadingContent = {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                },
                trailingContent = {
                    Switch(
                        checked = userProfile?.notificationsEnabled ?: true,
                        onCheckedChange = { onNotificationToggle("all", it) }
                    )
                }
            )

            if (userProfile?.notificationsEnabled == true) {
                ListItem(
                    headlineContent = { Text("Daily Check-in Reminders") },
                    leadingContent = { Spacer(modifier = Modifier.width(24.dp)) },
                    trailingContent = {
                        Switch(
                            checked = userProfile.dailyCheckInReminder,
                            onCheckedChange = { onNotificationToggle("daily", it) }
                        )
                    }
                )

                ListItem(
                    headlineContent = { Text("Weekly Reflection Prompts") },
                    leadingContent = { Spacer(modifier = Modifier.width(24.dp)) },
                    trailingContent = {
                        Switch(
                            checked = userProfile.weeklyReflectionReminder,
                            onCheckedChange = { onNotificationToggle("weekly", it) }
                        )
                    }
                )

                ListItem(
                    headlineContent = { Text("Experiment Lifecycle Alerts") },
                    leadingContent = { Spacer(modifier = Modifier.width(24.dp)) },
                    trailingContent = {
                        Switch(
                            checked = userProfile.experimentLifecycleAlerts,
                            onCheckedChange = { onNotificationToggle("lifecycle", it) }
                        )
                    }
                )

                ListItem(
                    headlineContent = { Text("Milestone Celebrations") },
                    leadingContent = { Spacer(modifier = Modifier.width(24.dp)) },
                    trailingContent = {
                        Switch(
                            checked = userProfile.milestoneNotifications,
                            onCheckedChange = { onNotificationToggle("milestone", it) }
                        )
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Settings Section
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Theme
            ListItem(
                headlineContent = { Text("Theme") },
                supportingContent = { Text(themeMode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                leadingContent = {
                    Icon(Icons.Default.Palette, contentDescription = null)
                },
                modifier = Modifier.clickable { showThemeDialog = true }
            )

            // Claude API Key
            ListItem(
                headlineContent = { Text("Claude API Key") },
                supportingContent = {
                    Text(if (apiKeySet) "API key configured" else "Not configured")
                },
                leadingContent = {
                    Icon(Icons.Default.Key, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    apiKeyInput = ""
                    showApiKeyDialog = true
                }
            )

            // Biometric
            if (biometricAvailable) {
                ListItem(
                    headlineContent = { Text("Biometric Unlock") },
                    supportingContent = { Text("Use fingerprint or face to unlock") },
                    leadingContent = {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = onBiometricToggle
                        )
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Data Management
            Text(
                text = "Data",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text("Export Data") },
                supportingContent = { Text("Download your data as encrypted JSON") },
                leadingContent = {
                    Icon(Icons.Default.Download, contentDescription = null)
                },
                modifier = Modifier.clickable(onClick = onExportData)
            )

            ListItem(
                headlineContent = { Text("Clear All Data") },
                supportingContent = { Text("Delete all paths, experiments, and journal entries") },
                leadingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier.clickable { showClearDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Developer Section
            Text(
                text = "Developer",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text("Debug Logs") },
                supportingContent = { Text("View app debug logs for troubleshooting") },
                leadingContent = {
                    Icon(Icons.Outlined.BugReport, contentDescription = null)
                },
                modifier = Modifier.clickable(onClick = onDebugLogsClick)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // App Info
            Text(
                text = "Life Coach v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onThemeChange(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = mode == themeMode,
                                onClick = {
                                    onThemeChange(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Data?") },
            text = {
                Text("This will permanently delete all your paths, experiments, and journal entries. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearData()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("Configure Claude API Key") },
            text = {
                Column {
                    Text(
                        text = "Enter your Claude API key. This will be securely encrypted and stored on your device.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key") },
                        placeholder = { Text("sk-ant-...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (apiKeyInput.isNotBlank()) {
                            onApiKeySave(apiKeyInput)
                            showApiKeyDialog = false
                        }
                    },
                    enabled = apiKeyInput.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
