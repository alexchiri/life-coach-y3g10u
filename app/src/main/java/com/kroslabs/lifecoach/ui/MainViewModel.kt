package com.kroslabs.lifecoach.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kroslabs.lifecoach.data.database.LifeCoachDatabase
import com.kroslabs.lifecoach.data.model.*
import com.kroslabs.lifecoach.data.repository.LifeCoachRepository
import com.kroslabs.lifecoach.network.ClaudeMessage
import com.kroslabs.lifecoach.network.ClaudeService
import com.kroslabs.lifecoach.security.SecurityManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val securityManager = SecurityManager(application)

    private var repository: LifeCoachRepository? = null
    private var claudeService: ClaudeService? = null

    // Auth State
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    val isAuthSetupComplete = securityManager.isAuthSetupComplete
    val isBiometricEnabled = securityManager.isBiometricEnabled
    val isApiKeySet = securityManager.isApiKeySet
    fun canUseBiometric() = securityManager.canUseBiometric()

    // UI State
    private val _paths = MutableStateFlow<List<LifePath>>(emptyList())
    val paths: StateFlow<List<LifePath>> = _paths.asStateFlow()

    private val _experiments = MutableStateFlow<List<Experiment>>(emptyList())
    val experiments: StateFlow<List<Experiment>> = _experiments.asStateFlow()

    private val _journalEntries = MutableStateFlow<List<JournalEntry>>(emptyList())
    val journalEntries: StateFlow<List<JournalEntry>> = _journalEntries.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _totalTokensUsed = MutableStateFlow(0)
    val totalTokensUsed: StateFlow<Int> = _totalTokensUsed.asStateFlow()

    private val _totalCostCents = MutableStateFlow(0f)
    val totalCostCents: StateFlow<Float> = _totalCostCents.asStateFlow()

    // AI Generation State
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generatedText = MutableStateFlow("")
    val generatedText: StateFlow<String> = _generatedText.asStateFlow()

    private val _aiInsights = MutableStateFlow("")
    val aiInsights: StateFlow<String> = _aiInsights.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            securityManager.isAuthSetupComplete.collect { isSetup ->
                _authState.value = if (isSetup) AuthState.NeedsUnlock else AuthState.NeedsSetup
            }
        }
    }

    fun setupPin(pin: String, enableBiometric: Boolean) {
        viewModelScope.launch {
            securityManager.setupPin(pin)
            securityManager.enableBiometric(enableBiometric)
            initializeDatabase()
            _isAuthenticated.value = true
        }
    }

    fun verifyPin(pin: String) {
        viewModelScope.launch {
            if (securityManager.verifyPin(pin)) {
                initializeDatabase()
                _isAuthenticated.value = true
                _errorMessage.value = null
            } else {
                _errorMessage.value = "Invalid PIN"
            }
        }
    }

    private suspend fun initializeDatabase() {
        val passphrase = securityManager.getDatabasePassphrase()
        val db = LifeCoachDatabase.getDatabase(getApplication(), passphrase)
        repository = LifeCoachRepository(db.dao())

        // Load and initialize Claude API key if available
        val apiKey = securityManager.getClaudeApiKey()
        if (apiKey != null) {
            claudeService = ClaudeService(apiKey)
        }

        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository?.let { repo ->
                launch {
                    repo.getActivePaths().collect { _paths.value = it }
                }
                launch {
                    repo.getActiveExperiments().collect { _experiments.value = it }
                }
                launch {
                    repo.getAllJournalEntries().collect { _journalEntries.value = it }
                }
                launch {
                    repo.getUserProfile().collect { _userProfile.value = it }
                }
                launch {
                    _totalTokensUsed.value = repo.getTotalTokensUsed()
                }
                launch {
                    val monthStart = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                    _totalCostCents.value = repo.getTotalCostSince(monthStart)
                }
            }
        }
    }

    fun setApiKey(apiKey: String) {
        claudeService = ClaudeService(apiKey)
    }

    fun saveClaudeApiKey(apiKey: String) {
        viewModelScope.launch {
            securityManager.setClaudeApiKey(apiKey)
            claudeService = ClaudeService(apiKey)
        }
    }

    fun completeOnboarding(answers: Map<String, List<String>>) {
        viewModelScope.launch {
            _isGenerating.value = true
            val profile = UserProfile(
                valuesResponses = answers.toString(),
                onboardingCompleted = true
            )
            repository?.insertUserProfile(profile)

            // Generate initial paths using Claude
            generateInitialPaths(answers)
        }
    }

    private suspend fun generateInitialPaths(answers: Map<String, List<String>>) {
        val prompt = buildString {
            appendLine("Based on the following questionnaire responses, suggest 3-5 potential life paths for exploration:")
            answers.forEach { (key, values) ->
                appendLine("$key: ${values.joinToString(", ")}")
            }
            appendLine()
            appendLine("For each path, provide:")
            appendLine("1. A clear, inspiring name")
            appendLine("2. A 2-3 sentence description")
            appendLine("3. Why this might resonate with the person")
            appendLine()
            appendLine("Format each path as:")
            appendLine("PATH: [name]")
            appendLine("DESCRIPTION: [description]")
            appendLine("RATIONALE: [why this fits]")
        }

        claudeService?.let { service ->
            val result = service.sendMessage(
                listOf(ClaudeMessage("user", prompt))
            )

            result.onSuccess { response ->
                // Parse paths from response
                val pathRegex = Regex("PATH:\\s*(.+?)\\nDESCRIPTION:\\s*(.+?)\\nRATIONALE:\\s*(.+?)(?=\\nPATH:|$)", RegexOption.DOT_MATCHES_ALL)
                val matches = pathRegex.findAll(response.text)

                matches.forEach { match ->
                    val (name, description, rationale) = match.destructured
                    repository?.insertPath(
                        LifePath(
                            name = name.trim(),
                            description = description.trim(),
                            aiRationale = rationale.trim(),
                            viabilityScore = 50f
                        )
                    )
                }

                // Log API usage
                repository?.insertApiUsage(
                    ApiUsage(
                        inputTokens = response.inputTokens,
                        outputTokens = response.outputTokens,
                        estimatedCostCents = service.calculateCost(response.inputTokens, response.outputTokens),
                        operationType = "path_discovery"
                    )
                )
            }
        }

        _isGenerating.value = false
        loadData()
    }

    fun generateExperiment(pathId: Long?) {
        viewModelScope.launch {
            _isGenerating.value = true
            _generatedText.value = ""

            val path = pathId?.let { repository?.getPathById(it) }
            val prompt = buildString {
                appendLine("Generate a PACT-compliant experiment for life exploration.")
                if (path != null) {
                    appendLine("This experiment is for the life path: ${path.name}")
                    appendLine("Path description: ${path.description}")
                }
                appendLine()
                appendLine("The experiment should be:")
                appendLine("- Purposeful: Aligned with personal values and curiosity")
                appendLine("- Actionable: Clear, specific steps that can be taken immediately")
                appendLine("- Continuous: Sustained over 1-4 weeks")
                appendLine("- Trackable: Easy to monitor progress")
                appendLine()
                appendLine("Provide the response in this exact format:")
                appendLine("Title: [short experiment title]")
                appendLine("Purpose: [why this experiment matters]")
                appendLine("Steps: [specific daily actions to take]")
                appendLine("Duration: [number] days")
            }

            claudeService?.let { service ->
                try {
                    service.streamMessage(
                        listOf(ClaudeMessage("user", prompt))
                    ).collect { chunk ->
                        _generatedText.value += chunk
                    }
                } catch (e: Exception) {
                    _errorMessage.value = "Failed to generate: ${e.message}"
                }
            }

            _isGenerating.value = false
        }
    }

    fun saveExperiment(
        title: String,
        purpose: String,
        steps: String,
        days: Int,
        method: TrackingMethod,
        pathId: Long?
    ) {
        viewModelScope.launch {
            val experiment = Experiment(
                pathId = pathId ?: 0,
                title = title,
                purpose = purpose,
                actionSteps = steps,
                durationDays = days,
                trackingMethod = method
            )
            repository?.insertExperiment(experiment)
            loadData()
        }
    }

    fun checkInExperiment(experimentId: Long, progress: Float, notes: String) {
        viewModelScope.launch {
            repository?.insertCheckIn(
                CheckIn(
                    experimentId = experimentId,
                    progressValue = progress,
                    notes = notes
                )
            )

            // Update experiment progress
            repository?.getExperimentById(experimentId)?.let { experiment ->
                val newProgress = ((experiment.progress + progress) / 2).coerceIn(0f, 100f)
                repository?.updateExperiment(experiment.copy(progress = newProgress, updatedAt = System.currentTimeMillis()))
            }

            loadData()
        }
    }

    fun saveJournalEntry(content: String, pathId: Long?, entryId: Long? = null) {
        viewModelScope.launch {
            if (entryId != null) {
                repository?.getJournalEntryById(entryId)?.let { entry ->
                    repository?.updateJournalEntry(
                        entry.copy(
                            content = content,
                            pathId = pathId,
                            aiInsights = _aiInsights.value,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            } else {
                repository?.insertJournalEntry(
                    JournalEntry(
                        content = content,
                        pathId = pathId,
                        aiInsights = _aiInsights.value
                    )
                )
            }
            _aiInsights.value = ""
            loadData()
        }
    }

    fun analyzeJournalEntry(content: String) {
        viewModelScope.launch {
            _isGenerating.value = true
            _aiInsights.value = ""

            val prompt = buildString {
                appendLine("Analyze this journal entry and provide brief, insightful observations:")
                appendLine()
                appendLine(content)
                appendLine()
                appendLine("Focus on:")
                appendLine("- Patterns in emotions or thoughts")
                appendLine("- Signs of what energizes vs drains")
                appendLine("- Potential connections to life goals")
                appendLine()
                appendLine("Keep the response to 2-3 sentences, warm and encouraging.")
            }

            claudeService?.let { service ->
                val result = service.sendMessage(
                    listOf(ClaudeMessage("user", prompt))
                )

                result.onSuccess { response ->
                    _aiInsights.value = response.text

                    repository?.insertApiUsage(
                        ApiUsage(
                            inputTokens = response.inputTokens,
                            outputTokens = response.outputTokens,
                            estimatedCostCents = service.calculateCost(response.inputTokens, response.outputTokens),
                            operationType = "reflection_analysis"
                        )
                    )
                }

                result.onFailure { e ->
                    _errorMessage.value = "Analysis failed: ${e.message}"
                }
            }

            _isGenerating.value = false
        }
    }

    fun deleteJournalEntry(entryId: Long) {
        viewModelScope.launch {
            repository?.getJournalEntryById(entryId)?.let {
                repository?.deleteJournalEntry(it)
            }
            loadData()
        }
    }

    fun updateTheme(themeMode: ThemeMode) {
        viewModelScope.launch {
            _userProfile.value?.let { profile ->
                repository?.updateUserProfile(profile.copy(themeMode = themeMode))
            }
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            securityManager.enableBiometric(enabled)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository?.clearAllData()
            loadData()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun logout() {
        _isAuthenticated.value = false
        LifeCoachDatabase.closeDatabase()
        repository = null
    }
}

sealed class AuthState {
    data object Loading : AuthState()
    data object NeedsSetup : AuthState()
    data object NeedsUnlock : AuthState()
}
