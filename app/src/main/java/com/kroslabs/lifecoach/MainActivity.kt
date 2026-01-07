package com.kroslabs.lifecoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.kroslabs.lifecoach.data.model.ThemeMode
import com.kroslabs.lifecoach.security.SecurityManager
import com.kroslabs.lifecoach.ui.MainViewModel
import com.kroslabs.lifecoach.ui.navigation.*
import com.kroslabs.lifecoach.ui.screens.auth.AuthScreen
import com.kroslabs.lifecoach.ui.screens.dashboard.DashboardScreen
import com.kroslabs.lifecoach.ui.screens.experiments.CreateExperimentScreen
import com.kroslabs.lifecoach.ui.screens.experiments.ExperimentsScreen
import com.kroslabs.lifecoach.ui.screens.journal.JournalEntryScreen
import com.kroslabs.lifecoach.ui.screens.journal.JournalScreen
import com.kroslabs.lifecoach.ui.screens.onboarding.OnboardingScreen
import com.kroslabs.lifecoach.ui.screens.profile.ProfileScreen
import com.kroslabs.lifecoach.ui.theme.LifeCoachTheme

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var securityManager: SecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        securityManager = SecurityManager(this)

        setContent {
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val themeMode = userProfile?.themeMode ?: ThemeMode.SYSTEM

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            LifeCoachTheme(darkTheme = darkTheme) {
                LifeCoachNavHost(
                    viewModel = viewModel,
                    securityManager = securityManager,
                    activity = this
                )
            }
        }
    }
}

@Composable
fun LifeCoachNavHost(
    viewModel: MainViewModel,
    securityManager: SecurityManager,
    activity: FragmentActivity
) {
    val navController = rememberNavController()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val isAuthSetupComplete by viewModel.isAuthSetupComplete.collectAsStateWithLifecycle(initialValue = false)
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle(initialValue = false)
    val isApiKeySet by viewModel.isApiKeySet.collectAsStateWithLifecycle(initialValue = false)
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val paths by viewModel.paths.collectAsStateWithLifecycle()
    val experiments by viewModel.experiments.collectAsStateWithLifecycle()
    val journalEntries by viewModel.journalEntries.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val generatedText by viewModel.generatedText.collectAsStateWithLifecycle()
    val aiInsights by viewModel.aiInsights.collectAsStateWithLifecycle()
    val totalTokensUsed by viewModel.totalTokensUsed.collectAsStateWithLifecycle()
    val totalCostCents by viewModel.totalCostCents.collectAsStateWithLifecycle()

    val startDestination = when {
        !isAuthSetupComplete -> Screen.Auth.route
        !isAuthenticated -> Screen.Auth.route
        userProfile?.onboardingCompleted != true -> Screen.Onboarding.route
        else -> Screen.Dashboard.route
    }

    LaunchedEffect(isAuthenticated, userProfile?.onboardingCompleted) {
        if (isAuthenticated) {
            val destination = if (userProfile?.onboardingCompleted == true) {
                Screen.Dashboard.route
            } else {
                Screen.Onboarding.route
            }
            navController.navigate(destination) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (isAuthenticated && userProfile?.onboardingCompleted == true) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                if (currentRoute in listOf(
                        Screen.Dashboard.route,
                        Screen.Experiments.route,
                        Screen.Journal.route,
                        Screen.Profile.route
                    )
                ) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (currentRoute == item.screen.route) {
                                            item.selectedIcon
                                        } else {
                                            item.unselectedIcon
                                        },
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                selected = currentRoute == item.screen.route,
                                onClick = {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(Screen.Dashboard.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(Screen.Auth.route) {
                AuthScreen(
                    isSetupMode = !isAuthSetupComplete,
                    biometricAvailable = viewModel.canUseBiometric(),
                    biometricEnabled = isBiometricEnabled,
                    onPinSubmit = { pin -> viewModel.verifyPin(pin) },
                    onPinSetup = { pin, biometric -> viewModel.setupPin(pin, biometric) },
                    onBiometricClick = {
                        securityManager.showBiometricPrompt(
                            activity = activity,
                            onSuccess = { viewModel.verifyPin("") },
                            onError = { }
                        )
                    },
                    errorMessage = errorMessage
                )
            }

            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    isLoading = isGenerating,
                    onComplete = { answers ->
                        viewModel.completeOnboarding(answers)
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    paths = paths,
                    onPathClick = { pathId ->
                        navController.navigate(Screen.PathDetail.createRoute(pathId))
                    },
                    onCreateExperiment = {
                        navController.navigate(Screen.CreateExperiment.createRoute())
                    }
                )
            }

            composable(Screen.Experiments.route) {
                ExperimentsScreen(
                    experiments = experiments,
                    onExperimentClick = { experimentId ->
                        navController.navigate(Screen.ExperimentDetail.createRoute(experimentId))
                    },
                    onCheckIn = { experimentId ->
                        viewModel.checkInExperiment(experimentId, 10f, "")
                    },
                    onCreateExperiment = {
                        navController.navigate(Screen.CreateExperiment.createRoute())
                    }
                )
            }

            composable(Screen.Journal.route) {
                JournalScreen(
                    entries = journalEntries,
                    onEntryClick = { entryId ->
                        navController.navigate(Screen.JournalEntry.createRoute(entryId))
                    },
                    onNewEntry = {
                        navController.navigate(Screen.JournalEntry.createRoute())
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    themeMode = userProfile?.themeMode ?: ThemeMode.SYSTEM,
                    biometricEnabled = isBiometricEnabled,
                    biometricAvailable = viewModel.canUseBiometric(),
                    totalTokensUsed = totalTokensUsed,
                    totalCostCents = totalCostCents,
                    apiKeySet = isApiKeySet,
                    onThemeChange = { viewModel.updateTheme(it) },
                    onBiometricToggle = { viewModel.toggleBiometric(it) },
                    onApiKeySave = { apiKey -> viewModel.saveClaudeApiKey(apiKey) },
                    onExportData = { /* TODO: Implement export */ },
                    onClearData = { viewModel.clearAllData() }
                )
            }

            composable(
                route = Screen.CreateExperiment.route,
                arguments = listOf(
                    navArgument("pathId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val pathIdStr = backStackEntry.arguments?.getString("pathId")
                val pathId = pathIdStr?.toLongOrNull()

                CreateExperimentScreen(
                    paths = paths,
                    selectedPathId = pathId,
                    isGenerating = isGenerating,
                    generatedText = generatedText,
                    onBack = { navController.popBackStack() },
                    onGenerate = { viewModel.generateExperiment(it) },
                    onSave = { title, purpose, steps, days, method, pId ->
                        viewModel.saveExperiment(title, purpose, steps, days, method, pId)
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.JournalEntry.route,
                arguments = listOf(
                    navArgument("entryId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val entryIdStr = backStackEntry.arguments?.getString("entryId")
                val entryId = entryIdStr?.toLongOrNull()
                val existingEntry = journalEntries.find { it.id == entryId }

                JournalEntryScreen(
                    existingEntry = existingEntry,
                    paths = paths,
                    aiInsights = aiInsights,
                    isAnalyzing = isGenerating,
                    onBack = { navController.popBackStack() },
                    onSave = { content, pathId ->
                        viewModel.saveJournalEntry(content, pathId, entryId)
                        navController.popBackStack()
                    },
                    onDelete = {
                        entryId?.let { viewModel.deleteJournalEntry(it) }
                        navController.popBackStack()
                    },
                    onAnalyze = { viewModel.analyzeJournalEntry(it) }
                )
            }
        }
    }
}
