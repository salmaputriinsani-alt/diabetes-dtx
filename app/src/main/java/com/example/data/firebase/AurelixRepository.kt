package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.DiagnosisTrack
import com.example.data.model.DoctorMessage
import com.example.data.model.LogEntryType
import com.example.data.model.MetabolicLog
import com.example.data.model.OrganScreening
import com.example.data.model.PatientProfile
import com.example.data.model.SenderRole
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AurelixRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeProfile(userId: String): Flow<PatientProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            PatientProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeLogs(userId: String): Flow<List<MetabolicLog>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$uid/logs"
        emitAll(
            db.collection("users").document(uid).collection("logs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MetabolicLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeDoctorMessages(userId: String): Flow<List<DoctorMessage>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$uid/doctorMessages"
        emitAll(
            db.collection("users").document(uid).collection("doctorMessages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DoctorMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeLatestScreening(userId: String): Flow<OrganScreening?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$uid/screenings"
        emitAll(
            db.collection("users").document(uid).collection("screenings")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        OrganScreening::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).firstOrNull()
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun seedInitialDataIfMissing(userId: String, googleDisplayName: String?): Result<Unit> {
        val userDocPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user seed." }
            val userRef = db.collection("users").document(uid)
            val existing = userRef.get().await()
            if (existing.exists()) {
                return Result.success(Unit)
            }

            val patientDisplay = "Encik Ahmad"
            val batch = db.batch()

            val profilePayload = mapOf(
                "userId" to uid,
                "patientName" to patientDisplay,
                "age" to 46L,
                "diagnosisTrack" to DiagnosisTrack.EARLY_T2D_REVERSAL.wireValue,
                "language" to AppLanguage.EN.code,
                "sensorDaysLeft" to 9L,
                "sensorBattery" to 82L,
                "targetLowMgDl" to 70L,
                "targetHighMgDl" to 180L,
                "sleepHours" to 4.5,
                "walkMinutes" to 15L,
                "simMealCarbs" to 45L,
                "simInsulinUnits" to 1.5,
                "neuroScore" to 82L,
                "renalEgfr" to 94L,
                "renalUacr" to 12.4,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.set(userRef, profilePayload)

            val seedLogs = listOf(
                mapOf(
                    "userId" to uid,
                    "entryType" to LogEntryType.ALERT.wireValue,
                    "title" to "High Glucose",
                    "timeLabel" to "09:20",
                    "dateGroup" to "Today",
                    "glucoseMgDl" to 188L,
                    "carbsGrams" to 0L,
                    "proteinGrams" to 0L,
                    "fatGrams" to 0L,
                    "fiberGrams" to 0L,
                    "glycemicLoad" to "High",
                    "insulinUnits" to 0.0,
                    "shapImpactSummary" to "Short Sleep (4.5h) +35 mg/dL & Nasi Lemak +30 mg/dL",
                    "habitTip" to "Take a brisk 15-min walk to drop post-meal spike by ~18 mg/dL.",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "userId" to uid,
                    "entryType" to LogEntryType.MEAL.wireValue,
                    "title" to "Nasi Lemak + Teh O Kosong",
                    "timeLabel" to "08:50",
                    "dateGroup" to "Today",
                    "glucoseMgDl" to 119L,
                    "carbsGrams" to 52L,
                    "proteinGrams" to 16L,
                    "fatGrams" to 18L,
                    "fiberGrams" to 4L,
                    "glycemicLoad" to "High",
                    "insulinUnits" to 0.0,
                    "shapImpactSummary" to "Meal Carbs +30 mg/dL · Sambal & Coconut Rice",
                    "habitTip" to "Ask for half-rice and extra cucumber/boiled egg to flatten peak.",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "userId" to uid,
                    "entryType" to LogEntryType.PREDICTION.wireValue,
                    "title" to "High Prediction",
                    "timeLabel" to "22:15",
                    "dateGroup" to "Yesterday",
                    "glucoseMgDl" to 133L,
                    "carbsGrams" to 0L,
                    "proteinGrams" to 0L,
                    "fatGrams" to 0L,
                    "fiberGrams" to 0L,
                    "glycemicLoad" to "Medium",
                    "insulinUnits" to 0.0,
                    "shapImpactSummary" to "Late supper curve projected +24 mg/dL over 60 mins",
                    "habitTip" to "Avoid late Teh Tarik; herbal tea keeps overnight TIR >85%.",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "userId" to uid,
                    "entryType" to LogEntryType.INSULIN.wireValue,
                    "title" to "Clinician-Adjusted Bolus",
                    "timeLabel" to "13:10",
                    "dateGroup" to "22 Nov",
                    "glucoseMgDl" to 142L,
                    "carbsGrams" to 0L,
                    "proteinGrams" to 0L,
                    "fatGrams" to 0L,
                    "fiberGrams" to 0L,
                    "glycemicLoad" to "Low",
                    "insulinUnits" to 1.0,
                    "shapImpactSummary" to "Confirmed by dr. Sarah Wijaya (-26 mg/dL correction)",
                    "habitTip" to "Paired with 15-min walk for gentle stabilization.",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            seedLogs.forEachIndexed { idx, payload ->
                val ref = userRef.collection("logs").document("seed_log_$idx")
                batch.set(ref, payload)
            }

            val seedMessages = listOf(
                mapOf(
                    "userId" to uid,
                    "senderRole" to SenderRole.PATIENT.wireValue,
                    "senderName" to patientDisplay,
                    "text" to "Good morning Dr Sarah, my post-Nasi Lemak reading hit 188 mg/dL after only 4.5h sleep. Should I take 1.5 units?",
                    "timeLabel" to "09:22",
                    "attachedGraphSummary" to "188 now | 1x meal | 1.5 unit estimate",
                    "attachedEstimateUnits" to 1.5,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "userId" to uid,
                    "senderRole" to SenderRole.DOCTOR.wireValue,
                    "senderName" to "dr. Sarah Wijaya",
                    "text" to "I reviewed your SHAP trend & 60-min cone. Sleep debt amplified your morning spike, so let's stay conservative: take 1 unit and walk 15 mins.",
                    "timeLabel" to "09:25",
                    "confirmedBadge" to "Confirmed by Doctor - 1 unit",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            seedMessages.forEachIndexed { idx, payload ->
                val ref = userRef.collection("doctorMessages").document("seed_msg_$idx")
                batch.set(ref, payload)
            }

            val screeningRef = userRef.collection("screenings").document("seed_screening_0")
            batch.set(
                screeningRef,
                mapOf(
                    "userId" to uid,
                    "footTingling" to false,
                    "hrvStressHigh" to true,
                    "postSpikeBrainFog" to false,
                    "neuroStabilityScore" to 82L,
                    "systolicBp" to 124L,
                    "diastolicBp" to 78L,
                    "hydrationGlasses" to 7L,
                    "egfr" to 94L,
                    "uacr" to 12.4,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDocPath)
            Result.failure(e)
        }
    }

    suspend fun updateProfileFields(
        userId: String,
        updates: Map<String, Any>
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Cross-user update rejected" }
            val payload = updates.toMutableMap().apply {
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("users").document(uid).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addMetabolicLog(userId: String, log: MetabolicLog): Result<Unit> {
        var docPath = "users/$userId/logs"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Cross-user log rejected" }
            val safeTitle = log.title.trim().take(120).ifBlank { "Meal Log" }
            val docRef = db.collection("users").document(uid).collection("logs").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "entryType" to log.entryType,
                "title" to safeTitle,
                "timeLabel" to log.timeLabel.take(32),
                "dateGroup" to log.dateGroup.take(32),
                "glucoseMgDl" to log.glucoseMgDl.coerceIn(20L, 600L),
                "carbsGrams" to log.carbsGrams.coerceIn(0L, 500L),
                "proteinGrams" to log.proteinGrams.coerceIn(0L, 300L),
                "fatGrams" to log.fatGrams.coerceIn(0L, 300L),
                "fiberGrams" to log.fiberGrams.coerceIn(0L, 150L),
                "glycemicLoad" to log.glycemicLoad.take(32),
                "insulinUnits" to log.insulinUnits.coerceIn(0.0, 100.0),
                "shapImpactSummary" to log.shapImpactSummary.take(240),
                "habitTip" to log.habitTip?.take(300),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun sendDoctorMessage(userId: String, message: DoctorMessage): Result<Unit> {
        var docPath = "users/$userId/doctorMessages"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Cross-user message rejected" }
            val trimmed = message.text.trim().take(1500)
            require(trimmed.isNotBlank()) { "Message text cannot be empty" }
            val docRef = db.collection("users").document(uid).collection("doctorMessages").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "senderRole" to message.senderRole,
                "senderName" to message.senderName.take(80),
                "text" to trimmed,
                "timeLabel" to message.timeLabel.take(32),
                "attachedGraphSummary" to message.attachedGraphSummary?.take(200),
                "attachedEstimateUnits" to message.attachedEstimateUnits?.coerceIn(0.0, 100.0),
                "confirmedBadge" to message.confirmedBadge?.take(120),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveScreening(userId: String, screening: OrganScreening): Result<Unit> {
        var docPath = "users/$userId/screenings"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Cross-user screening rejected" }
            val docRef = db.collection("users").document(uid).collection("screenings").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "footTingling" to screening.footTingling,
                "hrvStressHigh" to screening.hrvStressHigh,
                "postSpikeBrainFog" to screening.postSpikeBrainFog,
                "neuroStabilityScore" to screening.neuroStabilityScore.coerceIn(0L, 100L),
                "systolicBp" to screening.systolicBp.coerceIn(60L, 260L),
                "diastolicBp" to screening.diastolicBp.coerceIn(30L, 160L),
                "hydrationGlasses" to screening.hydrationGlasses.coerceIn(0L, 30L),
                "egfr" to screening.egfr.coerceIn(5L, 200L),
                "uacr" to screening.uacr.coerceIn(0.0, 5000.0),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            // Sync top-level profile neuroScore and renal markers
            db.collection("users").document(uid).update(
                mapOf(
                    "neuroScore" to screening.neuroStabilityScore.coerceIn(0L, 100L),
                    "renalEgfr" to screening.egfr.coerceIn(5L, 200L),
                    "renalUacr" to screening.uacr.coerceIn(0.0, 5000.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }
}
