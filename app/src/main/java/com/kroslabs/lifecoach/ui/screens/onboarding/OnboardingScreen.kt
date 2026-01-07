package com.kroslabs.lifecoach.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class OnboardingQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val multiSelect: Boolean = false
)

val onboardingQuestions = listOf(
    OnboardingQuestion(
        id = "core_values",
        question = "What values are most important to you?",
        options = listOf("Growth", "Freedom", "Security", "Connection", "Creativity", "Achievement", "Balance", "Impact"),
        multiSelect = true
    ),
    OnboardingQuestion(
        id = "life_areas",
        question = "Which area of life do you want to explore?",
        options = listOf("Career", "Relationships", "Health", "Personal Growth", "Finances", "Creativity", "Spirituality"),
        multiSelect = true
    ),
    OnboardingQuestion(
        id = "current_feeling",
        question = "How do you feel about your current direction?",
        options = listOf("Stuck and unsure", "Curious but cautious", "Ready for change", "Exploring options")
    ),
    OnboardingQuestion(
        id = "time_availability",
        question = "How much time can you dedicate to experiments?",
        options = listOf("15 min/day", "30 min/day", "1 hour/day", "Flexible")
    ),
    OnboardingQuestion(
        id = "experiment_style",
        question = "How do you prefer to try new things?",
        options = listOf("Small daily habits", "Weekly projects", "Intensive deep dives", "Mix of approaches")
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: (Map<String, List<String>>) -> Unit,
    isLoading: Boolean = false
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateMapOf<String, List<String>>() }
    val scrollState = rememberScrollState()

    val isWelcomeScreen = currentStep == 0
    val questionIndex = currentStep - 1
    val isComplete = currentStep > onboardingQuestions.size

    Scaffold(
        topBar = {
            if (!isWelcomeScreen && !isComplete) {
                TopAppBar(
                    title = { Text("Getting to Know You") },
                    actions = {
                        Text(
                            text = "${questionIndex + 1}/${onboardingQuestions.size}",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                )
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = currentStep,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            label = "onboarding"
        ) { step ->
            when {
                step == 0 -> WelcomeContent(onContinue = { currentStep = 1 })
                step <= onboardingQuestions.size -> {
                    val question = onboardingQuestions[step - 1]
                    QuestionContent(
                        question = question,
                        selectedAnswers = answers[question.id] ?: emptyList(),
                        onAnswerSelected = { selected ->
                            answers[question.id] = selected
                        },
                        onContinue = {
                            if (step < onboardingQuestions.size) {
                                currentStep = step + 1
                            } else {
                                currentStep = onboardingQuestions.size + 1
                            }
                        },
                        canContinue = !answers[question.id].isNullOrEmpty()
                    )
                }
                else -> CompletionContent(
                    isLoading = isLoading,
                    onComplete = { onComplete(answers.toMap()) }
                )
            }
        }
    }
}

@Composable
private fun WelcomeContent(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lightbulb,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to Life Coach",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Discover your path through small experiments",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "The PACT Framework",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                PactItem("Purposeful", "Aligned with your values")
                PactItem("Actionable", "Clear, specific steps")
                PactItem("Continuous", "Sustained over time")
                PactItem("Trackable", "Easy to measure progress")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Started")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun PactItem(title: String, description: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.first().toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuestionContent(
    question: OnboardingQuestion,
    selectedAnswers: List<String>,
    onAnswerSelected: (List<String>) -> Unit,
    onContinue: () -> Unit,
    canContinue: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = question.question,
            style = MaterialTheme.typography.headlineSmall
        )

        if (question.multiSelect) {
            Text(
                text = "Select all that apply",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        question.options.forEach { option ->
            val isSelected = option in selectedAnswers

            FilterChip(
                selected = isSelected,
                onClick = {
                    onAnswerSelected(
                        if (question.multiSelect) {
                            if (isSelected) selectedAnswers - option
                            else selectedAnswers + option
                        } else {
                            listOf(option)
                        }
                    )
                },
                label = { Text(option) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onContinue,
            enabled = canContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun CompletionContent(
    isLoading: Boolean,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Analyzing your responses...",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Claude is discovering potential life paths for you",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        } else {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "You're All Set!",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Let's discover your potential paths and start experimenting",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Discover My Paths")
            }
        }
    }
}
