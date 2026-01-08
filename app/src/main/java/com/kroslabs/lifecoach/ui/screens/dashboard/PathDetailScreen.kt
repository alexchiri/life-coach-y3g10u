package com.kroslabs.lifecoach.ui.screens.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kroslabs.lifecoach.data.model.Experiment
import com.kroslabs.lifecoach.data.model.ExperimentStatus
import com.kroslabs.lifecoach.data.model.LifePath
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathDetailScreen(
    path: LifePath?,
    experiments: List<Experiment>,
    pathInsights: String,
    isGenerating: Boolean,
    onBack: () -> Unit,
    onExperimentClick: (Long) -> Unit,
    onCreateExperiment: () -> Unit,
    onGenerateInsights: () -> Unit,
    onArchivePath: () -> Unit,
    onUpdateViability: (Float) -> Unit
) {
    var showArchiveDialog by remember { mutableStateOf(false) }
    var viabilitySlider by remember { mutableFloatStateOf(path?.viabilityScore ?: 50f) }

    LaunchedEffect(path) {
        path?.let { viabilitySlider = it.viabilityScore }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(path?.name ?: "Path Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showArchiveDialog = true }) {
                        Icon(Icons.Default.Archive, contentDescription = "Archive")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateExperiment,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Experiment") }
            )
        }
    ) { padding ->
        if (path == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Path Info Card
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = path.description,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Why this path?",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = path.aiRationale.ifEmpty { "Based on your initial questionnaire responses." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Viability Score Card
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Viability Score",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${viabilitySlider.toInt()}%",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Slider(
                                value = viabilitySlider,
                                onValueChange = { viabilitySlider = it },
                                onValueChangeFinished = { onUpdateViability(viabilitySlider) },
                                valueRange = 0f..100f
                            )

                            Text(
                                text = "Adjust based on how well this path resonates with you",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // AI Insights Card
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AI Insights",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (!isGenerating) {
                                    IconButton(onClick = onGenerateInsights) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = "Generate insights",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            if (isGenerating) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Analyzing path progress...")
                                }
                            } else if (pathInsights.isNotEmpty()) {
                                Text(
                                    text = pathInsights,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                Text(
                                    text = "Tap the sparkle icon to get AI-powered insights about this path",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Experiments Section
                item {
                    Text(
                        text = "Experiments (${experiments.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (experiments.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Science,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No experiments yet",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Start exploring this path with an experiment",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(experiments) { experiment ->
                        PathExperimentCard(
                            experiment = experiment,
                            onClick = { onExperimentClick(experiment.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = { Text("Archive Path?") },
            text = {
                Text("This will hide the path from your dashboard. You can restore it later from settings.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onArchivePath()
                        showArchiveDialog = false
                        onBack()
                    }
                ) {
                    Text("Archive")
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PathExperimentCard(
    experiment: Experiment,
    onClick: () -> Unit
) {
    val daysRemaining = TimeUnit.MILLISECONDS.toDays(
        experiment.endDate - System.currentTimeMillis()
    ).toInt().coerceAtLeast(0)

    val isCompleted = experiment.status == ExperimentStatus.COMPLETED
    val statusColor = when (experiment.status) {
        ExperimentStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        ExperimentStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
        ExperimentStatus.PAUSED -> MaterialTheme.colorScheme.secondary
        ExperimentStatus.ARCHIVED -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = experiment.title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                AssistChip(
                    onClick = {},
                    label = { Text(experiment.status.name) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = statusColor.copy(alpha = 0.1f),
                        labelColor = statusColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isCompleted) "Completed" else "$daysRemaining days left",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${experiment.progress.toInt()}% progress",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { experiment.progress / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
