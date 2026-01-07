package com.kroslabs.lifecoach.ui.screens.experiments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kroslabs.lifecoach.data.model.Experiment
import com.kroslabs.lifecoach.data.model.ExperimentStatus
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentsScreen(
    experiments: List<Experiment>,
    onExperimentClick: (Long) -> Unit,
    onCheckIn: (Long) -> Unit,
    onCreateExperiment: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Experiments") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateExperiment) {
                Icon(Icons.Default.Add, contentDescription = "New Experiment")
            }
        }
    ) { padding ->
        if (experiments.isEmpty()) {
            EmptyExperiments(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val activeExperiments = experiments.filter { it.status == ExperimentStatus.ACTIVE }
                val completedExperiments = experiments.filter { it.status == ExperimentStatus.COMPLETED }

                if (activeExperiments.isNotEmpty()) {
                    item {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(activeExperiments) { experiment ->
                        ExperimentCard(
                            experiment = experiment,
                            onClick = { onExperimentClick(experiment.id) },
                            onCheckIn = { onCheckIn(experiment.id) }
                        )
                    }
                }

                if (completedExperiments.isNotEmpty()) {
                    item {
                        Text(
                            text = "Completed",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }

                    items(completedExperiments) { experiment ->
                        ExperimentCard(
                            experiment = experiment,
                            onClick = { onExperimentClick(experiment.id) },
                            onCheckIn = null
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyExperiments(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Science,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Experiments Yet",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start an experiment to explore a potential life path",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExperimentCard(
    experiment: Experiment,
    onClick: () -> Unit,
    onCheckIn: (() -> Unit)?
) {
    val daysRemaining = TimeUnit.MILLISECONDS.toDays(
        experiment.endDate - System.currentTimeMillis()
    ).toInt().coerceAtLeast(0)

    val isCompleted = experiment.status == ExperimentStatus.COMPLETED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = experiment.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$daysRemaining days left",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = experiment.purpose,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { experiment.progress / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                if (!isCompleted && onCheckIn != null) {
                    Spacer(modifier = Modifier.width(16.dp))
                    FilledTonalButton(
                        onClick = onCheckIn,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Check In")
                    }
                }
            }
        }
    }
}
