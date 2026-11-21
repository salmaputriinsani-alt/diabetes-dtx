package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.Localization
import com.example.core.configureAppCheck
import com.example.data.ai.AurelixAiRepository
import com.example.data.firebase.AurelixRepository
import com.example.data.model.AurelixTab
import com.example.data.model.DiagnosisTrack
import com.example.data.model.PatientProfile
import com.example.data.model.UiState
import com.example.modules.chatbot.ChatbotBottomSheet
import com.example.modules.chatbot.FloatingDropletMascotButton
import com.example.modules.doctor.DoctorScreen
import com.example.modules.food.FoodScannerScreen
import com.example.modules.home.HomeScreen
import com.example.modules.log.LogScreen
import com.example.modules.profile.ProfileScreen
import com.example.ui.AurelixViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.AurelixBottomNavBar
import com.example.ui.components.AurelixTopBar
import com.example.ui.theme.AurelixTheme
import com.example.ui.theme.WarmCream
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch Intent before UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            AurelixTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        key(user.uid) {
            AurelixAuthenticatedShell(
                currentUserId = user.uid,
                googleDisplayName = user.displayName
            )
        }
    }
}

@Composable
private fun AurelixAuthenticatedShell(
    currentUserId: String,
    googleDisplayName: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: AurelixViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                AurelixViewModel(
                    repository = AurelixRepository(db),
                    aiRepository = AurelixAiRepository(),
                    currentUserId = currentUserId,
                    googleDisplayName = googleDisplayName
                )
            }
        }
    )

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedWindow by viewModel.selectedWindow.collectAsStateWithLifecycle()
    val stakeholderMode by viewModel.stakeholderMode.collectAsStateWithLifecycle()
    val activeLanguage by viewModel.activeLanguage.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val logsState by viewModel.logsState.collectAsStateWithLifecycle()
    val doctorMessagesState by viewModel.doctorMessagesState.collectAsStateWithLifecycle()
    val screeningState by viewModel.screeningState.collectAsStateWithLifecycle()
    val cgmSnapshot by viewModel.cgmSnapshot.collectAsStateWithLifecycle()
    val mealAnalysisState by viewModel.mealAnalysisState.collectAsStateWithLifecycle()
    val isChatbotOpen by viewModel.isChatbotOpen.collectAsStateWithLifecycle()
    val botMessages by viewModel.botMessages.collectAsStateWithLifecycle()
    val isBotThinking by viewModel.isBotThinking.collectAsStateWithLifecycle()
    val snackMessage by viewModel.snackMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackMessage) {
        val msg = snackMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackMessage()
        }
    }

    // Return to Home tab on system back when on secondary tabs
    if (selectedTab != AurelixTab.HOME) {
        BackHandler {
            viewModel.selectTab(AurelixTab.HOME)
        }
    }

    val profile = (profileState as? UiState.Success)?.data ?: PatientProfile(userId = currentUserId)
    val activeTrack = DiagnosisTrack.fromWire(profile.diagnosisTrack)
    val copy = Localization.of(activeLanguage)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = WarmCream,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                AurelixTopBar(
                    activeLanguage = activeLanguage,
                    activeTrack = activeTrack,
                    stakeholderMode = stakeholderMode,
                    copy = copy,
                    onLanguageSelected = { viewModel.setLanguage(it) },
                    onToggleTrack = {
                        viewModel.switchDemoProfile(
                            if (activeTrack == DiagnosisTrack.EARLY_T2D_REVERSAL) {
                                DiagnosisTrack.T1_INSULIN_ASSISTED
                            } else {
                                DiagnosisTrack.EARLY_T2D_REVERSAL
                            }
                        )
                    },
                    onToggleStakeholder = { viewModel.toggleStakeholderMode() },
                    onSignOut = {
                        signOutUser(credentialManager, scope)
                    }
                )
            },
            bottomBar = {
                AurelixBottomNavBar(
                    selectedTab = selectedTab,
                    copy = copy,
                    onSelectTab = { viewModel.selectTab(it) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    AurelixTab.HOME -> HomeScreen(
                        profile = profile,
                        cgm = cgmSnapshot,
                        selectedWindow = selectedWindow,
                        featureFlags = viewModel.featureFlags,
                        copy = copy,
                        onSelectWindow = { viewModel.selectWindow(it) },
                        onUpdateSimulator = { sleep, carbs, walk, insulin ->
                            viewModel.updateSimulatorParams(sleep, carbs, walk, insulin)
                        }
                    )

                    AurelixTab.LOG -> LogScreen(
                        logsState = logsState,
                        copy = copy
                    )

                    AurelixTab.SCAN -> FoodScannerScreen(
                        analysisState = mealAnalysisState,
                        copy = copy,
                        onAnalyzeMeal = { bmp, preset ->
                            viewModel.analyzeMealWithGemini(bmp, preset)
                        },
                        onLogMeal = { result ->
                            viewModel.logAnalyzedMeal(result)
                        },
                        onClearResult = { viewModel.clearMealAnalysis() }
                    )

                    AurelixTab.DOCTOR -> DoctorScreen(
                        profile = profile,
                        cgm = cgmSnapshot,
                        messagesState = doctorMessagesState,
                        featureFlags = viewModel.featureFlags,
                        copy = copy,
                        onSendMessage = { text, graph, estimate ->
                            viewModel.sendPatientMessageToDoctor(text, graph, estimate)
                        },
                        onConfirmTriagePlan = { units ->
                            viewModel.confirmDoctorTriagePlan(units)
                        }
                    )

                    AurelixTab.PROFILE -> ProfileScreen(
                        profile = profile,
                        screeningState = screeningState,
                        stakeholderMode = stakeholderMode,
                        featureFlags = viewModel.featureFlags,
                        copy = copy,
                        onSwitchTrack = { track ->
                            viewModel.switchDemoProfile(track)
                        },
                        onSaveScreening = { foot, hrv, fog, bp, dia, hydro ->
                            viewModel.saveOrganScreening(foot, hrv, fog, bp, dia, hydro)
                        }
                    )
                }
            }
        }

        // Floating Droplet Mascot Button above Bottom Navigation Bar
        if (viewModel.featureFlags.aiChatbot) {
            FloatingDropletMascotButton(
                onClick = { viewModel.openChatbot() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 96.dp)
            )
        }

        if (isChatbotOpen && viewModel.featureFlags.aiChatbot) {
            ChatbotBottomSheet(
                messages = botMessages,
                isThinking = isBotThinking,
                language = activeLanguage,
                copy = copy,
                onAsk = { prompt -> viewModel.askChatbot(prompt) },
                onDismiss = { viewModel.closeChatbot() }
            )
        }
    }
}
