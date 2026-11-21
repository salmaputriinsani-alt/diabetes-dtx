package com.example.modules.cgm

import com.example.data.model.AppLanguage
import com.example.data.model.CgmSnapshotSummary
import com.example.data.model.DiagnosisTrack
import com.example.data.model.GlucosePoint
import com.example.data.model.LogEntryType
import com.example.data.model.MetabolicLog
import com.example.data.model.PatientProfile
import com.example.data.model.ShapDriver
import com.example.data.model.TimeWindow
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Computes the live CGM curve, 60-minute predictive trajectory cone,
 * and dual Patient/Clinician SHAP explainability attribution from telemetry & simulator sliders.
 */
object CgmForecastEngine {

    fun compute(
        profile: PatientProfile,
        logs: List<MetabolicLog>,
        window: TimeWindow,
        language: AppLanguage
    ): CgmSnapshotSummary {
        val isT1 = DiagnosisTrack.fromWire(profile.diagnosisTrack) == DiagnosisTrack.T1_INSULIN_ASSISTED

        // SHAP feature deltas (mg/dL impact over 60 min)
        val sleepDebtHours = (7.5 - profile.sleepHours).coerceAtLeast(0.0)
        val sleepDelta = (sleepDebtHours * 11.6).roundToInt() // e.g., 4.5h -> 3h debt -> +35 mg/dL
        val carbDelta = ((profile.simMealCarbs * (if (isT1) 0.85 else 0.67))).roundToInt()
        val walkDelta = -((profile.walkMinutes * 1.2).roundToInt())
        val insulinDelta = -((profile.simInsulinUnits * (if (isT1) 18.0 else 10.0)).roundToInt())

        // Base CGM anchored at 113 mg/dL under default Encik Ahmad settings (4.5h sleep, 45g carbs, 15m walk, 1.5u)
        val defaultSleepDelta = 35
        val defaultCarbDelta = 30
        val defaultWalkDelta = -18
        val defaultInsulinDelta = -15
        val netShift = (sleepDelta - defaultSleepDelta) +
            (carbDelta - defaultCarbDelta) +
            (walkDelta - defaultWalkDelta) +
            (insulinDelta - defaultInsulinDelta)

        // Check if user recently logged a meal in Today's logs beyond seed
        val latestMealCarbsBoost = logs
            .firstOrNull { it.entryType == LogEntryType.MEAL.wireValue && !it.id.startsWith("seed_") }
            ?.let { (it.carbsGrams * 0.25).toInt() } ?: 0

        val currentMgDl = (113 + (netShift * 0.35).roundToInt() + latestMealCarbsBoost).coerceIn(68, 290)
        val currentMmol = (currentMgDl / 18.0f)

        val predicted60 = (currentMgDl + (sleepDelta * 0.25 + carbDelta * 0.45 + walkDelta * 0.6 + insulinDelta * 0.65))
            .roundToInt()
            .coerceIn(65, 310)

        val inTarget = currentMgDl in profile.targetLowMgDl.toInt()..profile.targetHighMgDl.toInt()
        val trendUp = predicted60 > currentMgDl + 8

        val tir = (81 - (sleepDebtHours * 2.5).roundToInt() + (profile.walkMinutes / 5)).toInt().coerceIn(52, 96)

        val totalAbs = (abs(sleepDelta) + abs(carbDelta) + abs(walkDelta) + abs(insulinDelta)).coerceAtLeast(1)
        val drivers = listOf(
            ShapDriver(
                label = when (language) {
                    AppLanguage.MS -> "Tidur Singkat (${String.format("%.1f", profile.sleepHours)}j)"
                    AppLanguage.ZH -> "睡眠不足 (${String.format("%.1f", profile.sleepHours)}小时)"
                    AppLanguage.EN -> "Short Sleep (${String.format("%.1f", profile.sleepHours)}h)"
                },
                emoji = "😴",
                deltaMgDl = sleepDelta,
                attributionPercent = ((abs(sleepDelta) * 100f) / totalAbs).roundToInt()
            ),
            ShapDriver(
                label = when (language) {
                    AppLanguage.MS -> "Karbohidrat Makan (${profile.simMealCarbs}g)"
                    AppLanguage.ZH -> "餐食碳水 (${profile.simMealCarbs}克)"
                    AppLanguage.EN -> "Meal Carbs (${profile.simMealCarbs}g)"
                },
                emoji = "🍛",
                deltaMgDl = carbDelta,
                attributionPercent = ((abs(carbDelta) * 100f) / totalAbs).roundToInt()
            ),
            ShapDriver(
                label = when (language) {
                    AppLanguage.MS -> "Berjalan (${profile.walkMinutes} min)"
                    AppLanguage.ZH -> "餐后步行 (${profile.walkMinutes}分钟)"
                    AppLanguage.EN -> "Post-Meal Walk (${profile.walkMinutes}m)"
                },
                emoji = "🚶",
                deltaMgDl = walkDelta,
                attributionPercent = ((abs(walkDelta) * 100f) / totalAbs).roundToInt()
            ),
            ShapDriver(
                label = when (language) {
                    AppLanguage.MS -> "Insulin / Ubat (${String.format("%.1f", profile.simInsulinUnits)}u)"
                    AppLanguage.ZH -> "胰岛素/药物 (${String.format("%.1f", profile.simInsulinUnits)}u)"
                    AppLanguage.EN -> "Insulin / Meds (${String.format("%.1f", profile.simInsulinUnits)}u)"
                },
                emoji = "💉",
                deltaMgDl = insulinDelta,
                attributionPercent = ((abs(insulinDelta) * 100f) / totalAbs).roundToInt()
            )
        )

        val tenSecBullets = when (language) {
            AppLanguage.MS -> listOf(
                "Masa dalam Sasaran (TIR): $tir% · Sasaran 70–180 mg/dL",
                "Punca Utama: Tidur ${String.format("%.1f", profile.sleepHours)}j (+$sleepDelta) + Nasi Lemak (+$carbDelta)",
                "1 Tindakan Terbaik: Berjalan 15 minit selepas makan (${walkDelta} mg/dL)"
            )
            AppLanguage.ZH -> listOf(
                "目标范围内时间 (TIR): $tir% · 目标 70–180 mg/dL",
                "主要驱动因素: 睡眠 ${String.format("%.1f", profile.sleepHours)}h (+$sleepDelta) + 椰浆饭 (+$carbDelta)",
                "最佳行动建议: 餐后快走 15 分钟 (${walkDelta} mg/dL)"
            )
            AppLanguage.EN -> listOf(
                "Time-in-Range: $tir% (Target 70–180 mg/dL)",
                "Main Driver: ${String.format("%.1f", profile.sleepHours)}h Sleep (+$sleepDelta) + Nasi Lemak (+$carbDelta)",
                "1 Best Action: 15-min post-meal walk (${walkDelta} mg/dL)"
            )
        }

        val patientSentence = when (language) {
            AppLanguage.MS -> "Tidur singkat malam tadi (${String.format("%.1f", profile.sleepHours)}j) meningkatkan kortisol pagi anda, jadi karbohidrat sarapan naik lebih cepat. Berjalan ${profile.walkMinutes} minit membantu menurunkannya kembali ke sasaran."
            AppLanguage.ZH -> "昨晚睡眠较短（${String.format("%.1f", profile.sleepHours)}小时）提高了清晨皮质醇敏感度，使早餐碳水更易引起波动。步行${profile.walkMinutes}分钟可有效拉回平稳区间。"
            AppLanguage.EN -> "Last night's ${String.format("%.1f", profile.sleepHours)}h sleep raised morning insulin resistance, amplifying your breakfast carbs. A ${profile.walkMinutes}-minute walk gently pulls your 60-min curve back into range."
        }

        val clinicianBrief = when (language) {
            AppLanguage.MS -> listOf(
                "TIR 7 hari: $tir% | Semasa: $currentMgDl mg/dL | Ramalan 60m: $predicted60 mg/dL",
                "Atribusi SHAP: Hutang Tidur (+${drivers[0].attributionPercent}%) & Karbohidrat (+${drivers[1].attributionPercent}%) diimbangi Berjalan (-${drivers[2].attributionPercent}%)",
                "Pelan KPJ: Teruskan 1u bolus konservatif + saringan awal neuro (${profile.neuroScore}/100) & renal (eGFR ${profile.renalEgfr})."
            )
            AppLanguage.ZH -> listOf(
                "7日 TIR: $tir% | 当前: $currentMgDl mg/dL | 60分钟预测: $predicted60 mg/dL",
                "SHAP 归因: 睡眠不足 (+${drivers[0].attributionPercent}%) 与碳水 (+${drivers[1].attributionPercent}%)，步行抵消 (-${drivers[2].attributionPercent}%)",
                "KPJ 临床建议: 维持保守 1u 剂量方案，神经代谢评分 ${profile.neuroScore}/100，肾功能 eGFR ${profile.renalEgfr} 稳定。"
            )
            AppLanguage.EN -> listOf(
                "7d TIR: $tir% | Live CGM: $currentMgDl mg/dL | 60-min Twin Forecast: $predicted60 mg/dL",
                "Top SHAP Attribution: Sleep Debt (+${drivers[0].attributionPercent}%) & Meal Carbs (+${drivers[1].attributionPercent}%) offset by Walk (-${drivers[2].attributionPercent}%)",
                "KPJ Action: Hold conservative 1u correction; Neuro (${profile.neuroScore}/100) & Renal (eGFR ${profile.renalEgfr}) remain protected."
            )
        }

        val points = buildTrajectoryPoints(window, currentMgDl, predicted60)

        return CgmSnapshotSummary(
            currentMgDl = currentMgDl,
            currentMmolL = currentMmol,
            predicted60MinMgDl = predicted60,
            trendUp = trendUp,
            inTarget = inTarget,
            timeInRangePercent = tir,
            tenSecondBullets = tenSecBullets,
            patientShapSentence = patientSentence,
            shapDrivers = drivers,
            points = points,
            clinicianBrief = clinicianBrief
        )
    }

    private fun buildTrajectoryPoints(
        window: TimeWindow,
        currentMgDl: Int,
        predicted60: Int
    ): List<GlucosePoint> {
        val baseHistory = when (window) {
            TimeWindow.H3 -> listOf(
                GlucosePoint("07:00", 98f),
                GlucosePoint("07:30", 104f, markerType = LogEntryType.INSULIN),
                GlucosePoint("08:00", 112f),
                GlucosePoint("08:30", 119f, markerType = LogEntryType.MEAL),
                GlucosePoint("09:00", 164f),
                GlucosePoint("09:20", 188f, markerType = LogEntryType.ALERT),
                GlucosePoint("09:45", 136f),
                GlucosePoint("Now", currentMgDl.toFloat())
            )
            TimeWindow.H6 -> listOf(
                GlucosePoint("04:00", 94f),
                GlucosePoint("05:30", 102f),
                GlucosePoint("07:00", 108f, markerType = LogEntryType.INSULIN),
                GlucosePoint("08:50", 119f, markerType = LogEntryType.MEAL),
                GlucosePoint("09:20", 188f, markerType = LogEntryType.ALERT),
                GlucosePoint("10:00", 129f),
                GlucosePoint("Now", currentMgDl.toFloat())
            )
            TimeWindow.H12 -> listOf(
                GlucosePoint("22:00", 133f, markerType = LogEntryType.PREDICTION),
                GlucosePoint("00:00", 118f),
                GlucosePoint("03:00", 96f),
                GlucosePoint("06:00", 104f),
                GlucosePoint("08:50", 119f, markerType = LogEntryType.MEAL),
                GlucosePoint("09:20", 188f, markerType = LogEntryType.ALERT),
                GlucosePoint("Now", currentMgDl.toFloat())
            )
            TimeWindow.H24 -> listOf(
                GlucosePoint("10:00", 115f),
                GlucosePoint("13:00", 142f, markerType = LogEntryType.INSULIN),
                GlucosePoint("19:30", 128f, markerType = LogEntryType.MEAL),
                GlucosePoint("22:15", 133f, markerType = LogEntryType.PREDICTION),
                GlucosePoint("04:00", 95f),
                GlucosePoint("08:50", 119f, markerType = LogEntryType.MEAL),
                GlucosePoint("09:20", 188f, markerType = LogEntryType.ALERT),
                GlucosePoint("Now", currentMgDl.toFloat())
            )
        }

        val mid30 = (currentMgDl + (predicted60 - currentMgDl) * 0.55f)
        val forecast = listOf(
            GlucosePoint(
                label = "+30m",
                mgDl = mid30,
                isForecast = true,
                upperBound = (mid30 + 16f).coerceAtMost(320f),
                lowerBound = (mid30 - 14f).coerceAtLeast(55f)
            ),
            GlucosePoint(
                label = "+60m",
                mgDl = predicted60.toFloat(),
                isForecast = true,
                upperBound = (predicted60 + 26f).coerceAtMost(330f),
                lowerBound = (predicted60 - 22f).coerceAtLeast(55f)
            )
        )
        return baseHistory + forecast
    }
}
