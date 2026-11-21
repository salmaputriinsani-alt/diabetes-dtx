package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.AppLanguage
import com.example.data.model.CgmSnapshotSummary
import com.example.data.model.MealAnalysisResult
import com.example.data.model.PatientProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AurelixAiRepository"

class AurelixAiRepository {

    private val mealSchema = Schema.obj(
        properties = mapOf(
            "mealName" to Schema.string(description = "Detected Malaysian dish or meal name"),
            "carbsGrams" to Schema.integer(description = "Estimated total carbohydrates in grams"),
            "proteinGrams" to Schema.integer(description = "Estimated protein in grams"),
            "fatGrams" to Schema.integer(description = "Estimated fat in grams"),
            "fiberGrams" to Schema.integer(description = "Estimated dietary fiber in grams"),
            "glycemicLoad" to Schema.enumeration(
                values = listOf("Low", "Medium", "High"),
                description = "Glycemic load category"
            ),
            "predictedRiseMgDl" to Schema.integer(description = "Predicted 60-minute blood glucose rise in mg/dL"),
            "malaysianHabitTip" to Schema.string(description = "Practical culturally authentic Malaysian swap or post-meal habit tip in the requested language")
        )
    )

    private val mealVisionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = mealSchema
                temperature = 0.2f
            }
        )
    }

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.5f
            }
        )
    }

    suspend fun analyzeMeal(
        bitmap: Bitmap?,
        presetDishName: String?,
        language: AppLanguage,
        currentGlucoseMgDl: Int
    ): Result<MealAnalysisResult> {
        return try {
            val langInstruction = when (language) {
                AppLanguage.MS -> "Respond in Bahasa Melayu for mealName and malaysianHabitTip."
                AppLanguage.ZH -> "Respond in Simplified Chinese for mealName and malaysianHabitTip."
                AppLanguage.EN -> "Respond in English for mealName and malaysianHabitTip."
            }
            val promptText = buildString {
                append("You are AURELIX AI, a Malaysian metabolic nutritionist for Early Type 2 Diabetes. ")
                append("Analyze this meal for a patient whose current CGM glucose is $currentGlucoseMgDl mg/dL. ")
                if (!presetDishName.isNullOrBlank()) {
                    append("The user selected or described the Malaysian meal: '$presetDishName'. ")
                }
                append("Estimate realistic Carbs (g), Protein (g), Fat (g), Fiber (g), Glycemic Load (Low/Medium/High), predicted 60-min glucose rise (mg/dL), and 1 practical Malaysian food swap or 15-minute post-meal walk tip. ")
                append(langInstruction)
            }

            val scaledBitmap = bitmap?.let { scaleBitmapSafely(it, 1024) }
            val response = withTimeout(60_000L) {
                mealVisionModel.generateContent(
                    content {
                        if (scaledBitmap != null) {
                            image(scaledBitmap)
                        }
                        text(promptText)
                    }
                )
            }

            val jsonString = response.text
                ?: throw IllegalStateException("Gemini returned an empty response.")
            val json = JSONObject(jsonString)
            Result.success(
                MealAnalysisResult(
                    mealName = json.optString("mealName", presetDishName ?: "Malaysian Meal"),
                    carbsGrams = json.optInt("carbsGrams", 45),
                    proteinGrams = json.optInt("proteinGrams", 15),
                    fatGrams = json.optInt("fatGrams", 14),
                    fiberGrams = json.optInt("fiberGrams", 4),
                    glycemicLoad = json.optString("glycemicLoad", "Medium"),
                    predictedRiseMgDl = json.optInt("predictedRiseMgDl", 28),
                    malaysianHabitTip = json.optString(
                        "malaysianHabitTip",
                        "Pair with extra leafy greens and take a 15-minute walk after eating."
                    )
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Meal analysis timed out", e)
            Result.failure(Exception("Meal analysis timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Meal analysis failed", e)
            Result.failure(e)
        }
    }

    fun streamCompanionReply(
        userPrompt: String,
        profile: PatientProfile,
        cgm: CgmSnapshotSummary,
        recentMealTitles: List<String>,
        language: AppLanguage
    ): Flow<String> = flow {
        val langName = when (language) {
            AppLanguage.MS -> "Bahasa Melayu"
            AppLanguage.ZH -> "Simplified Chinese"
            AppLanguage.EN -> "English"
        }

        val systemContext = buildString {
            appendLine("You are AURELIX AI, a warm, clinically grounded Metabolic Digital Twin companion for Early Type 2 Diabetes in Malaysia (collaborating with KPJ Endocrinology).")
            appendLine("ALWAYS reply in $langName. Keep replies concise (3-5 crisp sentences), empathetic, and grounded in the patient's live telemetry below:")
            appendLine("- Patient: ${profile.patientName}, Age ${profile.age}, Track: ${profile.diagnosisTrack}")
            appendLine("- Live CGM: ${cgm.currentMgDl} mg/dL (${String.format("%.1f", cgm.currentMmolL)} mmol/L), 60-min AI prediction: ${cgm.predicted60MinMgDl} mg/dL")
            appendLine("- Time-in-Range: ${cgm.timeInRangePercent}% (Target 70-180 mg/dL)")
            appendLine("- Sleep last night: ${profile.sleepHours}h, Post-meal walk: ${profile.walkMinutes} mins, Simulated Carbs: ${profile.simMealCarbs}g, Insulin/Med units: ${profile.simInsulinUnits}")
            appendLine("- SHAP Top Drivers: ${cgm.shapDrivers.joinToString { "${it.label} (${if (it.deltaMgDl >= 0) "+" else ""}${it.deltaMgDl} mg/dL)" }}")
            appendLine("- Recent Meals: ${recentMealTitles.ifEmpty { listOf("Nasi Lemak + Teh O Kosong") }.joinToString()}")
            appendLine("- Organ Protection: Neuro-Metabolic Stability ${profile.neuroScore}/100, Kidney eGFR ${profile.renalEgfr}, uACR ${profile.renalUacr}")
            appendLine("Never invent prescriptions; remind the patient that dr. Sarah Wijaya (KPJ Endocrinology) confirms insulin adjustments.")
        }

        val fullPrompt = "$systemContext\n\nPatient Question: $userPrompt"
        val accumulated = StringBuilder()

        withTimeout(60_000L) {
            chatModel.generateContentStream(fullPrompt).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulated.append(text)
                    emit(accumulated.toString())
                }
            }
        }
    }

    private fun scaleBitmapSafely(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val (targetW, targetH) = if (width >= height) {
            maxDimension to (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            (maxDimension * ratio).toInt().coerceAtLeast(1) to maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}
