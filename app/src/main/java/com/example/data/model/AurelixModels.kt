package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AppLanguage(val code: String, val label: String) {
    MS("ms", "MS"),
    EN("en", "EN"),
    ZH("zh", "ZH");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code } ?: EN
    }
}

enum class DiagnosisTrack(val wireValue: String) {
    EARLY_T2D_REVERSAL("EARLY_T2D_REVERSAL"),
    T1_INSULIN_ASSISTED("T1_INSULIN_ASSISTED");

    companion object {
        fun fromWire(value: String): DiagnosisTrack =
            entries.firstOrNull { it.wireValue == value } ?: EARLY_T2D_REVERSAL
    }
}

enum class LogEntryType(val wireValue: String) {
    MEAL("MEAL"),
    INSULIN("INSULIN"),
    ALERT("ALERT"),
    PREDICTION("PREDICTION");

    companion object {
        fun fromWire(value: String): LogEntryType =
            entries.firstOrNull { it.wireValue == value } ?: MEAL
    }
}

enum class SenderRole(val wireValue: String) {
    PATIENT("PATIENT"),
    DOCTOR("DOCTOR");

    companion object {
        fun fromWire(value: String): SenderRole =
            entries.firstOrNull { it.wireValue == value } ?: PATIENT
    }
}

enum class AurelixTab {
    HOME, LOG, SCAN, DOCTOR, PROFILE
}

enum class TimeWindow(val label: String, val hours: Int) {
    H3("3h", 3),
    H6("6h", 6),
    H12("12h", 12),
    H24("24h", 24)
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

// --- Firestore Persisted Models ---

@Keep
@IgnoreExtraProperties
data class PatientProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val patientName: String = "Encik Ahmad",
    val age: Long = 46L,
    val diagnosisTrack: String = DiagnosisTrack.EARLY_T2D_REVERSAL.wireValue,
    val language: String = AppLanguage.EN.code,
    val sensorDaysLeft: Long = 9L,
    val sensorBattery: Long = 82L,
    val targetLowMgDl: Long = 70L,
    val targetHighMgDl: Long = 180L,
    val sleepHours: Double = 4.5,
    val walkMinutes: Long = 15L,
    val simMealCarbs: Long = 45L,
    val simInsulinUnits: Double = 1.5,
    val neuroScore: Long = 82L,
    val renalEgfr: Long = 94L,
    val renalUacr: Double = 12.4,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MetabolicLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val entryType: String = LogEntryType.MEAL.wireValue,
    val title: String = "",
    val timeLabel: String = "08:50",
    val dateGroup: String = "Today",
    val glucoseMgDl: Long = 113L,
    val carbsGrams: Long = 0L,
    val proteinGrams: Long = 0L,
    val fatGrams: Long = 0L,
    val fiberGrams: Long = 0L,
    val glycemicLoad: String = "Low",
    val insulinUnits: Double = 0.0,
    val shapImpactSummary: String = "",
    val habitTip: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DoctorMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val senderRole: String = SenderRole.PATIENT.wireValue,
    val senderName: String = "",
    val text: String = "",
    val timeLabel: String = "",
    val attachedGraphSummary: String? = null,
    val attachedEstimateUnits: Double? = null,
    val confirmedBadge: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OrganScreening(
    @DocumentId val id: String = "",
    val userId: String = "",
    val footTingling: Boolean = false,
    val hrvStressHigh: Boolean = true,
    val postSpikeBrainFog: Boolean = false,
    val neuroStabilityScore: Long = 82L,
    val systolicBp: Long = 124L,
    val diastolicBp: Long = 78L,
    val hydrationGlasses: Long = 7L,
    val egfr: Long = 94L,
    val uacr: Double = 12.4,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// --- Domain & Forecast Models ---

data class GlucosePoint(
    val label: String,
    val mgDl: Float,
    val isForecast: Boolean = false,
    val upperBound: Float = mgDl,
    val lowerBound: Float = mgDl,
    val markerType: LogEntryType? = null
)

data class ShapDriver(
    val label: String,
    val emoji: String,
    val deltaMgDl: Int,
    val attributionPercent: Int
)

data class CgmSnapshotSummary(
    val currentMgDl: Int,
    val currentMmolL: Float,
    val predicted60MinMgDl: Int,
    val trendUp: Boolean,
    val inTarget: Boolean,
    val timeInRangePercent: Int,
    val tenSecondBullets: List<String>,
    val patientShapSentence: String,
    val shapDrivers: List<ShapDriver>,
    val points: List<GlucosePoint>,
    val clinicianBrief: List<String>
)

data class MealAnalysisResult(
    val mealName: String,
    val carbsGrams: Int,
    val proteinGrams: Int,
    val fatGrams: Int,
    val fiberGrams: Int,
    val glycemicLoad: String,
    val predictedRiseMgDl: Int,
    val malaysianHabitTip: String,
    val isPresetFallback: Boolean = false
)

data class BotChatMessage(
    val id: String,
    val isFromUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false
)
