package com.kroslabs.lifecoach.ui.screens.journal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kroslabs.lifecoach.data.model.JournalEntry
import com.kroslabs.lifecoach.data.model.LifePath

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEntryScreen(
    existingEntry: JournalEntry?,
    paths: List<LifePath>,
    aiInsights: String,
    isAnalyzing: Boolean,
    onBack: () -> Unit,
    onSave: (content: String, pathId: Long?) -> Unit,
    onDelete: () -> Unit,
    onAnalyze: (String) -> Unit
) {
    var content by remember(existingEntry) { mutableStateOf(existingEntry?.content ?: "") }
    var expandedPath by remember { mutableStateOf(false) }
    var selectedPath by remember(existingEntry) {
        mutableStateOf(paths.find { it.id == existingEntry?.pathId })
    }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingEntry != null) "Edit Entry" else "New Entry") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (existingEntry != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    IconButton(
                        onClick = { onSave(content, selectedPath?.id) },
                        enabled = content.isNotBlank()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Link to Path
            ExposedDropdownMenuBox(
                expanded = expandedPath,
                onExpandedChange = { expandedPath = it }
            ) {
                OutlinedTextField(
                    value = selectedPath?.name ?: "No path linked",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Link to Path (optional)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPath) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedPath,
                    onDismissRequest = { expandedPath = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("No path") },
                        onClick = {
                            selectedPath = null
                            expandedPath = false
                        }
                    )
                    paths.forEach { path ->
                        DropdownMenuItem(
                            text = { Text(path.name) },
                            onClick = {
                                selectedPath = path
                                expandedPath = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Journal Content
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("What's on your mind?") },
                placeholder = { Text("Reflect on your experiments, feelings, or discoveries...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                minLines = 8
            )

            Spacer(modifier = Modifier.height(16.dp))

            // AI Analysis Button
            OutlinedButton(
                onClick = { onAnalyze(content) },
                enabled = content.length >= 50 && !isAnalyzing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyzing...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Get AI Insights")
                }
            }

            // AI Insights Display
            if (aiInsights.isNotEmpty() || existingEntry?.aiInsights?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Insights",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = aiInsights.ifEmpty { existingEntry?.aiInsights ?: "" },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onSave(content, selectedPath?.id) },
                modifier = Modifier.fillMaxWidth(),
                enabled = content.isNotBlank()
            ) {
                Text(if (existingEntry != null) "Update Entry" else "Save Entry")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Entry?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
