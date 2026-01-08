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
import com.kroslabs.lifecoach.util.DebugLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

    // Detail screen state
    private val _selectedPath = MutableStateFlow<LifePath?>(null)
    val selectedPath: StateFlow<LifePath?> = _selectedPath.asStateFlow()

    private val _selectedExperiment = MutableStateFlow<Experiment?>(null)
    val selectedExperiment: StateFlow<Experiment?> = _selectedExperiment.asStateFlow()

    private val _pathExperiments = MutableStateFlow<List<Experiment>>(emptyList())
    val pathExperiments: StateFlow<List<Experiment>> = _pathExperiments.asStateFlow()

    private val _experimentCheckIns = MutableStateFlow<List<CheckIn>>(emptyList())
    val experimentCheckIns: StateFlow<List<CheckIn>> = _experimentCheckIns.asStateFlow()

    // Analytics state
    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    private val _completionRate = MutableStateFlow(0f)
    val completionRate: StateFlow<Float> = _completionRate.asStateFlow()

    private val _weeklyInsights = MutableStateFlow("")
    val weeklyInsights: StateFlow<String> = _weeklyInsights.asStateFlow()

    private val _pathInsights = MutableStateFlow("")
    val pathInsights: StateFlow<String> = _pathInsights.asStateFlow()

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
            DebugLogger.d("Auth", "verifyPin called")
            if (securityManager.verifyPin(pin)) {
                DebugLogger.i("Auth", "PIN verified successfully")
                initializeDatabase()
                _isAuthenticated.value = true
                _errorMessage.value = null
            } else {
                DebugLogger.w("Auth", "PIN verification failed")
                _errorMessage.value = "Invalid PIN"
            }
        }
    }

    fun biometricLogin() {
        viewModelScope.launch {
            DebugLogger.i("Auth", "Biometric authentication successful, initializing database")
            initializeDatabase()
            _isAuthenticated.value = true
            _errorMessage.value = null
        }
    }

    private suspend fun initializeDatabase() {
        DebugLogger.d("Database", "initializeDatabase called")
        try {
            val passphrase = securityManager.getDatabasePassphrase()
            DebugLogger.d("Database", "Got database passphrase (${passphrase.size} bytes)")
            val db = LifeCoachDatabase.getDatabase(getApplication(), passphrase)
            repository = LifeCoachRepository(db.dao())
            DebugLogger.i("Database", "Database initialized successfully")

            // Load and initialize Claude API key if available
            val apiKey = securityManager.getClaudeApiKey()
            if (apiKey != null) {
                DebugLogger.i("API", "Claude API key found, initializing service (key starts with: ${apiKey.take(10)}...)")
                claudeService = ClaudeService(apiKey)
            } else {
                DebugLogger.w("API", "No Claude API key configured - AI features will be unavailable")
            }

            loadData()
        } catch (e: Exception) {
            DebugLogger.e("Database", "Failed to initialize database", e)
        }
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
            DebugLogger.i("API", "saveClaudeApiKey called (key starts with: ${apiKey.take(10)}...)")
            try {
                securityManager.setClaudeApiKey(apiKey)
                DebugLogger.i("API", "API key saved to secure storage")
                claudeService = ClaudeService(apiKey)
                DebugLogger.i("API", "ClaudeService initialized with new API key")
            } catch (e: Exception) {
                DebugLogger.e("API", "Failed to save API key", e)
            }
        }
    }

    fun completeOnboarding(answers: Map<String, List<String>>) {
        viewModelScope.launch {
            DebugLogger.i("Onboarding", "completeOnboarding called with ${answers.size} answers")
            DebugLogger.d("Onboarding", "Answers: $answers")
            _isGenerating.value = true

            val profile = UserProfile(
                valuesResponses = answers.toString(),
                onboardingCompleted = true
            )

            DebugLogger.d("Onboarding", "Inserting user profile")
            repository?.insertUserProfile(profile)
            DebugLogger.i("Onboarding", "User profile saved, now generating initial paths")

            // Generate initial paths using Claude
            generateInitialPaths(answers)
        }
    }

    private suspend fun generateInitialPaths(answers: Map<String, List<String>>) {
        DebugLogger.i("PathGen", "generateInitialPaths called")

        if (claudeService == null) {
            DebugLogger.e("PathGen", "CRITICAL: claudeService is null! Cannot generate paths. API key may not be configured.")
            _isGenerating.value = false
            _errorMessage.value = "Cannot generate paths: API key not configured"
            loadData()
            return
        }

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

        DebugLogger.d("PathGen", "Prompt built (${prompt.length} chars)")
        DebugLogger.d("PathGen", "Sending request to Claude API...")

        claudeService?.let { service ->
            try {
                val result = service.sendMessage(
                    listOf(ClaudeMessage("user", prompt))
                )

                result.onSuccess { response ->
                    DebugLogger.i("PathGen", "API response received: ${response.inputTokens} input tokens, ${response.outputTokens} output tokens")
                    DebugLogger.d("PathGen", "Response text (first 500 chars): ${response.text.take(500)}")

                    // Parse paths from response
                    val pathRegex = Regex("PATH:\\s*(.+?)\\nDESCRIPTION:\\s*(.+?)\\nRATIONALE:\\s*(.+?)(?=\\nPATH:|$)", RegexOption.DOT_MATCHES_ALL)
                    val matches = pathRegex.findAll(response.text)
                    val matchList = matches.toList()

                    DebugLogger.i("PathGen", "Parsed ${matchList.size} paths from response")

                    if (matchList.isEmpty()) {
                        DebugLogger.w("PathGen", "No paths matched regex! Full response:\n${response.text}")
                    }

                    matchList.forEach { match ->
                        val (name, description, rationale) = match.destructured
                        DebugLogger.d("PathGen", "Inserting path: ${name.trim()}")
                        repository?.insertPath(
                            LifePath(
                                name = name.trim(),
                                description = description.trim(),
                                aiRationale = rationale.trim(),
                                viabilityScore = 50f
                            )
                        )
                    }

                    DebugLogger.i("PathGen", "All paths inserted successfully")

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

                result.onFailure { e ->
                    DebugLogger.e("PathGen", "API call failed", e)
                    _errorMessage.value = "Failed to generate paths: ${e.message}"
                }
            } catch (e: Exception) {
                DebugLogger.e("PathGen", "Exception during path generation", e)
                _errorMessage.value = "Failed to generate paths: ${e.message}"
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

    // Path Detail methods
    fun loadPathDetail(pathId: Long) {
        viewModelScope.launch {
            _selectedPath.value = repository?.getPathById(pathId)
            repository?.getExperimentsByPath(pathId)?.collect {
                _pathExperiments.value = it
            }
        }
    }

    fun generatePathInsights(pathId: Long) {
        viewModelScope.launch {
            _isGenerating.value = true
            _pathInsights.value = ""

            val path = repository?.getPathById(pathId) ?: return@launch
            val experiments = _pathExperiments.value
            val completedCount = experiments.count { it.status == ExperimentStatus.COMPLETED }

            val prompt = buildString {
                appendLine("Analyze this life path's progress and provide insights:")
                appendLine()
                appendLine("Path: ${path.name}")
                appendLine("Description: ${path.description}")
                appendLine("Current Viability Score: ${path.viabilityScore}")
                appendLine("Total Experiments: ${experiments.size}")
                appendLine("Completed Experiments: $completedCount")
                appendLine()
                appendLine("Provide 2-3 sentences of insights about:")
                appendLine("- How well this path is resonating based on experiment engagement")
                appendLine("- Suggestions for next steps or adjustments")
                appendLine("- Whether to continue exploring or consider pivoting")
            }

            claudeService?.let { service ->
                val result = service.sendMessage(listOf(ClaudeMessage("user", prompt)))
                result.onSuccess { response ->
                    _pathInsights.value = response.text
                    repository?.insertApiUsage(
                        ApiUsage(
                            inputTokens = response.inputTokens,
                            outputTokens = response.outputTokens,
                            estimatedCostCents = service.calculateCost(response.inputTokens, response.outputTokens),
                            operationType = "path_insights"
                        )
                    )
                }
            }
            _isGenerating.value = false
        }
    }

    fun updatePathViability(pathId: Long, newScore: Float) {
        viewModelScope.launch {
            repository?.getPathById(pathId)?.let { path ->
                repository?.updatePath(path.copy(viabilityScore = newScore, updatedAt = System.currentTimeMillis()))
                loadData()
            }
        }
    }

    fun archivePath(pathId: Long) {
        viewModelScope.launch {
            repository?.getPathById(pathId)?.let { path ->
                repository?.updatePath(path.copy(isActive = false, updatedAt = System.currentTimeMillis()))
                loadData()
            }
        }
    }

    // Experiment Detail methods
    fun loadExperimentDetail(experimentId: Long) {
        viewModelScope.launch {
            _selectedExperiment.value = repository?.getExperimentById(experimentId)
            repository?.getCheckInsForExperiment(experimentId)?.collect {
                _experimentCheckIns.value = it
            }
        }
    }

    fun updateExperimentStatus(experimentId: Long, status: ExperimentStatus) {
        viewModelScope.launch {
            repository?.getExperimentById(experimentId)?.let { experiment ->
                repository?.updateExperiment(
                    experiment.copy(
                        status = status,
                        progress = if (status == ExperimentStatus.COMPLETED) 100f else experiment.progress,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                loadExperimentDetail(experimentId)
                loadData()
            }
        }
    }

    fun deleteExperiment(experimentId: Long) {
        viewModelScope.launch {
            repository?.getExperimentById(experimentId)?.let {
                repository?.deleteExperiment(it)
                loadData()
            }
        }
    }

    // Analytics methods
    fun loadAnalytics() {
        viewModelScope.launch {
            // Calculate completion rate
            val total = repository?.getTotalExperiments() ?: 0
            val completed = repository?.getTotalCompletedExperiments() ?: 0
            _completionRate.value = if (total > 0) (completed.toFloat() / total) * 100 else 0f

            // Calculate streak
            calculateStreak()
        }
    }

    private suspend fun calculateStreak() {
        val dates = repository?.getCheckInDates() ?: emptyList()
        if (dates.isEmpty()) {
            _currentStreak.value = 0
            return
        }

        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        var streak = 0
        var currentDate = LocalDate.now()

        for (dateStr in dates) {
            try {
                val checkInDate = LocalDate.parse(dateStr, formatter)
                if (checkInDate == currentDate || checkInDate == currentDate.minusDays(1)) {
                    streak++
                    currentDate = checkInDate.minusDays(1)
                } else {
                    break
                }
            } catch (_: Exception) {
                break
            }
        }
        _currentStreak.value = streak
    }

    fun generateWeeklyReflection() {
        viewModelScope.launch {
            _isGenerating.value = true
            _weeklyInsights.value = ""

            val weekAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
            val recentEntries = repository?.getJournalEntriesSince(weekAgo) ?: emptyList()
            val recentCheckIns = repository?.getCheckInsSince(weekAgo) ?: emptyList()

            if (recentEntries.isEmpty() && recentCheckIns.isEmpty()) {
                _weeklyInsights.value = "Not enough data for weekly insights. Keep journaling and checking in on your experiments!"
                _isGenerating.value = false
                return@launch
            }

            val prompt = buildString {
                appendLine("Analyze the past week's activity and provide insights:")
                appendLine()
                if (recentEntries.isNotEmpty()) {
                    appendLine("Journal Entries:")
                    recentEntries.take(5).forEach { entry ->
                        appendLine("- ${entry.content.take(200)}")
                    }
                }
                if (recentCheckIns.isNotEmpty()) {
                    appendLine()
                    appendLine("Check-ins: ${recentCheckIns.size} total")
                    appendLine("Average progress: ${recentCheckIns.map { it.progressValue }.average().toInt()}%")
                }
                appendLine()
                appendLine("Provide 3-4 sentences covering:")
                appendLine("- Patterns in emotions or energy levels")
                appendLine("- Which experiments show strongest engagement")
                appendLine("- Suggestions for the coming week")
            }

            claudeService?.let { service ->
                val result = service.sendMessage(listOf(ClaudeMessage("user", prompt)))
                result.onSuccess { response ->
                    _weeklyInsights.value = response.text
                    repository?.insertApiUsage(
                        ApiUsage(
                            inputTokens = response.inputTokens,
                            outputTokens = response.outputTokens,
                            estimatedCostCents = service.calculateCost(response.inputTokens, response.outputTokens),
                            operationType = "weekly_reflection"
                        )
                    )
                }
            }
            _isGenerating.value = false
        }
    }

    // Deep dive questionnaire
    fun completeDeepDive(answers: Map<String, List<String>>) {
        viewModelScope.launch {
            _isGenerating.value = true

            _userProfile.value?.let { profile ->
                repository?.updateUserProfile(
                    profile.copy(
                        deepDiveResponses = answers.toString(),
                        deepDiveCompleted = true,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            // Generate refined path suggestions
            val prompt = buildString {
                appendLine("Based on this extended values exploration, suggest refinements to life paths:")
                appendLine()
                answers.forEach { (key, values) ->
                    appendLine("$key: ${values.joinToString(", ")}")
                }
                appendLine()
                appendLine("For each suggestion, provide:")
                appendLine("PATH: [name]")
                appendLine("DESCRIPTION: [description]")
                appendLine("RATIONALE: [why this fits]")
            }

            claudeService?.let { service ->
                val result = service.sendMessage(listOf(ClaudeMessage("user", prompt)))
                result.onSuccess { response ->
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

                    repository?.insertApiUsage(
                        ApiUsage(
                            inputTokens = response.inputTokens,
                            outputTokens = response.outputTokens,
                            estimatedCostCents = service.calculateCost(response.inputTokens, response.outputTokens),
                            operationType = "deep_dive"
                        )
                    )
                }
            }

            _isGenerating.value = false
            loadData()
        }
    }

    // Notification preferences
    fun updateNotificationPreference(field: String, enabled: Boolean) {
        viewModelScope.launch {
            _userProfile.value?.let { profile ->
                val updated = when (field) {
                    "all" -> profile.copy(notificationsEnabled = enabled)
                    "daily" -> profile.copy(dailyCheckInReminder = enabled)
                    "weekly" -> profile.copy(weeklyReflectionReminder = enabled)
                    "lifecycle" -> profile.copy(experimentLifecycleAlerts = enabled)
                    "milestone" -> profile.copy(milestoneNotifications = enabled)
                    else -> profile
                }
                repository?.updateUserProfile(updated.copy(updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun updateNotificationTime(time: String) {
        viewModelScope.launch {
            _userProfile.value?.let { profile ->
                repository?.updateUserProfile(
                    profile.copy(
                        preferredNotificationTime = time,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // Export data
    fun exportData(): String {
        val paths = _paths.value
        val experiments = _experiments.value
        val entries = _journalEntries.value

        return buildString {
            appendLine("{")
            appendLine("  \"exportDate\": \"${System.currentTimeMillis()}\",")
            appendLine("  \"paths\": [")
            paths.forEachIndexed { index, path ->
                appendLine("    {\"name\": \"${path.name}\", \"description\": \"${path.description}\", \"viabilityScore\": ${path.viabilityScore}}")
                if (index < paths.size - 1) appendLine(",")
            }
            appendLine("  ],")
            appendLine("  \"experiments\": [")
            experiments.forEachIndexed { index, exp ->
                appendLine("    {\"title\": \"${exp.title}\", \"purpose\": \"${exp.purpose}\", \"status\": \"${exp.status}\"}")
                if (index < experiments.size - 1) appendLine(",")
            }
            appendLine("  ],")
            appendLine("  \"journalEntries\": [")
            entries.forEachIndexed { index, entry ->
                val escapedContent = entry.content.replace("\"", "\\\"").replace("\n", "\\n")
                appendLine("    {\"content\": \"$escapedContent\", \"createdAt\": ${entry.createdAt}}")
                if (index < entries.size - 1) appendLine(",")
            }
            appendLine("  ]")
            appendLine("}")
        }
    }
}

sealed class AuthState {
    data object Loading : AuthState()
    data object NeedsSetup : AuthState()
    data object NeedsUnlock : AuthState()
}
