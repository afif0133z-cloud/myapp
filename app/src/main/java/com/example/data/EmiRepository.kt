package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class EmiRepository(
    private val db: FirebaseFirestore?,
    private val prefs: DevicePreferences
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    val isBackendConfigured: Boolean
        get() = db != null

    fun observeAppConfig(): Flow<AppConfig> {
        val firestore = db ?: return flow { emit(prefs.loadCachedConfig()) }
        val path = "config/app"
        return firestore.collection("config").document("app")
            .snapshots()
            .map { snapshot ->
                val raw = snapshot.toObject(
                    AppConfig::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                val cached = prefs.loadCachedConfig()
                val merged = if (raw != null) {
                    raw.copy(
                        logoUrl = raw.logoUrl.ifBlank { DEFAULT_LOGO_URL },
                        lockBackgroundUrl = raw.lockBackgroundUrl.ifBlank { DEFAULT_LOCK_BG_URL }
                    )
                } else {
                    cached
                }
                prefs.saveCachedConfig(merged)
                merged
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                emit(prefs.loadCachedConfig())
            }
    }

    fun observeDeviceState(deviceId: String): Flow<DeviceState?> {
        val firestore = db ?: return emptyFlow()
        if (deviceId.isBlank()) return emptyFlow()

        val path = "devices/$deviceId"
        return firestore.collection("devices").document(deviceId)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) {
                    null
                } else {
                    val state = snapshot.toObject(
                        DeviceState::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    if (state != null) {
                        prefs.saveCachedLockState(state.isLocked, state.amountDue)
                    }
                    state
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                throw error
            }
    }

    fun observeTasks(deviceId: String): Flow<List<DeviceTask>> {
        val firestore = db ?: return flow { emit(emptyList()) }
        if (deviceId.isBlank()) return flow { emit(emptyList()) }

        val deviceTasksFlow = firestore.collection("tasks")
            .whereEqualTo("deviceId", deviceId)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    DeviceTask::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

        val globalTasksFlow = firestore.collection("tasks")
            .whereEqualTo("deviceId", "all")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    DeviceTask::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

        val completionsFlow = firestore.collection("devices")
            .document(deviceId)
            .collection("taskCompletions")
            .snapshots()
            .map { snap -> snap.documents.map { it.id }.toSet() }
            .catch { emit(emptySet()) }

        return combine(deviceTasksFlow, globalTasksFlow, completionsFlow) { own, global, completedIds ->
            val mergedGlobal = global.map { task ->
                if (completedIds.contains(task.id)) task.copy(status = "done") else task
            }
            (own + mergedGlobal)
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            handleFirestoreError(firestoreError ?: error, OperationType.LIST, "tasks")
            throw error
        }
    }

    fun observeOrder(orderId: String): Flow<PaymentOrder?> {
        val firestore = db ?: return emptyFlow()
        if (orderId.isBlank()) return emptyFlow()

        val path = "orders/$orderId"
        return firestore.collection("orders").document(orderId)
            .snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(
                    PaymentOrder::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun markTaskDone(deviceId: String, task: DeviceTask): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firebase not configured"))

        return if (task.deviceId == "all") {
            val path = "devices/$deviceId/taskCompletions/${task.id}"
            try {
                firestore.collection("devices")
                    .document(deviceId)
                    .collection("taskCompletions")
                    .document(task.id)
                    .set(
                        mapOf(
                            "taskId" to task.id,
                            "deviceId" to deviceId,
                            "completedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    .await()
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, path)
                Result.failure(e)
            }
        } else {
            val path = "tasks/${task.id}"
            try {
                firestore.collection("tasks")
                    .document(task.id)
                    .update(mapOf("status" to "done"))
                    .await()
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, path)
                Result.failure(e)
            }
        }
    }

    /**
     * Registers the device in Firestore devices/{deviceId} using its unique Android ID without user login.
     * Preserves any existing "locked" status or amountDue if already present on the server.
     */
    suspend fun registerDevice(deviceId: String, fcmToken: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firebase not configured"))
        val path = "devices/$deviceId"
        return try {
            val docRef = firestore.collection("devices").document(deviceId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "deviceId" to deviceId,
                    "status" to "active",
                    "amountDue" to 0.0,
                    "fcmToken" to fcmToken,
                    "lastSeen" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "deviceId" to deviceId,
                    "lastSeen" to FieldValue.serverTimestamp()
                )
                if (fcmToken.isNotBlank()) {
                    updates["fcmToken"] = fcmToken
                }
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createPaymentOrder(deviceId: String): Result<CreateOrderResult> {
        return try {
            val payload = JSONObject().apply {
                put("deviceId", deviceId)
            }
            val json = postToCloudFunction("createPaymentOrder", payload)
            Result.success(
                CreateOrderResult(
                    orderId = json.getString("orderId"),
                    amount = json.optDouble("amount", 0.0),
                    gatewayUrl = json.getString("gatewayUrl")
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "createPaymentOrder Cloud Function failed", e)
            Result.failure(e)
        }
    }

    suspend fun verifyUnlockPin(deviceId: String, orderId: String?, pin: String): Result<Boolean> {
        return try {
            val payload = JSONObject().apply {
                put("deviceId", deviceId)
                if (!orderId.isNullOrBlank()) put("orderId", orderId)
                put("pin", pin)
            }
            val json = postToCloudFunction("verifyUnlockPin", payload)
            Result.success(json.optBoolean("verified", false))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "verifyUnlockPin Cloud Function failed", e)
            Result.failure(e)
        }
    }

    private suspend fun postToCloudFunction(functionName: String, body: JSONObject): JSONObject {
        val baseUrl = BuildConfig.CLOUD_FUNCTIONS_BASE_URL.trimEnd('/')
        val request = Request.Builder()
            .url("$baseUrl/$functionName")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        return withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
                if (!response.isSuccessful) {
                    val errMsg = json.optString("error").ifBlank { "HTTP ${response.code}" }
                    throw IllegalStateException(errMsg)
                }
                json
            }
        }
    }

    companion object {
        private const val TAG = "EmiRepository"
    }
}
