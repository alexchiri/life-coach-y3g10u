package com.kroslabs.lifecoach.ui.screens.experiments

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
import androidx.compose.ui.unit.dp
import com.kroslabs.lifecoach.data.model.CheckIn
import com.kroslabs.lifecoach.data.model.Experiment
import com.kroslabs.lifecoach.data.model.ExperimentStatus
import com.kroslabs.lifecoach.data.model.TrackingMethod
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentDetailScreen(
    experiment: Experiment?,
    checkIns: List<CheckIn>,
    onBack: () -> Unit,
    onCheckIn: () -> Unit,
    onComplete: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showArchiveDialog by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(experiment?.title ?: "Experiment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showStatusMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showStatusMenu,
                            onDismissRequest = { showStatusMenu = false }
                        ) {
                            if (experiment?.status == ExperimentStatus.ACTIVE) {
                                DropdownMenuItem(
                                    text = { Text("Mark Complete") },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, null) },
                                    onClick = {
                                        showStatusMenu = false
                                        onComplete()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Pause") },
                                    leadingIcon = { Icon(Icons.Default.Pause, null) },
                                    onClick = {
                                        showStatusMenu = false
                                        onPause()
                                    }
                                )
                            }
                            if (experiment?.status == ExperimentStatus.PAUSED) {
                                DropdownMenuItem(
                                    text = { Text("Resume") },
                                    leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                                    onClick = {
                                        showStatusMenu = false
                                        onResume()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Archive") },
                                leadingIcon = { Icon(Icons.Default.Archive, null) },
                                onClick = {
                                    showStatusMenu = false
                                    showArchiveDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showStatusMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (experiment?.status == ExperimentStatus.ACTIVE) {
                ExtendedFloatingActionButton(
                    onClick = onCheckIn,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Check In") }
                )
            }
        }
    ) { padding ->
        if (experiment == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val daysRemaining = TimeUnit.MILLISECONDS.toDays(
                experiment.endDate - System.currentTimeMillis()
            ).toInt().coerceAtLeast(0)
            val daysElapsed = experiment.durationDays - daysRemaining

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when (experiment.status) {
                                ExperimentStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                                ExperimentStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
                                ExperimentStatus.PAUSED -> MaterialTheme.colorScheme.secondaryContainer
                                ExperimentStatus.ARCHIVED -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = experiment.status.name,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = if (experiment.status == ExperimentStatus.COMPLETED) {
                                            "Completed"
                                        } else {
                                            "$daysRemaining days remaining"
                                        },
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                }
                                CircularProgressIndicator(
                                    progress = { experiment.progress / 100f },
                                    modifier = Modifier.size(64.dp),
                                    strokeWidth = 8.dp,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            LinearProgressIndicator(
                                progress = { daysElapsed.toFloat() / experiment.durationDays },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                trackColor = MaterialTheme.colorScheme.surface
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Day $daysElapsed",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "of ${experiment.durationDays}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // PACT Breakdown
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "PACT Framework",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            PactItem(
                                letter = "P",
                                title = "Purposeful",
                                content = experiment.purpose
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            PactItem(
                                letter = "A",
                                title = "Actionable",
                                content = experiment.actionSteps
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            PactItem(
                                letter = "C",
                                title = "Continuous",
                                content = "${experiment.durationDays} days"
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            PactItem(
                                letter = "T",
                                title = "Trackable",
                                content = when (experiment.trackingMethod) {
                                    TrackingMethod.DAILY_CHECKIN -> "Daily check-ins"
                                    TrackingMethod.MILESTONE_COMPLETION -> "Milestone completion"
                                    TrackingMethod.WEEKLY_REVIEW -> "Weekly reviews"
                                }
                            )
                        }
                    }
                }

                // Check-in History
                item {
                    Text(
                        text = "Check-in History (${checkIns.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (checkIns.isEmpty()) {
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
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No check-ins yet",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Record your first check-in to track progress",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(checkIns) { checkIn ->
                        CheckInCard(checkIn = checkIn)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Experiment?") },
            text = {
                Text("This will permanently delete this experiment and all its check-ins. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                        onBack()
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

    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = { Text("Archive Experiment?") },
            text = {
                Text("This will move the experiment to your archive. You can restore it later.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onArchive()
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
private fun PactItem(
    letter: String,
    title: String,
    content: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = letter,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun CheckInCard(checkIn: CheckIn) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(checkIn.createdAt))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AssistChip(
                    onClick = {},
                    label = { Text("${checkIn.progressValue.toInt()}%") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            if (checkIn.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = checkIn.notes,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
