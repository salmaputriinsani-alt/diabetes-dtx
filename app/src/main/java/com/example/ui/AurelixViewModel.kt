package com.example.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.config.DEFAULT_FEATURE_FLAGS
import com.example.config.FeatureFlags
import com.example.data.ai.AurelixAiRepository
import com.example.data.firebase.AurelixRepository
import com.example.data.model.AppLanguage
import com.example.data.model.AurelixTab
import com.example.data.model.BotChatMessage
import com.example.data.model.CgmSnapshotSummary
import com.example.data.model.DiagnosisTrack
import com.example.data.model.DoctorMessage
import com.example.data.model.LogEntryType
import com.example.data.model.MealAnalysisResult
import com.example.data.model.MetabolicLog
import com.example.data.model.OrganScreening
import com.example.data.model.PatientProfile
import com.example.data.model.SenderRole
import com.example.data.model.TimeWindow
import com.example.data.model.UiState
import com.example.modules.cgm.CgmForecastEngine
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val TAG = "AurelixViewModel"
private const val STOP_TIMEOUT_MILLIS = 5000L

class AurelixViewModel(
    private val repository: AurelixRepository,
    private val aiRepository: AurelixAiRepository,
    private val currentUserId: String,
    googleDisplayName: String?
) : ViewModel() {

    val featureFlags: FeatureFlags = DEFAULT_FEATURE_FLAGS

    private val _selectedTab = MutableStateFlow(AurelixTab.HOME)
    val selectedTab: StateFlow<AurelixTab> = _selectedTab.asStateFlow()

    private val _selectedWindow = MutableStateFlow(TimeWindow.H3)
    val selectedWindow: StateFlow<TimeWindow> = _selectedWindow.asStateFlow()

    private val _stakeholderMode = MutableStateFlow(false)
    val stakeholderMode: StateFlow<Boolean> = _stakeholderMode.asStateFlow()

    private val _localLanguageOverride = MutableStateFlow<AppLanguage?>(null)

    private val _snackMessage = MutableStateFlow<String?>(null)
    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

    // Meal scanner state
    private val _mealAnalysisState = MutableStateFlow<UiState<MealAnalysisResult>?>(null)
    val mealAnalysisState: StateFlow<UiState<MealAnalysisResult>?> = _mealAnalysisState.asStateFlow()

    // Chatbot state
    private val _isChatbotOpen = MutableStateFlow(false)
    val isChatbotOpen: StateFlow<Boolean> = _isChatbotOpen.asStateFlow()

    private val _botMessages = MutableStateFlow(
        listOf(
            BotChatMessage(
                id = "welcome",
                isFromUser = false,
                text = "Hi Encik Ahmad! I'm your AURELIX AI droplet companion. Your CGM is 113 mg/dL (In Target). Ask me about Nasi Lemak safety, your SHAP drivers, or a 10-second doctor summary."
            )
        )
    )
    val botMessages: StateFlow<List<BotChatMessage>> = _botMessages.asStateFlow()

    private val _isBotThinking = MutableStateFlow(false)
    val isBotThinking: StateFlow<Boolean> = _isBotThinking.asStateFlow()

    // Firestore streams with Two-Tier CancellationException unwrapping
    val profileState: StateFlow<UiState<PatientProfile>> = repository.observeProfile(currentUserId)
        .map<PatientProfile?, UiState<PatientProfile>> { profile ->
            UiState.Success(profile ?: PatientProfile(userId = currentUserId))
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing profile", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load patient profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val logsState: StateFlow<UiState<List<MetabolicLog>>> = repository.observeLogs(currentUserId)
        .map<List<MetabolicLog>, UiState<List<MetabolicLog>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing logs", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load timeline logs"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val doctorMessagesState: StateFlow<UiState<List<DoctorMessage>>> =
        repository.observeDoctorMessages(currentUserId)
            .map<List<DoctorMessage>, UiState<List<DoctorMessage>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing doctor messages", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load doctor thread"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val screeningState: StateFlow<UiState<OrganScreening>> =
        repository.observeLatestScreening(currentUserId)
            .map<OrganScreening?, UiState<OrganScreening>> {
                UiState.Success(it ?: OrganScreening(userId = currentUserId))
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing screening", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load organ screening"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val activeLanguage: StateFlow<AppLanguage> = combine(
        profileState,
        _localLanguageOverride
    ) { profState, localLang ->
        localLang ?: when (profState) {
            is UiState.Success -> AppLanguage.fromCode(profState.data.language)
            else -> AppLanguage.EN
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppLanguage.EN)

    val cgmSnapshot: StateFlow<CgmSnapshotSummary> = combine(
        profileState,
        logsState,
        _selectedWindow,
        activeLanguage
    ) { profState, lState, window, lang ->
        val profile = (profState as? UiState.Success)?.data ?: PatientProfile(userId = currentUserId)
        val logs = (lState as? UiState.Success)?.data ?: emptyList()
        CgmForecastEngine.compute(profile, logs, window, lang)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        CgmForecastEngine.compute(PatientProfile(userId = currentUserId), emptyList(), TimeWindow.H3, AppLanguage.EN)
    )

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfMissing(currentUserId, googleDisplayName)
                .onFailure { err ->
                    Log.w(TAG, "Initial seed check error: ${err.message}")
                }
        }
    }

    fun selectTab(tab: AurelixTab) {
        _selectedTab.value = tab
    }

    fun selectWindow(window: TimeWindow) {
        _selectedWindow.value = window
    }

    fun toggleStakeholderMode() {
        _stakeholderMode.value = !_stakeholderMode.value
    }

    fun clearSnackMessage() {
        _snackMessage.value = null
    }

    fun setLanguage(language: AppLanguage) {
        _localLanguageOverride.value = language
        viewModelScope.launch {
            repository.updateProfileFields(currentUserId, mapOf("language" to language.code))
        }
    }

    fun switchDemoProfile(track: DiagnosisTrack) {
        viewModelScope.launch {
            val updates = if (track == DiagnosisTrack.EARLY_T2D_REVERSAL) {
                mapOf(
                    "diagnosisTrack" to track.wireValue,
                    "patientName" to "Encik Ahmad",
                    "simInsulinUnits" to 1.5,
                    "simMealCarbs" to 45L
                )
            } else {
                mapOf(
                    "diagnosisTrack" to track.wireValue,
                    "patientName" to "Aisyah (T1D)",
                    "simInsulinUnits" to 3.5,
                    "simMealCarbs" to 50L
                )
            }
            repository.updateProfileFields(currentUserId, updates)
                .onSuccess {
                    _snackMessage.value = if (track == DiagnosisTrack.EARLY_T2D_REVERSAL) {
                        "Switched to Early T2D Reversal Twin (Encik Ahmad)"
                    } else {
                        "Switched to T1D Insulin-Assisted Preview (Aisyah)"
                    }
                }
                .onFailure { _snackMessage.value = it.localizedMessage }
        }
    }

    fun updateSimulatorParams(
        sleepHours: Double,
        mealCarbs: Int,
        walkMinutes: Int,
        insulinUnits: Double
    ) {
        viewModelScope.launch {
            repository.updateProfileFields(
                currentUserId,
                mapOf(
                    "sleepHours" to sleepHours.coerceIn(2.0, 12.0),
                    "simMealCarbs" to mealCarbs.coerceIn(0, 200).toLong(),
                    "walkMinutes" to walkMinutes.coerceIn(0, 120).toLong(),
                    "simInsulinUnits" to insulinUnits.coerceIn(0.0, 25.0)
                )
            )
        }
    }

    fun analyzeMealWithGemini(bitmap: Bitmap?, presetDishName: String?) {
        viewModelScope.launch {
            _mealAnalysisState.value = UiState.Loading
            val currentMgDl = cgmSnapshot.value.currentMgDl
            val lang = activeLanguage.value
            val result = aiRepository.analyzeMeal(bitmap, presetDishName, lang, currentMgDl)
            result.onSuccess { analysis ->
                _mealAnalysisState.value = UiState.Success(analysis)
            }.onFailure { err ->
                _mealAnalysisState.value = UiState.Error(
                    err.localizedMessage ?: "Gemini meal analysis failed. Check connection or App Check."
                )
            }
        }
    }

    fun clearMealAnalysis() {
        _mealAnalysisState.value = null
    }

    fun logAnalyzedMeal(analysis: MealAnalysisResult) {
        viewModelScope.launch {
            val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val projectedGlucose = (cgmSnapshot.value.currentMgDl + analysis.predictedRiseMgDl)
                .coerceIn(60, 400).toLong()
            val log = MetabolicLog(
                userId = currentUserId,
                entryType = LogEntryType.MEAL.wireValue,
                title = analysis.mealName,
                timeLabel = nowTime,
                dateGroup = "Today",
                glucoseMgDl = projectedGlucose,
                carbsGrams = analysis.carbsGrams.toLong(),
                proteinGrams = analysis.proteinGrams.toLong(),
                fatGrams = analysis.fatGrams.toLong(),
                fiberGrams = analysis.fiberGrams.toLong(),
                glycemicLoad = analysis.glycemicLoad,
                insulinUnits = 0.0,
                shapImpactSummary = "Meal Carbs +${analysis.predictedRiseMgDl} mg/dL (${analysis.carbsGrams}g carbs, GL: ${analysis.glycemicLoad})",
                habitTip = analysis.malaysianHabitTip
            )
            repository.addMetabolicLog(currentUserId, log)
                .onSuccess {
                    repository.updateProfileFields(
                        currentUserId,
                        mapOf("simMealCarbs" to analysis.carbsGrams.toLong())
                    )
                    _snackMessage.value = "Logged ${analysis.mealName} to CGM & Timeline"
                    _mealAnalysisState.value = null
                    _selectedTab.value = AurelixTab.LOG
                }
                .onFailure { err ->
                    _snackMessage.value = err.localizedMessage ?: "Failed to save meal log"
                }
        }
    }

    fun sendPatientMessageToDoctor(
        text: String,
        includeGraph: Boolean,
        includeEstimate: Boolean
    ) {
        val clean = text.trim()
        if (clean.isEmpty() && !includeGraph && !includeEstimate) return

        viewModelScope.launch {
            val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val prof = (profileState.value as? UiState.Success)?.data ?: PatientProfile(userId = currentUserId)
            val cgm = cgmSnapshot.value
            val finalMsg = if (clean.isNotEmpty()) clean else "Sharing my live CGM snapshot & estimate for review."
            val graphText = if (includeGraph) {
                "${cgm.currentMgDl} now | 60m pred ${cgm.predicted60MinMgDl} | TIR ${cgm.timeInRangePercent}%"
            } else null
            val estimate = if (includeEstimate) prof.simInsulinUnits else null

            val msg = DoctorMessage(
                userId = currentUserId,
                senderRole = SenderRole.PATIENT.wireValue,
                senderName = prof.patientName,
                text = finalMsg,
                timeLabel = nowTime,
                attachedGraphSummary = graphText,
                attachedEstimateUnits = estimate
            )
            repository.sendDoctorMessage(currentUserId, msg)
                .onSuccess {
                    // Simulate KPJ clinician triage confirmation response after short delay
                    delay(1200L)
                    val safeDose = ((prof.simInsulinUnits * 0.7).coerceAtLeast(0.5))
                    val roundedDose = String.format(Locale.US, "%.1f", safeDose)
                    val docReply = DoctorMessage(
                        userId = currentUserId,
                        senderRole = SenderRole.DOCTOR.wireValue,
                        senderName = "dr. Sarah Wijaya",
                        text = "Received your live CGM (${cgm.currentMgDl} mg/dL) and SHAP sleep-debt alert. Let's use $roundedDose units + 15-min walk.",
                        timeLabel = nowTime,
                        confirmedBadge = "Confirmed by Doctor - $roundedDose unit"
                    )
                    repository.sendDoctorMessage(currentUserId, docReply)
                }
                .onFailure { err ->
                    _snackMessage.value = err.localizedMessage ?: "Failed to send message"
                }
        }
    }

    fun confirmDoctorTriagePlan(recommendedUnits: Double) {
        viewModelScope.launch {
            val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val formatted = String.format(Locale.US, "%.1f", recommendedUnits)
            val msg = DoctorMessage(
                userId = currentUserId,
                senderRole = SenderRole.DOCTOR.wireValue,
                senderName = "dr. Sarah Wijaya",
                text = "KPJ Triage Verified: Sleep debt + post-meal load addressed. Proceed with $formatted units & hydration.",
                timeLabel = nowTime,
                confirmedBadge = "Confirmed by Doctor - $formatted unit"
            )
            repository.sendDoctorMessage(currentUserId, msg)
                .onSuccess {
                    _snackMessage.value = "Doctor triage order ($formatted u) confirmed & synced!"
                }
                .onFailure { _snackMessage.value = it.localizedMessage }
        }
    }

    fun saveOrganScreening(
        footTingling: Boolean,
        hrvStressHigh: Boolean,
        postSpikeBrainFog: Boolean,
        systolicBp: Int,
        diastolicBp: Int,
        hydrationGlasses: Int
    ) {
        viewModelScope.launch {
            var score = 96
            if (footTingling) score -= 18
            if (hrvStressHigh) score -= 14
            if (postSpikeBrainFog) score -= 12
            if (systolicBp >= 135) score -= 8
            if (hydrationGlasses < 6) score -= 4
            val clampedScore = score.coerceIn(35, 100).toLong()

            val screening = OrganScreening(
                userId = currentUserId,
                footTingling = footTingling,
                hrvStressHigh = hrvStressHigh,
                postSpikeBrainFog = postSpikeBrainFog,
                neuroStabilityScore = clampedScore,
                systolicBp = systolicBp.toLong(),
                diastolicBp = diastolicBp.toLong(),
                hydrationGlasses = hydrationGlasses.toLong(),
                egfr = if (hydrationGlasses >= 6) 94L else 89L,
                uacr = if (systolicBp >= 135) 18.2 else 12.4
            )
            repository.saveScreening(currentUserId, screening)
                .onSuccess {
                    _snackMessage.value = "Neuro-Metabolic & Renal Screening saved ($clampedScore/100)"
                }
                .onFailure { err ->
                    _snackMessage.value = err.localizedMessage ?: "Failed to save screening"
                }
        }
    }

    fun openChatbot() {
        _isChatbotOpen.value = true
    }

    fun closeChatbot() {
        _isChatbotOpen.value = false
    }

    fun askChatbot(prompt: String) {
        val clean = prompt.trim()
        if (clean.isEmpty() || _isBotThinking.value) return

        val userMsgId = UUID.randomUUID().toString()
        val botMsgId = UUID.randomUUID().toString()

        _botMessages.value = _botMessages.value +
            BotChatMessage(id = userMsgId, isFromUser = true, text = clean) +
            BotChatMessage(id = botMsgId, isFromUser = false, text = "", isStreaming = true)

        _isBotThinking.value = true
        viewModelScope.launch {
            val profile = (profileState.value as? UiState.Success)?.data ?: PatientProfile(userId = currentUserId)
            val logs = (logsState.value as? UiState.Success)?.data ?: emptyList()
            val recentMeals = logs.filter { it.entryType == LogEntryType.MEAL.wireValue }.take(3).map { it.title }

            try {
                var latestText = ""
                aiRepository.streamCompanionReply(
                    userPrompt = clean,
                    profile = profile,
                    cgm = cgmSnapshot.value,
                    recentMealTitles = recentMeals,
                    language = activeLanguage.value
                ).collect { chunk ->
                    latestText = chunk
                    _botMessages.value = _botMessages.value.map { msg ->
                        if (msg.id == botMsgId) msg.copy(text = latestText, isStreaming = true) else msg
                    }
                }
                _botMessages.value = _botMessages.value.map { msg ->
                    if (msg.id == botMsgId) {
                        msg.copy(
                            text = latestText.ifBlank { "I couldn't generate a reply right now. Please try again." },
                            isStreaming = false
                        )
                    } else msg
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chatbot streaming failed", e)
                _botMessages.value = _botMessages.value.map { msg ->
                    if (msg.id == botMsgId) {
                        msg.copy(
                            text = "AI error: ${e.localizedMessage ?: "Unable to reach Gemini via Firebase AI Logic."}",
                            isStreaming = false
                        )
                    } else msg
                }
            } finally {
                _isBotThinking.value = false
            }
        }
    }
}
