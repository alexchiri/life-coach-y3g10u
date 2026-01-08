package com.kroslabs.lifecoach.ui.screens.profile

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class DeepDiveQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val multiSelect: Boolean = false
)

val deepDiveQuestions = listOf(
    DeepDiveQuestion(
        id = "core_values",
        question = "Which values are most important to you in life?",
        options = listOf("Freedom", "Security", "Growth", "Connection", "Achievement", "Creativity", "Service", "Balance"),
        multiSelect = true
    ),
    DeepDiveQuestion(
        id = "energy_sources",
        question = "What activities give you the most energy?",
        options = listOf("Creating something new", "Solving complex problems", "Helping others", "Learning new skills", "Leading teams", "Working independently", "Building relationships", "Physical activities"),
        multiSelect = true
    ),
    DeepDiveQuestion(
        id = "ideal_day",
        question = "What does your ideal workday look like?",
        options = listOf("Structured with clear tasks", "Flexible and varied", "Collaborative with others", "Deep focused work alone", "Mix of meetings and solo time", "Outdoors or active", "Remote from anywhere", "In a bustling environment")
    ),
    DeepDiveQuestion(
        id = "past_fulfillment",
        question = "When have you felt most fulfilled in the past?",
        options = listOf("Completing a challenging project", "Mentoring or teaching others", "Creative expression", "Building something from scratch", "Making a difference in community", "Learning something new", "Achieving recognition", "Deep connection with others"),
        multiSelect = true
    ),
    DeepDiveQuestion(
        id = "fear_avoidance",
        question = "What fears might be holding you back?",
        options = listOf("Fear of failure", "Fear of success", "Fear of judgment", "Fear of change", "Financial insecurity", "Disappointing others", "Wasting time", "Missing out"),
        multiSelect = true
    ),
    DeepDiveQuestion(
        id = "dream_impact",
        question = "What impact do you want to have on the world?",
        options = listOf("Inspire others", "Create lasting innovation", "Support and heal", "Educate and inform", "Build community", "Protect the environment", "Advance knowledge", "Create beauty")
    ),
    DeepDiveQuestion(
        id = "skills_develop",
        question = "What skills would you love to develop?",
        options = listOf("Leadership", "Communication", "Technical/coding", "Creative arts", "Business/entrepreneurship", "Teaching", "Writing", "Public speaking"),
        multiSelect = true
    ),
    DeepDiveQuestion(
        id = "life_balance",
        question = "What areas of life need more attention?",
        options = listOf("Career/purpose", "Relationships", "Health/fitness", "Financial security", "Personal growth", "Fun/recreation", "Spirituality", "Contribution/service"),
        multiSelect = true
    ),
    DeepDiveQuestion(
        id = "time_horizon",
        question = "What's your preferred time horizon for goals?",
        options = listOf("Quick wins (weeks)", "Medium term (months)", "Long term (years)", "Legacy (decades)")
    ),
    DeepDiveQuestion(
        id = "risk_tolerance",
        question = "How do you feel about taking risks?",
        options = listOf("Love taking bold risks", "Comfortable with calculated risks", "Prefer safer paths", "Risk-averse, security first")
    ),
    DeepDiveQuestion(
        id = "work_style",
        question = "What's your natural work style?",
        options = listOf("Early bird, morning person", "Night owl, creative at night", "Burst of intense focus", "Steady consistent pace", "Deadline-driven", "Self-paced and flexible")
    ),
    DeepDiveQuestion(
        id = "success_definition",
        question = "How do you define success?",
        options = listOf("Financial abundance", "Freedom and flexibility", "Recognition and status", "Making a difference", "Mastery and expertise", "Strong relationships", "Inner peace", "Adventures and experiences"),
        multiSelect = true
    ),
    DeepDiveQuestion(
        id = "obstacles",
        question = "What obstacles do you typically face?",
        options = listOf("Lack of time", "Lack of clarity", "Lack of confidence", "Too many options", "External responsibilities", "Perfectionism", "Procrastination", "Resource constraints"),
        multiSelect = true
    ),
    DeepDiveQuestion(
        id = "support_needs",
        question = "What kind of support helps you most?",
        options = listOf("Accountability partner", "Expert guidance", "Emotional encouragement", "Practical resources", "Structure and deadlines", "Freedom and space", "Feedback and critique", "Community and peers")
    ),
    DeepDiveQuestion(
        id = "legacy",
        question = "What would you want to be remembered for?",
        options = listOf("Innovation and creativity", "Kindness and compassion", "Wisdom and knowledge", "Leadership and inspiration", "Building something lasting", "Raising great family", "Community contribution", "Artistic expression")
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepDiveScreen(
    isLoading: Boolean,
    onBack: () -> Unit,
    onComplete: (Map<String, List<String>>) -> Unit
) {
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateMapOf<String, List<String>>() }

    val currentQuestion = deepDiveQuestions[currentQuestionIndex]
    val progress = (currentQuestionIndex + 1).toFloat() / deepDiveQuestions.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extended Values Exploration") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentQuestionIndex > 0) {
                            currentQuestionIndex--
                        } else {
                            onBack()
                        }
                    }) {
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
                .padding(16.dp)
        ) {
            // Progress
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Question ${currentQuestionIndex + 1} of ${deepDiveQuestions.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Question
            AnimatedContent(
                targetState = currentQuestionIndex,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                },
                label = "question"
            ) { index ->
                val question = deepDiveQuestions[index]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
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
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val selectedOptions = answers[question.id] ?: emptyList()

                    if (question.multiSelect) {
                        // Checkbox options
                        question.options.forEach { option ->
                            val isSelected = option in selectedOptions

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                ),
                                onClick = {
                                    val newList = if (isSelected) {
                                        selectedOptions - option
                                    } else {
                                        selectedOptions + option
                                    }
                                    answers[question.id] = newList
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    } else {
                        // Radio options
                        Column(modifier = Modifier.selectableGroup()) {
                            question.options.forEach { option ->
                                val isSelected = option in selectedOptions

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .selectable(
                                            selected = isSelected,
                                            onClick = { answers[question.id] = listOf(option) },
                                            role = Role.RadioButton
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = null
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = option,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentQuestionIndex > 0) {
                    OutlinedButton(
                        onClick = { currentQuestionIndex-- },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Previous")
                    }
                }

                val hasAnswer = answers[currentQuestion.id]?.isNotEmpty() == true
                val isLastQuestion = currentQuestionIndex == deepDiveQuestions.size - 1

                Button(
                    onClick = {
                        if (isLastQuestion) {
                            onComplete(answers.toMap())
                        } else {
                            currentQuestionIndex++
                        }
                    },
                    enabled = hasAnswer && !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else if (isLastQuestion) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Complete")
                    } else {
                        Text("Next")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}
