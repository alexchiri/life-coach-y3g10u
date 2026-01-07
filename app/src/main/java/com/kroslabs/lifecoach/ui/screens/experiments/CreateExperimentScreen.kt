package com.kroslabs.lifecoach.ui.screens.experiments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kroslabs.lifecoach.data.model.LifePath
import com.kroslabs.lifecoach.data.model.TrackingMethod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExperimentScreen(
    paths: List<LifePath>,
    selectedPathId: Long?,
    isGenerating: Boolean,
    generatedText: String,
    onBack: () -> Unit,
    onGenerate: (Long?) -> Unit,
    onSave: (title: String, purpose: String, steps: String, days: Int, method: TrackingMethod, pathId: Long?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }
    var duration by remember { mutableIntStateOf(7) }
    var trackingMethod by remember { mutableStateOf(TrackingMethod.DAILY_CHECKIN) }
    var expandedPath by remember { mutableStateOf(false) }
    var selectedPath by remember(selectedPathId) { mutableStateOf(paths.find { it.id == selectedPathId }) }

    LaunchedEffect(generatedText) {
        if (generatedText.isNotEmpty()) {
            // Parse AI-generated experiment
            val lines = generatedText.lines()
            lines.forEach { line ->
                when {
                    line.startsWith("Title:") -> title = line.removePrefix("Title:").trim()
                    line.startsWith("Purpose:") -> purpose = line.removePrefix("Purpose:").trim()
                    line.startsWith("Steps:") -> steps = line.removePrefix("Steps:").trim()
                    line.startsWith("Duration:") -> {
                        val daysMatch = Regex("(\\d+)").find(line)
                        daysMatch?.value?.toIntOrNull()?.let { duration = it }
                    }
                }
            }
            if (title.isEmpty() && purpose.isEmpty()) {
                purpose = generatedText
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Experiment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Path Selection
            ExposedDropdownMenuBox(
                expanded = expandedPath,
                onExpandedChange = { expandedPath = it }
            ) {
                OutlinedTextField(
                    value = selectedPath?.name ?: "No path selected",
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

            // AI Generate Button
            OutlinedButton(
                onClick = { onGenerate(selectedPath?.id) },
                enabled = !isGenerating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate with AI")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Experiment Title") },
                placeholder = { Text("e.g., Morning meditation practice") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Purpose (Purposeful)
            OutlinedTextField(
                value = purpose,
                onValueChange = { purpose = it },
                label = { Text("Purpose (Why this matters)") },
                placeholder = { Text("What do you hope to learn or discover?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Steps (Actionable)
            OutlinedTextField(
                value = steps,
                onValueChange = { steps = it },
                label = { Text("Action Steps") },
                placeholder = { Text("Specific actions you'll take each day") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Duration (Continuous)
            Text(
                text = "Duration: $duration days",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = duration.toFloat(),
                onValueChange = { duration = it.toInt() },
                valueRange = 3f..30f,
                steps = 26,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tracking Method (Trackable)
            Text(
                text = "Tracking Method",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            TrackingMethod.entries.forEach { method ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = trackingMethod == method,
                        onClick = { trackingMethod = method }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (method) {
                            TrackingMethod.DAILY_CHECKIN -> "Daily check-ins"
                            TrackingMethod.MILESTONE_COMPLETION -> "Milestone completion"
                            TrackingMethod.WEEKLY_REVIEW -> "Weekly review"
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    onSave(title, purpose, steps, duration, trackingMethod, selectedPath?.id)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && purpose.isNotBlank()
            ) {
                Text("Start Experiment")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
